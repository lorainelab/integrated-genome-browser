package com.affymetrix.igb.graphTypes;

import com.affymetrix.genometry.style.GraphType;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.lorainelab.igb.genoviz.extensions.GraphGlyph;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

public class LineGraphType extends GraphGlyph.GraphStyle {

    public LineGraphType(GraphGlyph graphGlyph) {
        graphGlyph.super();
    }

    @Override
    public String getName() {
        return "linegraph";
    }

    @Override
    protected void doBigDraw(Graphics g, GraphSym graphSym,
            Point curr_x_plus_width, Point max_x_plus_width, float ytemp, int draw_end_index,
            double offset, double yscale, ViewI view, int i) {
        if (!graphSym.hasWidth()) {
            g.drawLine(prev_point.x, prev_point.y, curr_point.x, curr_point.y);
        } else {
            // Draw a line representing the width: (x,y) to (x + width,y)
            // Draw a line representing the width: (x,y) to (x + width,y)
            g.drawLine(curr_point.x, curr_point.y, curr_x_plus_width.x, curr_x_plus_width.y);
            // Usually draw a line from (xA + widthA,yA) to next (xB,yB), but when there
            // are overlapping spans, only do this from the largest previous (x+width) value
            // to an xA that is larger than that.
            // Usually draw a line from (xA + widthA,yA) to next (xB,yB), but when there
            // are overlapping spans, only do this from the largest previous (x+width) value
            // to an xA that is larger than that.
            if (curr_point.x >= max_x_plus_width.x && max_x_plus_width.x != Integer.MIN_VALUE) {
                g.drawLine(max_x_plus_width.x, max_x_plus_width.y, curr_point.x, curr_point.y);
            }
            if (curr_x_plus_width.x >= max_x_plus_width.x) {
                max_x_plus_width.x = curr_x_plus_width.x; // xB + widthB
                // xB + widthB
                max_x_plus_width.y = curr_x_plus_width.y; // yB
                // yB
            }
        }
    }

    @Override
    protected void DrawPoints(double offset, double yscale,
            ViewI view, Graphics g, int plot_bottom_ypixel, int plot_top_ypixel, float yzero, double coords_per_pixel) {
        if (yzero == 0) {
            g.setColor(Color.gray);
            g.drawLine(getPixelBox().x, zero_point.y, getPixelBox().width, zero_point.y);
        }
        g.setColor(getBackgroundColor());
        super.DrawPoints(offset, yscale, view, g, plot_bottom_ypixel, plot_top_ypixel, yzero, coords_per_pixel);
    }

    @Override
    protected void colorChange(Graphics g) {
        g.setColor(getBackgroundColor());
    }

    @Override
    protected void drawSingleRect(int ymin_pixel, int plot_bottom_ypixel, int plot_top_ypixel, int ymax_pixel, Graphics g, int ysum, int points_in_pixel, int width, int i) {
        int ystart = Math.max(Math.min(ymin_pixel, plot_bottom_ypixel), plot_top_ypixel);
        int yend = Math.min(Math.max(ymax_pixel, plot_top_ypixel), plot_bottom_ypixel);
        int len = Math.max(1, yend - ystart);
        GraphGlyph.drawRectOrLine(g, prev_point.x, ystart, 1, len);
        if (i > 0) {
            int y1 = Math.min(Math.max(prev_point.y, plot_top_ypixel), plot_bottom_ypixel);
            int y2 = Math.min(Math.max(curr_point.y, plot_top_ypixel), plot_bottom_ypixel);
            g.drawLine(prev_point.x, y1, curr_point.x, y2);
        }
    }

    @Override
    protected void doDraw(ViewI view) {
        double xpixels_per_coord = (view.getTransform()).getScaleX();
        double xcoords_per_pixel = 1 / xpixels_per_coord;
        if (xcoords_per_pixel < transition_scale) {
            oldDraw(view);
        } else {
            drawSmart(view);
        }
    }

    @Override
    public GraphType getGraphStyle() {
        return GraphType.LINE_GRAPH;
    }
}
