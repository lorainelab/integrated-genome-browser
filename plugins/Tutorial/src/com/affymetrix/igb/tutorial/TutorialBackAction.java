package com.affymetrix.igb.tutorial;

import com.affymetrix.genometryImpl.event.GenericAction;
import java.awt.event.ActionEvent;

public class TutorialBackAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private final TutorialManager tutorialManager;

	public TutorialBackAction(TutorialManager tutorialManager) {
		super();
		this.tutorialManager = tutorialManager;
	}

	@Override
	public String getText() {
		return "back";
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		tutorialManager.back();
	}

	@Override
	public String getIconPath() {
		return "images/left.png";
	}
}
