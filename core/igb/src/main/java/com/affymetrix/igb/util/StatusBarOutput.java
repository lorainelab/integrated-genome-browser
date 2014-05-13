
package com.affymetrix.igb.util;

import com.affymetrix.igb.Application;
import java.text.MessageFormat;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class StatusBarOutput extends Handler {
	private static StatusBarOutput singleton = new StatusBarOutput();
	
	public static void initStatusBarOutput(){
		Logger.getLogger(StatusBarOutput.class.getName()).log(Level.INFO, "Initializing statusbar output messages");
		
		Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		logger.addHandler(singleton);
		
		logger = Logger.getLogger("");
		logger.addHandler(singleton);
	}
	
	@Override
	public boolean isLoggable(LogRecord record) {
		if(record.getLevel() == Level.SEVERE) {
			return true;
		}
		return false;
	}
	
	@Override
	public void publish(LogRecord record) {
		if(isLoggable(record)){
			//System.out.println("Received log: "+ MessageFormat.format(record.getMessage(),record.getParameters()));
			if(record.getParameters() != null){
				Application.getSingleton().showError(null, MessageFormat.format(record.getMessage(),record.getParameters()), null, record.getLevel());
			}else{
				Application.getSingleton().showError(null, record.getMessage(), null, record.getLevel());
			}
		}
	}

	@Override
	public void flush() { }

	@Override
	public void close() throws SecurityException {}
	
}
