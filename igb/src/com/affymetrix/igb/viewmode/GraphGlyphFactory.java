
package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class GraphGlyphFactory extends AbstractGraphGlyphFactory {
	private final String name;
	Class<? extends AbstractGraphGlyph> clazz;
	
	public GraphGlyphFactory(Class<? extends AbstractGraphGlyph> clazz){
		this.clazz = clazz;
		AbstractGraphGlyph newInstance = createInstance(null, null, null);
		if(newInstance != null){
			this.name = newInstance.getName();
		}else{
			this.name = "";
		}
		
	}
	
	@Override
	protected AbstractGraphGlyph createViewModeGlyph(GraphSym newgraf, GraphState gstate, SeqMapViewExtendedI smv) {
		return createInstance(newgraf, gstate, smv);
	}

	@Override
	public String getName() {
		return name;
	}
	
	private AbstractGraphGlyph createInstance(GraphSym newgraf, GraphState gstate, SeqMapViewExtendedI smv){
		try {
			AbstractGraphGlyph result = clazz.getConstructor(new Class[]{GraphSym.class, GraphState.class}).newInstance(new Object[]{newgraf, gstate});
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
