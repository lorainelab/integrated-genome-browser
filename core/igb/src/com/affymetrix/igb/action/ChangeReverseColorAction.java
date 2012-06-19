package com.affymetrix.igb.action;

import javax.swing.JColorChooser;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

import com.affymetrix.igb.IGBConstants;
import java.awt.Color;

public class ChangeReverseColorAction extends ChangeColorActionA {
	private static final long serialVersionUID = 1L;
	private static final ChangeReverseColorAction ACTION = new ChangeReverseColorAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	public static ChangeReverseColorAction getAction() {
		return ACTION;
	}

	public ChangeReverseColorAction() {
		super(IGBConstants.BUNDLE.getString("changeReverseColorAction"), "16x16/actions/go-previous.png", "22x22/actions/go-previous.png");
	}

	@Override
	protected void setChooserColor(JColorChooser chooser, ITrackStyleExtended style) {
		chooser.setColor(style.getReverseColor());
	}

	@Override
	protected void setStyleColor(Color color, ITrackStyleExtended style) {
		style.setReverseColor(color);
	}
}
