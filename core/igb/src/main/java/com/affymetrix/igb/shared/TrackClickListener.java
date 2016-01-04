package com.affymetrix.igb.shared;

import org.lorainelab.igb.igb.genoviz.extensions.glyph.TierGlyph;
import java.util.List;
import javax.swing.JPopupMenu;

public interface TrackClickListener {

    public void trackClickNotify(JPopupMenu popup, List<TierGlyph> selectedGlyphs);
}
