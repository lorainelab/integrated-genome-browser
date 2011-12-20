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
		super();
		this.putValue(SELECTED_KEY, IGB.getSingleton().getMapView().getShrinkWrap());
	}

	public static ShrinkWrapAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		SeqMapView map_view = IGB.getSingleton().getMapView();
		map_view.setShrinkWrap(!map_view.getShrinkWrap());
	}

	@Override
	public String getText() {
		return BUNDLE.getString("toggleShrinkWrapping");
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_S;
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
