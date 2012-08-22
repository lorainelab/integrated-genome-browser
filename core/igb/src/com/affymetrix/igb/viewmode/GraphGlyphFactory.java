
package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.igb.graphTypes.BarGraphGlyph;
import com.affymetrix.igb.graphTypes.DotGraphGlyph;
import com.affymetrix.igb.graphTypes.FillBarGraphGlyph;
import com.affymetrix.igb.graphTypes.HeatMapGraphGlyph;
import com.affymetrix.igb.graphTypes.LineGraphGlyph;
import com.affymetrix.igb.graphTypes.MinMaxAvgGraphGlyph;
import com.affymetrix.igb.graphTypes.StairStepGraphGlyph;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.GraphGlyph.GraphStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class GraphGlyphFactory extends AbstractGraphGlyphFactory {
	public final static Map<GraphType, Class<? extends GraphStyle>> type2Style;
	static{
		type2Style = new HashMap<GraphType, Class<? extends GraphStyle>>();
		type2Style.put(GraphType.BAR_GRAPH, BarGraphGlyph.class);
		type2Style.put(GraphType.DOT_GRAPH, DotGraphGlyph.class);
		type2Style.put(GraphType.FILL_BAR_GRAPH, FillBarGraphGlyph.class);
		type2Style.put(GraphType.HEAT_MAP, HeatMapGraphGlyph.class);
		type2Style.put(GraphType.LINE_GRAPH, LineGraphGlyph.class);
		type2Style.put(GraphType.MINMAXAVG, MinMaxAvgGraphGlyph.class);
		type2Style.put(GraphType.STAIRSTEP_GRAPH, StairStepGraphGlyph.class);
	}
	
	@Override
	protected void setGraphType(GraphSym newgraf, GraphState gstate, GraphGlyph graphGlyph) {
		try {
			GraphStyle style = type2Style.get(gstate.getGraphStyle()).getConstructor(new Class[]{GraphGlyph.class}).newInstance(graphGlyph);
			graphGlyph.setGraphStyle(style);
			
		} catch (Exception ex) {
			Logger.getLogger(GraphGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
			graphGlyph.setGraphStyle(new MinMaxAvgGraphGlyph(graphGlyph));
		}
	}
	

	@Override
	public String getName() {
		return "Graph";
	}
	
		
}
