/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bioviz.protannot;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 *
 * @author hiralv
 */
public class ToggleHairlineAction extends AbstractAction{

	final private GenomeView gview;

	ToggleHairlineAction(GenomeView gview){
		super("Show Zoom Stripe");
		this.gview = gview;
		this.putValue(SELECTED_KEY, true);
	}

	public void actionPerformed(ActionEvent e) {
		gview.toggleHairline();
	}

}
