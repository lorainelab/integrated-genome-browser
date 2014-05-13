package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.bioviews.GlyphDragger;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.igb.shared.TierGlyph;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author hiralv
 */
public class TierGlyphDragger extends GlyphDragger{
	AffyLabelledTierMap labelledMap;
	TierGlyph tg;
	protected Point2D.Double t_prev_point = new Point2D.Double(0, 0);
	protected Point2D.Double t_cur_point = new Point2D.Double(0, 0);
	
	public TierGlyphDragger(AffyLabelledTierMap labelledMap) {
		super(labelledMap.getLabelMap());
		this.labelledMap = labelledMap;
	}
	
	public void startDrag(TierLabelGlyph tlg, NeoMouseEvent nevt) {
		startDrag(tlg, nevt, null);
		this.tg = tlg.getReferenceTier();
	}
	
	@Override
	public void mouseDragged(MouseEvent evt) {
		t_prev_point = prev_point;
		t_cur_point = cur_point;
		super.mouseDragged(evt);
		if (force_within_parent) {
			Rectangle2D.Double pbox = dragged_glyph.getParent().getCoordBox();
			Rectangle2D.Double cbox = dragged_glyph.getCoordBox();
			if (cur_point.y < pbox.y) {
				tg.moveAbsolute(cbox.x, pbox.y);
			}
			else {
				tg.moveRelative(t_cur_point.x - t_prev_point.x, t_cur_point.y - t_prev_point.y);
			}
		}
		else {
			tg.moveRelative(t_cur_point.x - t_prev_point.x, t_cur_point.y - t_prev_point.y);
		}
		t_prev_point = prev_point;
		t_cur_point = cur_point;
		labelledMap.updateWidget();
	}
	
	@Override
	public void mouseReleased(MouseEvent evt) {
		super.mouseReleased(evt);
		labelledMap = null;
		tg = null;
	}
}
