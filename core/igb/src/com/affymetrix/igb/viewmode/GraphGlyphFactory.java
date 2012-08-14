
package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.AbstractGraphGlyph.GraphStyle;
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
			AbstractGraphGlyph result = new AbstractGraphGlyph(newgraf, gstate);
			GraphStyle style = preferredGraphType.getConstructor(new Class[]{AbstractGraphGlyph.class}).newInstance(result);
			result.setGraphStyle(style);
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
