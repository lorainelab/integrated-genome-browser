package com.affymetrix.igb.viewmode;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;

public class SemanticZoomGlyphFactory extends AbstractViewModeGlyph implements MapViewGlyphFactoryI {
	private SeqMapViewExtendedI smv;
	private final MapViewGlyphFactoryI depthFactory;
	private final MapViewGlyphFactoryI annotFactory;
	private ViewModeGlyph depthGlyph;
	private ViewModeGlyph annotationGlyph;

	// glyph class
	private class SemanticZoomGlyph extends AbstractViewModeGlyph {
		private static final double ZOOM_X_SCALE = 0.002;
		private final ViewModeGlyph depthGlyph;
		private final ViewModeGlyph annotationGlyph;
		
		public SemanticZoomGlyph(ITrackStyleExtended style, ViewModeGlyph depthGlyph, ViewModeGlyph annotationGlyph) {
			super();
			this.depthGlyph = depthGlyph;
			this.annotationGlyph = annotationGlyph;
			setStyle(style);
		}

		public void draw(ViewI view) {
			ViewModeGlyph glyph = getGlyph(view);
			glyph.setCoordBox(getCoordBox());
			glyph.draw(view);
		}

		@Override
		public void setPreferredHeight(double height, ViewI view) {
			depthGlyph.setPreferredHeight(height, view);
			annotationGlyph.setPreferredHeight(height, view);
		}

		@Override
		public int getActualSlots() {
			return 0;
		}

		private ViewModeGlyph getGlyph(ViewI view) {
			return view.getTransform().getScaleX() < ZOOM_X_SCALE ? depthGlyph : annotationGlyph;
		}

		@Override
		public void setFillColor(Color col) {
			super.setFillColor(col);
			depthGlyph.setFillColor(col);
			annotationGlyph.setFillColor(col);
		}

		// Glyph methods
		@Override
		public void addChild(GlyphI glyph)  {
			annotationGlyph.addChild(glyph);
		}
		@Override
		public void addChild(GlyphI glyph, int position) {
			annotationGlyph.addChild(glyph, position);
		}
		@Override
		public void calcPixels(ViewI view)  {
			getGlyph(view).calcPixels(view);
		}
		@Override
		public void moveAbsolute(double x, double y) {
			super.moveAbsolute(x, y);
			depthGlyph.moveAbsolute(x, y);
			annotationGlyph.moveAbsolute(x, y);
		}
		@Override
		public void moveRelative(double diffx, double diffy) {
			super.moveRelative(diffx, diffy);
			depthGlyph.moveRelative(diffx, diffy);
			annotationGlyph.moveRelative(diffx, diffy);
		}
		@Override
		public void pack(ViewI view, boolean manual) {
			getGlyph(view).pack(view, manual);
		}
		@Override
		public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList, ViewI view)  {
			getGlyph(view).pickTraversal(pickRect, pickList, view);
		}
		@Override
		public void removeAllChildren() {
			annotationGlyph.removeAllChildren();
		}
		@Override
		public void removeChild(GlyphI glyph)  {
			annotationGlyph.removeChild(glyph);
		}
		@Override
		public void select(double x, double y, double width, double height) {
			super.select(x, y, width, height);
			depthGlyph.select(x, y, width, height);
			annotationGlyph.select(x, y, width, height);
		}
		@Override
		public void setBackgroundColor(Color color)  {
			super.setBackgroundColor(color);
			depthGlyph.setBackgroundColor(color);
			annotationGlyph.setBackgroundColor(color);
		}
		@Override
		public void setColor(Color color)  {
			super.setColor(color);
			depthGlyph.setColor(color);
			annotationGlyph.setColor(color);
		}
		@Override
		public void setCoordBox(Rectangle2D.Double coordbox)   {
			super.setCoordBox(coordbox);
			depthGlyph.setCoordBox(coordbox);
			annotationGlyph.setCoordBox(coordbox);
		}
		@Override
		public void setCoords(double x, double y, double width, double height)  {
			super.setCoords(x, y, width, height);
			depthGlyph.setCoords(x, y, width, height);
			annotationGlyph.setCoords(x, y, width, height);
		}
		@Override
		public void setDrawOrder(int order) {
			super.setDrawOrder(order);
			depthGlyph.setDrawOrder(order);
			annotationGlyph.setDrawOrder(order);
		}
		@Override
		public void setFont(Font f) {
			super.setFont(f);
			depthGlyph.setFont(f);
			annotationGlyph.setFont(f);
		}
		@Override
		public void setForegroundColor(Color color)  {
			super.setForegroundColor(color);
			depthGlyph.setForegroundColor(color);
			annotationGlyph.setForegroundColor(color);
		}
		@Override
		public void setInfo(Object info)  {
			super.setInfo(info);
			depthGlyph.setInfo(info);
			annotationGlyph.setInfo(info);
		}
		@Override
		public void setMinimumPixelBounds(Dimension d)   {
			super.setMinimumPixelBounds(d);
			depthGlyph.setMinimumPixelBounds(d);
			annotationGlyph.setMinimumPixelBounds(d);
		}
		@Override
		public void setOverlapped(boolean overlapped){
			super.setOverlapped(overlapped);
			depthGlyph.setOverlapped(overlapped);
			annotationGlyph.setOverlapped(overlapped);
		}
		@Override
		public void setPacker(PackerI packer)  {
			throw new IllegalStateException();
		}
		@Override
		public void setParent(GlyphI glyph)  {
			super.setParent(glyph);
			depthGlyph.setParent(glyph);
			annotationGlyph.setParent(glyph);
		}
		@Override
		public void setScene(Scene s) {
			super.setScene(s);
			depthGlyph.setScene(s);
			annotationGlyph.setScene(s);
		}
		@Override
		public void setSelectable(boolean selectability) {
			super.setSelectable(selectability);
			depthGlyph.setSelectable(selectability);
			annotationGlyph.setSelectable(selectability);
		}
		@Override
		public void setSelected(boolean selected) {
			super.setSelected(selected);
			depthGlyph.setSelected(selected);
			annotationGlyph.setSelected(selected);
		}
		@Override
		public void setVisibility(boolean isVisible)  {
			super.setVisibility(isVisible);
			depthGlyph.setVisibility(isVisible);
			annotationGlyph.setVisibility(isVisible);
		}

