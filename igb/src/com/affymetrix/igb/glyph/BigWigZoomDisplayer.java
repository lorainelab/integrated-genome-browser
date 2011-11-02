package com.affymetrix.igb.glyph;

import java.io.IOException;
import java.util.ArrayList;

import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BBZoomLevelHeader;
import org.broad.igv.bbfile.ZoomDataRecord;
import org.broad.igv.bbfile.ZoomLevelIterator;
import org.broad.tribble.util.SeekableStreamFactory;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.symmetry.GraphIntervalSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ZoomDisplayer;

public class BigWigZoomDisplayer implements ZoomDisplayer {
	private static final int SEGMENT_COUNT = 256;
	public static final String BIGWIG_ZOOM_DISPLAYER_EXTENSION = "bw";
	private String url;
	private TierGlyph tier;

	public BigWigZoomDisplayer(SeqSymmetry sym, TierGlyph tier) {
		super();
		String method = BioSeq.determineMethod(sym);
		Direction direction = tier.getDirection();
		if (direction == null) {
			direction = Direction.BOTH;
		}
		this.url = method == null ? null : GeneralUtils.fixFileName(getBigWigFileName(method, direction));
		this.tier = tier;
	}

	public static String getBigWigFileName(String method, Direction direction) {
		return method + "." + Direction.FORWARD.toString().toLowerCase() + "." + BIGWIG_ZOOM_DISPLAYER_EXTENSION;
	}

	public static boolean hasBigWig(SeqSymmetry sym) {
		String meth = BioSeq.determineMethod(sym);
		return meth != null &&
			GeneralUtils.urlExists(getBigWigFileName(meth, Direction.FORWARD)) &&
			GeneralUtils.urlExists(getBigWigFileName(meth, Direction.REVERSE)) &&
			GeneralUtils.urlExists(getBigWigFileName(meth, Direction.BOTH));
	}

	@Override
	public GlyphI getZoomGlyph(ViewI view) {
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
        int startBase = (int)Math.round(view.getCoordBox().getX());
		int length = (int)Math.round(view.getCoordBox().getWidth());
        int endBase = startBase + length;
		int basesPerSegment = length / SEGMENT_COUNT;
		BBFileReader bbReader;
		try {
			bbReader = new BBFileReader(url, SeekableStreamFactory.getStreamFor(url));
		}
		catch (IOException x) {
			return null;
		}
        if (!bbReader.isBigWigFile()) {
        	return null;
        }
        BBZoomLevelHeader bestZoom = bbiBestZoom(bbReader.getZoomLevels().getZoomLevelHeaders(), basesPerSegment);
        if (bestZoom == null) {
        	return null;
        }
        final int level = bestZoom.getZoomLevel();
        ZoomLevelIterator zoomIterator = bbReader.getZoomLevelIterator(level, seq.getID(),
        		startBase, seq.getID(), endBase, true);
        int nextStart = -1;
        ZoomDataRecord nextRecord = null;
        ArrayList<Integer> xList = new ArrayList<Integer>();
        ArrayList<Float> yList = new ArrayList<Float>();
        ArrayList<Integer> wList = new ArrayList<Integer>();
        while (zoomIterator.hasNext()) {
            nextRecord = zoomIterator.next();
            if (nextRecord == null) {
                break;
            }
            if (nextStart != -1 && nextStart != nextRecord.getChromStart()) {
                xList.add(nextStart);
                wList.add(nextRecord.getChromStart() - nextStart);
                yList.add(0.0f);
            }
            xList.add(nextRecord.getChromStart());
            wList.add(nextRecord.getChromEnd() - nextRecord.getChromStart());
            yList.add(nextRecord.getSumData() / (nextRecord.getChromEnd() - nextRecord.getChromStart()));
            nextStart = nextRecord.getChromEnd();
        }
		int[] x = new int[xList.size()];
		for (int i = 0; i < xList.size(); i++) {
			x[i] = xList.get(i);
		}
		int[] w = new int[wList.size()];
		for (int i = 0; i < wList.size(); i++) {
			w[i] = wList.get(i);
		}
		float[] y = new float[yList.size()];
		for (int i = 0; i < yList.size(); i++) {
			y[i] = yList.get(i);
		}
        String id = tier.getLabel();

        GraphIntervalSym gsym = new GraphIntervalSym(x, w, y, id, seq);
		GraphState state = new GraphState(tier.getAnnotStyle());
		GraphGlyph graph_glyph = new GraphGlyph(gsym, state) {
			public String getLabel() {
				return super.getLabel() + " at zoom " + level;
			}
		};
		graph_glyph.drawHandle(false);
		graph_glyph.setSelectable(false);
		graph_glyph.setGraphStyle(GraphType.STAIRSTEP_GRAPH);
		graph_glyph.setCoords(startBase, 0, length, tier.getCoordBox().getHeight());
		return graph_glyph;
	}

	/* from bbiRead.c */
	private BBZoomLevelHeader bbiBestZoom(ArrayList<BBZoomLevelHeader> levelList, int desiredReduction)
	/* Return zoom level that is the closest one that is less than or equal to 
	 * desiredReduction. */
	{
		if (desiredReduction <= 1) {
		    return null;
		}
		int closestDiff = Integer.MAX_VALUE;
		BBZoomLevelHeader closestLevel = null;
	
		for (BBZoomLevelHeader level : levelList)
		{
		    int diff = desiredReduction - level.getReductionLevel();
		    if (diff >= 0 && diff < closestDiff)
		    {
			    closestDiff = diff;
			    closestLevel = level;
			}
		}
		return closestLevel;
	}
}
