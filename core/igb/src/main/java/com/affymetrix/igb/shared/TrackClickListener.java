package com.affymetrix.igb.shared;

import java.util.List;

import javax.swing.JPopupMenu;

public interface TrackClickListener {

    public void trackClickNotify(JPopupMenu popup, List<TierGlyph> selectedGlyphs);
}
