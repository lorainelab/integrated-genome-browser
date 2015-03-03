package com.affymetrix.igb.view.factories;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.GraphState;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.MisMatchPileupGraphSym;
import com.affymetrix.igb.graphTypes.MismatchGraphType;
import com.affymetrix.igb.graphTypes.MismatchPileupType;
import com.lorainelab.igb.genoviz.extensions.GraphGlyph;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 *
 * @author hiralv
 */
@Component(name = MismatchGlyphFactory.COMPONENT_NAME, provide = {MapTierGlyphFactoryI.class}, immediate = true)
public class MismatchGlyphFactory extends GraphGlyphFactory {

    public static final String COMPONENT_NAME = "MismatchGlyphFactory";

    @Override
    public String getName() {
        return COMPONENT_NAME;
    }

    @Override
    protected void setGraphType(GraphSym newgraf, GraphState gstate, GraphGlyph graphGlyph) {
        graphGlyph.setGraphStyle(newgraf instanceof MisMatchPileupGraphSym ? new MismatchPileupType(graphGlyph) : new MismatchGraphType(graphGlyph));
    }

    @Override
    public Set<FileTypeCategory> getSupportedCategories() {
        return ImmutableSet.<FileTypeCategory>builder()
                .add(FileTypeCategory.Mismatch).build();
    }
}
