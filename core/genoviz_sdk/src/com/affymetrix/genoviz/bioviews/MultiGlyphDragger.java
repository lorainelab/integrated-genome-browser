package com.affymetrix.genoviz.bioviews;

import com.affymetrix.genoviz.event.NeoGlyphDragListener;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;

/**
 *
 * @author hiralv
 */
public class MultiGlyphDragger implements NeoConstants {
	protected GlyphI[] dragged_glyphs;
	protected GlyphDragger[] glyph_draggers;
	NeoAbstractWidget widget;

	//  public GlyphDragger(NeoAbstractWidget widg, GlyphI gl, NeoMouseEvent nevt) {
	public MultiGlyphDragger(NeoAbstractWidget widg, GlyphI[] gl) {
		this.widget = widg;
		dragged_glyphs = gl;
		glyph_draggers = new GlyphDragger[gl.length];
		for(int i=0; i<gl.length; i++){
			glyph_draggers[i] = new GlyphDragger(widget);
		}
	}

	public void startDrag(NeoMouseEvent nevt) {
		startDrag(nevt, null);
	}

	public void startDrag(NeoMouseEvent nevt, LinearTransform t) {
		startDrag(nevt, t, false);
	}

	public void startDrag(NeoMouseEvent nevt, LinearTransform t, boolean restrict_to_parent) {
		for(int i=0; i<dragged_glyphs.length; i++){
			glyph_draggers[i].startDrag(dragged_glyphs[i], nevt, t, restrict_to_parent);
		}
	}

	public void setConstraint(int axis, boolean is_constrained) {
		for(GlyphDragger glyph_dragger : glyph_draggers){
			glyph_dragger.setConstraint(axis, is_constrained);
		}
	}

	public void setUseCopy(boolean b) {
		for(GlyphDragger glyph_dragger : glyph_draggers){
			glyph_dragger.setUseCopy(b);
		}
	}

	/**
	 *  Add a glyph drag listener.
	 *  Note: may want to change this to distinguish between listeners that
	 *  want to know about DRAG_IN_PROGRESS events,
	 *  versus listeners that just want to know about DRAG_STARTED and
	 *  DRAG_ENDED events
	 *  (since posting lots of DRAG_IN_PROGRESS events may be inefficient
	 *     on the other hand, another way to deal with this is to recycle the same
	 *     NeoGlyphDragEvent...).
	 */
	public void addGlyphDragListener(NeoGlyphDragListener listener) {
		for(GlyphDragger glyph_dragger : glyph_draggers){
			glyph_dragger.addGlyphDragListener(listener);
		}
	}

	public void removeGlyphDragListener(NeoGlyphDragListener listener) {
		for(GlyphDragger glyph_dragger : glyph_draggers){
			glyph_dragger.removeGlyphDragListener(listener);
		}
	}

	public void removeAllListeners() {
		for(GlyphDragger glyph_dragger : glyph_draggers){
			glyph_dragger.removeAllListeners();
		}
	}
}

