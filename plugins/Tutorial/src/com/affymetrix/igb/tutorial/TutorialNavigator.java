package com.affymetrix.igb.tutorial;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class TutorialNavigator extends JPanel {
	private static final long serialVersionUID = 1L;
	private final JLabel instructions;
	private final GenericAction nextAction;

	public TutorialNavigator(GenericAction backAction, GenericAction nextAction, GenericAction cancelAction) {
		super();
		this.nextAction = nextAction;
		setLayout(new BorderLayout());
		JRPButton backButton = new JRPButton("TutorialNavigator_back", backAction);
		add(backButton, BorderLayout.WEST);
		JRPButton cancelButton = new JRPButton("TutorialNavigator_cancel", cancelAction);
		add(cancelButton, BorderLayout.WEST);
		instructions = new JLabel("", JLabel.CENTER);
		instructions.setHorizontalTextPosition(JLabel.CENTER);
		add(instructions, BorderLayout.CENTER);
		JRPButton nextButton = new JRPButton("TutorialNavigator_next", nextAction);
		add(nextButton, BorderLayout.EAST);
	}

	public JLabel getInstructions() {
		return instructions;
	}

	public GenericAction getNextAction() {
		return nextAction;
	}
}
