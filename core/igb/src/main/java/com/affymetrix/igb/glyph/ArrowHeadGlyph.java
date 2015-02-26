package com.affymetrix.igb.glyph;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.DirectedGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.lorainelab.igb.genoviz.extensions.SeqMapViewExtendedI;
import java.awt.Graphics;

/**
 * An arrow glyph.
 */
public class ArrowHeadGlyph extends DirectedGlyph {

    private static final int buffer_pixel = 10;

//	@Override
//	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
//		if(isVisible() && coord_hitbox.intersects(coordbox)){
//			Rectangle pixbox = new Rectangle();
//			view.transformToPixels(coord_hitbox,pixbox);
//			return pixbox.intersects(bounds);
//		}
//		return false;
//	}
    public static void addDirectionGlyphs(SeqMapViewExtendedI gviewer, SeqSymmetry sym, GlyphI pglyph, BioSeq annotSeq, BioSeq coordSeq, double cy, double cheight, boolean useArrow) {
        SeqSymmetry intronSym = SeqUtils.getIntronSym(sym, annotSeq);
        if (intronSym != null) {
            int childCount = intronSym.getChildCount();
            for (int i = 0; i < childCount; i++) {
                SeqSymmetry child = intronSym.getChild(i);
                SeqSpan cspan = child.getSpan(coordSeq);
                if (cspan == null || cspan.getLength() == 0) {
                    continue;
                } else {
                    DirectedGlyph cglyph;
                    if (useArrow) {
                        cglyph = new ArrowHeadGlyph();
                    } else {
                        cglyph = new DirectedGlyph() {
                        };
                    }
                    cglyph.setHitable(false);
                    cglyph.setCoords(cspan.getMin(), cy, cspan.getLength(), cheight);
                    cglyph.setForward(cspan.isForward());
                    cglyph.setColor(pglyph.getColor());
                    pglyph.addChild(cglyph);
                    //gviewer.setDataModelFromOriginalSym(cglyph, child);
                }
            }
        }
    }
    private int x[];
    private int y[];
    private int headX, headY;
    //final Rectangle bounds;

    protected boolean fillArrowHead = true;

    public ArrowHeadGlyph() {
        x = new int[6];
        y = new int[6];
        //bounds = new Rectangle();
    }

    private void calHead() {
        switch (this.getOrientation()) {
            case NeoConstants.HORIZONTAL:
                this.headY = getPixelBox().height;
                break;
            case NeoConstants.VERTICAL:
                this.headY = getPixelBox().width;
                break;
        }
        this.headX = this.headY / 2;
    }

    @Override
    public void draw(ViewI view) {
        double hold_y = getCoordBox().y;

        getCoordBox().y += (getCoordBox().height / 2);
        view.transformToPixels(getCoordBox(), getPixelBox());
        int offset_center = getPixelBox().y;
        getCoordBox().y = hold_y;
        view.transformToPixels(getCoordBox(), getPixelBox());
        calHead();
        if (headY < 8 || getPixelBox().x + getPixelBox().width / 2 + headX / 2 + buffer_pixel > getPixelBox().x + getPixelBox().width
                || getPixelBox().x + getPixelBox().width / 2 - headX / 2 - buffer_pixel < getPixelBox().x) {
            return;
        }

        Graphics g = view.getGraphics();
        g.setColor(getBackgroundColor());


        /* Note that the arrow glyph seems to point in the direction
         * opposite to what might be expected.
         * This is for backward compatibility.
         * We don't want to break the NeoAssembler, in particular.
         * Perhaps this should be corrected at some point.
         * When that time comes, EAST and WEST should be switched,
         * as should be SOUTH and NORTH.
         */
        switch (this.getDirection()) {
            case EAST:  // forward strand
                //bounds.setBounds(getPixelBox().x + getPixelBox().width/2 - headX/2, offset_center, headX, headY);
                drawArrowHead(g, getPixelBox().x + getPixelBox().width / 2 + headX / 2,
                        getPixelBox().x + getPixelBox().width / 2 - headX / 2,
                        offset_center, getPixelBox().x + getPixelBox().width / 2);
                break;
            case WEST:
                //bounds.setBounds(getPixelBox().x + getPixelBox().width/2 - headX/2, offset_center, headX, headY);
                drawArrowHead(g, getPixelBox().x + getPixelBox().width / 2 - headX / 2,
                        getPixelBox().x + getPixelBox().width / 2 + headX / 2,
                        offset_center, getPixelBox().x + getPixelBox().width / 2);
                break;
            case SOUTH:  // forward strand
                //bounds.setBounds(getPixelBox().x + getPixelBox().width/2 - headY/2, getPixelBox().x + getPixelBox().width/2, headY, headX);
                drawArrowHead(g, getPixelBox().y + getPixelBox().height / 2 + headX / 2,
                        getPixelBox().y + getPixelBox().height / 2 - headX / 2,
                        getPixelBox().x + getPixelBox().width / 2, offset_center);
                break;
            case NORTH:  // reverse strand
                //bounds.setBounds(getPixelBox().x + getPixelBox().width/2 - headY/2, getPixelBox().x + getPixelBox().width/2, headY, headX);
                drawArrowHead(g, getPixelBox().y + getPixelBox().height / 2 - headX / 2,
                        getPixelBox().y + getPixelBox().height / 2 + headX / 2,
                        getPixelBox().x + getPixelBox().width / 2, offset_center);
                break;
            default:
        }
        super.draw(view);
    }

    /**
     * draws the triangle that forms the head of the arrow.
     * Note that the "x"s in the parameter names hark back
     * to a time when arrows did not work on vertical maps.
     */
    private void drawArrowHead(Graphics g, int tip_x, int flat_x, int tip_center, int x_center) {
        switch (this.getOrientation()) {
            case NeoConstants.HORIZONTAL:
                x[0] = flat_x;
                y[0] = tip_center - headY / 2;
                x[1] = tip_x;
                y[1] = tip_center;
                x[2] = flat_x;
                y[2] = tip_center + headY / 2;
                x[3] = flat_x;
                y[3] = tip_center + headY / 4;
                x[4] = x_center;
                y[4] = tip_center;
                x[5] = flat_x;
                y[5] = tip_center - headY / 4;
                break;
            case NeoConstants.VERTICAL:
                y[0] = flat_x;
                x[0] = tip_center - headY / 2;
                y[1] = tip_x;
                x[1] = tip_center;
                y[2] = flat_x;
                x[2] = tip_center + headY / 2;
                y[3] = flat_x;
                x[3] = tip_center + headY / 4;
                y[4] = x_center;
                x[4] = tip_center;
                y[5] = flat_x;
                x[5] = tip_center - headY / 4;
                break;
        }
        if (fillArrowHead) {
            g.fillPolygon(x, y, 6);
        } else {
            g.drawPolygon(x, y, 6);
        }
    }

}
