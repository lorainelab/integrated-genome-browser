package com.gene.transcriptisoform;

import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.impl.BAMSym;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;
import net.sf.samtools.Cigar;

/**
 * A glyph to connect exons to display isoforms.
 */
public class ExonConnectorGlyph extends SolidGlyph {

    enum DensityDisplay {

        THICKNESS,
        TRANSPARENCY,
        BRIGHTNESS;
    }
    private static final boolean DEBUG = false;
    private static final Rectangle MAX_BOUNDS = new Rectangle(Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, Integer.MAX_VALUE, Integer.MAX_VALUE);
    private static final Color FOUND_COLOR = Color.BLUE; //new Color(0.25f, 0.25f, 1.0f);
    private static final Color UNFOUND_COLOR = Color.RED;
    private static final Color SELECTED_COLOR = Color.YELLOW;
    private static final double SELECTION_FUDGE_AMOUNT = 5.0;
    private static final double MAX_VERT_THICKNESS = 32.0;
    private static final double LEG_HEIGHT_FACTOR = 0.00625;
    private static final double LEG_HEIGHT_MIN = 8.0;
    private static final double LEG_HEIGHT_MAX = 144.0;
    private static final double MIN_DENSITY_PERCENT = 0.25;
    private static final int X_INDEX = 0;
    private static final int Y_INDEX = 1;

    private final SimpleSeqSpan span;
    private final double vertThickness;
    private final Set<GlyphI> intronGlyphs;
    private final GlyphI beforeGlyph;
//	private final GlyphI afterGlyph;
    private final boolean found;
    private final boolean forwardStrand;
    private final Color unselectedColor;
    private final double legHeight;

    public ExonConnectorGlyph(SimpleSeqSpan span, Set<GlyphI> intronGlyphs, int count, GlyphI beforeGlyph, GlyphI afterGlyph, boolean forwardStrand, DensityDisplay showDensity) {
        super();
        this.span = span;
        double percentOfMax = (double) intronGlyphs.size() / (double) count;
        vertThickness = (showDensity == DensityDisplay.THICKNESS) ? MAX_VERT_THICKNESS * percentOfMax : 1;
        this.intronGlyphs = intronGlyphs;
        this.beforeGlyph = beforeGlyph;
//		this.afterGlyph = afterGlyph;
        this.found = beforeGlyph != null && afterGlyph != null;
        this.forwardStrand = forwardStrand;
        Color baseColor = found ? FOUND_COLOR : UNFOUND_COLOR;
        switch (showDensity) {
            case THICKNESS:
                unselectedColor = baseColor;
                break;
            case TRANSPARENCY:
                percentOfMax = Math.max(percentOfMax, MIN_DENSITY_PERCENT);
                unselectedColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int) (percentOfMax * 255));
                break;
            case BRIGHTNESS:
                percentOfMax = Math.max(percentOfMax, MIN_DENSITY_PERCENT);
                unselectedColor = new Color((int) (percentOfMax * baseColor.getRed()), (int) (percentOfMax * baseColor.getGreen()), (int) (percentOfMax * baseColor.getBlue()));
                break;
            default:
                unselectedColor = baseColor;
        }
        setSelected(false);
        if (DEBUG) {
            System.out.println("ExonConnectorGlyph = " + span.getBioSeq() + ":" + span.getMin() + " - " + span.getMax() + ", found = " + found);
            for (GlyphI glyph : intronGlyphs) {
                if ((glyph.getInfo() instanceof BAMSym)) {
                    BAMSym bs = (BAMSym) glyph.getInfo();
                    String dirTag = bs.isForward() ? "+" : "-";
                    Cigar cg = bs.getCigar();
                    System.out.println("    CigarSym = " + bs.getID() + " = " + dirTag + " " + bs.getMin() + " - " + bs.getMax() + ", cigar = " + cg.toString());
                }
            }
        }
        this.legHeight = Math.max(Math.min(LEG_HEIGHT_FACTOR * span.getLength(), LEG_HEIGHT_MAX), LEG_HEIGHT_MIN);
