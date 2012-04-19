package com.affymetrix.igb.action;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.ImageIcon;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqMapView;

public class ShowStrandAction extends ShowStrandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static ShowStrandAction ACTION;

	public static ShowStrandAction getAction() {
		if (ACTION == null) {
			ACTION = new ShowStrandAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	protected ShowStrandAction(SeqMapView gviewer) {
		super(gviewer, IGBConstants.BUNDLE.getString("showTwoTiersAction"), null, "images/strand_separate.png");
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	protected void processChange(boolean hasSeparate, boolean hasMixed) {
		separateStrands = !hasSeparate || hasMixed;
		String text = separateStrands ? IGBConstants.BUNDLE.getString("showTwoTiersAction") : IGBConstants.BUNDLE.getString("showSingleTierAction") ;
		putValue(Action.NAME, text);
		putValue(SHORT_DESCRIPTION, text);
		String iconPath = separateStrands ? "images/strand_separate.png" : "images/strand_mixed.png";
		ImageIcon icon = CommonUtils.getInstance().getIcon(iconPath);
		if (icon == null) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "icon " + iconPath + " returned null");
		}
		else {
			putValue(Action.SMALL_ICON, icon);
		}
	}
}
