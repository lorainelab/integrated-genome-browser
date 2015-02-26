package com.affymetrix.igb.view.factories;

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.comparator.GlyphMinXComparator;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * copy / modification of TierGlyph for ViewModeGlyph for annotations
 */
public class AnnotationTierGlyph extends AbstractTierGlyph {

    private static final int MAX_EXPAND = 0;
    // extending solid glyph to inherit hit methods (though end up setting as not hitable by default...)
    private static final Map<String, Class<?>> PREFERENCES;

    static {
        Map<String, Class<?>> temp = new HashMap<>();
        temp.put("collapsed", Boolean.class);
        temp.put("connected", Boolean.class);
        temp.put("arrow", Boolean.class);
        temp.put("max_depth", Integer.class);
        temp.put("forward_color", Integer.class);
        temp.put("reverse_color", Integer.class);
        PREFERENCES = Collections.unmodifiableMap(temp);
    }
    private static final float default_trans = 0.5f;
    private static final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC, default_trans);

    private boolean sorted = true;
    private static final Comparator<GlyphI> child_sorter = new GlyphMinXComparator();
    private static final int MAX_CHILD_IN_SLOP_ROW = 10;

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

    private static final int BUFFER = 50;
    private static final Font default_font = NeoConstants.default_plain_font;

    //private List<GlyphI> max_child_sofar = null;
    private static final int handle_width = 10;  // width of handle in pixels

    public AnnotationTierGlyph(ITrackStyleExtended style) {
        super(style);
    }

    @Override
    protected RootSeqSymmetry loadRegion(SeqSpan span) {
        RootSeqSymmetry detailSym = new TypeContainerAnnot(style.getMethodName());

        for (Entry<String, List<? extends SeqSymmetry>> entry : loadData(span).entrySet()) {
            entry.getValue().forEach(detailSym::addChild);
        }

        return detailSym;
    }

    @Override
    protected void updateParent(TierGlyph vmg) {
        if (vmg.getChildCount() > 0) {
            copyChildren(vmg);
        }
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
            //max_child_sofar = new ArrayList<GlyphI>(child_count);
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
                //max_child_sofar.add(curMaxChild);
            }
        } else {
            //max_child_sofar = null;
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
    }

    /**
     * Overridden to allow background shading by a collection of non-child
     * "middle ground" glyphs. These are rendered after the solid background but
     * before all of the children (which could be considered the "foreground").
     */
    @Override
    public void draw(ViewI view) {
        if (shouldDrawLabel()) {
            drawLabelLeft(view);
        }
        if (Boolean.TRUE.equals(style.getTransientPropertyMap().get(SHOW_TIER_HANDLES_PROPERTY))) {
            drawHandle(view);
        }

        super.draw(view);
    }

    @Override
    public void drawChildren(ViewI view) {
        try {
            int max_depth = getStyleDepth();
            if (getChildren() != null) {
                GlyphI child;
                int numChildren = getChildren().size();
                for (int i = 0; i < numChildren; i++) {
                    child = getChildren().get(i);
                    // TransientGlyphs are usually NOT drawn in standard drawTraversal
                    if (!(child instanceof TransientGlyph) || drawTransients()) {
                        if (child.isOverlapped()) {
                            if (child.getRowNumber() < max_depth + MAX_CHILD_IN_SLOP_ROW) {
                                Graphics2D g = view.getGraphics();
                                Composite dac = g.getComposite();
                                g.setComposite(ac);
                                child.drawTraversal(view);
                                g.setComposite(dac);
                            }
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

    /**
     * Changes the maximum depth of the expanded packer. This does not call
     * pack() afterwards.
     */
    @Override
    protected void setMaxExpandDepth(int max) {
        expand_packer.setMaxSlots(max);
    }

    @Override
    public int getSlotsNeeded(ViewI theView) {
        if (getPacker() == expand_packer) {
            return expand_packer.getSlotsNeeded(this, theView);
        }
        return 1;
    }

    @Override
    public int getActualSlots() {
        if (getPacker() == expand_packer) {
            return expand_packer.getActualSlots();
        }
        return 1;
    }

    @Override
    protected double getMaxChildHeight() {
        double max = 0;
        int children = this.getChildCount();
        for (int i = 0; i < children; i++) {
            max = Math.max(max, this.getChild(i).getCoordBox().height);
        }
        return max;
    }

    /**
     * Set the preferred height for a tier.
     *
     * @param height new height in scene (coord) space.
     * @param view onto the scene with these coordinates (units).
     */
    @Override
    public void setPreferredHeight(double height, ViewI view) {

        // Remove the padding at top and bottom.
        // Shouldn't we get this info from the packer?
        height -= 2 * getSpacing();

        if (getPacker() == expand_packer) {
            // Now figure out how deep to set max depth.
            // Get current slot height. Should actually get this from the packer.
            double h = this.getMaxChildHeight() + 2 * expand_packer.getSpacing();
            long depth = (long) Math.floor(height / h);
            assert -1 < depth && depth < Integer.MAX_VALUE;
            expand_packer.setMaxSlots((int) depth);
            switch (this.direction) {
                case FORWARD:
                    this.style.setForwardMaxDepth((int) depth);
                    break;
                case REVERSE:
                    this.style.setReverseMaxDepth((int) depth);
                    break;
                default:
                case BOTH:
                case NONE:
                case AXIS:
                    this.style.setMaxDepth((int) depth);
            }
        } else { // Not expanded (using an expand packer).
            int numberOfSlotsInUse = getActualSlots();
            double totalInteriorSpacing = (numberOfSlotsInUse - 1) * getSpacing();
            double newSlotHeight = (height - totalInteriorSpacing) / numberOfSlotsInUse;
            double scale;

            if (useLabel()) {
                // Hiral says: because annotGlyphFactory multiplies by 2 when labeled.
                newSlotHeight /= 2;
            }

            switch (this.direction) {
                case FORWARD:
                    scale = newSlotHeight / style.getForwardHeight();
                    style.setForwardHeight(newSlotHeight);
                    break;
                case REVERSE:
                    scale = newSlotHeight / style.getReverseHeight();
                    style.setReverseHeight(newSlotHeight);
                    break;
                default:
                case BOTH:
                case NONE:
                case AXIS:
                    scale = newSlotHeight / style.getHeight();
                    style.setHeight(newSlotHeight);
            }

            scaleChildHeights(scale, getChildren(), view);
        }

    }

    private boolean useLabel() {
        String label_field = style.getLabelField();
        boolean use_label = label_field != null && (label_field.trim().length() > 0);
        if (use_label) {
            return true;
        }

        return false;
    }

    @Override
    public void setDirection(Direction d) {
        super.setDirection(d);
        if (direction != Direction.REVERSE) {
            expand_packer.setMoveType(NeoConstants.UP);
        }
    }

    /**
     * Not implemented. Will behave the same as drawSelectedOutline(ViewI).
     */
    @Override
    protected void drawSelectedFill(ViewI view) {
        this.drawSelectedOutline(view);
    }

    /**
     * Not implemented. Will behave the same as drawSelectedOutline(ViewI).
     */
    @Override
    protected void drawSelectedReverse(ViewI view) {
        this.drawSelectedOutline(view);
    }

    @Override
    public Map<String, Class<?>> getPreferences() {
        return new HashMap<>(PREFERENCES);
    }

    @Override
    public void setPreferences(Map<String, Object> preferences) {
        Integer maxDepth = (Integer) preferences.get("max_depth");
        setMaxExpandDepth(maxDepth);
        Boolean collapsed = (Boolean) preferences.get("collapsed");
        if (collapsed) {
            setPacker(collapse_packer);
        } else {
            setPacker(expand_packer);
        }
    }
}
