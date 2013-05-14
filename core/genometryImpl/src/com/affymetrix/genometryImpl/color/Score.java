package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.HeatMapExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class Score extends ColorProvider {
	
	private final static String HEATMAP = "heatmap";
	public final static float DEFAULT_MIN_SCORE = 1.0f;
	public final static float DEFAULT_MAX_SCORE = 1000.0f;
	public final static HeatMap DEFAULT_HEATMAP = HeatMap.StandardHeatMap.BLACK_WHITE.getHeatMap();
	
	private final static Map<String, Class<?>> PARAMETERS = new LinkedHashMap<String, Class<?>>();
	static {
		PARAMETERS.put(HEATMAP, HeatMapExtended.class);
	}
	
	private float min_score_color  = DEFAULT_MIN_SCORE;
	private float max_score_color  = DEFAULT_MAX_SCORE;
	private HeatMap custom_heatmap = DEFAULT_HEATMAP;
	
	@Override
	public Color getColor(SeqSymmetry sym){
		if(sym instanceof Scored) {
			float score = ((Scored) sym).getScore();
			if (score != Float.NEGATIVE_INFINITY && score > 0.0f) {
				return getScoreColor(score);
			}
		}
		return null;
	}
	
	@Override
	public void update(){
		
	}
	
	private float getMinScoreColor() {
		return min_score_color;
	}

	private float getMaxScoreColor() {
		return max_score_color;
	}
	
	private Color getScoreColor(float score) {
		final float min = getMinScoreColor();
		final float max = getMaxScoreColor();

		if (score < min) {
			score = min;
		} else if (score >= max) {
			score = max;
		}

		final float range = max - min;
		int index = (int) (((score - min) / range) * 255);

		return custom_heatmap.getColors()[index];
	}
	
	@Override
	public Map<String, Class<?>> getParameters(){
		return PARAMETERS;
	}

	@Override
	public boolean setParameter(String key, Object value){
		if (HEATMAP.equals(key) && value instanceof HeatMapExtended){
			HeatMapExtended heatmap = (HeatMapExtended)value;
			custom_heatmap = heatmap;
			min_score_color = heatmap.getValues()[0];
			max_score_color = heatmap.getValues()[heatmap.getValues().length - 1];
			return true;
		}
		return false;
	}
	
	@Override
	public Object getParameterValue(String key) {
		if (HEATMAP.equals(key)) {
			return custom_heatmap;
		}
		return null;
	}
}
