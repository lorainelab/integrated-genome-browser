package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;

import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;

import com.affymetrix.igb.view.SeqMapView;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public abstract class ViewReadAlignmentAction extends GenericAction {
	private static final long serialVersionUID = 1l;

	private static final String RESTOREREAD = BUNDLE.getString("restoreAlignment");
	private static final String SHOWMISMATCH = BUNDLE.getString("showMismatch");

	private static final ViewReadAlignmentAction restoreRead = new ViewReadAlignmentAction() {
		private static final long serialVersionUID = 1L;
		@Override public String getText() { return RESTOREREAD; }
	};
	private static final ViewReadAlignmentAction showMismatch = new ViewReadAlignmentAction() {
		private static final long serialVersionUID = 1L;
		@Override public String getText() { return SHOWMISMATCH; }
	};

	private final List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();

	SeqMapView gViewer = IGB.getSingleton().getMapView();

	private ViewReadAlignmentAction(){
		super();
	}

	public static ViewReadAlignmentAction getReadRestoreAction(List<SeqSymmetry> syms){
		return getAction(restoreRead, syms);
	}

	public static ViewReadAlignmentAction getMismatchAligmentAction(List<SeqSymmetry> syms){
		return getAction(showMismatch, syms);
	}

	private static ViewReadAlignmentAction getAction(ViewReadAlignmentAction action, List<SeqSymmetry> syms){
		action.syms.clear();
		action.syms.addAll(syms);
		return action;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		boolean set = true;
		
		if(RESTOREREAD.equals(e.getActionCommand())){
			set = false;
		}

		for (SeqSymmetry sym : syms) {
			if (sym instanceof SymWithProps) {
				SymWithProps swp = (SymWithProps) sym;
				if (swp.getProperty(BAM.SHOWMASK) != null) {
					swp.setProperty(BAM.SHOWMASK, set);
				}
			}
		}

		gViewer.getSeqMap().repaint();
	}

}