		// SolidGlyph methods
		public void setHitable(boolean hitable) {
			super.setHitable(hitable);
			depthGlyph.setHitable(hitable);
			annotationGlyph.setHitable(hitable);
		}

		// TierGlyph methods
		@Override
		public void setDirection(Direction d) {
			super.setDirection(d);
			depthGlyph.setDirection(d);
			annotationGlyph.setDirection(d);
		}
		@Override
		public void setLabel(String str) {
			super.setLabel(str);
			depthGlyph.setLabel(str);
			annotationGlyph.setLabel(str);
		}
	}
	// end glyph class

	public SemanticZoomGlyphFactory(MapViewGlyphFactoryI depthFactory, MapViewGlyphFactoryI annotFactory) {
		super();
		this.depthFactory = depthFactory;
		this.annotFactory = annotFactory;
	}

	@Override
	public void init(Map<String, Object> options) {
	}

	@Override
	public void createGlyph(SeqSymmetry sym, SeqMapViewExtendedI smv) {
		// not implemented
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym,
			ITrackStyleExtended style, Direction tier_direction) {
		depthGlyph = depthFactory.getViewModeGlyph(sym, style, tier_direction);
		annotationGlyph = annotFactory.getViewModeGlyph(sym, style, tier_direction);
		return new SemanticZoomGlyph(style, depthGlyph, annotationGlyph);
	}

	@Override
	public String getName() {
		return "semantic zoom " + depthFactory.getName(); // default for annotations
	}

	@Override
	public boolean isFileSupported(FileTypeCategory category) {
		return depthFactory.isFileSupported(category) && annotFactory.isFileSupported(category);
	}
	
	public void setSeqMapView(SeqMapViewExtendedI gviewer) {
		this.smv = gviewer;
	}

	@Override
	public final SeqMapViewExtendedI getSeqMapView(){
		return smv;
	}

	@Override
	public void setPreferredHeight(double height, ViewI view) {
	}

	@Override
	public int getActualSlots() {
		return 0;
	}
}
