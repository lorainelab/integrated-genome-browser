package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.viewmode.*;
import com.affymetrix.igb.viewmode.UnloadedGlyphFactory.UnloadedGlyph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.*;
import java.awt.geom.Rectangle2D;

/**
 *  !!! the TierGlyph and ViewModeGlyph and all its subordinate ViewModeGlyphs should
 *  all contain the same instance of CoordBox. !!!
 *  TierGlyph is intended for use with AffyTieredMap.
 *  Each tier in the TieredNeoMap is implemented as a TierGlyph, which can have different
 *  states as indicated below.
 *  In a AffyTieredMap, TierGlyphs pack relative to each other but not to other glyphs added
 *  directly to the map.
 *
 *  TierGlyph contains a ViewModeGlyph
 *  that is the "real" top level glyph. All methods are passed to the ViewModeGlyph.
 *  This is an implementation of the Delegation Design Pattern.
 */
public class TierGlyph extends SolidGlyph {
	protected ITrackStyleExtended style;
	public static enum Direction {
		FORWARD, NONE, REVERSE, BOTH, AXIS
	};
	private Direction direction = Direction.NONE;

	private final SeqMapViewExtendedI smv;
	private SeqSymmetry modelSym;
	private boolean ignoreUnloaded = false;

	public TierGlyph(SeqSymmetry sym, ITrackStyleExtended style, Direction direction, SeqMapViewExtendedI smv, ViewModeGlyph viewModeGlyph) {
 		this.modelSym = sym;
 		this.smv = smv;
		initViewModeGlyph(viewModeGlyph);
		this.style = style;
 		setDirection(direction);
		setInfo(modelSym);
 		setHitable(false);
	}

	private ViewModeGlyph viewModeGlyph;
	
	public ViewModeGlyph getViewModeGlyph() {
		return viewModeGlyph;
	}

	private void setViewModeGlyph(ViewModeGlyph vmg) {
		if (smv.isGenomeSequenceSupported() && smv.getViewSeq().getComposition() != null && 
				smv.getViewSeq() != smv.getAnnotatedSeq() && !(vmg instanceof UnloadedGlyph)) {
			if (viewModeGlyph == null || viewModeGlyph instanceof UnloadedGlyph) {
				initViewModeGlyph(vmg);
			} else {
				viewModeGlyph.copyChildren(vmg);
			}
		} else {
			initViewModeGlyph(vmg);
		}
	}

	private void initViewModeGlyph(ViewModeGlyph vmg){
		viewModeGlyph = vmg;
		viewModeGlyph.setTierGlyph(this);
		// Below code does not work in case of bai zoom loader glyph.
		// viewModeGlyph.processParentCoordBox(super.getCoordBox());
		viewModeGlyph.processParentCoordBox(getCoordBox());
	}
	
	private void setViewModeGlyph(ITrackStyleExtended style) {
		if (isSymLoaded()) {
			MapViewGlyphFactoryI factory = getViewGlyphFactory(style.getViewMode());
			if (factory != null) {
				Operator operator = getOperator(style.getOperator());
				if (operator != null) {
					factory = new OperatorGlyphFactory(operator, factory);
					style.setViewMode(((OperatorGlyphFactory)factory).getActualFactoryName());
				} else {
					if (modelSym instanceof RootSeqSymmetry) {
						if (!factory.isCategorySupported(((RootSeqSymmetry) modelSym).getCategory()) || !factory.isURISupported(style.getMethodName())) {
							factory = MapViewModeHolder.getInstance().getDefaultFactoryFor(((RootSeqSymmetry) modelSym).getCategory());
							style.setViewMode(factory.getName());
						}
					}
				}
			}
			setViewModeGlyph(factory.getViewModeGlyph(modelSym, style, getDirection(), smv));
		}
		this.style = style;
	}

