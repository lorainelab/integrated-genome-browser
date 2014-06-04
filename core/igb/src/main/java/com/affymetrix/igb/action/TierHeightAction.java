package com.affymetrix.igb.action;

import com.affymetrix.igb.view.factories.DefaultTierGlyph;

/**
 *
 * @author hiralv
 */
public abstract class TierHeightAction extends SeqMapViewActionA {

    protected TierHeightAction(String name, String smallIcon, String largeIcon) {
        super(name, smallIcon, largeIcon);
    }

    protected abstract void setHeightFixed(DefaultTierGlyph dtg);
}
