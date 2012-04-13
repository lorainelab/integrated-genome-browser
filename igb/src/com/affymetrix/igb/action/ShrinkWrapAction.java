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
 * @version $Id$
 */
public class ShrinkWrapAction extends GenericAction {
	private static final long serialVersionUID = 1;
	private static final ShrinkWrapAction ACTION = new ShrinkWrapAction();

	private ShrinkWrapAction() {
		super(BUNDLE.getString("toggleShrinkWrapping"), KeyEvent.VK_S);
		this.putValue(SELECTED_KEY, IGB.getSingleton().getMapView().getShrinkWrap());
	}

	public static ShrinkWrapAction getAction() {
		return ACTION;
	}

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
