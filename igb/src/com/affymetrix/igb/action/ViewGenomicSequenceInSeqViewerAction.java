/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.action;

import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.view.SequenceViewer;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author auser
 */
public class ViewGenomicSequenceInSeqViewerAction  extends AbstractAction{
	private static final long serialVersionUID = 1l;

	public ViewGenomicSequenceInSeqViewerAction(JComponent comp) {
		super(BUNDLE.getString("ViewGenomicSequenceInSeqViewer"));
//		KeyStroke ks = MenuUtil.addAccelerator(comp, this, BUNDLE.getString("ViewGenomicSequenceInSeqViewer"));
//		if (ks != null) {
//			this.putValue(MNEMONIC_KEY, ks.getKeyCode());
//		}
	}

	public void actionPerformed(ActionEvent e) {
		try {
			SequenceViewer sv = new SequenceViewer();
			sv.startSequenceViewer();
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem occured in copying sequences to sequence viewer", ex);
		}
	}

}
