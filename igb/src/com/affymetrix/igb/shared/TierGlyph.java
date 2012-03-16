package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.comparator.GlyphMinXComparator;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.AbstractCoordPacker;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.tieredmap.PaddedPackerI;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.*;
import java.awt.geom.Rectangle2D;

/**
 *  TierGlyph is intended for use with AffyTieredMap.
 *  Each tier in the TieredNeoMap is implemented as a TierGlyph, which can have different
 *  states as indicated below.
 *  In a AffyTieredMap, TierGlyphs pack relative to each other but not to other glyphs added
 *  directly to the map.
 *
 */
public class TierGlyph extends SolidGlyph {
	// extending solid glyph to inherit hit methods (though end up setting as not hitable by default...)
	private static final float default_trans = 0.5f;
    private static final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC, default_trans);

	private boolean sorted = true;
	private static final Comparator<GlyphI> child_sorter = new GlyphMinXComparator();
	private Direction direction = Direction.NONE;
	/** glyphs to be drawn in the "middleground" --
	 *    in front of the solid background, but behind the child glyphs
	 *    For example, to indicate how much of the xcoord range has been covered by feature retrieval attempts
	 */
	private final List<GlyphI> middle_glyphs = new ArrayList<GlyphI>();

	public static enum Direction {

		FORWARD, NONE, REVERSE, BOTH, AXIS
	};
	/** A property for the IAnnotStyle.getTransientPropertyMap().  If set to
	 *  Boolean.TRUE, the tier will draw a label next to where the handle
	 *  would be.
	 *  Note: You probably do NOT want the TierGlyph to draw a label and for the
	 *  included GraphGlyph to also draw a label.
	 */
	private static final String SHOW_TIER_LABELS_PROPERTY = "Show Track Labels";
	/** A property for the IAnnotStyle.getTransientPropertyMap().  If set to
	 *  Boolean.TRUE, the tier will draw a handle on the left side.
	 *  Note: You probably do NOT want the TierGlyph to draw a handle and for the
	 *  included GraphGlyph to also draw a handle.
	 */
	private static final String SHOW_TIER_HANDLES_PROPERTY = "Show Track Handles";
	private double spacer = 2;

	/*
	 * other_fill_color is derived from fill_color whenever setFillColor() is called.
	 * if there are any "middle" glyphs, then background is drawn with other_fill_color and
	 *    middle glyphs are drawn with fill_color
	 * if no "middle" glyphs, then background is drawn with fill_color
	 */
	private Color other_fill_color = null;
	private String label = null;
	private static final Font default_font = NeoConstants.default_plain_font;
	private FasterExpandPacker expand_packer = new FasterExpandPacker();
	private CollapsePacker collapse_packer = new CollapsePacker();
	private List<GlyphI> max_child_sofar = null;
	private static final int handle_width = 10;  // width of handle in pixels
	protected ITrackStyleExtended style;

	public TierGlyph() {
	}

	public TierGlyph(ITrackStyleExtended style) {
		setHitable(false);
		setSpacer(spacer);
		setStyle(style);
	}

	public void setStyle(ITrackStyleExtended style) {
		this.style = style;
		// most tier glyphs ignore their foreground color, but AffyTieredLabelMap copies
		// the fg color to the TierLabel glyph, which does pay attention to that color.
		setForegroundColor(style.getForeground());
		setFillColor(style.getBackground());

		if (style.getCollapsed()) {
			setPacker(collapse_packer);
		} else {
			setPacker(expand_packer);
		}
		setVisibility(!style.getShow());
		setMaxExpandDepth(style.getMaxDepth());
		setLabel(style.getTrackName());
	}

	public ITrackStyleExtended getAnnotStyle() {
		return style;
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
	public void addMiddleGlyph(GlyphI gl) {
		middle_glyphs.add(gl);
	}

	private void initForSearching() {
		int child_count = getChildCount();
		if (child_count > 0) {
			sortChildren(true);  // forcing sort
			//    sortChildren(false); // not forcing sort (relying on sorted field instead...)

			// now construct the max list, which is:
			//   for each entry in min sorted children list, the maximum max
			//     value up to (and including) that position
			// could do max list as int array or as symmetry list, for now doing symmetry list
			max_child_sofar = new ArrayList<GlyphI>(child_count);
			GlyphI curMaxChild = getChild(0);
			Rectangle2D.Double curbox = curMaxChild.getCoordBox();
			double max = curbox.x + curbox.width;
			for (int i = 0; i < child_count; i++) {
				GlyphI child = this.getChild(i);
				curbox = child.getCoordBox();
				double newmax = curbox.x + curbox.width;
				if (newmax > max) {
					curMaxChild = child;
					max = newmax;
				}
				max_child_sofar.add(curMaxChild);
			}
		} else {
			max_child_sofar = null;
		}

	}

	@Override
	public void addChild(GlyphI glyph, int position) {
		throw new RuntimeException("TierGlyph.addChild(glyph, position) not allowed, "
				+ "use TierGlyph.addChild(glyph) instead");
	}

	// overriding addChild() to keep track of whether children are sorted
	//    by ascending min
	@Override
	public void addChild(GlyphI glyph) {
		int count = this.getChildCount();
		if (count <= 0) {
			sorted = true;
		} else if (glyph.getCoordBox().x < this.getChild(count - 1).getCoordBox().x) {
			sorted = false;
		}
		super.addChild(glyph);
	}

	private void sortChildren(boolean force) {
		int child_count = this.getChildCount();
		if (((!sorted) || force) && (child_count > 0)) {
			// make sure child symmetries are sorted by ascending min along search_seq
			// to avoid unecessary sort, first go through child list and see if it's
			//     already in ascending order -- if so, then no need to sort
			//     (not sure if this is necessary -- Collections.sort() may already
			//        be optimized to catch this case)
			sorted = true;
			//      int prev_min = Integer.MIN_VALUE;
			double prev_min = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < child_count; i++) {
				GlyphI child = getChild(i);
				double min = child.getCoordBox().x;
				if (prev_min > min) {
					sorted = false;
					break;
				}
				prev_min = min;
			}
			if (!sorted) {
				Collections.sort(getChildren(), child_sorter);
			}
		}
		sorted = true;
	}

	public void setLabel(String str) {
		label = str;
	}

	public String getLabel() {
		return label;
	}

	// overriding pack to ensure that tier is always the full width of the scene
	@Override
	public void pack(ViewI view, boolean manual) {
		initForSearching();
		setMaxExpandDepth(style.getMaxDepth());
		super.pack(view, manual);
		Rectangle2D.Double mbox = getScene().getCoordBox();
		Rectangle2D.Double cbox = this.getCoordBox();

		if (shouldDrawLabel()) {
			// Add extra space to make room for the label.

			// Although the space SHOULD be computed based on font metrics, etc,
			// that doesn't really work any better than a fixed coord value
			this.setCoords(mbox.x, cbox.y - 6, mbox.width, cbox.height + 6);
		} else {
			this.setCoords(mbox.x, cbox.y, mbox.width, cbox.height);
		}
	}

	/**
	 *  Overridden to allow background shading by a collection of non-child
	 *    "middleground" glyphs.  These are rendered after the solid background but before
	 *    all of the children (which could be considered the "foreground").
	 */
	@Override
	public void draw(ViewI view) {
		view.transformToPixels(getCoordBox(), getPixelBox());

		getPixelBox().width = Math.max(getPixelBox().width, getMinPixelsWidth());
		getPixelBox().height = Math.max(getPixelBox().height, getMinPixelsHeight());

		Graphics g = view.getGraphics();
		Rectangle vbox = view.getPixelBox();
		setPixelBox(getPixelBox().intersection(vbox));

		if (middle_glyphs.isEmpty()) { // no middle glyphs, so use fill color to fill entire tier
			if (style.getBackground() != null) {
				g.setColor(style.getBackground());
				//Hack : Add one to height to resolve black line bug.
				g.fillRect(getPixelBox().x, getPixelBox().y, getPixelBox().width, getPixelBox().height+1);
			}
		} else {
			if (style.getBackground() != null) {
				g.setColor(style.getBackground());
				//Hack : Add one to height to resolve black line bug.
				g.fillRect(getPixelBox().x, getPixelBox().y, 2 * getPixelBox().width, getPixelBox().height+1);
			}

			// cycle through "middleground" glyphs,
			//   make sure their coord box y and height are set to same as TierGlyph,
			//   then call mglyph.draw(view)
			// TODO: This will draw middle glyphs on the Whole Genome, which appears to cause problems due to coordinates vs. pixels
			// See bug 3032785
			if(other_fill_color != null){
				for (GlyphI mglyph : middle_glyphs) {
					Rectangle2D.Double mbox = mglyph.getCoordBox();
					mbox.setRect(mbox.x, getCoordBox().y, mbox.width, getCoordBox().height);
					mglyph.setColor(other_fill_color);
					mglyph.drawTraversal(view);
				}
			}
		}

		if (!style.isGraphTier()) {
			// graph tiers take care of drawing their own handles and labels.
			if (shouldDrawLabel()) {
				drawLabelLeft(view);
			}
			if (Boolean.TRUE.equals(style.getTransientPropertyMap().get(SHOW_TIER_HANDLES_PROPERTY))) {
				drawHandle(view);
			}
		}


		super.draw(view);
	}

	@Override
	public void drawChildren(ViewI view) {
		try{
		if (getChildren() != null) {
			GlyphI child;
			int numChildren = getChildren().size();
			for (int i = 0; i < numChildren; i++) {
				child = getChildren().get(i);
				// TransientGlyphs are usually NOT drawn in standard drawTraversal
				if (!(child instanceof TransientGlyph) || drawTransients()) {
					if (child.isOverlapped()) {
						Graphics2D g = view.getGraphics();
						Composite dac = g.getComposite();
						g.setComposite(ac);
						child.drawTraversal(view);
						g.setComposite(dac);
					} else {
						child.drawTraversal(view);
					}
				}
			}
		}

		}catch(Exception ex){
			System.out.println(ex);
		}
	}

	private boolean shouldDrawLabel() {
		// graph tiers take care of drawing their own handles and labels.
		return (!style.isGraphTier() && Boolean.TRUE.equals(style.getTransientPropertyMap().get(SHOW_TIER_LABELS_PROPERTY)));
	}

	private void drawLabelLeft(ViewI view) {
		if (getLabel() == null) {
			return;
		}
		Rectangle hpix = calcHandlePix(view);
		if (hpix != null) {
			Graphics g = view.getGraphics();
			g.setFont(default_font);
			FontMetrics fm = g.getFontMetrics();
			g.setColor(this.getColor());
			g.drawString(getLabel(), (hpix.x + hpix.width + 1), (hpix.y + fm.getMaxAscent() - 1));
		}
	}

	private Rectangle calcHandlePix(ViewI view) {
		// could cache pixelbox of handle, but then will have problems if try to
		//    have multiple views on same scene / glyph hierarchy
		// therefore reconstructing handle pixel bounds here... (although reusing same object to
		//    cut down on object creation)

		// if full view differs from current view, and current view doesn't left align with full view,
		//   don't draw handle (only want handle at left side of full view)
		if (view.getFullView().getCoordBox().x != view.getCoordBox().x) {
			return null;
		}
		view.transformToPixels(getCoordBox(), getPixelBox());
		Rectangle view_pixbox = view.getPixelBox();
		int xbeg = Math.max(view_pixbox.x, getPixelBox().x);
		Graphics g = view.getGraphics();
		g.setFont(default_font);

		Rectangle handle_pixbox = new Rectangle();
		FontMetrics fm = g.getFontMetrics();
		int h = Math.min(fm.getMaxAscent(), getPixelBox().height);
		handle_pixbox.setBounds(xbeg, getPixelBox().y, handle_width, h);
		return handle_pixbox;
	}

	private void drawHandle(ViewI view) {
		Rectangle hpix = calcHandlePix(view);
		if (hpix != null) {
			Graphics g = view.getGraphics();
			Color c = new Color(style.getForeground().getRed(), style.getForeground().getGreen(), style.getForeground().getBlue(), 64);
			g.setColor(c);
			g.fillRect(hpix.x, hpix.y, hpix.width, hpix.height);
			g.drawRect(hpix.x, hpix.y, hpix.width, hpix.height);
		}
	}

	/**
	 *  Remove all children of the glyph, including those added with
	 *  addMiddleGlyph(GlyphI).
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

	/** Sets the expand packer.  Note that you are responsible for setting
	 *  any properties of the packer, such as those based on the AnnotStyle.
	 */
	public void setExpandedPacker(FasterExpandPacker packer) {
		this.expand_packer = packer;
		setSpacer(getSpacer());
		setStyle(getAnnotStyle()); // make sure the correct packer is used, and that its properties are set
	}

	public void setCollapsedPacker(CollapsePacker packer) {
		this.collapse_packer = packer;
		setSpacer(getSpacer());
		setStyle(getAnnotStyle()); // make sure the correct packer is used, and that its properties are set
	}

	private void setSpacer(double spacer) {
		this.spacer = spacer;
		((PaddedPackerI) collapse_packer).setParentSpacer(spacer);
		((PaddedPackerI) expand_packer).setParentSpacer(spacer);
	}

	private double getSpacer() {
		return spacer;
	}

	/** Sets the color used to fill the tier background, or null if no color
	 *  @param col  A color, or null if no background color is desired.
	 */
	public void setFillColor(Color col) {
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

	// very, very deprecated
	@Override
	public Color getColor() {
		return getForegroundColor();
	}

	// very, very deprecated
	@Override
	public void setColor(Color c) {
		setForegroundColor(c);
	}

	/** Returns the color used to draw the tier background, or null
	if there is no background. */
	public Color getFillColor() {
		return style.getBackground();
	}

	@Override
	public void setForegroundColor(Color color) {
		if (style.getForeground() != color) {
			style.setForeground(color);
		}
	}

	@Override
	public Color getForegroundColor() {
		return style.getForeground();
	}

	@Override
	public void setBackgroundColor(Color color) {
		setFillColor(color);
	}

	@Override
	public Color getBackgroundColor() {
		return getFillColor();
	}

	public Direction getDirection() {
		return direction;
	}

	/**
	 * Sets direction.
	 * @param d use one of the constants:
	 *  Direction.FORWARD, Direction.REVERSE,
	 *  Direction.BOTH, or Direction.NONE.
	 */
	public void setDirection(Direction d) {
		this.direction = d;
	}

	/** Changes the maximum depth of the expanded packer.
	 *  This does not call pack() afterwards.
	 */
	private void setMaxExpandDepth(int max) {
		expand_packer.setMaxSlots(max);
	}

	public int getSlotsNeeded(ViewI theView) {
		if(getPacker() == expand_packer) {
			return expand_packer.getSlotsNeeded(this, theView);
		}
		return 1;
	}
	public int getActualSlots(){
		if(getPacker() == expand_packer)
			return expand_packer.getActualSlots();
		return 1;
	}

	private double getSpacing() {
		if(getPacker() instanceof AbstractCoordPacker){
			return ((AbstractCoordPacker)getPacker()).getSpacing();
		}
		return 2;
	}

	/**
	 * Set the preferred height for a tier.
	 * @param height new height in scene (coord) space.
	 * @param view onto the scene with these coordinates (units).
	 */
	public void setPreferredHeight(double height, ViewI view){
		if(getChildCount() == 1 && getChild(0) instanceof AbstractGraphGlyph){
			AbstractGraphGlyph child = (AbstractGraphGlyph)getChild(0);
			Rectangle2D.Double  coord = child.getCoordBox();
			child.setCoords(coord.x, coord.y, coord.width, height);
			//Note : Fix to handle height in a view mode.
			// But this also causes minor change in height while switching back to default view mode.
			setCoords(coord.x, coord.y, coord.width, height + 2 * getSpacing());
			return;
		}
        height = height - 2 * getSpacing(); // remove the padding at top and bottom.
		int numberOfSlotsInUse = getActualSlots();
		double totalInteriorSpacing = (numberOfSlotsInUse - 1) * getSpacing();
		double newSlotHeight = (height - totalInteriorSpacing)/numberOfSlotsInUse;
		
		if(useLabel()) {
			newSlotHeight = newSlotHeight / 2; // Hiral says: because annotGlyphFactory multiplies by 2 when labeled.
		}

		double scale;
		switch (this.direction) {
			case FORWARD:
				scale = newSlotHeight/style.getForwardHeight();
				style.setForwardHeight(newSlotHeight);
				break;
			case REVERSE:
				scale = newSlotHeight/style.getReverseHeight();
				style.setReverseHeight(newSlotHeight);
				break;
			default:
			case BOTH:
			case NONE:
			case AXIS:
				scale = newSlotHeight/style.getHeight();
				style.setHeight(newSlotHeight);
		}

		scaleChildHeights(scale, getChildren(), view);
	}

	private static void scaleChildHeights(double theScale, List<GlyphI> theSiblings, ViewI theView) {
		int numberOfSiblings = theSiblings.size();
		GlyphI child;
		Rectangle2D.Double coordbox;
		for (int i = 0; i < numberOfSiblings; i++) {
			child =  theSiblings.get(i);
			coordbox = child.getCoordBox();
			child.setCoords(coordbox.x, 0, coordbox.width, coordbox.height * theScale);
			if (0 < child.getChildCount()) {
				// The above test is needed as of 2011-03-01
				// because child.getChildren() returns null instead of an empty list.
				scaleChildHeights(theScale, child.getChildren(), theView);
			}
			child.pack(theView, false);
		}
	}

	private boolean useLabel() {
		String label_field = style.getLabelField();
		boolean use_label = label_field != null && (label_field.trim().length() > 0);
		if (!style.isGraphTier() && use_label) {
			return true;
		}

		return false;
	}

	/** Not implemented.  Will behave the same as drawSelectedOutline(ViewI). */
	@Override
	protected void drawSelectedFill(ViewI view) {
		this.drawSelectedOutline(view);
	}

	/** Not implemented.  Will behave the same as drawSelectedOutline(ViewI). */
	@Override
	protected void drawSelectedReverse(ViewI view) {
		this.drawSelectedOutline(view);
	}
}
