package com.lorainelab.igb.genoviz.extensions.api;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import java.awt.geom.Rectangle2D;
import java.util.List;

public interface TierGlyph extends GlyphI, StyledGlyph {

    public static int MINIMUM_TIER_HEIGHT = 45;

    public static enum TierType {

        ANNOTATION, GRAPH, SEQUENCE, NONE
    }

    public void setTierType(TierType method);

    public TierType getTierType();

    public void setStyle(ITrackStyleExtended annotStyle);

    public void setDirection(Direction direction);

    public void addMiddleGlyphs(BioSeq seq);

    public void addMiddleGlyph(GlyphI mglyph);

    public void clearMiddleGlyphs();

    public List<SeqSymmetry> getSelected();

    public int getActualSlots();

    public int getSlotsNeeded(ViewI view);

    public boolean isManuallyResizable();

    public void resizeHeight(double d, double height);

    public double getChildHeight();

    public void setPreferredHeight(double maxHeight, ViewI view);

    public boolean toolBarHit(Rectangle2D.Double hitrect, ViewI view);

    public List<GlyphI> pickTraversal(Rectangle2D.Double coordrect, ViewI view);

    public <G extends GlyphI> G getItem(Object datamodel);

    public boolean reomveItem(GlyphI glyph);

    public void setDataModelFromOriginalSym(GlyphI glyph, Object datamodel);
}
