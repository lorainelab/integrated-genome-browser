package com.affymetrix.igb.graphTypes;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.MisMatchPileupGraphSym;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.util.ResidueColorHelper;
import com.lorainelab.igb.genoviz.extensions.glyph.GraphGlyph;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MismatchPileupType extends MismatchGraphType {

    private static final Map<Character, int[]> BAR_ORDERS = new HashMap<>();

    private static final Color MATCH_COLOR = Color.GRAY;
    private static Color[] baseColors;

    static {
        BAR_ORDERS.put('A', new int[]{1, 2, 3, 4});
        BAR_ORDERS.put('T', new int[]{0, 2, 3, 4});
        BAR_ORDERS.put('G', new int[]{0, 1, 3, 4});
        BAR_ORDERS.put('C', new int[]{0, 1, 2, 4});
        BAR_ORDERS.put('N', new int[]{0, 1, 2, 3});
    }

    private static void setBaseColor() {
        baseColors = new Color[]{
            ResidueColorHelper.getColorHelper().determineResidueColor('A'),
            ResidueColorHelper.getColorHelper().determineResidueColor('T'),
            ResidueColorHelper.getColorHelper().determineResidueColor('G'),
            ResidueColorHelper.getColorHelper().determineResidueColor('C'),
            ResidueColorHelper.getColorHelper().determineResidueColor('N')
        };
    }

    public MismatchPileupType(GraphGlyph graphGlyph) {
        super(graphGlyph);
    }

    @Override
    protected void bigDrawLoop(int draw_beg_index,
            int draw_end_index, double offset, double yscale, ViewI view,
            Point curr_x_plus_width, Graphics g, Point max_x_plus_width, GraphSym graphSym) {
        MisMatchPileupGraphSym mmgs = (MisMatchPileupGraphSym) graphSym;
        Optional<BioSeq> seq = GenometryModel.getInstance().getSelectedSeq();
        if (seq.isPresent()) {
            if (!mmgs.hasReferenceSequence()) {
                seq.get().getResidues(graphSym.getMinXCoord(), graphSym.getMaxXCoord()); // so all are loaded, not one by one
            }
        }
        super.bigDrawLoop(draw_beg_index, draw_end_index, offset, yscale, view, curr_x_plus_width,
                g, max_x_plus_width, graphSym);
    }

    @Override
    protected void doBigDraw(Graphics g, GraphSym graphSym, Point curr_x_plus_width, Point max_x_plus_width, float ytemp, int draw_end_index, double offset, double yscale, ViewI view, int i) {
        setBaseColor();
        MisMatchPileupGraphSym mmgs = (MisMatchPileupGraphSym) graphSym;
        Color saveColor = g.getColor();

//		final int width = Math.max(1, curr_x_plus_width.x - curr_point.x - 1);
        final int width = curr_x_plus_width.x;

        BioSeq seq = GenometryModel.getInstance().getSelectedSeq().orElse(null);
        // need to draw coverage first, then mismatches so that the mismatches are not covered up
        g.setColor(MATCH_COLOR);
        super.doBigDraw(g, graphSym, curr_x_plus_width, max_x_plus_width, ytemp, draw_end_index, offset, yscale, view, i);

        // now draw the mismatches, piled up
        // flipping about yaxis... should probably make this optional
        // also offsetting to place within glyph bounds
        int xtemp = graphSym.getGraphXCoord(i);
        coord.x = xtemp;
        Point2D.Double x_plus_width2D = new Point2D.Double(0, 0);
        x_plus_width2D.x = xtemp + 1;
        int savey = zero_point.y;
        char referenceBase = mmgs.hasReferenceSequence() ? mmgs.getReferenceBase(i) : seq.getResidues(xtemp, xtemp + 1).toUpperCase().charAt(0);
        float y_accum = 0;
        int[] barOrder = BAR_ORDERS.get(referenceBase);
        if (barOrder == null) {
            return;
        }
        float[] residuesY = mmgs.getAllResiduesY(i);
        for (int loopIndex : barOrder) {
            float _ytemp = residuesY[loopIndex];
            if (_ytemp == 0) {
                continue;
            }
            if (Double.isNaN(_ytemp) || Double.isInfinite(_ytemp)) {
                return;
            }
            _ytemp += y_accum;
            y_accum = _ytemp;
            // flattening any points > getVisibleMaxY() or < getVisibleMinY()...
            _ytemp = Math.min(_ytemp, getVisibleMaxY());
            _ytemp = Math.max(_ytemp, getVisibleMinY());

            Point2D.Double coordMMP = new Point2D.Double(0, 0);
            coordMMP.y = offset - ((_ytemp - getVisibleMinY()) * yscale);
            Point curr_pointMMP = new Point(0, 0);
            view.transformToPixels(coordMMP, curr_pointMMP);
//				x_plus_width2D.y = coord.y;
//				view.transformToPixels(x_plus_width2D, curr_x_plus_width);

            int ymin_pixel = Math.min(curr_pointMMP.y, savey);
            int yheight_pixel = Math.abs(curr_pointMMP.y - savey);
            yheight_pixel = Math.max(1, yheight_pixel);
            g.setColor(baseColors[loopIndex]);
            g.fillRect(curr_point.x, ymin_pixel, width, yheight_pixel);
            savey = curr_pointMMP.y;

        }
        g.setColor(saveColor);
    }
}
