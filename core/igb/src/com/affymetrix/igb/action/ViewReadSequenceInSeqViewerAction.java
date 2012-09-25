package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SymWithResidues;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.view.ReadSequenceViewer;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.util.logging.Level;

/**
 *
 * @author auser
 */
public class ViewReadSequenceInSeqViewerAction extends GenericAction implements SymSelectionListener{
	private static final long serialVersionUID = 1l;
	private static final ViewReadSequenceInSeqViewerAction ACTION = new ViewReadSequenceInSeqViewerAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ViewReadSequenceInSeqViewerAction getAction() {
		return ACTION;
	}

	private ViewReadSequenceInSeqViewerAction() {
		super(BUNDLE.getString("ViewReadSequenceInSeqViewer"), null, "16x16/actions/Genome Viewer reads.png", "22x22/actions/Genome Viewer reads.png", KeyEvent.VK_UNDEFINED, null, true);
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
			ReadSequenceViewer sv = new ReadSequenceViewer();
			sv.startSequenceViewer();
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem occured in copying sequences to sequence viewer", ex, Level.WARNING);
		}
	}

	public void symSelectionChanged(SymSelectionEvent evt) {
		if (!evt.getSelectedGraphSyms().isEmpty() && !(evt.getSelectedGraphSyms().get(0) instanceof GraphSym)) {
			if(evt.getSelectedGraphSyms().get(0) instanceof SymWithResidues){
				setEnabled(true);
			}else{
				setEnabled(false);
			}
		} else{
			setEnabled(false);
		}
	}
}
