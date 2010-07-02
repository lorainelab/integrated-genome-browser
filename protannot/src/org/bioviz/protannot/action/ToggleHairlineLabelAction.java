package org.bioviz.protannot.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import org.bioviz.protannot.GenomeView;
import static org.bioviz.protannot.ProtAnnotMain.BUNDLE;

/**
 *
 * @author hiralv
 */
public class ToggleHairlineLabelAction extends AbstractAction{

	final private GenomeView gview;

	public ToggleHairlineLabelAction(GenomeView gview){
		super(BUNDLE.getString("toggleHairlineLabel"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_L);
		this.putValue(SHORT_DESCRIPTION, BUNDLE.getString("toggleHairlineLabelTip"));
		this.gview = gview;
		this.putValue(SELECTED_KEY, true);
	}

	public void actionPerformed(ActionEvent e) {
		gview.toggleHairlineLabel();
	}
}