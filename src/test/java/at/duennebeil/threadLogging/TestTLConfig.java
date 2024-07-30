package at.duennebeil.threadLogging;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestTLConfig {

	
	@AfterEach
	void cleanupThreadLogging() {
	/*
	 * The tests in here leave the logging system in a state which can be understood as "unknown"
	 * In the sense of "any new test has no idea of the state the logging system is at the beginning".
	 * Thus we clean up here.
	 */
//		ThreadLoggingData.unregisterThreadLoggingData(tldKey);
//		tldKey=null;
		
		ThreadContext.clearAll();
		
		LogManager.shutdown();
		
	}

	
	
	@Test
	/**
	 * Test a situation where both appenders, the static and the thread controlled one fire.
	 */
	void testConfig() throws IOException {
		
		String strConfig=
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
				"<Configuration status=\"warn\">" +
				"<filters>"+
				"    <LevelFilter onMatch=\"ACCEPT\" onMismatch=\"NEUTRAL\"/>"+
				"</filters>"+
		        "<Appenders>" + 
		        "    <Console name=\"console\" target=\"SYSTEM_OUT\">" +
		        "        <PatternLayout " +
		        "            pattern=\"%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n\" />" +
				"        <LevelFilter onMatch=\"ACCEPT\" onMismatch=\"NEUTRAL\"/>"+
		        "    </Console>" +
				"    <ThreadAppender name=\"threadAppender\">" +
		        "        <PatternLayout " +
		        "            pattern=\"%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n\" />" +
				"    </ThreadAppender>" +
		        "</Appenders>" +
		        "<Loggers>" + 
		        "<Root level=\"info\" additivity=\"false\">" +
		        "    <AppenderRef ref= \"console\" /> " +
		        "    <AppenderRef ref= \"threadAppender\" /> " +
		        "</Root>"+
		        "</Loggers>" +
		        "</Configuration>";
		
		ByteArrayInputStream is=new ByteArrayInputStream(strConfig.getBytes());
		
		ConfigurationSource source=new ConfigurationSource(is);
		Configurator.initialize(null, source);

		// If we come here without any exception, we have succeeded
	}

}
