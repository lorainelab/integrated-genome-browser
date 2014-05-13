/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import java.awt.Color;
import java.awt.event.ActionEvent;

/**
 *
 * @author lorainelab
 */
public class ChangeTierLabelForegroundColorAction extends ChangeTierLabelColorActionA {

	protected static final java.awt.Color DEFAULT_COLOR = javax.swing.UIManager.getColor("Button.background");
	private static final long serialVersionUID = 1L;
	private static final ChangeTierLabelForegroundColorAction ACTION = new ChangeTierLabelForegroundColorAction();

	private ChangeTierLabelForegroundColorAction() {
		super(BUNDLE.getString("changeTierLabelForegroundColorAction"), null, null);
	}

	static {
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}

	public static ChangeTierLabelForegroundColorAction getAction() {
		return ACTION;
	}

	public Color getBackgroundColor() {
		return DEFAULT_COLOR;
	}

	public Color getForegroundColor() {
		return DEFAULT_COLOR.brighter();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		changeColor(true);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
