package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.AbstractCoordPacker;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.action.SetSummaryThresholdAction;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 *  !!! the TierGlyph and ViewModeGlyph and all its subordinate ViewModeGlyphs should
 *  all contain the same instance of CoordBox. !!!
 *  This is the glyph that displays the contents of a Tier/Track. Each TierGlyph
 *  contains a ViewModeGlyph and delegates all calls to the ViewModeGlyph.
 */
public abstract class ViewModeGlyph extends SolidGlyph implements NeoRangeListener {
	private TierGlyph tierGlyph;
	SwingWorker previousWorker, worker;
	protected ITrackStyleExtended style;
	protected Direction direction = Direction.NONE;
	private static final int handle_width = 10;  // width of handle in pixels
	private final Rectangle pixel_hitbox = new Rectangle();  // caching rect for hit detection
	/** glyphs to be drawn in the "middleground" --
	 *    in front of the solid background, but behind the child glyphs
	 *    For example, to indicate how much of the xcoord range has been covered by feature retrieval attempts
	 */
	protected final List<GlyphI> middle_glyphs = new ArrayList<GlyphI>();
	/*
	 * other_fill_color is derived from fill_color whenever setFillColor() is called.
	 * if there are any "middle" glyphs, then background is drawn with other_fill_color and
	 *    middle glyphs are drawn with fill_color
	 * if no "middle" glyphs, then background is drawn with fill_color
	 */
	protected Color other_fill_color = null;
	
	protected String label = null;
	
	public abstract void setPreferredHeight(double height, ViewI view);
	public abstract int getActualSlots();
	public abstract void setStyle(ITrackStyleExtended style);
	public abstract String getLabel();
	public abstract void setLabel(String str);
	public abstract void setDirection(Direction d);
	public abstract void drawMiddle(ViewI view);
	public abstract Map<String,Class<?>> getPreferences();
	public abstract void setPreferences(Map<String,Object> preferences);
	public abstract boolean initUnloaded();
	protected abstract boolean shouldDrawToolBar();
	protected abstract RootSeqSymmetry loadRegion(SeqSpan span);
	protected abstract void updateParent(ViewModeGlyph vmg);
	
	/**
	 * Remove all children of the glyph,
	 * including those added with addMiddleGlyph(GlyphI).
	 */
	@Override
	public void removeAllChildren() {
		super.removeAllChildren();
		// also remove all middleground glyphs
		// this is currently the only place where middleground glyphs are treated as if they were children
		//   maybe should rename this method clear() or something like that...
		// only reference to middle glyphs should be in this.middle_glyphs, so should be able to GC them by
		//     clearing middle_glyphs.  These glyphs never have setScene() called on them,
		//     so it is not necessary to call setScene(null) on them.
		middle_glyphs.clear();
	}
	
	protected boolean isAutoLoadMode() {
		if (this.getAnnotStyle() == null) {
			return false;
		}

		if (this.getAnnotStyle().getFeature() == null) {
			return false;
		}

		if (this.getAnnotStyle().getFeature().getLoadStrategy() != LoadUtils.LoadStrategy.AUTOLOAD) {
			return false;
		}
		return true;
	}
		
	public boolean isDetail(ViewI view) {
		return SetSummaryThresholdAction.getAction().isDetail(getAnnotStyle());
	}
	
	@Override
	public void rangeChanged(NeoRangeEvent evt){
		if(evt.getSource() instanceof SeqMapViewExtendedI){
			rangeChanged(((SeqMapViewExtendedI)evt.getSource()));
		}
	}
	
