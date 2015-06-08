package com.affymetrix.igb.graphTypes;

import com.affymetrix.genometry.style.GraphType;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.lorainelab.igb.genoviz.extensions.glyph.GraphGlyph;
import java.awt.Graphics;
import java.awt.Point;

public class FillBarGraphType extends BarGraphType {

    public FillBarGraphType(GraphGlyph graphGlyph) {
        super(graphGlyph);
    }

    @Override
    public String getName() {
        return "fillbargraph";
    }

    @Override
    protected void doBigDraw(Graphics g, GraphSym graphSym,
            Point curr_x_plus_width, Point max_x_plus_width, float ytemp,
            int draw_end_index, double offset, double yscale, ViewI view, int i) {

        //			if(helper != null){
        //			g.setColor(helper.determineResidueColor((char)residues[i]));
        //		}
        int ymin_pixel = Math.min(curr_point.y, zero_point.y);
        int yheight_pixel = Math.abs(curr_point.y - zero_point.y);
        yheight_pixel = Math.max(1, yheight_pixel);
        if (!graphSym.hasWidth()) {
            //g.drawLine(curr_point.x, ymin_pixel, curr_point.x, ymin_pixel + yheight_pixel);
            g.fillRect(curr_point.x, ymin_pixel, curr_x_plus_width.x + 1, yheight_pixel);
        } else {
            final int width = Math.max(1, curr_x_plus_width.x - curr_point.x - 1);
            g.fillRect(curr_point.x, ymin_pixel, width, yheight_pixel);
        }
    }

    @Override
    public GraphType getGraphStyle() {
        return GraphType.FILL_BAR_GRAPH;
    }
}
