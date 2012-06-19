package com.affymetrix.igb.action;

import com.affymetrix.igb.shared.ChangeColorActionA;
import javax.swing.JColorChooser;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

import com.affymetrix.igb.IGBConstants;
import java.awt.Color;

public class ChangeForwardColorAction extends ChangeColorActionA {
	private static final long serialVersionUID = 1L;
	private static final ChangeForwardColorAction ACTION = new ChangeForwardColorAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	public static ChangeForwardColorAction getAction() {
		return ACTION;
	}

	public ChangeForwardColorAction() {
		super(IGBConstants.BUNDLE.getString("changeForwardColorAction"), "16x16/actions/go-next.png", "22x22/actions/go-next.png");
	}

	@Override
	protected void setChooserColor(JColorChooser chooser, ITrackStyleExtended style) {
		chooser.setColor(style.getForwardColor());
	}

	@Override
	protected void setStyleColor(Color color, ITrackStyleExtended style) {
		style.setForwardColor(color);
	}
}
