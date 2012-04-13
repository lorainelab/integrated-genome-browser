package com.affymetrix.igb.tutorial;

import com.affymetrix.genometryImpl.event.GenericAction;
import java.awt.event.ActionEvent;

public class TutorialCancelAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private final TutorialManager tutorialManager;

	public TutorialCancelAction(TutorialManager tutorialManager) {
		super("cancel", "images/stop.png");
		this.tutorialManager = tutorialManager;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		tutorialManager.stop();
	}
}
