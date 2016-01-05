/*  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.IGBConstants;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.util.ThresholdReader;
import java.awt.event.ActionEvent;
import javax.swing.JSlider;

public class SetSummaryThresholdAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static final SetSummaryThresholdAction ACTION = new SetSummaryThresholdAction();

//	static{
//		GenericActionHolder.getInstance().addGenericAction(ACTION);
//	}
//	
//	public static SetSummaryThresholdAction getAction() {
//		return ACTION;
//	}
    protected SetSummaryThresholdAction() {
        super(IGBConstants.BUNDLE.getString("setSummaryThresholdAction"),
                "16x16/actions/set summary threshold.png", "22x22/actions/set summary threshold.png");
        this.ordinal = -4006000;
    }

    public boolean isDetail(ITrackStyleExtended style) {
        int trackThreshold = PreferenceUtils.getIntParam(PreferenceUtils.PREFS_THRESHOLD, ThresholdReader.default_threshold);
        if (style != null && style.getSummaryThreshold() > 0) {
            trackThreshold = style.getSummaryThreshold();
        }
        return ThresholdReader.getInstance().isDetail(trackThreshold);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        int summaryThreshold;
        for (TierGlyph tierGlyph : getTierManager().getSelectedTiers()) {

            ITrackStyleExtended style = tierGlyph.getAnnotStyle();
            if (style != null && style instanceof TrackStyle) {
                boolean isDetail = isDetail(style);
                if (isDetail) {
                    summaryThreshold = ThresholdReader.getInstance().toSummary();
                } else {
                    summaryThreshold = ThresholdReader.getInstance().toDetail();
                }
                style.setSummaryThreshold(summaryThreshold);
                if (tierGlyph instanceof NeoRangeListener) {
                    NeoRangeEvent newevt = new NeoRangeEvent(getSeqMapView(), tierGlyph.getCoordBox().x, tierGlyph.getCoordBox().x + tierGlyph.getCoordBox().width);
                    ((NeoRangeListener) tierGlyph).rangeChanged(newevt);
                }
            }
        }
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
        getSeqMapView().getSeqMap().updateWidget();
        ((JSlider) getSeqMapView().getSeqMap().getZoomer(NeoMap.X)).repaint();
    }
}