	private MapViewGlyphFactoryI getViewGlyphFactory(String viewMode) {
		if ("default".equals(viewMode) && modelSym != null) {
			return MapViewModeHolder.getInstance().getDefaultFactoryFor(((RootSeqSymmetry)modelSym).getCategory());
		}
		return MapViewModeHolder.getInstance().getViewFactory(viewMode);
	}

	private Operator getOperator(String operator){
		return TransformHolder.getInstance().getOperator(operator);
	}

	private boolean isSymLoaded() {
		if (ignoreUnloaded) {
			return true;
		}
		if (modelSym == null) {
			return false;
		}
		boolean loaded = false;
		BioSeq seq = smv.getAnnotatedSeq();
		if (seq != null) {
			for (int i = 0; i < seq.getAnnotationCount(); i++) {
				if (modelSym.equals(seq.getAnnotation(i))) {
					loaded = true;
					break;
				}
			}
		}
		return loaded;
	}

	public void reset(){
		setStyle(this.style, viewModeGlyph.getChildCount() == 0);
	}

	private void setStyle(ITrackStyleExtended style, boolean force){
		ITrackStyleExtended saveStyle = this.style;
		this.style = style;
		if(!isSymLoaded()){
			if (force) {
				MapViewGlyphFactoryI factory = MapViewModeHolder.getInstance().getAutoloadFactory(style.getMethodName());
				setViewModeGlyph(factory.getViewModeGlyph(modelSym, style, getDirection(), smv));
			}
		}
		else if (force || saveStyle == null || !saveStyle.getViewMode().equals(style.getViewMode())) {
			setViewModeGlyph(style);
		}
		else {
			viewModeGlyph.setStyle(style);
		}
	}

	public Rectangle2D.Double getTierCoordBox()   {
		return super.getCoordBox();
	}

	/**
	 * Make the glyph a floating glyph.
	 * Note - the viewModeGlyph will leave its tierGlyph pointing to this glyph.
	 * @param floater the PixelFloaterGlyph
	 */
	public void enfloat(Glyph floater, NeoMap map) {
		Rectangle2D.Double cbox = map.getCoordBounds();
		viewModeGlyph.setCoordBox(new Rectangle2D.Double(cbox.x, style.getY(), cbox.width, style.getHeight())); // viewModeGlyph does not have same CoordBox as TierGlyph
		viewModeGlyph.setVisibility(true);
		GraphGlyphUtils.checkPixelBounds(viewModeGlyph, map);
		floater.setCoords(cbox.x, 0, cbox.width, 0);
		floater.addChild(viewModeGlyph);
		// replace viewModeGlyph with dummy, so that it won't show as a regular glyph in the track
		viewModeGlyph = DummyGlyphFactory.getInstance().getViewModeGlyph(modelSym, style, getDirection(), smv);
		viewModeGlyph.setTierGlyph(this);
		viewModeGlyph.setCoordBox(super.getCoordBox());
		setVisibility(false);
	}

	/**
	 * make the glyph a regular (nonfloating) glyph
	 * @param floater the PixelFloaterGlyph
	 * @param floatingGlyph the glyph
	 */
	public void defloat(Glyph floater, ViewModeGlyph floatingGlyph) {
		floater.removeChild(floatingGlyph);
		viewModeGlyph = floatingGlyph;
		viewModeGlyph.setCoordBox(super.getCoordBox());
	}

	/**
	 * Make the viewModeGlyph a joined glyph.
	 * Note - the viewModeGlyph will leave its tierGlyph pointing to this glyph.
	 * @param comboGlyph the ComboGlyph
	 */
	public void enjoin(ViewModeGlyph comboGlyph, NeoMap map) {
		viewModeGlyph.setVisibility(true);
		comboGlyph.addChild(viewModeGlyph);
		// replace viewModeGlyph with dummy, so that it won't show as a regular glyph in the track
		viewModeGlyph = DummyGlyphFactory.getInstance().getViewModeGlyph(modelSym, style, getDirection(), smv);
		viewModeGlyph.setTierGlyph(this);
		viewModeGlyph.setCoordBox(super.getCoordBox());
		setVisibility(false);
	}

