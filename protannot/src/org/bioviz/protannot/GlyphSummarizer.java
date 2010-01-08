package org.bioviz.protannot;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.awt.geom.Rectangle2D;
import com.affymetrix.genoviz.glyph.StretchContainerGlyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;

/**
 * This class builds an exon summary Glyph, which appears at the bottom of the
 * display. The summary's height at any given base position indicates the
 * number of features above it that overlap that particular base.
 */
final class GlyphSummarizer {

    private static final boolean filter_zeros = false;
    // scaling factor -- scale to apply to hit counts to get height of summary
    // at any point
    private static final float scale_factor = 10.0f;
    private Color glyph_color = Color.blue;

    /**
     * Create a new GlyphSummarizer, which will be drawn using the given
     * Color.
     *
     * @param col   Color of GlyphSummarizer
     */
    GlyphSummarizer(Color col) {
        this.glyph_color = col;
    }


    /**
     * Given a Collection of Glyphs that should contribute to the exon
     * summary graphic at the bottom of the display (i.e., Glyphs representing
     * individual exons, components of transcripts) create an exon summary Glyph
     * that draws a block for each position with an overlapping exon above it.
     * The height of the exon summary Glyph should be proportional to the number
     * of exons above it.
     *
     * @param glyphsToSummarize
     * @return a GlyphI
     * @see     com.affymetrix.genoviz.glyph.StretchContainerGlyph
     * @see     com.affymetrix.genoviz.bioviews.GlyphI
     */
    GlyphI getSummaryGlyph(Collection<GlyphI> glyphsToSummarize) {
        GlyphI summaryGlyph = new StretchContainerGlyph();
        /**
         * 1) Construct a list (array?)  of all the transition points (every edge of
         *  every glyph at the proper depth, excluding redundant edges)
         * [by convention, first element of array is first edge _into_ a glyph]
         */
        // The list to keep transition points in --
        // using an array instead of Collection so don't have to turn the
        // ints into proper Objects, but also  but also checking array bounds to
        // stretch if needed...
        int[] transitions = new int[glyphsToSummarize.size() * 2];
        int index = 0;
        for (GlyphI gl : glyphsToSummarize) {
            Rectangle2D cbox = gl.getCoordBox();
            // note: the new Genoviz SDK may express the coordinates of a coord
            // box as floats ... not ints? Note sure what would happen here:
            transitions[index] = (int) cbox.getX();
            index++;
            transitions[index] = (int) (cbox.getX() + cbox.getWidth());
            index++;
        }
        // at this point, we've got an array that contains the start and end
        // coordinate for each Glyph we need to summarize via the exon summary
        // graphic
        // now, we sort the array.
        // each number now represents a Glyph boundary, and runs of the same number
        // represent Glyphs that ended or started at the same position along the
        // genomic sequence axis
        Arrays.sort(transitions);
        // now, we construct new array based on transitions but with
        // no redundancies
        int[] temp_trans = new int[transitions.length];
        int previous = transitions[0];
        temp_trans[0] = previous;
        int uindex = 1;
        for (int i = 1; i < transitions.length; i++) {
            int current = transitions[i];
            if (current != previous) {
                temp_trans[uindex] = current;
                previous = current;
                uindex++;
            }
        }
        int[] unique_transitions = new int[uindex];
        System.arraycopy(temp_trans, 0, unique_transitions, 0, uindex);
        int segCount = uindex - 1;
        for (int i = 0; i < segCount; i++) {
            /**
             * 2) Construct a glyph summarySegment for every sequential pair of transition points,
             *    with x = array[i] and width = array[i+1] - array[i],
             *    and y = FLOAT_MIN/2 and height = FLOAT_MAX (???)
             */
            int segStart = unique_transitions[i];
            int segEnd = unique_transitions[i + 1];

            GlyphI newgl = new FillRectGlyph();
            newgl.setColor(glyph_color);

            /**
             * 3) Query every glyph in the glyphsToSummarize vector (or their children at the proper
             *    depth if depthToCheck > 0) for intersection with summarySegment glyph,
             *    an tally up hits
             */
            int hitCount = 0;
            for (GlyphI gl : glyphsToSummarize) {
                Rectangle2D cbox = gl.getCoordBox();
                //      Rectangle2D cbox = gl.getCoordBox();
                // assumes widths are positive...
                int glStart = (int) cbox.getX();
                int glEnd = (int) (cbox.getX() + cbox.getWidth());

                if (!(segEnd <= glStart || segStart >= glEnd)) {
                    hitCount++;
                }
            }

            /**
             * 4) reset y = 0 and height = # of hits
             */
            //      newgl.setCoords(segStart, 0, segEnd - segStart, hitCount);
            // just hardwiring height multiple till get normalization code implemented
            //      newgl.setCoords(segStart, 0, segEnd - segStart, hitCount*10);
            // if want to filter out regions with no hits, uncomment out conditional
            if ((!filter_zeros) || (hitCount > 0)) {
                newgl.setCoords(segStart, -hitCount * scale_factor, segEnd - segStart, hitCount * scale_factor);
                summaryGlyph.addChild(newgl);
            }
        }

        /**
         * 5) normalize height somehow...
         *  NOT YET IMPLEMENTED
         */
        return summaryGlyph;
    }
}

