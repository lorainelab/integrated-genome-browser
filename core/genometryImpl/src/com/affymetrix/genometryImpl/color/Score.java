package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author hiralv
 */
public class Score extends ColorProvider {
	
	public static String MIN_SCORE = "min";
	public static String MAX_SCORE = "max";
	public static float DEFAULT_MIN_SCORE = 1.0f;
	public static float DEFAULT_MAX_SCORE = 1000.0f;
	
	private static Map<String, Class<?>> PARAMETERS = new HashMap<String, Class<?>>();
	static {
		PARAMETERS.put(MIN_SCORE, Float.class);
		PARAMETERS.put(MAX_SCORE, Float.class);
	}
	
	private float min_score_color = DEFAULT_MIN_SCORE;
	private float max_score_color = DEFAULT_MAX_SCORE;
	private HeatMap custom_heatmap;
	private ITrackStyle style;

	public Score(){
		this.style = style;
	}
	
	public Score(ITrackStyle style){
		this.style = style;
		this.custom_heatmap = generateNewHeatmap(style);
	}
	
	@Override
	public Color getColor(Object obj){
		if(obj instanceof Scored) {
			float score = ((Scored) obj).getScore();
			if (score != Float.NEGATIVE_INFINITY && score > 0.0f) {
				return getScoreColor(score);
			}
		}
		return null;
	}
	
	@Override
	public void update(){
		custom_heatmap = generateNewHeatmap(style);
	}
	
	/**
	 * Returns a HeatMap that interpolates between colors based on getColor()
	 * and getBackgroundColor(). The color at the low end of the HeatMap will be
	 * slightly different from the background color so that it can be
	 * distinguished from it. This will return a HeatMap even if
	 * getColorByScore() is false.
	 */
	private static HeatMap generateNewHeatmap(ITrackStyle style){
		Color bottom_color = HeatMap.interpolateColor(style.getForeground(), style.getBackground(), 0.20f);
		return HeatMap.makeLinearHeatmap("Custom", bottom_color, style.getForeground());
	}
	
	public void setMinScoreColor(float min_score_color) {
		this.min_score_color = min_score_color;
	}

	public float getMinScoreColor() {
		return min_score_color;
	}

	public void setMaxScoreColor(float max_score_color) {
		this.max_score_color = max_score_color;
	}

	public float getMaxScoreColor() {
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
	public void setParameters(Map<String, Object> params){
		for(Entry<String, Object> param : params.entrySet()){
			setParameter(param.getKey(), param.getValue());
		}
	}

	@Override
	public boolean setParameter(String key, Object value){
		if(MIN_SCORE.equals(key) && value instanceof Number){
			min_score_color = (Float)value;
			return true;
		} else if (MAX_SCORE.equals(key) && value instanceof Number){
			max_score_color = (Float)value;
			return true;
		}
		return false;
	}
}