	protected void rangeChanged(SeqMapViewExtendedI smv){
		if(isAutoLoadMode() && isDetail(smv.getSeqMap().getView())){
			try {
				MapViewGlyphFactoryI factory = MapViewModeHolder.getInstance().getViewFactory(getAnnotStyle().getViewMode());
				if(factory != null){
					loadAndDisplayRegion(smv, factory);
				}
			} catch (Exception ex) {
				Logger.getLogger(AbstractViewModeGlyph.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	};
	
	/** Returns the color used to draw the tier background, or null
	if there is no background. */
	public final Color getFillColor() {
		return style.getBackground();
	}
	
	/** Sets the color used to fill the tier background, or null if no color
	 *  @param col  A color, or null if no background color is desired.
	 */
	public final void setFillColor(Color col) {
		if (style.getBackground() != col) {
			style.setBackground(col);
		}

		// Now set the "middleground" color based on the fill color
		if (col == null) {
			other_fill_color = Color.DARK_GRAY;
		} else {
			int intensity = col.getRed() + col.getGreen() + col.getBlue();
			if (intensity == 0) {
				other_fill_color = Color.darkGray;
			} else if (intensity > (255 + 127)) {
				other_fill_color = col.darker();
			} else {
				other_fill_color = col.brighter();
			}
		}
	}
	
	public final ITrackStyleExtended getAnnotStyle() {
		return style;
	}
		
	public final Direction getDirection() {
		return direction;
	}
	
	public final void copyChildren(ViewModeGlyph temp) {
		if(temp == null)
			return;
		
		List<GlyphI> childrens = new ArrayList<GlyphI>();
		childrens.addAll(temp.getChildren());

		for (int i = 0; i < childrens.size(); i++) {
			addChild(childrens.get(i));
		}
		
		childrens.clear();
		childrens.addAll(((AbstractViewModeGlyph)temp).middle_glyphs);
		
		for (int i = 0; i < childrens.size(); i++) {
			addMiddleGlyph(childrens.get(i));
		}
		
		//TODO: Set list of all getInfo
//		if(!(getInfo() instanceof List)){
//			List<Object> info = new ArrayList<Object>();
//			info.add(getInfo());
//			setInfo(info);
//		}else{
//			((List)(getInfo())).add(temp.getInfo());
//		}
		
	}
		
	/**
	 *  Adds "middleground" glyphs, which are drawn in front of the background but
	 *    behind all "real" child glyphs.
	 *  These are generally not considered children of
	 *    the glyph.  The TierGlyph will render these glyphs, but they can't be selected since they
	 *    are not considered children in pickTraversal() method.
	 *  The only way to remove these is via removeAllChildren() method,
	 *    there is currently no external access to them.
	 */
	public final void addMiddleGlyph(GlyphI gl) {
		middle_glyphs.add(gl);
	}
	
	public final List<SeqSymmetry> getSelected(){
	
		int childCount = getChildCount();
		List<SeqSymmetry> selectedSyms = new ArrayList<SeqSymmetry>(childCount);
		for(int i=0;i<childCount;i++){
			if(getChild(i).isSelected()){
				if(getChild(i).getInfo() instanceof SeqSymmetry){
					selectedSyms.add((SeqSymmetry)(getChild(i).getInfo()));
				}
			}
		}
		return selectedSyms;
	}
		
	public final boolean toolBarHit(Rectangle2D.Double coord_hitbox, ViewI view){
		if (shouldDrawToolBar() && isVisible() && coord_hitbox.intersects(getCoordBox())) {
			// overlapping handle ?  (need to do this one in pixel space?)
			Rectangle hpix = new Rectangle();
			view.transformToPixels(coord_hitbox, hpix);
			if (getToolbarPixel(view).intersects(hpix)) {
				return true;
			}
		}
		return false;
	}
	
	protected Rectangle getToolbarPixel(ViewI view){
		pixel_hitbox.setBounds(getPixelBox().x + 4, getPixelBox().y + 4, handle_width, handle_width);
		return pixel_hitbox;
	}
		
	public final double getChildHeight(){
		double child_height = MapViewGlyphFactoryI.DEFAULT_CHILD_HEIGHT;
		child_height = useLabel(getAnnotStyle()) ? child_height * 2 : child_height;
		child_height = child_height + getSpacing() * 2;
		return child_height;
	}
	
	protected final double getSpacing() {
		if(getPacker() instanceof AbstractCoordPacker){
			return ((AbstractCoordPacker)getPacker()).getSpacing();
		}
		return 2;
	}
	
	public final int getStyleDepth(){
		switch(getDirection()){
			case REVERSE:
				return getAnnotStyle().getReverseMaxDepth();
			
			case FORWARD:
				return getAnnotStyle().getForwardMaxDepth();
						
			default:
				return getAnnotStyle().getMaxDepth();
		}
	}
	
	protected final ViewModeGlyph createGlyphs(RootSeqSymmetry rootSym, MapViewGlyphFactoryI factory, SeqMapViewExtendedI smv){
		return factory.getViewModeGlyph(rootSym, style, direction, smv);
	};
	
	protected final void loadAndDisplayRegion(final SeqMapViewExtendedI smv, final MapViewGlyphFactoryI factory) throws Exception{
		if (previousWorker != null && !previousWorker.isCancelled() && !previousWorker.isDone()) {
			previousWorker.cancel(true);
			previousWorker = null;
		}

		worker = new SwingWorker() {

			@Override
			protected Void doInBackground() throws Exception {
				RootSeqSymmetry rootSym = loadRegion(smv.getVisibleSpan());
				if (rootSym.getChildCount() > 0) {
					final ViewModeGlyph vmg = createGlyphs(rootSym, factory, smv);
					ThreadUtils.runOnEventQueue(new Runnable() {

						public void run() {
							updateParent(vmg);
							GeneralLoadUtils.setLastRefreshStatus(style.getFeature(), vmg.getChildCount() > 0);
							GeneralLoadView.getLoadView().refreshDataManagementView();
							//TODO: Find a way to avoid this
							//if (lastUsedGlyph == saveDetailGlyph) {
							smv.repackTheTiers(true, true);
							smv.getSeqMap().updateWidget();
							Application.getSingleton().removeNotLockedUpMsg("Loading " + getAnnotStyle().getTrackName());
							//}
						}
					});
				}else{
					Application.getSingleton().removeNotLockedUpMsg("Loading " + getAnnotStyle().getTrackName());
				}
				return null;
			}

		};
		worker.execute();
		previousWorker = worker;
		worker = null;
	};
	
	public boolean isGarbage() {
		return getChildCount() == 0;
	}
	public boolean isCombo() {
		return false;
	}
	public boolean isPreLoaded() {
		return false;
	}
	public void addSym(SeqSymmetry sym) {}
	public TierGlyph getTierGlyph() {
		return tierGlyph;
	}
	public void setTierGlyph(TierGlyph tierGlyph) {
		this.tierGlyph = tierGlyph;
	}
	public void processParentCoordBox(Rectangle2D.Double parentCoordBox) {
		setCoordBox(parentCoordBox); // so all use the same coordbox
	}
	public int getSlotsNeeded(ViewI theView) {
		return 1;
	}
	public boolean isManuallyResizable() {
		if (this.getPacker() instanceof CollapsePacker) {
			return false;
		}
		return true;
	}
	public final void setMinimumPixelBounds(Graphics g){
		java.awt.FontMetrics fm = g.getFontMetrics();
		int h = fm.getHeight();
		h += 2 * 2; // border height
		h += 4; // padding top
		int w = fm.stringWidth("A Moderate Label");
		w += 2; // border left
		w += 4; // padding left
		java.awt.Dimension minTierSizeInPixels = new java.awt.Dimension(w, h);
		setMinimumPixelBounds(minTierSizeInPixels);
	}
	
	public static boolean useLabel(ITrackStyleExtended style){
		return style.getLabelField() != null && 
		!style.getLabelField().equals(TrackConstants.NO_LABEL) && 
		(style.getLabelField().trim().length() > 0);
	}
}
