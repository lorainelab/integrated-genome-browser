package com.affymetrix.igb.colorproviders;

import com.affymetrix.genometryImpl.color.ColorProvider;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.HeatMapExtended;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import cytoscape.visual.ui.editors.continuous.BasicVirtualRange;
import cytoscape.visual.ui.editors.continuous.ColorInterpolator;
import cytoscape.visual.ui.editors.continuous.GradientColorInterpolator;
import cytoscape.visual.ui.editors.continuous.VirtualRange;
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
	public static final VirtualRange VIRTUAL_RANGE = new BasicVirtualRange(DEFAULT_VALUES, DEFAULT_COLORS);
	public static final ColorInterpolator COLOR_INTERPOLATOR = new GradientColorInterpolator(VIRTUAL_RANGE);
	public final static HeatMap DEFAULT_HEATMAP = new HeatMapExtended("Default_Mapq_HeatMapExtended",
										COLOR_INTERPOLATOR.getColorRange(HeatMap.BINS),
										VIRTUAL_RANGE.getVirtualValues(),
										VIRTUAL_RANGE.getColors());
		
	private float min_score_color  = DEFAULT_MIN_SCORE;
	private float max_score_color  = DEFAULT_MAX_SCORE;
	private float range			   = max_score_color - min_score_color;
	private Color botton_color		= DEFAULT_COLORS[0];
	private Color top_color			= DEFAULT_COLORS[DEFAULT_COLORS.length - 1];
	
	private Parameter<HeatMap> custom_heatmap = new Parameter<HeatMap>(DEFAULT_HEATMAP){
		@Override
		public boolean set(Object e){
			super.set(e);
			HeatMapExtended heatmap = (HeatMapExtended)e;
			min_score_color = heatmap.getValues()[0];
			max_score_color = heatmap.getValues()[heatmap.getValues().length - 1];
			range = max_score_color - min_score_color;
			botton_color	= heatmap.getRangeColors()[0];
			top_color		= heatmap.getRangeColors()[heatmap.getValues().length - 1];
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

	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory) {
		return fileTypeCategory == FileTypeCategory.Alignment;
	}
	
	protected Color getScoreColor(float score) {
		if (score < min_score_color) {
			return botton_color;
		} else if (score > max_score_color) {
			return top_color;
		}

		int index = (int) (((score - min_score_color) / range) * HeatMap.BINS);

		return custom_heatmap.get().getColors()[index];
	}
}
