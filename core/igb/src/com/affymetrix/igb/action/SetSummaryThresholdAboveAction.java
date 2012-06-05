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
import java.awt.event.ActionEvent;

/**
 * Stub for the action that will set the summary threshold
 * just above the current threshold.
 */
public class SetSummaryThresholdAboveAction extends SeqMapViewActionA {
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
    public void actionPerformed(ActionEvent e) {
		javax.swing.JOptionPane.showMessageDialog(null,
				"Not implemented yet.",
				this.getClass().getSimpleName(),
				javax.swing.JOptionPane.INFORMATION_MESSAGE);
	}

}
