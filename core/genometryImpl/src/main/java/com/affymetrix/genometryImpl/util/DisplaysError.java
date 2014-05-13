package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.event.GenericAction;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author hiralv
 */
public interface DisplaysError {
	public void showError(final String title, final String message, final List<GenericAction> actions, Level level); 
}
