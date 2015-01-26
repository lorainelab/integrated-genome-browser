package com.affymetrix.genometry.symmetry.impl;

import com.affymetrix.genometry.BioSeq;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MisMatchPileupGraphSym extends MisMatchGraphSym {

    private float min_totalycoord = Float.POSITIVE_INFINITY;
    private float max_totalycoord = Float.NEGATIVE_INFINITY;
    private char[] bases;

    public MisMatchPileupGraphSym(int[] x, int[] w, float[] y, int[] a,
            int[] t, int[] g, int[] c, int[] n, String id, BioSeq seq, char[] bases) {
        super(x, w, y, a, t, g, c, n, id, seq);
        this.bases = bases;
    }

    public MisMatchPileupGraphSym(int[] x, int[] w, float[] y, int[] a,
            int[] t, int[] g, int[] c, int[] n, String id, BioSeq seq) {
        this(x, w, y, a, t, g, c, n, id, seq, null);
    }

    @Override
    void setAllResidues(int[] a, int[] t, int[] g, int[] c, int[] n) {
        super.setAllResidues(a, t, g, c, n);
        setVisibleTotalYRange(residuesTot);
        bases = null;
    }

    @Override
    public float[] getVisibleYRange() {
        return getVisibleTotalYRange();
    }

    @Override
    protected File index(String graphName, int[] a, int[] t, int[] g, int[] c, int[] n) {
        File file = super.index(graphName, a, t, g, c, n);
        setVisibleTotalYRange(residuesTot);
        bases = null;
        return file;
    }

    @Override
    protected synchronized void readIntoBuffers(int start) {
        super.readIntoBuffers(start);
        setVisibleTotalYRange(residuesTot);
        bases = null;
    }

    @Override
    public float getGraphYCoord(int i) {
        float totalY = 0.0f;
        for (float y : getAllResiduesY(i)) {
            totalY += y;
        }
        return totalY;
    }

    private final float[] getVisibleTotalYRange() {
        float[] result = new float[2];
        if (min_totalycoord == Float.POSITIVE_INFINITY || max_totalycoord == Float.NEGATIVE_INFINITY) {
            setVisibleTotalYRange(residuesTot);
        }
        result[0] = min_totalycoord;
        result[1] = max_totalycoord;
        return result;
    }

    private synchronized void setVisibleTotalYRange(int[][] resT) {
        min_totalycoord = Float.POSITIVE_INFINITY;
        max_totalycoord = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < resT[0].length; i++) {
            float f = 0;
            for (int j = 0; j < 5; j++) {
                f += resT[j][i];
            }
            if (f < min_totalycoord) {
                min_totalycoord = f;
            }
            if (f > max_totalycoord) {
                max_totalycoord = f;
            }
        }
    }

	// this is overriden to "normalize" the y values, to merge consecutive
    // repeated values into one
    @Override
    public synchronized float[] normalizeGraphYCoords() {
        List<Float> coords = new ArrayList<>();
        float lastY = Float.NaN;
        int lastX = Integer.MAX_VALUE;
//		if (getFirstYCoord() != getGraphYCoord(0)) {
//			coords.add(getFirstYCoord());
//			lastY = getFirstYCoord();
//		}
        for (int i = 0; i < getPointCount(); i++) {
            int x = getGraphXCoord(i);
            if (hasWidth() && x != lastX && lastX != Integer.MAX_VALUE) {
                coords.add(0.0f);
                lastY = 0.0f;
            }
            float y = getGraphYCoord(i);
            if (y != lastY) {
                coords.add(y);
                lastY = y;
            }
            if (hasWidth()) {
                lastX = x + getGraphWidthCoord(i);
            }
        }
        coords.add(0.0f);
        float[] tempCoords = new float[coords.size()];
        for (int i = 0; i < coords.size(); i++) {
            tempCoords[i] = coords.get(i);
        }
        return tempCoords;
    }

    public boolean hasReferenceSequence() {
        return bases != null;
    }

    public char getReferenceBase(int i) {
        return bases[i];
    }

    @Override
    public boolean isSpecialGraph() {
        return true;
    }
}
