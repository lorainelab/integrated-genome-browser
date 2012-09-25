package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.IGB;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ClampViewAction.java 11335 2012-05-01 18:00:52Z anuj4159 $
 */
public class ClampViewAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ClampViewAction ACTION = new ClampViewAction();

	private ClampViewAction() {
		super(BUNDLE.getString("clampToView"),  null, "16x16/actions/Clamp to view.png", "22x22/actions/Clamp to view.png", KeyEvent.VK_V);
		this.putValue(SELECTED_KEY, false);
	}
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ClampViewAction getAction(){
		return ACTION;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		IGB.getSingleton().getMapView().toggleClamp();
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
