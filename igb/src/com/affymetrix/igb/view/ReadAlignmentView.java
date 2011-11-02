package com.affymetrix.igb.view;

import java.util.List;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;

import com.affymetrix.igb.action.ViewReadAlignmentAction;

/**
 *
 * @author hiralv
 */
public class ReadAlignmentView implements ContextualPopupListener{


	public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_items, SeqSymmetry primary_sym) {
		if (!selected_items.isEmpty() && selected_items.get(0) instanceof SymWithProps) {
			SymWithProps swp = (SymWithProps) selected_items.get(0);
			Object prop = swp.getProperty(BAM.SHOWMASK);
			if (prop != null) {
				if (selected_items.size() == 1) {
					if (Boolean.parseBoolean(prop.toString())) {
						JMenuItem menu = new JMenuItem(ViewReadAlignmentAction.getReadRestoreAction(selected_items));
						BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
						SeqSpan span = selected_items.get(0).getSpan(seq);
						menu.setEnabled(seq.isComplete(span.getMin(), span.getMax()));
						popup.add(menu);
					} else {
						popup.add(new JMenuItem(ViewReadAlignmentAction.getMismatchAligmentAction(selected_items)));
					}
				} else {
					popup.add(new JMenuItem(ViewReadAlignmentAction.getReadRestoreAction(selected_items)));
					popup.add(new JMenuItem(ViewReadAlignmentAction.getMismatchAligmentAction(selected_items)));
				}
			}
		}
	}

}
