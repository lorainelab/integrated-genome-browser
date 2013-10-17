package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.SeqMapView.MapMode;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hiralv
 */
final public class MouseShortCut implements MouseListener{
	final SeqMapViewPopup popup;
	final SeqMapView smv;
	
	public MouseShortCut(SeqMapViewPopup popup){
		this.popup = popup;
		this.smv = popup.getSeqMapView();
	}

	public void mouseClicked(MouseEvent e) {

	}

	public void mouseEntered(MouseEvent e) {

	}

	public void mouseExited(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {
		
	}

	public void mouseReleased(MouseEvent e) {
		
		if (e.getSource() instanceof AffyLabelledTierMap && 
				e.getID() == MouseEvent.MOUSE_RELEASED) {
			NeoMouseEvent nme = (NeoMouseEvent)e;
			TierGlyph tier = smv.getSeqMap().getHitGlyph(nme.getCoordX(),nme.getCoordY());
			if(tier != null){
				TierLabelManager.setTierCollapsed(tier, !tier.getAnnotStyle().getCollapsed());
				smv.getSeqMap().setTierStyles();
				popup.repack(false, false);
				return;
			}
		}
		processDoubleClick(e);
		
	}

	private void processDoubleClick(MouseEvent e) {

		if(e.getClickCount() != 2)
			return;

		//AffyLabelledTierMap
		if (e.getSource() instanceof AffyLabelledTierMap) {
			if (e.isAltDown()) {
				//if alt is pressed.
				
				return;
			}

			if (e.isShiftDown()) {
				//Center hairline if shift is pressed.
				smv.centerAtHairline();
			}

			List<GlyphI> glyphs = smv.getSeqMap().getSelected();
			
			if(smv.getMapMode() == MapMode.MapScrollMode && e instanceof NeoMouseEvent){
				NeoMouseEvent nevt = (NeoMouseEvent)e;
				glyphs = new ArrayList<GlyphI>();
				Point2D.Double zoom_point = new Point2D.Double(nevt.getCoordX(), nevt.getCoordY());
				
				GlyphI topgl = null;
				if (!nevt.getItems().isEmpty()) {
					topgl = nevt.getItems().get(nevt.getItems().size() - 1);
					topgl = smv.getSeqMap().zoomCorrectedGlyphChoice(topgl, zoom_point);
					glyphs.add(topgl);
				}
				
			}

			if(glyphs == null || glyphs.isEmpty() || glyphs.get(0) instanceof GraphGlyph)
				return;
			
			//Zoom to glyphs.
			smv.zoomToGlyphs(glyphs);
			if(smv.getAutoLoadAction() != null) {
				smv.getAutoLoadAction().loadData();
			}
		}
		
	}

}
