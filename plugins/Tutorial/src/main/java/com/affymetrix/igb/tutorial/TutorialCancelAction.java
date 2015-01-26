package com.affymetrix.igb.tutorial;

import com.affymetrix.genometry.event.GenericAction;
import java.awt.event.ActionEvent;

public class TutorialCancelAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private final TutorialManager tutorialManager;

    public TutorialCancelAction(TutorialManager tutorialManager) {
        super("cancel", "16x16/actions/stop.png", null);
        this.tutorialManager = tutorialManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tutorialManager.stop();
    }
}
