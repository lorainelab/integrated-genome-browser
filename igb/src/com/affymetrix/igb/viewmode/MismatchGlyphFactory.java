package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.MisMatchPileupGraphSym;
import com.affymetrix.igb.shared.ResidueColorHelper;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class MismatchGlyphFactory extends AbstractGraphGlyphFactory {

	private static final class MismatchPileupGlyph extends FillBarGraphGlyph {

		private static final Map<Character, int[]> BAR_ORDERS = new HashMap<Character, int[]>();

		static {
			BAR_ORDERS.put('A', new int[]{1, 2, 3, 4});
			BAR_ORDERS.put('T', new int[]{0, 2, 3, 4});
			BAR_ORDERS.put('G', new int[]{0, 1, 3, 4});
			BAR_ORDERS.put('C', new int[]{0, 1, 2, 4});
			BAR_ORDERS.put('N', new int[]{0, 1, 2, 3});
		}
		private static final Color MATCH_COLOR = Color.GRAY;
		private static Color[] baseColors;

		private static void setBaseColor() {
			baseColors = new Color[]{
				ResidueColorHelper.getColorHelper().determineResidueColor('A'),
				ResidueColorHelper.getColorHelper().determineResidueColor('T'),
				ResidueColorHelper.getColorHelper().determineResidueColor('G'),
				ResidueColorHelper.getColorHelper().determineResidueColor('C'),
				ResidueColorHelper.getColorHelper().determineResidueColor('N')
			};
		}

		public MismatchPileupGlyph(GraphSym graf, GraphState gstate) {
			super(graf, gstate);
		}

		public GraphType getGraphStyle() {
			return GraphType.STAIRSTEP_GRAPH;
		}

		@Override
		protected void doBigDraw(Graphics g, GraphSym graphSym, Point curr_x_plus_width, Point max_x_plus_width, float ytemp, int draw_end_index, int i) {
			setBaseColor();
			MisMatchPileupGraphSym mmgs = (MisMatchPileupGraphSym) graphSym;
			Color saveColor = g.getColor();

			final int width = Math.max(1, curr_x_plus_width.x - curr_point.x - 1);
			BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
			if (!mmgs.hasReferenceSequence()) {
				seq.getResidues(graphSym.getMinXCoord(), graphSym.getMaxXCoord()); // so all are loaded, not one by one
			}
			// need to draw coverage first, then mismatches so that the mismatches are not covered up
			g.setColor(MATCH_COLOR);
			super.doBigDraw(g, graphSym, curr_x_plus_width, max_x_plus_width, ytemp, draw_end_index, i);

			// now draw the mismatches, piled up

			// flipping about yaxis... should probably make this optional
			// also offsetting to place within glyph bounds
			int xtemp = graphSym.getGraphXCoord(i);
			coord.x = xtemp;
			Point2D.Double x_plus_width2D = new Point2D.Double(0, 0);
			x_plus_width2D.x = xtemp + 1;
			int savey = zero_point.y;
			char referenceBase = mmgs.hasReferenceSequence()
					? mmgs.getReferenceBase(i)
					: seq.getResidues(xtemp, xtemp + 1).toUpperCase().charAt(0);
			float y_accum = 0;
			int[] barOrder = BAR_ORDERS.get(referenceBase);
			if (barOrder == null) {
				return;
			}
			float[] residuesY = mmgs.getAllResiduesY(i);
			for (int j = 0; j < barOrder.length; j++) {
				int loopIndex = barOrder[j];
				float _ytemp = residuesY[loopIndex];
				if (_ytemp == 0) {
					continue;
				}
				if (Double.isNaN(_ytemp) || Double.isInfinite(_ytemp)) {
					return;
				}
				_ytemp += y_accum;
				y_accum = _ytemp;
				// flattening any points > getVisibleMaxY() or < getVisibleMinY()...
				_ytemp = Math.min(_ytemp, getVisibleMaxY());
				_ytemp = Math.max(_ytemp, getVisibleMinY());

//				coord.y = offset - ((_ytemp - getVisibleMinY()) * yscale);
//				view.transformToPixels(coord, curr_point);
//				x_plus_width2D.y = coord.y;
//				view.transformToPixels(x_plus_width2D, curr_x_plus_width);

				int ymin_pixel = Math.min(curr_point.y, savey);
				int yheight_pixel = Math.abs(curr_point.y - savey);
				yheight_pixel = Math.max(1, yheight_pixel);
				g.setColor(baseColors[loopIndex]);
				g.fillRect(curr_point.x, ymin_pixel, width, yheight_pixel);
				savey = curr_point.y;

			}
			g.setColor(saveColor);
		}
	}

	public String getName() {
		return "mismatch";
	}

	@Override
	protected AbstractGraphGlyph createViewModeGlyph(GraphSym newgraf, GraphState gstate) {
		if(newgraf instanceof MisMatchPileupGraphSym)
			return new MismatchPileupGlyph(newgraf, gstate);
		
		return new FillBarGraphGlyph(newgraf, gstate); 
	}
	
	@Override
	public boolean isFileSupported(FileTypeCategory category) {
		if (category == FileTypeCategory.Mismatch){
			return true;
		}
		return false;
	}
}
