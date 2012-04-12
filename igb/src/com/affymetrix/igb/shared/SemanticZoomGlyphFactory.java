package com.affymetrix.igb.shared;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.event.NeoViewBoxChangeEvent;
import com.affymetrix.genoviz.event.NeoViewBoxListener;
import com.affymetrix.genoviz.glyph.GlyphStyle;
import com.affymetrix.igb.shared.TierGlyph.Direction;

public abstract class SemanticZoomGlyphFactory extends MapViewGlyphFactoryA {

	// glyph class
	public static abstract class SemanticZoomGlyph extends AbstractViewModeGlyph implements NeoViewBoxListener {
		protected Map<String, ViewModeGlyph> viewModeGlyphs;
		protected ViewModeGlyph lastUsedGlyph;
		
		protected SemanticZoomGlyph(SeqSymmetry sym, ITrackStyleExtended style, Direction direction, SeqMapViewExtendedI smv) {
			super();
			super.setInfo(sym);
			init(sym, style, direction, smv);
			setDirection(direction);
			lastUsedGlyph = getDefaultGlyph();
			setStyle(style);
		}

		protected abstract ViewModeGlyph getGlyph(ViewI view);

		protected abstract void init(SeqSymmetry sym, ITrackStyleExtended style, Direction direction, SeqMapViewExtendedI smv);
		protected abstract ViewModeGlyph getDefaultGlyph();

		@Override
		public void viewBoxChanged(final NeoViewBoxChangeEvent e){
			if(e.getSource() instanceof ViewI){
				lastUsedGlyph = getGlyph((ViewI)e.getSource());
			}
		}
				
		@Override
		public void setStyle(ITrackStyleExtended style){
			super.setStyle(style);
			for(ViewModeGlyph vmg : viewModeGlyphs.values()){
				vmg.setStyle(style);
			}
		}

		@Override
		public void processParentCoordBox(Rectangle2D.Double parentCoordBox) {
			super.processParentCoordBox(parentCoordBox);
//			if (parentCoordBox.getHeight() == 0) {
				Rectangle2D.Double useCoordBox = null;
				for (ViewModeGlyph vmg : viewModeGlyphs.values()) {
					if (vmg.getCoordBox().getHeight() > 0) {
						useCoordBox = vmg.getCoordBox();
					}
					vmg.setCoordBox(parentCoordBox);
				}
				if (useCoordBox != null) {
					parentCoordBox.setRect(useCoordBox);
				}
//			}
		}

		@Override
		public void setPreferredHeight(double height, ViewI view) {
			lastUsedGlyph.setPreferredHeight(height, view);
		}

		@Override
		public int getActualSlots() {
			return lastUsedGlyph.getActualSlots();
		}

		@Override
		public void setFillColor(Color col) {
			super.setFillColor(col);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setFillColor(col);
			}
		}

