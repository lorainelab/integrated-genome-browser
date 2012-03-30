package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.view.SequenceViewer;
import java.awt.event.ActionEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.view.AlignmentSequenceViewer;

/**
 *
 * @author auser
 */
public class ViewAlignmentSequenceInSeqViewerAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ViewAlignmentSequenceInSeqViewerAction ACTION = new ViewAlignmentSequenceInSeqViewerAction();

	public static ViewAlignmentSequenceInSeqViewerAction getAction() {
		return ACTION;
	}

	private ViewAlignmentSequenceInSeqViewerAction() {
		super();
		this.setEnabled(false);
//		KeyStroke ks = MenuUtil.addAccelerator(comp, this, BUNDLE.getString("ViewGenomicSequenceInSeqViewer"));
//		if (ks != null) {
//			this.putValue(MNEMONIC_KEY, ks.getKeyCode());
//		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		try {
			AlignmentSequenceViewer sv = new AlignmentSequenceViewer();
			sv.startSequenceViewer();
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem occured in copying sequences to sequence viewer", ex);
		}
	}

	@Override
	public String getText() {
		return BUNDLE.getString("ViewAlignmentSequenceInSeqViewer");
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
