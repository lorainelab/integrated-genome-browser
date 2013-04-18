package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class Score implements ColorProvider {
	
	private float min_score_color = 1.0f;
	private float max_score_color = 1000f;
	private HeatMap custom_heatmap;
	private final ITrackStyle style;
	
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
}
