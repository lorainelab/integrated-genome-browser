
package org.bioviz.protannot.action;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.bioviz.protannot.GenomeView;
import static org.bioviz.protannot.ProtAnnotMain.BUNDLE;

/**
 *
 * @author hiralv
 */
public class ToggleHairlineAction extends AbstractAction{

	final private GenomeView gview;

	public ToggleHairlineAction(GenomeView gview){
		super(BUNDLE.getString("toggleHairline"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_H);
		this.putValue(SHORT_DESCRIPTION, BUNDLE.getString("toggleHairlineTip"));
		this.gview = gview;
		this.putValue(SELECTED_KEY, true);
	}

	public void actionPerformed(ActionEvent e) {
		gview.toggleHairline();
	}

}
