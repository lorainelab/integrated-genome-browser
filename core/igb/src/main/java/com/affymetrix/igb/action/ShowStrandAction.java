package com.affymetrix.igb.action;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.IGBConstants;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;

/**
 * Toggle between showing both strands in a single track or in separate tracks.
 * @deprecated Use {@link SeqMapToggleAction}.
 */
@Deprecated
public class ShowStrandAction extends ShowStrandActionA implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final ShowStrandAction ACTION = new ShowStrandAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ShowStrandAction getAction() {
		return ACTION;
	}

	protected ShowStrandAction() {
		super(IGBConstants.BUNDLE.getString("showTwoTiersAction"), "22x22/actions/strandseparate.png", null);
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
		String iconPath = separateStrands ? "22x22/actions/strandseparate.png" : "22x22/actions/strandstogether.png";
		ImageIcon icon = CommonUtils.getInstance().getIcon(iconPath);
		if (icon == null) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "icon {0} returned null", iconPath);
		}
		else {
			putValue(Action.LARGE_ICON_KEY, icon);
		}
	}
}
