package com.affymetrix.igb.view.factories;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.comparator.GlyphMinXComparator;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.tieredmap.PaddedPackerI;
import com.affymetrix.igb.shared.CollapsePacker;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.ITransformableTierGlyph;
import com.affymetrix.igb.shared.ScrollableFasterExpandPacker;
import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * copy / modification of TierGlyph for ViewModeGlyph for annotations
 */
public class ScrollableAnnotationTierGlyph extends AbstractTransformTierGlyph implements ITransformableTierGlyph {

    private static final int MAX_EXPAND = 0;
    private static final float default_trans = 0.5f;
    private static final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC, default_trans);

    private boolean sorted = true;
    private static final Comparator<GlyphI> child_sorter = new GlyphMinXComparator();

    /**
     * A property for the IAnnotStyle.getTransientPropertyMap(). If set to
     * Boolean.TRUE, the tier will draw a label next to where the handle would
     * be.
     */
    private static final String SHOW_TIER_LABELS_PROPERTY = "Show Track Labels";
    /**
     * A property for the IAnnotStyle.getTransientPropertyMap(). If set to
     * Boolean.TRUE, the tier will draw a handle on the left side.
     */
    private static final String SHOW_TIER_HANDLES_PROPERTY = "Show Track Handles";
    private double spacer = 2;

    private static final int BUFFER = 50;
    private static final Font default_font = NeoConstants.default_plain_font;
    private FasterExpandPacker expand_packer = new ScrollableFasterExpandPacker();
    private CollapsePacker collapse_packer = new CollapsePacker();
    private List<GlyphI> max_child_sofar = null;
    private static final int handle_width = 10;  // width of handle in pixels

    // Variable for scrolling in tier
    private int offset = 1;
    private float scale = 1.0f;
    private Rectangle lower_pixelbox = new Rectangle();
    private Rectangle upper_pixelbox = new Rectangle();
    private Rectangle child_temp = new Rectangle();

    public ScrollableAnnotationTierGlyph(ITrackStyleExtended style) {
        super(style);
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
            max_child_sofar = new ArrayList<>(child_count);
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

    /**
     * Overriding addChild() to keep track of whether children are sorted by
     * ascending min
     */
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

    /**
     * Overriding to ensure that tier is always the full width of the scene.
     */
    @Override
    public void pack(ViewI view) {
        initForSearching();
        switch (this.direction) {
            case FORWARD:
                setMaxExpandDepth(style.getForwardMaxDepth());
                break;
            case REVERSE:
                setMaxExpandDepth(style.getReverseMaxDepth());
                break;
            default:
                setMaxExpandDepth(style.getMaxDepth());
        }
        super.pack(view);
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
        setInitialOffset();
    }

    /**
     * Overridden to allow background shading by a collection of non-child
     * "middle ground" glyphs. These are rendered after the solid background but
     * before all of the children (which could be considered the "foreground").
     */
    @Override
    public void draw(ViewI view) {
        //drawMiddle(view);

        if (shouldDrawLabel()) {
            drawLabelLeft(view);
        }
        if (Boolean.TRUE.equals(style.getTransientPropertyMap().get(SHOW_TIER_HANDLES_PROPERTY))) {
            drawHandle(view);
        }

        super.draw(view);
    }

    @Override
    public void setScale(float scale) {
        this.scale = scale;
        tier_transform.setTransform(tier_transform.getScaleX(), 0, 0,
                scale, tier_transform.getTranslateX(), tier_transform.getTranslateY());
    }

    @Override
    public float getScale() {
        return scale;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
        tier_transform.setTransform(tier_transform.getScaleX(), 0, 0,
                tier_transform.getScaleY(), tier_transform.getTranslateX(), offset);
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public boolean isScrollingAllowed() {
        if (getPacker() != expand_packer) {
            return false;
        }

        return true;
    }

    @Override
    protected void setModifiedViewCoords(ViewI view) {
        Rectangle temp_width = new Rectangle();
        view.transformToPixels(new Rectangle2D.Double(0, 0, 0, BUFFER), temp_width);
        Rectangle temp = new Rectangle(this.getPixelBox().x,
                this.getPixelBox().y + temp_width.height, this.getPixelBox().width,
                this.getPixelBox().height - 2 * temp_width.height);
        view.transformToCoords(temp, modified_view_coordbox);
    }

    private void setInitialOffset() {
        int coord_offset = 0;
        if (isScrollingAllowed()) {
            coord_offset = (int) (BUFFER * 1.5);
            if (getDirection() != StyledGlyph.Direction.REVERSE) {
                if (getInitialRowsToScroll() > 0) {
                    coord_offset = (int) getChildHeight() * (getInitialRowsToScroll() - 1) + coord_offset;
                }
                coord_offset = -coord_offset;
                //int pixel_offset = (int) (view.getTransform().getScaleY() * coord_offset);
            }
        }
        setOffset(coord_offset);
    }

    private int getInitialRowsToScroll() {
        if (getStyleDepth() == MAX_EXPAND) {
            return MAX_EXPAND;
        }

        return getActualSlots() - getStyleDepth();
    }

    @Override
    public void drawChildren(ViewI view) {
        if (isScrollingAllowed()) {
            // Convert the upper and lower coords to pixelbox before modifying the view
            view.transformToPixels(new Rectangle2D.Double(getCoordBox().x,
                    getCoordBox().y, getCoordBox().width, BUFFER), upper_pixelbox);
            view.transformToPixels(new Rectangle2D.Double(getCoordBox().x,
                    getCoordBox().y + getCoordBox().height - BUFFER,
                    getCoordBox().width, BUFFER), lower_pixelbox);

            // Find the intersection
            upper_pixelbox = upper_pixelbox.intersection(view.getPixelBox());
            lower_pixelbox = lower_pixelbox.intersection(view.getPixelBox());
        } else {
            upper_pixelbox.setBounds(0, 0, 0, 0);
            lower_pixelbox.setBounds(0, 0, 0, 0);
        }
        super.drawChildren(view);
    }

    @Override
    protected void superDrawChildren(ViewI view) {
        try {
            if (getChildren() != null) {
                GlyphI child;
                int numChildren = getChildren().size();
                for (int i = 0; i < numChildren; i++) {
                    child = getChildren().get(i);
                    // TransientGlyphs are usually NOT drawn in standard drawTraversal
                    if (!(child instanceof TransientGlyph) || drawTransients()) {
                        view.transformToPixels(child.getCoordBox(), child_temp);
                        if (child_temp.width == 0) {
                            child_temp.width = 1;
                        }
                        if (child_temp.intersects(lower_pixelbox) || child_temp.intersects(upper_pixelbox)) {
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

        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    private boolean shouldDrawLabel() {
        return (Boolean.TRUE.equals(style.getTransientPropertyMap().get(SHOW_TIER_LABELS_PROPERTY)));
    }

    private void drawLabelLeft(ViewI view) {
        if (style.getTrackName() == null) {
            return;
        }
        Rectangle hpix = calcHandlePix(view);
        if (hpix != null) {
            Graphics g = view.getGraphics();
            g.setFont(default_font);
            FontMetrics fm = g.getFontMetrics();
            g.setColor(this.getColor());
            g.drawString(style.getTrackName(), (hpix.x + hpix.width + 1), (hpix.y + fm.getMaxAscent() - 1));
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

    private void setSpacer(double spacer) {
        this.spacer = spacer;
        ((PaddedPackerI) collapse_packer).setParentSpacer(spacer);
        ((PaddedPackerI) expand_packer).setParentSpacer(spacer);
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

    @Override
    public void setDirection(Direction d) {
        super.setDirection(d);
        if (direction != Direction.REVERSE) {
            expand_packer.setMoveType(NeoConstants.UP);
        }
    }

}