		@Override
		public Map<String, Class<?>> getPreferences() {
			Map<String, Class<?>> preferences = new HashMap<String, Class<?>>();
			// TODO - error message if two ViewModeGlyph have the same key and different values
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				preferences.putAll(viewModeGlyph.getPreferences());
			}
			return preferences;
		}

		@Override
		public void setPreferences(Map<String, Object> preferences) {
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setPreferences(preferences);
			}
		}

		@Override
		public void setTierGlyph(TierGlyph tierGlyph) {
			super.setTierGlyph(tierGlyph);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setTierGlyph(tierGlyph);
			}
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
			return lastUsedGlyph.getPacker();
			//throw new IllegalStateException();
		}
		@Override
		public Rectangle getPixelBox()  {
			return lastUsedGlyph.getPixelBox();
		}
		@Override
		public Rectangle getPixelBox(ViewI view)  {
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
			return lastUsedGlyph.getGlobalChildTransform(view, trans);
		}
		@Override
		public boolean getGlobalTransform(ViewI view, LinearTransform trans) {
			return lastUsedGlyph.getGlobalTransform(view, trans);
		}
		@Override
		public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
			return lastUsedGlyph.hit(pixel_hitbox, view);
		}
		@Override
		public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
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
			lastUsedGlyph.calcPixels(view);
		}
		@Override
		public void clearChildren() {
			lastUsedGlyph.clearChildren();
		}
		@Override
		public void draw(ViewI view)  {
			lastUsedGlyph.draw(view);
		}
		@Override
		public void drawSelected(ViewI view) {
			lastUsedGlyph.drawSelected(view);
		}
		@Override
		public void drawTraversal(ViewI view)  {
			lastUsedGlyph.drawTraversal(view);
		}
		@Override
		public void getChildTransform(ViewI view, LinearTransform trans) {
			lastUsedGlyph.getChildTransform(view, trans);
		}
		@Override
		public void moveAbsolute(double x, double y) {
			super.moveAbsolute(x, y);
		}
		@Override
		public void moveRelative(double diffx, double diffy) {
			super.moveRelative(diffx, diffy);
			for(ViewModeGlyph vmg : viewModeGlyphs.values()){
				List<GlyphI> vmgChildren = vmg.getChildren();
				if (vmgChildren != null) {
					int numchildren = vmgChildren.size();
					for (int i=0; i<numchildren; i++) {
						vmgChildren.get(i).moveRelative(diffx, diffy);
					}
				}
			}
		}
		@Override
		public void pack(ViewI view) {
			for(ViewModeGlyph vmg : viewModeGlyphs.values()){
				vmg.pack(view);
			}
		}
		@Override
		public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList, ViewI view)  {
			lastUsedGlyph.pickTraversal(pickRect, pickList, view);
		}
		@Override
		public void removeAllChildren() {
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.removeAllChildren();
			}
		}
		@Override
		public void removeChild(GlyphI glyph)  {
			lastUsedGlyph.removeChild(glyph);
		}
		@Override
		public void resetChildren() {
			lastUsedGlyph.resetChildren();
		}
		@Override
		public List<SeqSymmetry> getSelected(){
			return lastUsedGlyph.getSelected();	
		}
		@Override
		public void select(double x, double y, double width, double height) {
			super.select(x, y, width, height);
			lastUsedGlyph.select(x, y, width, height);
		}
		@Override
		public void setBackgroundColor(Color color)  {
			super.setBackgroundColor(color);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setBackgroundColor(color);
			}
		}
		@Override
		public void setColor(Color color)  {
			super.setColor(color);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setColor(color);
			}
		}
		@Override
		public void setCoordBox(Rectangle2D.Double coordbox)   {
			super.setCoordBox(coordbox);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setCoordBox(coordbox);
			}
		}
		@Override
		public void setCoords(double x, double y, double width, double height)  {
			super.setCoords(x, y, width, height);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setCoordBox(super.getCoordBox());
			}
		}
		@Override
		public void setDrawOrder(int order) {
			super.setDrawOrder(order);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setDrawOrder(order);
			}
		}
		@Override
		public void setFont(Font f) {
			super.setFont(f);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setFont(f);
			}
		}
		@Override
		public void setForegroundColor(Color color)  {
			super.setForegroundColor(color);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setForegroundColor(color);
			}
		}
		@Override
		public void setGlyphStyle(GlyphStyle glyphStyle) {
			super.setGlyphStyle(glyphStyle);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setGlyphStyle(glyphStyle);
			}
		}
		@Override
		public void setInfo(Object info)  {
			super.setInfo(info);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setInfo(info);
			}
		}
		@Override
		public void setMinimumPixelBounds(Dimension d)   {
			super.setMinimumPixelBounds(d);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setMinimumPixelBounds(d);
			}
		}
		@Override
		public void setOverlapped(boolean overlapped){
			super.setOverlapped(overlapped);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setOverlapped(overlapped);
			}
		}
		@Override
		public void setPacker(PackerI packer)  {
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setPacker(packer);
			}
		}
		@Override
		public void setParent(GlyphI glyph)  {
			super.setParent(glyph);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setParent(glyph);
			}
		}
		@Override
		public void setPixelBox(Rectangle pixelbox) {
			super.setPixelBox(pixelbox);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setPixelBox(pixelbox);
			}
		}
		@Override
		public void setScene(Scene s) {
			super.setScene(s);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setScene(s);
			}
		}
		@Override
		public void setSelectable(boolean selectability) {
			super.setSelectable(selectability);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setSelectable(selectability);
			}
		}
		@Override
		public void setSelected(boolean selected) {
			super.setSelected(selected);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setSelected(selected);
			}
		}
		@Override
		public void setVisibility(boolean visible)  {
			super.setVisibility(visible);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setVisibility(visible);
			}
		}

		// SolidGlyph methods
		public void setHitable(boolean hitable) {
			super.setHitable(hitable);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setHitable(hitable);
			}
		}

		// TierGlyph methods
		@Override
		public void setDirection(Direction d) {
			super.setDirection(d);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setDirection(d);
			}
		}
		@Override
		public void setLabel(String str) {
			super.setLabel(str);
			for (ViewModeGlyph viewModeGlyph : viewModeGlyphs.values()) {
				viewModeGlyph.setLabel(str);
			}
		}
		
		//FIXME: Fix graph view mode.
		@Override
		public void copyChildren(ViewModeGlyph temp) {
			if(temp instanceof SemanticZoomGlyph){
				for(Entry<String, ViewModeGlyph> vg : ((SemanticZoomGlyph)temp).viewModeGlyphs.entrySet()){
					if(viewModeGlyphs.get(vg.getKey()) != null){
						viewModeGlyphs.get(vg.getKey()).copyChildren(vg.getValue());
					}
				}
			}
		}
	}
	// end glyph class
}
