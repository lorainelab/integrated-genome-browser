package com.affymetrix.genometry.util;

import com.affymetrix.genometry.event.GenericAction;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author hiralv
 */
public interface DisplaysError {
	public void showError(final String title, final String message, final List<GenericAction> actions, Level level); 
}
