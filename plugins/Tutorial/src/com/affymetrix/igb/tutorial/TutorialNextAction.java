package com.affymetrix.igb.tutorial;

import com.affymetrix.genometryImpl.event.GenericAction;
import java.awt.event.ActionEvent;

public class TutorialNextAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private final TutorialManager tutorialManager;

	public TutorialNextAction(TutorialManager tutorialManager) {
		super("Next", "images/right.png");
		this.tutorialManager = tutorialManager;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		tutorialManager.next();
		super.actionPerformed(e);
	}
}
