package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.glyph.MapViewModeHolder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 *  TierGlyphViewMode is an extension of TierGlyph that contains a ViewModeGlyph
 *  that is the "real" top level glyph. All methods are passed to the ViewModeGlyph.
 *  This is an implementation of the Delecation Design Pattern.
 */
public class TierGlyphViewMode extends TierGlyph {
	private SeqSymmetry modelSym;

	public TierGlyphViewMode(SeqSymmetry sym, ITrackStyleExtended style, Direction direction) {
		this.modelSym = sym;
		setStyleWithDirection(style, direction);
		super.setDirection(direction);
		setHitable(false);
		this.setInfo(sym);
	}

	private ViewModeGlyph viewModeGlyph;

	public ViewModeGlyph getViewModeGlyph() {
		return viewModeGlyph;
	}

	private MapViewGlyphFactoryI getViewGlyphFactory(String viewMode) {
		return MapViewModeHolder.getInstance().getViewFactory(viewMode);
	}

	private void setStyleWithDirection(ITrackStyleExtended style, Direction direction) {
		this.style = style;
		if (viewModeGlyph == null || !viewModeGlyph.getViewMode().equals(style.getViewMode())) {
			viewModeGlyph = null;
			MapViewGlyphFactoryI factory = getViewGlyphFactory(style.getViewMode());
			if (factory != null) {
				viewModeGlyph = factory.getViewModeGlyph(modelSym, style, direction);
			}
			// TODO get real default / empty factory
			if (viewModeGlyph == null) {
//				viewModeGlyph = new ExpandedAnnotationGlyph(style);
			}
		}
	}

	@Override
	public void setStyle(ITrackStyleExtended style) {
		setStyleWithDirection(style, getDirection());
	}

	@Override
	public void setFillColor(Color col) {
		viewModeGlyph.setFillColor(col);
	}

	@Override
	public int getActualSlots(){
		return viewModeGlyph.getActualSlots();
	}

	@Override
	public void setPreferredHeight(double height, ViewI view){
		viewModeGlyph.setPreferredHeight(height, view);
	}

	// Glyph methods
	@Override
	public Color getBackgroundColor()  {
		return viewModeGlyph.getBackgroundColor();
	}
	@Override
	public Color getColor()  {
		return viewModeGlyph.getColor();
	}
	@Override
	public Color getForegroundColor()  {
		return viewModeGlyph.getForegroundColor();
	}
	@Override
	public Font getFont() {
		return viewModeGlyph.getFont();
	}
	@Override
	public GlyphI getChild(int index) {
/////		System.out.println((" + index + ")");
		return viewModeGlyph.getChild(index);
	}
	@Override
	public GlyphI getParent()  {
		return viewModeGlyph.getParent();
	}
	@Override
	public List<GlyphI> getChildren()  {
		return viewModeGlyph.getChildren();
	}
	@Override
	public Object getInfo()  {
		return viewModeGlyph.getInfo();
	}
	@Override
	public PackerI getPacker()  {
		return viewModeGlyph.getPacker();
	}
	@Override
	public Rectangle getPixelBox()  {
		return viewModeGlyph.getPixelBox();
	}
	@Override
	public Rectangle getPixelBox(ViewI view)  {
		return viewModeGlyph.getPixelBox(view);
	}
	@Override
	public Rectangle2D.Double getCoordBox()   {
		return viewModeGlyph.getCoordBox();
	}
	@Override
	public Rectangle2D.Double getSelectedRegion() {
		return viewModeGlyph.getSelectedRegion();
	}
	@Override
	public Scene getScene() {
		return viewModeGlyph.getScene();
	}
	@Override
	public boolean drawTransients() {
		return viewModeGlyph.drawTransients();
	}
	@Override
	public boolean getGlobalChildTransform(ViewI view, LinearTransform trans) {
		return viewModeGlyph.getGlobalChildTransform(view, trans);
	}
	@Override
	public boolean getGlobalTransform(ViewI view, LinearTransform trans) {
		return viewModeGlyph.getGlobalTransform(view, trans);
	}
	@Override
	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		return viewModeGlyph.hit(pixel_hitbox, view);
	}
	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		return viewModeGlyph.hit(coord_hitbox, view);
	}
	@Override
	public boolean inside(int x, int y)  {
		return viewModeGlyph.inside(x, y);
	}
	@Override
	public boolean intersects(Rectangle rect)  {
		return viewModeGlyph.intersects(rect);
	}
	@Override
	public boolean intersects(Rectangle2D.Double rect, ViewI view)  {
		return viewModeGlyph.intersects(rect, view);
	}
	@Override
	public boolean isHitable() {
		return viewModeGlyph.isHitable();
	}
	@Override
	public boolean isOverlapped(){
		return viewModeGlyph.isOverlapped();
	}
	@Override
	public boolean isSelectable() {
		return viewModeGlyph.isSelectable();
	}
	@Override
	public boolean isVisible()  {
		return viewModeGlyph.isVisible();
	}
	@Override
	public boolean supportsSubSelection() {
		return viewModeGlyph.supportsSubSelection();
	}
	@Override
	public boolean withinView(ViewI view) {
		return viewModeGlyph.withinView(view);
	}
