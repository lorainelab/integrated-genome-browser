package com.affymetrix.igb.shared;

import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import java.util.List;

public interface TrackListProvider {

    public List<TierGlyph> getTrackList();
}
