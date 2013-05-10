package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.HeatMapExtended;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class Score extends ColorProvider {
	
	private final static String COLOR_1 = "color 1";
	private final static String COLOR_2 = "color 2";
	private final static String MIN_SCORE = "min";
	private final static String MAX_SCORE = "max";
	private final static String STYLE = "style";
	private final static String HEATMAP = "heatmap";
	public final static float DEFAULT_MIN_SCORE = 1.0f;
	public final static float DEFAULT_MAX_SCORE = 1000.0f;
	public final static Color DEFAULT_COLOR_1 = new Color(204, 255, 255);
	public final static Color DEFAULT_COLOR_2 = new Color(51, 255, 255);
	
	private final static Map<String, Class<?>> PARAMETERS = new LinkedHashMap<String, Class<?>>();
	static {
		PARAMETERS.put(MIN_SCORE, Float.class);
		PARAMETERS.put(MAX_SCORE, Float.class);
		PARAMETERS.put(COLOR_1, Color.class);
		PARAMETERS.put(COLOR_2, Color.class);
//		PARAMETERS.put(HEATMAP, HeatMapExtended.class);
	}
	
	private float min_score_color = DEFAULT_MIN_SCORE;
	private float max_score_color = DEFAULT_MAX_SCORE;
	private Color color_1 = DEFAULT_COLOR_1;
	private Color color_2 = DEFAULT_COLOR_2;
	private HeatMap custom_heatmap;
	
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
		custom_heatmap = generateNewHeatmap(color_1, color_2);
	}
	
	/**
	 * Returns a HeatMap that interpolates between colors based on getColor()
	 * and getBackgroundColor(). The color at the low end of the HeatMap will be
	 * slightly different from the background color so that it can be
	 * distinguished from it. This will return a HeatMap even if
	 * getColorByScore() is false.
	 */	
	private static HeatMap generateNewHeatmap(Color color1, Color color2){
		Color bottom_color = HeatMap.interpolateColor(color1, color2, 0.20f);
		return HeatMap.makeLinearHeatmap("Custom", bottom_color, color1);
	}
	
	public void setTrackStyle(ITrackStyle style){
		color_1 = style.getForeground();
		color_2 = style.getBackground();
		update();
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
		} if(COLOR_1.equals(key) && value instanceof Color){
			color_1 = (Color)value;
			update();
			return true;
		} else if (COLOR_2.equals(key) && value instanceof Color){
			color_2 = (Color)value;
			update();
			return true;
		} else if (STYLE.equals(key) && value instanceof ITrackStyle){
			color_1 = ((ITrackStyle)value).getForeground();
			color_2 = ((ITrackStyle)value).getBackground();
			update();
			return true;
		} else if (HEATMAP.equals(key) && value instanceof HeatMapExtended){
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
		if(MIN_SCORE.equals(key)){
			return min_score_color;
		} else if (MAX_SCORE.equals(key)){
			return max_score_color;
		} if(COLOR_1.equals(key)){
			return color_1;
		} else if (COLOR_2.equals(key)){
			return color_2;
		} 
		return null;
	}
}
