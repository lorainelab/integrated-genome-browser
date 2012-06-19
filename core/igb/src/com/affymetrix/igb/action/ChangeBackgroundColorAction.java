package com.affymetrix.igb.action;

import javax.swing.JColorChooser;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

import com.affymetrix.igb.IGBConstants;
import java.awt.Color;

public class ChangeBackgroundColorAction extends ChangeColorActionA {
	private static final long serialVersionUID = 1L;
	private static final ChangeBackgroundColorAction ACTION = new ChangeBackgroundColorAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	public static ChangeBackgroundColorAction getAction() {
		return ACTION;
	}

	public ChangeBackgroundColorAction() {
		super(IGBConstants.BUNDLE.getString("changeBGColorAction"), "16x16/actions/applications-graphics-5.png", "22x22/actions/applications-graphics-5.png");
	}

	@Override
	protected void setChooserColor(JColorChooser chooser, ITrackStyleExtended style) {
		chooser.setColor(style.getBackground());
	}

	@Override
	protected void setStyleColor(Color color, ITrackStyleExtended style) {
		style.setBackground(color);
	}
}
