package logger;

import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.filter.AbstractFilterable;

import at.duennebeil.threadLogging.LevelFilter;
import at.duennebeil.threadLogging.ThreadLoggingAppender;
import at.duennebeil.threadLogging.ThreadLoggingData;


public class LoggerMain {

	public static void main(String[] args) {

//		PluginManager.addPackage("at.duennebeil.threadLogging");

		
		ConfigurationBuilder<BuiltConfiguration> builder
		 = ConfigurationBuilderFactory.newConfigurationBuilder();

		builder.setStatusLevel(Level.TRACE);
		
		AppenderComponentBuilder consoleBuilder 
		  = builder.newAppender("stdout", "Console"); 

		LayoutComponentBuilder standard 
		  = builder.newLayout("PatternLayout");
		standard.addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable");

		FilterComponentBuilder levelFilterBuilder=builder.newFilter("LevelFilter", Result.ACCEPT, Result.ACCEPT);
		
		
		
		consoleBuilder.add(standard);
		
		builder.add(consoleBuilder);

		RootLoggerComponentBuilder rootLogger 
		  = builder.newRootLogger(Level.ERROR);
		rootLogger.add(builder.newAppenderRef("stdout"));
		rootLogger.add(levelFilterBuilder);
		
//		FilterComponentBuilder level = builder.newFilter(
//				  "LevelFilter", 
//				  Filter.Result.ACCEPT,
//				  Filter.Result.DENY);  
//				level.addAttribute("marker", "FLOW");

		builder.add(rootLogger);
		
		BuiltConfiguration builtConfig = builder.build();
		LoggerContext loggerCtx=Configurator.initialize(builtConfig);
		
		
		Logger root=LogManager.getRootLogger();
		Logger moreLevel=LogManager.getLogger("level1.level2.level3");
		

		LevelFilter lf=new LevelFilter(Level.ALL, Result.ACCEPT, Result.DENY);

		Configuration config = loggerCtx.getConfiguration();
		config.addFilter(lf);
		
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		AbstractFilterable appStdOut=(AbstractFilterable)config.getAppender("stdout");
//		appStdOut.addFilter(lf);
		
		ConsoleAppender conApp=(ConsoleAppender)appStdOut;
		Layout layout=conApp.getLayout();
		
		loggerConfig.addFilter(lf);
		
		ThreadLoggingAppender tlApp=new ThreadLoggingAppender("TLApp", null, layout, false);
		tlApp.start();
		loggerConfig.addAppender(tlApp, Level.ALL, null);
		
		loggerCtx.updateLoggers(config);
		
		Map<String, Object> map=(Map<String, Object>)((Object)ThreadContext.getContext());

		ThreadLoggingData tld=new ThreadLoggingData();
		
		tld.outStream=System.err;
		tld.setLoggerLevel("", "Debug");
		
		
		
		
		String tldKey=ThreadLoggingData.registerThreadLoggingData(tld);
		
		
		
		ThreadContext.put(ThreadLoggingData.ContextKey, tldKey);
		
		root.error("Das ist error");
		moreLevel.error("Das ist level 3");
		

		ThreadLoggingData.unregisterThreadLoggingData(tldKey);

	}

}
