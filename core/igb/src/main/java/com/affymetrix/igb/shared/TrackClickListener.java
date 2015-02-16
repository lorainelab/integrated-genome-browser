package com.affymetrix.igb.shared;

import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import java.util.List;
import javax.swing.JPopupMenu;

public interface TrackClickListener {

    public void trackClickNotify(JPopupMenu popup, List<TierGlyph> selectedGlyphs);
}