//	@Override
//	public final boolean isSelected() {
//		
//		return viewModeGlyph.isSelected();
//	}
	@Override
	public int getChildCount() {
/////		
		return viewModeGlyph.getChildCount();
	}
	@Override
	public int getDrawOrder() {
		return viewModeGlyph.getDrawOrder();
	}
	@Override
	public void addChild(GlyphI glyph)  {
		viewModeGlyph.addChild(glyph);
	}
	@Override
	public void addChild(GlyphI glyph, int position) {
		viewModeGlyph.addChild(glyph);
	}
	@Override
	public void calcPixels(ViewI view)  {
		viewModeGlyph.calcPixels(view);
	}
	@Override
	public void draw(ViewI view)  {
		viewModeGlyph.draw(view);
	}
	@Override
	public void drawSelected(ViewI view) {
		viewModeGlyph.drawSelected(view);
	}
	@Override
	public void drawTraversal(ViewI view)  {
		viewModeGlyph.drawTraversal(view);
	}
	@Override
	public void getChildTransform(ViewI view, LinearTransform trans) {
		viewModeGlyph.getChildTransform(view, trans);
	}
	@Override
	public void moveAbsolute(double x, double y) {
		viewModeGlyph.moveAbsolute(x, y);
	}
	@Override
	public void moveRelative(double diffx, double diffy) {
		viewModeGlyph.moveRelative(diffx, diffy);
	}
	@Override
	public void pack(ViewI view, boolean manual) {
		viewModeGlyph.pack(view, manual);
	}
	@Override
	public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList, ViewI view)  {
		viewModeGlyph.pickTraversal(pickRect, pickList, view);
	}
	@Override
	public void removeAllChildren() {
		viewModeGlyph.removeAllChildren();
	}
	@Override
	public void removeChild(GlyphI glyph)  {
		viewModeGlyph.removeChild(glyph);
	}
	@Override
	public void select(double x, double y, double width, double height) {
		viewModeGlyph.select(x, y, width, height);
	}
	@Override
	public void setBackgroundColor(Color color)  {
		viewModeGlyph.setBackgroundColor(color);
	}
	@Override
	public void setColor(Color color)  {
		viewModeGlyph.setColor(color);
	}
	@Override
	public void setCoordBox(Rectangle2D.Double coordbox)   {
		viewModeGlyph.setCoordBox(coordbox);
	}
	@Override
	public void setCoords(double x, double y, double width, double height)  {
		viewModeGlyph.setCoords(x, y, width, height);
	}
	@Override
	public void setDrawOrder(int order) {
		viewModeGlyph.setDrawOrder(order);
	}
	@Override
	public void setFont(Font f) {
		viewModeGlyph.setFont(f);
	}
	@Override
	public void setForegroundColor(Color color)  {
		viewModeGlyph.setForegroundColor(color);
	}
	@Override
	public void setInfo(Object info)  {
		viewModeGlyph.setInfo(info);
	}
	@Override
	public void setMinimumPixelBounds(Dimension d)   {
		viewModeGlyph.setMinimumPixelBounds(d);
	}
	@Override
	public void setOverlapped(boolean overlapped){
		viewModeGlyph.setOverlapped(overlapped);
	}
	@Override
	public void setPacker(PackerI packer)  {
		viewModeGlyph.setPacker(packer);
	}
	@Override
	public void setParent(GlyphI glyph)  {
		viewModeGlyph.setParent(glyph);
	}
	@Override
	public void setScene(Scene s) {
		viewModeGlyph.setScene(s);
	}
	@Override
	public void setSelectable(boolean selectability) {
		viewModeGlyph.setSelectable(selectability);
	}
	@Override
	public void setSelected(boolean selected) {
		viewModeGlyph.setSelected(selected);
	}
	@Override
	public void setVisibility(boolean isVisible)  {
		viewModeGlyph.setVisibility(isVisible);
	}

	// SolidGlyph methods
	public void setHitable(boolean hitable) {
		viewModeGlyph.setHitable(hitable);
	}

	// TierGlyph methods
	@Override
	public ITrackStyleExtended getAnnotStyle() {
		return viewModeGlyph.getAnnotStyle();
	}
	@Override
	public Color getFillColor() {
		return viewModeGlyph.getFillColor();
	}
	@Override
	public Direction getDirection() {
		return viewModeGlyph.getDirection();
	}
	@Override
	public String getLabel() {
		return viewModeGlyph.getLabel();
	}
	@Override
	public void addMiddleGlyph(GlyphI gl) {
		viewModeGlyph.addMiddleGlyph(gl);
	}
	@Override
	public void setCollapsedPacker(CollapsePacker packer) {
		System.out.println("!!!!!!!!! setCollapsedPacker(CollapsePacker packer)");
	}
	@Override
	public void setDirection(Direction d) {
		viewModeGlyph.setDirection(d);
	}
	@Override
	public void setExpandedPacker(FasterExpandPacker packer) {
		System.out.println("!!!!!!!!! setExpandedPacker(FasterExpandPacker packer)");
	}
	@Override
	public void setLabel(String str) {
		viewModeGlyph.setLabel(str);
	}
	@Override
	public void drawChildren(ViewI view) {
		System.out.println("!!!!!!!!! drawChildren(ViewI view)");
/////		viewModeGlyph.drawChildren(view);
	}
}
