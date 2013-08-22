package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.HeatMapExtended;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class MapqScore extends ColorProvider {
	
	private final static String HEATMAP = "heatmap";
	public final static float DEFAULT_MIN_SCORE = 1.0f;
	public final static float DEFAULT_MAX_SCORE = 254.0f;
	public static final float[] DEFAULT_VALUES = new float[]{0,50,200,254};
	public static final Color[] DEFAULT_COLORS = new Color[]{Color.BLACK, Color.BLACK, Color.RED, Color.RED};
	public final static HeatMap DEFAULT_HEATMAP = new HeatMapExtended(
			"DEFAULT_MAPQSCORE_HEATMAP", null, DEFAULT_VALUES, DEFAULT_COLORS);
		
	private float min_score_color  = DEFAULT_MIN_SCORE;
	private float max_score_color  = DEFAULT_MAX_SCORE;
	private float range			   = max_score_color - min_score_color;
	
	private Parameter<HeatMap> custom_heatmap = new Parameter<HeatMap>(DEFAULT_HEATMAP){
		@Override
		public boolean set(Object e){
			super.set(e);
			HeatMapExtended heatmap = (HeatMapExtended)e;
			min_score_color = heatmap.getValues()[0];
			max_score_color = heatmap.getValues()[heatmap.getValues().length - 1];
			range = max_score_color - min_score_color;
			return true;
		}
	};
	
	public MapqScore(){
		super();
		parameters.addParameter(HEATMAP, HeatMapExtended.class, custom_heatmap);
	}
	
	@Override
	public Color getColor(SeqSymmetry sym){
		if(sym instanceof BAMSym) {
			int score = ((BAMSym) sym).getMapq();
			if(score != BAMSym.NO_MAPQ) {
				return getScoreColor(score);
			}
		}
		return null;
	}

	protected Color getScoreColor(float score) {
		if (score <= min_score_color) {
			score = min_score_color;
		} else if (score >= max_score_color) {
			score = max_score_color;
		}

		int index = (int) (((score - min_score_color) / range) * 255);

		return custom_heatmap.get().getColors()[index];
	}
}
