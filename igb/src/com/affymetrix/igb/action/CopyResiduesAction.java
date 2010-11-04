package com.affymetrix.igb.action;

import com.affymetrix.igb.view.SequenceViewer;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.igb.IGB;
import com.affymetrix.genometryImpl.util.MenuUtil;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class CopyResiduesAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public CopyResiduesAction() {
		super(BUNDLE.getString("copySelectedResiduesToClipboard"),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Copy16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_C);
	}

	public void actionPerformed(ActionEvent e) {
		SeqSpan[] seqSpans = IGB.getSingleton().getMapView().copySelectedResidues(true);
		SequenceViewer neoSeqDemo = new SequenceViewer();
		neoSeqDemo.tempChange(seqSpans);
	}

}
