package at.duennebeil.threadLogging;

import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

@Plugin(name = "LevelFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public final class LevelFilter extends AbstractFilter {
 
    private final Level level;
 
    public LevelFilter(Level level, Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        this.level = level;
    }

 
    /**
     * This method is called when the framework tries to decide whether to process a log event.
     */
    @Override
    public Result filter(
            Logger logger, Level level, Marker marker, Object msg, Throwable t) {

    	return filter(logger, level);
    }

    

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
    	return filter(logger, level);
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
    	return filter(logger, level);
    }



    private Result filter(Logger logger, Level levelOfEvent) {

    	Map<String, String> context=ThreadContext.getContext();
    	String key=context.get(ThreadLoggingData.ContextKey);
    	if (key==null)
    		return Result.NEUTRAL;
    	
    	ThreadLoggingData tld=ThreadLoggingData.getThreadLoggingData(key);
    	if (tld==null)
    		return Result.NEUTRAL;
    	
    	String loggerName=logger.getName();

    	Level levelThreadLogger=tld.getLoggerLevel(loggerName);
    	if (levelThreadLogger==null)
    		return Result.NEUTRAL;
    	
    	
    	if (levelThreadLogger.isLessSpecificThan(levelOfEvent))
    		return Result.ACCEPT;
    	
    	// No information from ThreadLoggingData? Accept the level from global config
    	return Result.NEUTRAL;

    	
    }


//////////////////////////////////////////////////////////////////////////////////////////////////    
    
    /**
     * This method is called by the appenders when to decide whether they should append.
     */
    @Override
    public Result filter(LogEvent event) {
    	ReadOnlyStringMap threadContextData=event.getContextData();
    	String tldKey=threadContextData.getValue(ThreadLoggingData.ContextKey);
    	if (tldKey==null)
    		return Result.NEUTRAL;

    	
    	ThreadLoggingData tld=ThreadLoggingData.getThreadLoggingData(tldKey);
    	if (tld==null)
        	return Result.NEUTRAL;

    	String loggerName=event.getLoggerName();

    	Logger logger=(org.apache.logging.log4j.core.Logger)LogManager.getLogger(loggerName);
    	
    	Level levelOfLogger=logger.getLevel();
    	Level levelOfEvent=event.getLevel();
    	if (!levelOfLogger.isLessSpecificThan(levelOfEvent))
    		return Result.DENY;
    	
    	// No information from ThreadLoggingData? Accept the level from global config
    	return Result.NEUTRAL;

    }
 
    @Override
    public String toString() {
        return level.toString();
    }
 
    /**
     * Create a LevelFilter.
     * @param loggerLevel The log Level.
     * @param match The action to take on a match.
     * @param mismatch The action to take on a mismatch.
     * @return The created ThresholdFilter.
     */
    @PluginFactory
    public static LevelFilter createFilter(@PluginAttribute(value = "level", defaultString = "ERROR") Level level,
                                               @PluginAttribute(value = "onMatch", defaultString = "NEUTRAL") Result onMatch,
                                               @PluginAttribute(value = "onMismatch", defaultString = "DENY") Result onMismatch) {
        return new LevelFilter(level, onMatch, onMismatch);
    }
}