package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class Score implements ColorProvider {
	
	private float min_score_color = 1.0f;
	private float max_score_color = 1000f;
	private final HeatMap custom_heatmap;
	
	public Score(Color foreGround, Color backGround){
		Color bottom_color = HeatMap.interpolateColor(foreGround, backGround, 0.20f);
		custom_heatmap = HeatMap.makeLinearHeatmap("Custom", bottom_color, foreGround);
	}
	
	@Override
	public Color getColor(SymWithProps sym){
		if(sym instanceof Scored) {
			float score = ((Scored) sym).getScore();
			if (score != Float.NEGATIVE_INFINITY && score > 0.0f) {
				return getScoreColor(score);
			}
		}
		return null;
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
