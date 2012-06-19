package com.affymetrix.igb.action;

import javax.swing.JColorChooser;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

import com.affymetrix.igb.IGBConstants;
import java.awt.Color;

public class ChangeLabelColorAction extends ChangeColorActionA {
	private static final long serialVersionUID = 1L;
	private static final ChangeLabelColorAction ACTION = new ChangeLabelColorAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	public static ChangeLabelColorAction getAction() {
		return ACTION;
	}

	public ChangeLabelColorAction() {
		super(IGBConstants.BUNDLE.getString("changeLabelColorAction"), "16x16/actions/format-text-bold.png", "22x22/actions/format-text-bold.png");
	}

	@Override
	protected void setChooserColor(JColorChooser chooser, ITrackStyleExtended style) {
		chooser.setColor(style.getLabelForeground());
	}

	@Override
	protected void setStyleColor(Color color, ITrackStyleExtended style) {
		style.setLabelForeground(color);
	}
}