//	    System.out.println(">>>>> span.getLength() = " + span.getLength() + ", legHeight = " + this.legHeight);
    }

    public void init() {
        updateCoordbox();
    }

    private Color getFillColor() {
        if (isSelected()) {
            return SELECTED_COLOR;
        }
        return unselectedColor;
    }

    private void updateCoordbox() {
        getCoordBox().x = span.getMinDouble();
        if (found) {
            getCoordBox().y = beforeGlyph.getCoordBox().getY() + beforeGlyph.getCoordBox().getHeight() / 2.0;
        } else {
            getCoordBox().y = getParent().getCoordBox().getY() + (forwardStrand ? 0 : getParent().getCoordBox().getHeight());
        }
        getCoordBox().width = span.getMaxDouble() - span.getMinDouble();
        getCoordBox().height = legHeight + vertThickness;
    }

    public void draw(ViewI view) {
        updateCoordbox();
        Graphics g = view.getGraphics();
        g.setColor(getFillColor());

        drawConnector(g, view);
        super.draw(view);
    }

    /**
     * @param fudgeFactor the amount to add/subtract to the polygon points
     * @return a 2 dim array first dim is x vs y, second dim is point index (6
     * points)
     *
     * 2
     * ... ..... ...5... ... ... ... ... ... ... 1.. ..3 .. .. 0 4
     *
     */
    private double[][] getCoordChevron(double fudgeFactor, Rectangle bounds) {
        double[][] chevron = new double[2][6];
        double strandMult = forwardStrand ^ found ? +1.0 : -1.0;
        chevron[X_INDEX][0] = span.getMinDouble();
        chevron[Y_INDEX][0] = getCoordBox().y - strandMult * fudgeFactor;
        chevron[X_INDEX][1] = chevron[X_INDEX][0];
        chevron[Y_INDEX][1] = getCoordBox().y + strandMult * (vertThickness + fudgeFactor);
        chevron[X_INDEX][2] = span.getMinDouble() + (span.getMaxDouble() - span.getMinDouble()) / 2.0;
        chevron[Y_INDEX][2] = getCoordBox().y + strandMult * (legHeight + vertThickness + fudgeFactor);
        chevron[X_INDEX][3] = span.getMaxDouble();
        chevron[Y_INDEX][3] = chevron[Y_INDEX][1];
        chevron[X_INDEX][4] = chevron[X_INDEX][3];
        chevron[Y_INDEX][4] = chevron[Y_INDEX][0];
        chevron[X_INDEX][5] = chevron[X_INDEX][2];
        chevron[Y_INDEX][5] = getCoordBox().y + strandMult * (legHeight - fudgeFactor);
        return chevron;
    }

    private double[][] chevronCoordToPixel(double[][] coordChevron, ViewI view) {
        double[][] pixelChevron = new double[2][6];
        for (int i = 0; i < 6; i++) {
            Point dst = new Point();
            view.transformToPixels(new Point2D.Double(coordChevron[X_INDEX][i], coordChevron[Y_INDEX][i]), dst);
            pixelChevron[X_INDEX][i] = dst.getX();
            pixelChevron[Y_INDEX][i] = dst.getY();
        }
        return pixelChevron;
    }

    private int[][] chevronDoubleToInt(double[][] doubleChevron) {
        int[][] intChevron = new int[2][6];
        for (int i = 0; i < 6; i++) {
            intChevron[X_INDEX][i] = (int) Math.round(doubleChevron[X_INDEX][i]);
            intChevron[Y_INDEX][i] = (int) Math.round(doubleChevron[Y_INDEX][i]);
        }
        return intChevron;
    }

    /**
     * draws the connector between exons, a six point polygon (chevron).
     */
    private void drawConnector(Graphics g, ViewI view) {
        int[][] intPixelChevron = chevronDoubleToInt(chevronCoordToPixel(getCoordChevron(0, view.getPixelBox()), view));
        g.drawPolygon(intPixelChevron[X_INDEX], intPixelChevron[Y_INDEX], 6);
        g.fillPolygon(intPixelChevron[X_INDEX], intPixelChevron[Y_INDEX], 6);
    }

    public boolean checkClicked(Point2D.Double coord) {
        int[][] intCoordChevron = chevronDoubleToInt(getCoordChevron(SELECTION_FUDGE_AMOUNT, MAX_BOUNDS));
        Polygon polygon = new Polygon(intCoordChevron[X_INDEX], intCoordChevron[Y_INDEX], 6);
        return polygon.contains(coord);
    }

    public void applyColorChange() {
        GlyphI glParent = getParent();
        if (glParent instanceof TierGlyph) {
            List<ViewI> views = glParent.getScene().getViews();
            for (ViewI v : views) {
                if (withinView(v)) {
                    draw(v);
                }
            }
        }

    }

    public Set<GlyphI> getIntronGlyphs() {
        return intronGlyphs;
    }

    @Override
    public boolean hit(Rectangle pixel_hitbox, ViewI view) {
        return false;
    }

    @Override
    public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view) {
        return false;
    }
}
