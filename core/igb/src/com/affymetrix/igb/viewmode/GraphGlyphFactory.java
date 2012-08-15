
package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.GraphGlyph.GraphStyle;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class GraphGlyphFactory extends AbstractGraphGlyphFactory {
	Class<? extends GraphStyle> preferredGraphType;
	
	public GraphGlyphFactory(Class<? extends GraphStyle> clazz){
		this.preferredGraphType = clazz;		
	}
	
	@Override
	protected AbstractGraphGlyph createViewModeGlyph(GraphSym newgraf, GraphState gstate, SeqMapViewExtendedI smv) {
		return createInstance(newgraf, gstate, smv);
	}

	@Override
	public String getName() {
		return "Graph";
	}
	
	private AbstractGraphGlyph createInstance(GraphSym newgraf, GraphState gstate, SeqMapViewExtendedI smv){
		try {
			GraphGlyph graphGlyph = new GraphGlyph(newgraf, gstate);
			AbstractGraphGlyph result = new AbstractGraphGlyph(graphGlyph);
			GraphStyle style = preferredGraphType.getConstructor(new Class[]{GraphGlyph.class}).newInstance(graphGlyph);
			result.getGraphGlyph().setGraphStyle(style);
			if(smv != null){
				result.setMinimumPixelBounds(smv.getSeqMap().getGraphics());
			}
			return result;
		} catch (Exception ex) {
			Logger.getLogger(GraphGlyphFactory.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}
}
