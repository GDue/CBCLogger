package at.duennebeil.threadLogging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;


/**
 * An appender which "prints" to a byte array.
 * Intended usage: unit testing.
 * Note: This class is tested just for the intended purpose but not for anything else.
 * @author Gerhard
 *
 */
public class ByteArrayAppender extends AbstractAppender {

	ByteArrayOutputStream bos=new ByteArrayOutputStream();
	
	public ByteArrayAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions, null);
	}

	public void append(LogEvent event) {

		Layout<? extends Serializable> layout=this.getLayout();
		byte[] byFormatted=layout.toByteArray(event);

		try {
			this.bos.write(byFormatted);
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
	}

	
	public String getLogOutput() {
		
		byte[] byTheOutput=bos.toByteArray();
		String strTheOutput=new String(byTheOutput);
		
		return strTheOutput;
	}

}