	/**
	 * Make the glyph a regular (nonjoined) glyph.
	 * @param comboGlyph the comboGlyph
	 * @param joinedGlyph the glyph
	 */
	public void dejoin(ViewModeGlyph comboGlyph, ViewModeGlyph joinedGlyph) {
		comboGlyph.removeChild(joinedGlyph);
		viewModeGlyph = joinedGlyph;
		viewModeGlyph.setCoordBox(super.getCoordBox());
	}

	public void setStyle(ITrackStyleExtended style) {
		setStyle(style, false);
	}

	public void setFillColor(Color col) {
		viewModeGlyph.setFillColor(col);
	}

	public int getActualSlots(){
		return viewModeGlyph.getActualSlots();
	}

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
	public int getSlotsNeeded(ViewI theView) {
		return viewModeGlyph.getSlotsNeeded(theView);
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
		return viewModeGlyph == null ? 0 : viewModeGlyph.getChildCount();
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
		viewModeGlyph.drawMiddle(view);
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
	public void pack(ViewI view) {
		if (viewModeGlyph.getParent() == null) {
			viewModeGlyph.setParent(super.getParent());
		}
		viewModeGlyph.pack(view);
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
		super.setCoordBox(coordbox);
		viewModeGlyph.setCoordBox(coordbox);
	}
	@Override
	public void setCoords(double x, double y, double width, double height)  {
		super.setCoords(x, y, width, height);
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
		this.modelSym = (SeqSymmetry)info;
		setViewModeGlyph(this.style);
		//viewModeGlyph.setInfo(info); // Let factory setInfo for viewModeGlyph : HV 02/17/12
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
		super.setParent(glyph);
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
	public ITrackStyleExtended getAnnotStyle() {
		return viewModeGlyph.getAnnotStyle();
	}
	public Color getFillColor() {
		return viewModeGlyph.getFillColor();
	}
	public Direction getDirection() {
		return direction;
	}
	public String getLabel() {
		return viewModeGlyph.getLabel();
	}
	public void addMiddleGlyph(GlyphI gl) {
		viewModeGlyph.addMiddleGlyph(gl);
	}
	public void setCollapsedPacker(CollapsePacker packer) {
		System.out.println("!!!!!!!!! setCollapsedPacker(CollapsePacker packer)");
	}
	public void setDirection(Direction d) {
		this.direction = d;
		viewModeGlyph.setDirection(d);
	}
	public void setExpandedPacker(FasterExpandPacker packer) {
		System.out.println("!!!!!!!!! setExpandedPacker(FasterExpandPacker packer)");
	}
	public void setLabel(String str) {
		viewModeGlyph.setLabel(str);
	}
	public List<SeqSymmetry> getSelected(){
		return viewModeGlyph.getSelected();
	}
	@Override
	public void drawChildren(ViewI view) {
		System.out.println("!!!!!!!!! drawChildren(ViewI view)");
/////		viewModeGlyph.drawChildren(view);
	}
	public void resizeHeight(double top, double height) {
		Rectangle2D.Double cbox = getCoordBox();
		setCoords(cbox.x, top, cbox.width, height);
	}
	
	public boolean isManuallyResizable() {
		return this.viewModeGlyph.isManuallyResizable();
	}

	public void setIgnoreUnloaded(boolean ignoreUnloaded) {
		this.ignoreUnloaded = ignoreUnloaded;
	}

	@Override
	public String toString() {
		return "viewModeGlyph=" + (viewModeGlyph == null ? "null" : viewModeGlyph.getClass().getSimpleName()) + (viewModeGlyph.getChildCount() == 0 ? "###>" : " ---> ") + ";direction=" + getDirection() + ";style=" + (style == null ? "null" : style.toString());
	}
}
