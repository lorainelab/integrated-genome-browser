package com.affymetrix.igb.glyph;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.UcscGffSym;
import com.affymetrix.igb.tiers.*;
import java.awt.*;

/**
 *  A glyph factory for displaying the 5-level hierarchy of SeqSymmetries
 *  resulting from parsing the special GFF files used for the WTA GeneChips.
 *  (WTA = Whole Transcript Array.)
 *
 *  <p>
 *  The hierarchy is as follows:<br>
 *  <ol>
 *  <li>transcript_cluster</li>
 *  <li>exon_cluster <i>or</i> intron_cluster</li>
 *  <li>psr</li>
 *  <li>probeset <i>or</i> probe_set</li>
 *  <li>probe</li>
 *  </ol>
 *  </p>
 *
 */
public class WTAGlyphFactory extends GenericAnnotGlyphFactory {

  public void addLeafsToTier(SeqSymmetry sym,
                             TierGlyph ftier, TierGlyph rtier,
                             int desired_leaf_depth) {
    if (sym instanceof UcscGffSym) {
      addLeafsToTier((UcscGffSym) sym, ftier, rtier);
    } else {
      // It is possible that there is a container SeqSymmetry that is not a UcscGffSym,
      // so allow the super-class method to recurse down to the next level.
      super.addLeafsToTier(sym, ftier, rtier, desired_leaf_depth);
    }
  }
  
  public void addLeafsToTier(UcscGffSym sym,
                             TierGlyph ftier, TierGlyph rtier) {
    // Creating glyphs based on feature type, not depth.
    // Depth is not a dependable categorization for these symmetries, because
    // there are some psr's that have no probe-set's in them, and so forth.
    
    String type = sym.getFeatureType();    
    
    this.label_field = null;
    if (type.startsWith("transcript")) { 
      // parent is transcript_cluster, child is exon_cluster
      addToTier(sym, ftier, rtier);
    }
    else if (type.startsWith("exon") || type.startsWith("intron")) { 
      // parent is exon_cluster or intron_cluster, child is psr
      addToTier(sym, ftier, rtier);
    }
    else if ("probeset".equals(type) || "probe-set".equals(type)) {
      //parent is probeset, child is probe
      addToTier(sym, ftier, rtier);
    }
    
    if (! type.startsWith("probe")) {
      for (int i=0; i<sym.getChildCount(); i++) {
        SeqSymmetry child = sym.getChild(i);
        addLeafsToTier((UcscGffSym) child, ftier, rtier);
      }
    }
  }
}

