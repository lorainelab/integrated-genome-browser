package com.affymetrix.igb.viewmode;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.GlyphStyle;
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
		private ViewModeGlyph lastUsedGlyph;
		
		public SemanticZoomGlyph(ITrackStyleExtended style, ViewModeGlyph depthGlyph, ViewModeGlyph annotationGlyph) {
			super();
			this.depthGlyph = depthGlyph;
			this.annotationGlyph = annotationGlyph;
			lastUsedGlyph = annotationGlyph;
			setStyle(style);
		}

		private ViewModeGlyph getGlyph(ViewI view) {
			return view.getTransform().getScaleX() < ZOOM_X_SCALE ? depthGlyph : annotationGlyph;
		}

		@Override
		public void setPreferredHeight(double height, ViewI view) {
			lastUsedGlyph = getGlyph(view);
			lastUsedGlyph.setPreferredHeight(height, view);
		}

		@Override
		public int getActualSlots() {
			return lastUsedGlyph.getActualSlots();
		}

		@Override
		public void setFillColor(Color col) {
			super.setFillColor(col);
			depthGlyph.setFillColor(col);
			annotationGlyph.setFillColor(col);
		}

		// Glyph methods
	
		@Override
		public GlyphI getChild(int index) {
			return lastUsedGlyph.getChild(index);
		}
		@Override
		public GlyphStyle getGlyphStyle() {
			return lastUsedGlyph.getGlyphStyle();
		}
		@Override
		public List<GlyphI> getChildren()  {
			return lastUsedGlyph.getChildren();
		}
		@Override
		public PackerI getPacker()  {
			throw new IllegalStateException();
		}
		@Override
		public Rectangle getPixelBox()  {
			return lastUsedGlyph.getPixelBox();
		}
		@Override
		public Rectangle getPixelBox(ViewI view)  {
			lastUsedGlyph = getGlyph(view);
			return lastUsedGlyph.getPixelBox(view);
		}
		@Override
		public Rectangle2D.Double getCoordBox()   {
			return lastUsedGlyph.getCoordBox();
		}
		@Override
		public Rectangle2D.Double getSelectedRegion() {
			return lastUsedGlyph.getSelectedRegion();
		}
		@Override
		public boolean drawTransients() {
			return lastUsedGlyph.drawTransients();
		}
		@Override
		public boolean getGlobalChildTransform(ViewI view, LinearTransform trans) {
			lastUsedGlyph = getGlyph(view);
			return lastUsedGlyph.getGlobalChildTransform(view, trans);
		}
		@Override
		public boolean getGlobalTransform(ViewI view, LinearTransform trans) {
			lastUsedGlyph = getGlyph(view);
			return lastUsedGlyph.getGlobalTransform(view, trans);
		}
		@Override
		public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
			lastUsedGlyph = getGlyph(view);
			return lastUsedGlyph.hit(pixel_hitbox, view);
		}
		@Override
		public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
			lastUsedGlyph = getGlyph(view);
			return lastUsedGlyph.hit(coord_hitbox, view);
		}
		@Override
		public boolean inside(int x, int y)  {
			return lastUsedGlyph.inside(x, y);
		}
		@Override
		public boolean intersects(Rectangle rect)  {
			return lastUsedGlyph.intersects(rect);
		}
		@Override
		public boolean intersects(Rectangle2D.Double rect, ViewI view)  {
			lastUsedGlyph = getGlyph(view);
			return lastUsedGlyph.intersects(rect, view);
		}
		@Override
		public boolean isHitable() {
			return lastUsedGlyph.isHitable();
		}
		@Override
		public boolean isSelectable() {
			return lastUsedGlyph.isHitable();
		}
		@Override
		public boolean supportsSubSelection() {
			return lastUsedGlyph.supportsSubSelection();
		}
		@Override
		public boolean withinView(ViewI view) {
			lastUsedGlyph = getGlyph(view);
			return lastUsedGlyph.withinView(view);
		}
		@Override
		public int getChildCount() {
			return lastUsedGlyph.getChildCount();
		}
		@Override
		public int getDrawOrder() {
			return lastUsedGlyph.getDrawOrder();
		}
		@Override
		public int getMinPixelsHeight() {
			return lastUsedGlyph.getMinPixelsHeight();
		}
		@Override
		public int getMinPixelsWidth() {
			return lastUsedGlyph.getMinPixelsWidth();
		}
		@Override
		public void addChild(GlyphI glyph)  {
			lastUsedGlyph.addChild(glyph);
		}
		@Override
		public void addChild(GlyphI glyph, int position) {
			lastUsedGlyph.addChild(glyph, position);
		}
		@Override
		public void calcPixels (ViewI view)  {
			lastUsedGlyph = getGlyph(view);
			lastUsedGlyph.calcPixels(view);
		}
		@Override
		public void clearChildren() {
		}
		@Override
		public void draw(ViewI view)  {
			lastUsedGlyph = getGlyph(view);
			lastUsedGlyph.draw(view);
		}
		@Override
		public void drawSelected(ViewI view) {
		}
		@Override
		public void drawTraversal(ViewI view)  {
		}
		@Override
		public void getChildTransform(ViewI view, LinearTransform trans) {
		}
		@Override
		public void moveAbsolute(double x, double y) {
			super.moveAbsolute(x, y);
			lastUsedGlyph.moveAbsolute(x, y);
		}
		@Override
		public void moveRelative(double diffx, double diffy) {
			super.moveRelative(diffx, diffy);
			lastUsedGlyph.moveRelative(diffx, diffy);
		}
		@Override
		public void pack(ViewI view, boolean manual) {
			lastUsedGlyph = getGlyph(view);
			lastUsedGlyph.pack(view, manual);
		}
		@Override
		public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList, ViewI view)  {
			lastUsedGlyph = getGlyph(view);
			lastUsedGlyph.pickTraversal(pickRect, pickList, view);
		}
		@Override
		public void removeAllChildren() {
			lastUsedGlyph.removeAllChildren();
		}
		@Override
		public void removeChild(GlyphI glyph)  {
			lastUsedGlyph.removeChild(glyph);
		}
		@Override
		public void resetChildren() {
		}
		@Override
		public void select(double x, double y, double width, double height) {
			super.select(x, y, width, height);
			lastUsedGlyph.select(x, y, width, height);
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
			lastUsedGlyph.setCoordBox(coordbox);
		}
		@Override
		public void setCoords(double x, double y, double width, double height)  {
			super.setCoords(x, y, width, height);
			lastUsedGlyph.setCoords(x, y, width, height);
		}
		@Override
		public void setDrawOrder(int order) {
			super.setDrawOrder(order);
			lastUsedGlyph.setDrawOrder(order);
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
		public void setGlyphStyle(GlyphStyle glyphStyle) {
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
			lastUsedGlyph.setMinimumPixelBounds(d);
		}
		@Override
		public void setOverlapped(boolean overlapped){
			super.setOverlapped(overlapped);
			lastUsedGlyph.setOverlapped(overlapped);
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
		public void setPixelBox(Rectangle pixelbox) {
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
			annotationGlyph.setSelectable(selectability);
		}
		@Override
		public void setSelected(boolean selected) {
			super.setSelected(selected);
			annotationGlyph.setSelected(selected);
		}
		@Override
		public void setVisibility(boolean visible)  {
			super.setVisibility(visible);
			depthGlyph.setVisibility(visible);
			annotationGlyph.setVisibility(visible);
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
