package at.duennebeil.threadLogging;

import java.io.IOException;
import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

@Plugin(name = "ThreadAppender", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class ThreadLoggingAppender extends AbstractAppender {


	public ThreadLoggingAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions, null);
	}

	public void append(LogEvent event) {

    	ReadOnlyStringMap threadContextData=event.getContextData();
    	String tldKey=threadContextData.getValue(ThreadLoggingData.ContextKey);
    	if (tldKey==null)
    		return;

		ThreadLoggingData tld=ThreadLoggingData.getThreadLoggingData(tldKey);
		if (tld==null)
			return;
		
		if (tld.outStream==null)
			return;

		Level levelEvent=event.getLevel();
		
		String loggerName=event.getLoggerName();
		Level levelLogger=tld.getLoggerLevel(loggerName);
		
		if ( ! levelLogger.isLessSpecificThan(levelEvent))
			return;
		
		Layout<?> layout=this.getLayout();
		byte[] byFormatted=layout.toByteArray(event);

		try {
			tld.outStream.write(byFormatted);
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
	}

	
	
	/**
	 * Create a ThreadLoggingAppender.
	 */
	@PluginFactory
	public static ThreadLoggingAppender createAppender(@PluginAttribute("name") String name,
	                                          @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
	                                          @PluginElement("Layout") Layout<?> layout,
	                                          @PluginElement("Filters") Filter filter) {
	
	    if (name == null) {
	        LOGGER.error("No name provided for ThreadLoggingAppender");
	        return null;
	    }
	
	    if (layout == null) {
	        layout = PatternLayout.createDefaultLayout();
	    }
	    return new ThreadLoggingAppender(name, filter, layout, ignoreExceptions);
	}

}

