/**
 * Copyright (c) 2001-2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package org.lorainelab.igb.igb.genoviz.extensions;

import com.affymetrix.genometry.util.GraphSymUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FloaterGlyph;
import org.lorainelab.igb.igb.genoviz.extensions.glyph.GraphGlyph;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphGlyphUtils {

    private static final float sliders_per_percent = 10.0f;

    public static final String PREF_USE_FLOATING_GRAPHS = "use floating graphs";
    public static final String PREF_ATTACHED_COORD_HEIGHT = "default attached graph coord height";
    public static final NumberFormat numberParser = NumberFormat.getNumberInstance();

    public static boolean hasFloatingAncestor(GlyphI gl) {
        if (gl == null) {
            return false;
        }
        if (gl instanceof FloaterGlyph) {
            return true;
        } else if (gl.getParent() == null) {
            return false;
        } else {
            return hasFloatingAncestor(gl.getParent());
        }
    }

    /**
     * Checks to make sure that two graphs can be compared with one another for
     * operations like diff, ratio, etc. (Graphs must have exact same x
     * positions, and if one has width coords, the other must also.)
     *
     * @return null if the graphs are comparable, or an explanation string if
     * they are not.
     */
    public static String graphsAreComparable(GraphGlyph graphA, GraphGlyph graphB) {
        // checking that both graphs are non-null
        if (graphA == null || graphB == null) {
            return "Must select exactly two graphs";
        }
        int numpoints = graphA.getPointCount();
        // checking that both graph have same number of points
        if (numpoints != graphB.getPointCount()) {
            return "Graphs must have the same X points";
        }
        if (graphA.hasWidth() != graphB.hasWidth()) {
            // one has width coords, the other doesn't.
            return "Must select two graphs of the same type";
        }

        // checking that both graphs have same x points
        for (int i = 0; i < numpoints; i++) {
            if (graphA.getXCoord(i) != graphB.getXCoord(i)) {
                return "Graphs must have the same X points";
            }
        }

        return null;
    }

    /**
     * Parse a String floating-point number that may optionally end with a "%"
     * symbol.
     */
    public static float parsePercent(String text) throws ParseException {
        if (text.endsWith("%")) {
            text = text.substring(0, text.length() - 1);
        }

        return numberParser.parse(text).floatValue();
    }

    public static int[] intListToArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public static float[] floatListToArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private static final int MAX_INFO2PSCORES_SIZE = 1000; // prevent it from getting too big
    private static final Map<Object, float[]> info2pscores = new HashMap<>();

    /**
     * Gets the percents2scores array for the given graph, creating the array if
     * necessary.
     */
    private static float[] getPercents2Scores(GraphGlyph gl) {
        Object info = gl.getInfo();
//		if (info == null && !(gl instanceof MultiGraphGlyph)) {
//            Logger.getLogger("com.affymetrix.igb.shared").log(Level.INFO, "Graph has no info! " + gl);
//		}
        float[] p2score = info2pscores.get(info);

        if (p2score == null) {
            float[] ycoords = gl.copyYCoords();
            p2score = GraphSymUtils.calcPercents2Scores(ycoords, sliders_per_percent);
            if (info2pscores.size() >= MAX_INFO2PSCORES_SIZE) {
                info2pscores.clear();
            }
            info2pscores.put(info, p2score);
        }
        return p2score;
    }

    public static float getValueForPercent(GraphGlyph gl, float percent) {
        float[] percent2score = getPercents2Scores(gl);
        int index = Math.round(percent * sliders_per_percent);

        // I have actually seen a case where index was calculated as -1,
        // and an exception was thrown. That is why I added this check. (Ed)
        if (index < 0) {
            index = 0;
        } else if (index >= percent2score.length) {
            index = percent2score.length - 1;
        }

        return percent2score[index];
    }

    public static float getPercentForValue(GraphGlyph gl, float value) {
        float percent = Float.NEGATIVE_INFINITY;
        float[] percent2score = getPercents2Scores(gl);
        // do a binary search through percent2score array to find percent bin closest to value
        int index = Arrays.binarySearch(percent2score, value);
        if (index < 0) {
            index = -index - 2;
        }
        if (index >= percent2score.length) {
            percent = 100;
        } else if (value >= gl.getGraphMaxY()) {
            percent = 100;
        } else if (index < 0) {
            percent = 0;
        } else if (value <= gl.getGraphMinY()) {
            percent = 0;
        } else {
            percent = index / sliders_per_percent;
        }

        return percent;
    }

    private GraphGlyphUtils() {
    }
}
