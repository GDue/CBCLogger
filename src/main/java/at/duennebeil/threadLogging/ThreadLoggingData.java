package at.duennebeil.threadLogging;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Level;

/**
 * A container for all data that thread logging needs on a per thread basis.
 * Create an entity, fill it and register it.
 * Then put the String you obtain when registering into the thread context.
 * Remember to clean the map once the work is done (think try..finally).
 * Also unregister the ThreadLoggingData.
 * 
 * Motivation: Log4j2 does not allow to put objects other than strings in a threadd context.
 * But we need more structured data here. Thus we work through some sort of reference to lookup data.
 * @author Gerhard
 *
 */
public class ThreadLoggingData {
	
	/**
	 * The key, the filters of the package are looking for.
	 */
	public static final String ContextKey = "ThreadLoggingData";
	
	
	// TODO: HashMap is not thread save.
	private static Map<String, ThreadLoggingData> registry=new HashMap<String, ThreadLoggingData>();
	
	public static String registerThreadLoggingData(ThreadLoggingData tld) {
		String key=UUID.randomUUID().toString();
		registry.put(key, tld);
		return key;
	}

	public static ThreadLoggingData getThreadLoggingData(String key) {
		return registry.get(key);
	}
	
	public static ThreadLoggingData unregisterThreadLoggingData(String key) {
		return registry.remove(key);
	}
	
	
	// Per instant data starts here
	public OutputStream outStream;

	Map<String, Level> loggerLevel=new HashMap<String, Level>();
	
	public void setLoggerLevel(String logger, String strLevel) {
		Level level=Level.valueOf(strLevel);
		setLoggerLevel(logger, level);
	}

	private void setLoggerLevel(String logger, Level level) {
		loggerLevel.put(logger, level);
	}

	public Level getLoggerLevel(String loggerName) {
		
		while (true) {
			Level level=loggerLevel.get(loggerName);
			if (level!=null)
				return level;
			
			if (loggerName.isEmpty()) {	// we just checked the root logger in the lines above and obviously we were unsuccessful.
				return null;
			}
			
			int pos=loggerName.lastIndexOf(".");
			if (pos<0)	// No more dots? Try the root logger.
				loggerName="";
			else
				loggerName=loggerName.substring(0, pos);
		}
		
	}
	
}
