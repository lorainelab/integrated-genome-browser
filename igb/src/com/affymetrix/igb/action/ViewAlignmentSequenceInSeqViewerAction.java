package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SymWithResidues;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.view.AlignmentSequenceViewer;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author auser
 */
public class ViewAlignmentSequenceInSeqViewerAction extends GenericAction implements SymSelectionListener{
	private static final long serialVersionUID = 1l;
	private static final ViewAlignmentSequenceInSeqViewerAction ACTION = new ViewAlignmentSequenceInSeqViewerAction();

	public static ViewAlignmentSequenceInSeqViewerAction getAction() {
		return ACTION;
	}

	private ViewAlignmentSequenceInSeqViewerAction() {
		super();
		this.setEnabled(false);
		GenometryModel.getGenometryModel().addSymSelectionListener(this);
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

	public void symSelectionChanged(SymSelectionEvent evt) {
		if (!evt.getSelectedSyms().isEmpty() && !(evt.getSelectedSyms().get(0) instanceof GraphSym)) {
			if(evt.getSelectedSyms().get(0) instanceof SymWithResidues){
				setEnabled(true);
			}else{
				setEnabled(false);
			}
		} else{
			setEnabled(false);
		}
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Zoom16.gif";
	}
}
