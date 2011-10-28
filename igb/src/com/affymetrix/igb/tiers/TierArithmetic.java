package com.affymetrix.igb.tiers;

import java.util.*;
import javax.swing.*;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.operator.annotation.AnnotationOperator;
import com.affymetrix.igb.action.TierAnnotationOperationAction;
import com.affymetrix.igb.view.SeqMapView;

/**
 *  A PopupListener that adds the ability to create "union", "intersection", etc.,
 *  tiers based on selected annotation tiers.  Is not used on graph tiers.
 */
public final class TierArithmetic implements TierLabelManager.PopupListener {
	private final SeqMapView gviewer;
	private final TierLabelManager handler;

	public TierArithmetic(TierLabelManager handler, SeqMapView gviewer) {
		this.handler = handler;
		this.gviewer = gviewer;
	}

	public void popupNotify(JPopupMenu popup, TierLabelManager handler) {
		if (handler != this.handler) {
			throw new RuntimeException("");
		}
		List<TierLabelGlyph> labels = handler.getSelectedTierLabels();
		boolean all_are_annotations = areAllAnnotations(labels);
		if (labels.size() == 0 || !all_are_annotations) {
			return;
		}
		JMenu combineMenu = new JMenu("Track Operations...");
		for (AnnotationOperator annotationOperator : ExtensionPointHandler.getExtensionPoint(AnnotationOperator.class).getExtensionPointImpls()) {
			String name = annotationOperator.getName();
			String title = name.substring(0, 1).toUpperCase() + name.substring(1);
			JMenuItem operatorMI = new JMenuItem(title);
			operatorMI.addActionListener(
				new TierAnnotationOperationAction(gviewer, annotationOperator)
			);
			combineMenu.add(operatorMI);
		}
		int num_selected = labels.size();

		combineMenu.setEnabled(all_are_annotations && num_selected > 0);
		for (int i = 0; i < combineMenu.getItemCount(); i++) {
			JMenuItem jMenuItem = combineMenu.getItem(i);
			if (jMenuItem.getActionListeners().length == 1 && jMenuItem.getActionListeners()[0] instanceof TierAnnotationOperationAction) {
				TierAnnotationOperationAction action = (TierAnnotationOperationAction)jMenuItem.getActionListeners()[0];
				jMenuItem.setEnabled(all_are_annotations && action.isEnabled());
			}
		}

		popup.add(combineMenu);
	}

	private boolean areAllAnnotations(List<TierLabelGlyph> labels) {
		for (TierLabelGlyph tlg : labels) {
			if (tlg.getReferenceTier().getAnnotStyle().isGraphTier()) {
				return false;
			}
		}
		return true;
	}
}
