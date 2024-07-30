package at.duennebeil.threadLogging;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestThreadLogging {

	ThreadLoggingData tld;				// Test specific setup of thread specific logging
	String tldKey;						// The key to get a grip to our tld;
	
	
	ByteArrayAppender baa;				// This is our access to the log output done statically.
	ThreadLoggingAppender tlApp;		// Thread specific logging goes here
	ByteArrayOutputStream bosThread;	// Thread specific output goes here.
	
	@BeforeEach
	void setupLoggingFramework() {
		

		ConfigurationBuilder<BuiltConfiguration> builder
		 = ConfigurationBuilderFactory.newConfigurationBuilder();


//		builder.setStatusLevel(Level.TRACE);

		// Add an appender which is statically bound to the logger.
		// Note, that this is a placeholder which will be deleted later.
		AppenderComponentBuilder consoleBuilder 
		  = builder.newAppender("stdout", "Console");
		
		
		// We add a layout which only holds the level and the message to make things testable.
		LayoutComponentBuilder standard = builder.newLayout("PatternLayout");
//		standard.addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable");
		standard.addAttribute("pattern", "%-5level: %msg");

		consoleBuilder.add(standard);
		
		builder.add(consoleBuilder);

		RootLoggerComponentBuilder rootLogger 
		  = builder.newRootLogger(Level.INFO);
		rootLogger.add(builder.newAppenderRef("stdout"));
		
		builder.add(rootLogger);
		
		BuiltConfiguration config=builder.build();
		
		LoggerContext loggerCtx=(org.apache.logging.log4j.core.LoggerContext)Configurator.initialize(config);

		// At this point we have a config which is absolutely conservative.
		// One logger, one console appender, that's it.
		// Now we start setting up the situation we need for automatic unit testing.


		// Remove the stdout appender (remember, it's a placeholder).
		// Get it's layout and create a new appender with this layout and add this appender to the root logger.
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		Appender consoleAppender=config.getAppender("stdout");
		loggerConfig.removeAppender("stdout");

		Layout<? extends Serializable> layout=consoleAppender.getLayout();

		baa=new ByteArrayAppender("ByteArrayAppender", null, layout, false);
		baa.start();
		loggerConfig.addAppender(baa, Level.ALL, null);
		
		
		// At this point the setup of appenders is complete and -- beside the bytearrayappender-- is quite standard.
		// Now we add the special filters and appenders which implement thread specific logging.
		
		LevelFilter lf=new LevelFilter(Level.ALL, Result.ACCEPT, Result.DENY);
		config.addFilter(lf);	// This filters logevents

		baa.addFilter(lf);		// This filters log events that should only go to thread logging
		
//		loggerConfig.addFilter(lf);
		
		tlApp=new ThreadLoggingAppender("TLApp", null, layout, false);
		tlApp.start();
		loggerConfig.addAppender(tlApp, Level.ALL, null);
		
		loggerCtx.updateLoggers(config);
		
		// Now register a thread logging object
		tld=new ThreadLoggingData();
		
		this.bosThread=new ByteArrayOutputStream();
		tld.outStream=this.bosThread;
		
		
		// Remember, the tld setup is not complete. We do the rest in the specific tests.
		tldKey=ThreadLoggingData.registerThreadLoggingData(tld);
		
		ThreadContext.put(ThreadLoggingData.ContextKey, tldKey);

	}
	
	
	@AfterEach
	void cleanupThreadLogging() {
		ThreadLoggingData.unregisterThreadLoggingData(tldKey);
		tldKey=null;
		
		ThreadContext.clearAll();
		
		LogManager.shutdown();
		
	}
	
	
	
	@Test
	/**
	 * Test a situation where both appenders, the static and the thread controlled one fire.
	 */
	void testBoth() {

		// Default level for root is INFO, so we log more severe
		Logger root=LogManager.getRootLogger();
		Logger moreLevel=LogManager.getLogger("level1.level2.level3");

		tld.setLoggerLevel("", "Info");
		
		
		root.fatal("This is fatal");	// Level for root is INFO, so fatal should print

		String staticOutput=baa.getLogOutput();
		String threadOutput=new String(bosThread.toByteArray());
		
		String expectedResult="FATAL: This is fatal";
		
		assertEquals(expectedResult, staticOutput);
		assertEquals(expectedResult, threadOutput);
		
	}

	
	@Test
	/**
	 * Test a situation where only the static appender fires.
	 */
	void testOnlyStatic() {

		// Default level for root is INFO, so we log on info here
		Logger root=LogManager.getRootLogger();
		Logger moreLevel=LogManager.getLogger("level1.level2.level3");

		tld.setLoggerLevel("", "Error");
		
		
		root.info("This is info");	// Level for root is INFO, so info should print

		String staticOutput=baa.getLogOutput();
		String threadOutput=new String(bosThread.toByteArray());
		
		String expectedResult="INFO : This is info";
		
		assertEquals(expectedResult, staticOutput);
		assertEquals("", threadOutput);

	}

	
	@Test
	/**
	 * Test a situation where only the thread controlled appender fires.
	 */
	void testOnlyThread() {
		
		// Default level for root is INFO, so we log on debug here
		Logger root=LogManager.getRootLogger();
		Logger moreLevel=LogManager.getLogger("level1.level2.level3");

		tld.setLoggerLevel("", "Debug");
		
		
		root.debug("This is debug");	// Level for root is INFO, so info should not print

		String staticOutput=baa.getLogOutput();
		String threadOutput=new String(bosThread.toByteArray());
		
		String expectedResult="DEBUG: This is debug";
		
		assertEquals(expectedResult, threadOutput);
		assertEquals("", staticOutput);

	}


	@Test
	/**
	 * Test a situation where no appender fires.
	 */
	void testNone() {

		// Default level for root is INFO, so we log on debug here
		Logger root=LogManager.getRootLogger();
		Logger moreLevel=LogManager.getLogger("level1.level2.level3");

		tld.setLoggerLevel("", "INFO");
		
		
		root.debug("This is debug");	// Level for root is INFO, so info should not print

		String staticOutput=baa.getLogOutput();
		String threadOutput=new String(bosThread.toByteArray());
		
		String expectedResult="";
		
		assertEquals(expectedResult, staticOutput);
		assertEquals("", threadOutput);
	}

	private void printAll(Logger l) {
		l.trace("This is trace");
		l.debug("This is debug");
		l.info ("This is info");
		l.warn ("This is warn");
		l.error("This is error");
		l.fatal("This is fatal");
	}
	
	/**
	 * Test whether log levels are filtered correctly.
	 */
	@Test
	void testLoglevel() {

		org.apache.logging.log4j.core.Logger root=(org.apache.logging.log4j.core.Logger)LogManager.getRootLogger();
		Logger moreLevel=LogManager.getLogger("level1.level2.level3");

		// TRACE
		root.setLevel(Level.TRACE);
		tld.setLoggerLevel("", "TRACE");

		printAll(root);
		
		String staticOutput=baa.getLogOutput();
		String threadOutput=new String(bosThread.toByteArray());
		
		String expectedResult="TRACE: This is traceDEBUG: This is debugINFO : This is infoWARN : This is warnERROR: This is errorFATAL: This is fatal";
		
		assertEquals(expectedResult, staticOutput);
		assertEquals(expectedResult, threadOutput);
		
		baa.bos.reset();
		bosThread.reset();
		
		// DEBUG
		root.setLevel(Level.DEBUG);
		tld.setLoggerLevel("", "DEBUG");

		printAll(root);
		
		staticOutput=baa.getLogOutput();
		threadOutput=new String(bosThread.toByteArray());
		
		expectedResult="DEBUG: This is debugINFO : This is infoWARN : This is warnERROR: This is errorFATAL: This is fatal";
		
		assertEquals(expectedResult, staticOutput);
		assertEquals(expectedResult, threadOutput);
		
		baa.bos.reset();
		bosThread.reset();
		
		
		// INFO
		root.setLevel(Level.INFO);
		tld.setLoggerLevel("", "INFO");

		printAll(root);
		
		staticOutput=baa.getLogOutput();
		threadOutput=new String(bosThread.toByteArray());
		
		expectedResult="INFO : This is infoWARN : This is warnERROR: This is errorFATAL: This is fatal";
		
		assertEquals(expectedResult, staticOutput);
		assertEquals(expectedResult, threadOutput);
		
		baa.bos.reset();
		bosThread.reset();
		
		
		// WARN
		root.setLevel(Level.WARN);
		tld.setLoggerLevel("", "WARN");

		printAll(root);
		
		staticOutput=baa.getLogOutput();
		threadOutput=new String(bosThread.toByteArray());
		
		expectedResult="WARN : This is warnERROR: This is errorFATAL: This is fatal";
		
		assertEquals(expectedResult, staticOutput);
		assertEquals(expectedResult, threadOutput);
		
		baa.bos.reset();
		bosThread.reset();
		
		
		// ERROR
		root.setLevel(Level.ERROR);
		tld.setLoggerLevel("", "ERROR");

		printAll(root);
		
		staticOutput=baa.getLogOutput();
		threadOutput=new String(bosThread.toByteArray());
		
		expectedResult="ERROR: This is errorFATAL: This is fatal";
		
		assertEquals(expectedResult, staticOutput);
		assertEquals(expectedResult, threadOutput);
		
		baa.bos.reset();
		bosThread.reset();
		
		
		// FATAL
		root.setLevel(Level.FATAL);
		tld.setLoggerLevel("", "FATAL");

		printAll(root);
		
		staticOutput=baa.getLogOutput();
		threadOutput=new String(bosThread.toByteArray());
		
		expectedResult="FATAL: This is fatal";
		
		assertEquals(expectedResult, staticOutput);
		assertEquals(expectedResult, threadOutput);
		
		baa.bos.reset();
		bosThread.reset();
		
		
	}

	/**
	 * Test whether multiple parameters are handled correctly.
	 */
	@Test
	void testParameters() {

		org.apache.logging.log4j.core.Logger root=(org.apache.logging.log4j.core.Logger)LogManager.getRootLogger();
		Logger moreLevel=LogManager.getLogger("level1.level2.level3");

//		root.setLevel(Level.TRACE);	// Defaults to INFO
		tld.setLoggerLevel("", "DEBUG");

		// 1 param
		root.debug("Debug with param: {}", "1");	// Note: We use strings here to avoid implicit calls to Integer.getValue(). 
													// They make debugging uncomfortable.
		
		String staticOutput=baa.getLogOutput();
		String threadOutput=new String(bosThread.toByteArray());
		
		String expectedResult="DEBUG: Debug with param: 1";
		
		assertEquals("", staticOutput);
		assertEquals(expectedResult, threadOutput);
		
		baa.bos.reset();
		bosThread.reset();
		
		// 2 params
		root.debug("Debug with param: {}, {}", "1", "2");
		
		staticOutput=baa.getLogOutput();
		threadOutput=new String(bosThread.toByteArray());
		
		expectedResult="DEBUG: Debug with param: 1, 2";
		
		assertEquals("", staticOutput);
		assertEquals(expectedResult, threadOutput);
		
		baa.bos.reset();
		bosThread.reset();
		
		
		// 3 params (Three and more parameters use a different api call to loggers. So we stop testing here).
		root.debug("Debug with param: {}, {}, {}", "1", "2", "3");
		
		staticOutput=baa.getLogOutput();
		threadOutput=new String(bosThread.toByteArray());
		
		expectedResult="DEBUG: Debug with param: 1, 2, 3";
		
		assertEquals("", staticOutput);
		assertEquals(expectedResult, threadOutput);
		
		baa.bos.reset();
		bosThread.reset();

		NullPointerException npe=new NullPointerException();
		
		// 0 params + Exception
		root.debug("Debug with exception", npe);
		
		staticOutput=baa.getLogOutput();
		threadOutput=new String(bosThread.toByteArray());
		
		expectedResult="DEBUG: Debug with exception";
		
		assertTrue(threadOutput.startsWith(expectedResult));
		
		baa.bos.reset();
		bosThread.reset();
		

		
		// ex + 1 param
		root.debug("Debug with exception and param: {}", "1", npe);
		
		staticOutput=baa.getLogOutput();
		threadOutput=new String(bosThread.toByteArray());
		
		expectedResult="DEBUG: Debug with exception and param: 1";
		
		assertTrue(threadOutput.startsWith(expectedResult));
		
		baa.bos.reset();
		bosThread.reset();
		
				
	}

	
}
