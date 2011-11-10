package com.affymetrix.igb.tutorial;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericAction;

public class TutorialCancelAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private final TutorialManager tutorialManager;

	public TutorialCancelAction(TutorialManager tutorialManager) {
		super();
		this.tutorialManager = tutorialManager;
	}

	@Override
	public String getText() {
		return "cancel";
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		tutorialManager.stop();
	}

	@Override
	public String getIconPath() {
		return "images/stop.png";
	}
}
