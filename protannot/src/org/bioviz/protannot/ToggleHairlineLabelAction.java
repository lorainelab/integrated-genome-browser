package org.bioviz.protannot;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 *
 * @author hiralv
 */
public class ToggleHairlineLabelAction extends AbstractAction{

	final private GenomeView gview;

	ToggleHairlineLabelAction(GenomeView gview){
		super("Show Zoom Stripe Label");
		this.gview = gview;
		this.putValue(SELECTED_KEY, true);
	}

	public void actionPerformed(ActionEvent e) {
		gview.toggleHairlineLabel();
	}
}