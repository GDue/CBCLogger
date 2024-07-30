# The usecase

I wanted to have a way to do excessive logging enabled temporarily on a web service encapsulated in a docker container.
Due to the architecture only administrators with a lot of rights have the ability to change the log config files.
And once the log level is raised for one or more loggers, this level is applied to ALL subsequent calls until the log level is lowered again.
Excessive logging in the service comes with a severe performance penalty, so having it enabled for a longer time is not advisable.
Users do not have a chance to access log output, so whatever you do, troubleshooting the service is something that requires somebody 
with administrative powers and most probably even more abilities. 
Even more probable, this means ME :-( So, I do not like this idea.

A better solution would be to enable logging on a per call basis by a user and giving the user access to exactly the log output for exactly this call. 
Adding this enabler to a call's parameters and adding the log output to the result would enable everybody (who knows how to write a json request) 
to trace the execution of a call.

# The problem(s)
* You want different log levels per call.
* You want output separated on a per call basis.
* Requests are worked upon in parallel. Changing the log level of loggers will influence all calls to this logger, no matter 
from where it is called. That means, that a changed log level might change the output of all calls which work in parallel.
* You do not want to rewrite all the log statements

Currently, the service uses slf4j in all libraries and an underlying log4j2 system. Therefore a solution was chosen that works for log4j2.

# The solution
The service at hand uses threads to service individual calls.
This way the task to enable logging for a call is more or less identical to enable logging for individual threads.

Log4j2 offers the capability to add information to individual threads. We use this mechanism to control logging for each thread.

This is done by conveying log information via the Thread Context.
Prepare the thread context at the beginning of a request. But, also make sure to wipe this information at it's end if you re-use threads for requests!!!!!


## Changing the logger's level on a per call basis
Log4j2 allows to add filter to different phases of a logging event.
We add such a filter to the phase where the log level is evaluated.
If the standard log level says "do not log", the filter can superseed this when the call's data says "do log in this thread".

## Making the log output available on a per thread basis
We add a special appender to the loggers which writes to an output stream which is related to the thread.
It's up to the app to provide a stream which is useful for it.

## Not logging to the standard appenders
If a logger's level is low by standard configuration but is enabled on a per call basis, the log event is forwarded to the appender stage.
So we need to prevent logging to the standard appenders in this case.
This is done by adding another filter to all appenders which are only used by default logging.

# Implemention details, the developers view point
## preparing the stage
In this example we modify the root logger by adding two appenders. The console appender should show standard behavior, the ThreadLoggingAppender should work on a per call basis.
Note the filters added to the system and to the console appender.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="warn">
<filters>
    <LevelFilter onMatch="ACCEPT" onMismatch="NEUTRAL"/>
</filters>
<Appenders> 
    <Console name="console" target="SYSTEM_OUT">
        <PatternLayout 
            pattern="%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n" />
        <LevelFilter onMatch="ACCEPT" onMismatch="NEUTRAL"/>
    </Console>
    <ThreadAppender name="threadAppender">
        <PatternLayout 
            pattern="%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n" />
    </ThreadAppender>
</Appenders>
<Loggers> 
<Root level="info" additivity="false">
    <AppenderRef ref= "console" /> 
    <AppenderRef ref= "threadAppender" /> 
</Root>
</Loggers>
</Configuration>
```

The system level filter will control the log level while the filter at the appender will suppress unwanted logging to the console.

## Setting up a thread for logging
To do logging for a thread we need to communicate our wishes to the log4j2 system. This is done by creating a ThreadLoggingData-object and fill it according to your wishes.

```java
		// Create a thread logging object
		tld=new ThreadLoggingData();
		
		// We accept the logging output in a byte array output stream.
		// Note, that this stream is local this thread!
		this.bosThread=new ByteArrayOutputStream();
		tld.outStream=this.bosThread;
		
		// Set the log level of the root logger.
		// You may set other loggers here
		// Loggers not mentioned here inherit their levels 
		// the same way log4j2 inherits levels.
		tld.setLoggerLevel("", "Info");
		
		
		// We cannot put the ThreadLoggingData object directy into to the Thread Context of log4j2
		// as we only can put string data there.
		// Thus we register a unique key which is to be put to the thread context.
		tldKey=ThreadLoggingData.registerThreadLoggingData(tld);
		
		// Note: This is THE enabler for thread specific logging.
		// If this entry in the Thread context is missing, nothing will be logged.
		ThreadContext.put(ThreadLoggingData.ContextKey, tldKey);
		
		// Some last words:
		// Remember to remove the key from the thread context when your call is done.
		// This is especially important if your thread will be reused (think "thread pool here!").
		// Also remember to unregister the ThreadLoggingData to avoid memory leaks.
		// Consider doing this in the finally block of a try/catch construct to ensure, 
		// unregistering is performed, no matter what.
```	

## More processing
Somehow you need to convey your logging needs through your call. But it's up to you to do this as this lib has no knowledge of your requests.

And you need to wrap and send the log output to your client. Again, this is up to you.
This lib's responsibility ends with filling the stream you did provide.
