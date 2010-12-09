package com.affymetrix.igb.view;

import java.util.List;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.symloader.BAM;

import com.affymetrix.igb.action.ViewReadAlignmentAction;

/**
 *
 * @author hiralv
 */
public class ReadAlignmentView implements ContextualPopupListener{

	//By default mask the residues.
	public static final boolean DEFAULT_SHOWMASK = true;

	public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_items, SeqSymmetry primary_sym) {
		if (!selected_items.isEmpty() && selected_items.get(0) instanceof SymWithProps) {
			SymWithProps swp = (SymWithProps) selected_items.get(0);
			Object prop = swp.getProperty(BAM.SHOWMASK);
			if (prop != null) {
				if (selected_items.size() == 1) {
					if (Boolean.parseBoolean(prop.toString())) {
						popup.add(new JMenuItem(ViewReadAlignmentAction.getShowAligmentAction(selected_items)));

					} else {
						popup.add(new JMenuItem(ViewReadAlignmentAction.getMismatchAligmentAction(selected_items)));
					}
				} else {
					popup.add(new JMenuItem(ViewReadAlignmentAction.getShowAligmentAction(selected_items)));
					popup.add(new JMenuItem(ViewReadAlignmentAction.getMismatchAligmentAction(selected_items)));
				}
			}
		}
	}

}
