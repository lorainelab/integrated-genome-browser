package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.menuitem.MenuUtil;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.Collections;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class AutoScrollAction extends AbstractAction implements SeqSelectionListener {
	private static final long serialVersionUID = 1l;

	public AutoScrollAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("autoScroll")),
				MenuUtil.getIcon("toolbarButtonGraphics/media/Movie16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_A);

		GenometryModel model = GenometryModel.getGenometryModel();
		model.addSeqSelectionListener(this);
		this.seqSelectionChanged(new SeqSelectionEvent(this, Collections.<BioSeq>singletonList(model.getSelectedSeq())));
	}

	public void actionPerformed(ActionEvent ae) {
		IGB.getSingleton().getMapView().toggleAutoScroll();
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		this.setEnabled(evt.getSelectedSeq() != null);
	}

}
