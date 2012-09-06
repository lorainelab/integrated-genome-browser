
package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphIntervalSym;
import com.affymetrix.igb.shared.GraphTierGlyph;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;

/**
 *
 * @author hiralv
 */
public class ScoredContainerGlyphFactory extends AbstractScoredContainerGlyphFactory{
	final GraphGlyphFactory graphGlyphFactory;
	
	public ScoredContainerGlyphFactory(GraphGlyphFactory graphGlyphFactory){
		this.graphGlyphFactory = graphGlyphFactory;
	}
	
	@Override
	protected GraphGlyph createViewModeGlyph(GraphIntervalSym graf, GraphState graphState, SeqMapViewExtendedI smv) {
//		return graphGlyphFactory.setGraphType(graf, graphState, smv);
		return null;
	}
	
	@Override
	public String getName(){
		return (super.getName() + "_" + graphGlyphFactory.getName());
	}
}
