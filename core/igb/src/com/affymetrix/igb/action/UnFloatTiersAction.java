package com.affymetrix.igb.action;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.action.SeqMapViewActionA;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class UnFloatTiersAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	private static final UnFloatTiersAction ACTION = new UnFloatTiersAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static UnFloatTiersAction getAction() {
		return ACTION;
	}

	public UnFloatTiersAction() {
		super("Float", "16x16/actions/anchor.png", "22x22/actions/anchor.png");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean something_changed = false;
		List<ViewModeGlyph> selectedTiers = getSelectedFloatingTiers();
		for (ViewModeGlyph gl : selectedTiers) {
			ITrackStyleExtended style = gl.getAnnotStyle();
			boolean is_floating = style.getFloatTier();
			if (is_floating) {
				//GraphGlyphUtils.attachGraph(gl, gviewer);
				// figure out correct height
				Rectangle2D.Double tempbox = gl.getCoordBox();  // pixels, since in PixelFloaterGlyph 1:1 mapping of pixel:coord
				Rectangle pixbox = new Rectangle((int) tempbox.x, (int) tempbox.y, (int) tempbox.width, (int) tempbox.height);
				Rectangle2D.Double coordbox = new Rectangle2D.Double();
				getSeqMapView().getSeqMap().getView().transformToCoords(pixbox, coordbox);
				style.setY(coordbox.y); // currently y has no effect on attached graphs, but will someday
				style.setHeight(coordbox.height);
				style.setFloatTier(false);
				
				if(gl instanceof AbstractGraphGlyph){
					((AbstractGraphGlyph)gl).getGraphState().setShowLabel(false);
				}

				something_changed = true;
			}
		}
		if (something_changed) {
			updateViewer();
		}
	}

	private List<ViewModeGlyph> getSelectedFloatingTiers() {
		List<ViewModeGlyph> selectedTiers = new ArrayList<ViewModeGlyph>();
		for (GlyphI glyph : getSeqMapView().getPixelFloater().getChildren()) {
			if (glyph.isSelected()) {
				selectedTiers.add((ViewModeGlyph)glyph);
			}
		}
		return selectedTiers;
	}
	private void updateViewer() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				getSeqMapView().setAnnotatedSeq(GenometryModel.getGenometryModel().getSelectedSeq(), true, true);
			}
		});
	}
}
