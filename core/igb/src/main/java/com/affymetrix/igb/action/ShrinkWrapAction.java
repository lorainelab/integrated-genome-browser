package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ShrinkWrapAction.java 11366 2012-05-02 14:57:55Z anuj4159 $
 */
public class ShrinkWrapAction extends GenericAction {
	private static final long serialVersionUID = 1;
//	private static final ShrinkWrapAction ACTION = new ShrinkWrapAction();

	private ShrinkWrapAction() {
		super(BUNDLE.getString("toggleShrinkWrapping"), null, "16x16/actions/blank_placeholder.png", null, KeyEvent.VK_S);
		this.putValue(SELECTED_KEY, IGB.getSingleton().getMapView().getShrinkWrap());
	}
	
//	static{
//		GenericActionHolder.getInstance().addGenericAction(ACTION);
//	}
//	
//	public static ShrinkWrapAction getAction() {
//		return ACTION;
//	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		SeqMapView map_view = IGB.getSingleton().getMapView();
		map_view.setShrinkWrap(!map_view.getShrinkWrap());
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
