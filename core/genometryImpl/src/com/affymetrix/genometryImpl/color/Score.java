package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author hiralv
 */
public class Score extends ColorProvider {
	
	private final static String MIN_SCORE = "min";
	private final static String MAX_SCORE = "max";
	private final static String STYLE = "style";
	public static float DEFAULT_MIN_SCORE = 1.0f;
	public static float DEFAULT_MAX_SCORE = 1000.0f;
	
	private final static Map<String, Class<?>> PARAMETERS = new HashMap<String, Class<?>>();
	static {
		PARAMETERS.put(MIN_SCORE, Float.class);
		PARAMETERS.put(MAX_SCORE, Float.class);
		PARAMETERS.put(STYLE, ITrackStyle.class);
	}
	
	private float min_score_color = DEFAULT_MIN_SCORE;
	private float max_score_color = DEFAULT_MAX_SCORE;
	private HeatMap custom_heatmap;
	private ITrackStyle style;

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
	
	public void setTrackStyle(ITrackStyle style){
		this.style = style;
		update();
	}
	
	public ITrackStyle getTrackStyle(){
		return style;
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
	public boolean setParameter(String key, Object value){
		if(MIN_SCORE.equals(key) && value instanceof Number){
			min_score_color = (Float)value;
			return true;
		} else if (MAX_SCORE.equals(key) && value instanceof Number){
			max_score_color = (Float)value;
			return true;
		} else if (STYLE.equals(key) && value instanceof ITrackStyle){
			style = (ITrackStyle)value;
			update();
			return true;
		}
		return false;
	}
	
	@Override
	public Object getParameterValue(String key) {
		if(MIN_SCORE.equals(key)){
			return min_score_color;
		} else if (MAX_SCORE.equals(key)){
			return max_score_color;
		} else if (STYLE.equals(key)){
			return style;
		}
		return null;
	}
}
