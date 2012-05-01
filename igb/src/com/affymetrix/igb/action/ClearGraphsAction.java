package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.IGB;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ClearGraphsAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ClearGraphsAction ACTION = new ClearGraphsAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ClearGraphsAction getAction() {
		return ACTION;
	}

	private ClearGraphsAction() {
		super(MessageFormat.format(BUNDLE.getString("menuItemHasDialog"), BUNDLE.getString("clearGraphs")), KeyEvent.VK_L);
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (IGB.confirmPanel("Really clear graphs?")) {
			IGB.getSingleton().getMapView().clearGraphs();
		}
	}
}
