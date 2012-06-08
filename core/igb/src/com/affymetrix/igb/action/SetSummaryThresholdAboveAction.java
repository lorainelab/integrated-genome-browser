/*  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stub for the action that will set the summary threshold
 * just above the current threshold.
 */
public class SetSummaryThresholdAboveAction extends SetSummaryThresholdActionA {
	private static final long serialVersionUID = 1L;
	private static final SetSummaryThresholdAboveAction ACTION = new SetSummaryThresholdAboveAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static SetSummaryThresholdAboveAction getAction() {
		return ACTION;
	}

	protected SetSummaryThresholdAboveAction() {
		super("Set Threshold Above"/*IGBConstants.BUNDLE.getString("expandAction")*/,
		null, "22x22/actions/detail.png");
	}

	@Override
	protected int adjustThreshold(int threshold) {
		if (threshold == 100) { // can't go any higher
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "cannot set Summary Threshold above this level");
			return threshold;
		}
		else {
			return threshold + 1;
		}
	}
}
