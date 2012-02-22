package com.affymetrix.igb.tutorial;

import com.affymetrix.genometryImpl.event.GenericAction;
import java.awt.event.ActionEvent;

public class TutorialNextAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private final TutorialManager tutorialManager;

	public TutorialNextAction(TutorialManager tutorialManager) {
		super();
		this.tutorialManager = tutorialManager;
	}

	@Override
	public String getText() {
		return "next";
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		tutorialManager.next();
		super.actionPerformed(e);
	}

	@Override
	public String getIconPath() {
		return "images/right.png";
	}
}
