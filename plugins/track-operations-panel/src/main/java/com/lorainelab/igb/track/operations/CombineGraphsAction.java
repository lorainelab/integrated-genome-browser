package com.lorainelab.igb.track.operations;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.style.GraphState;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.style.SimpleTrackStyle;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.igb.service.api.IgbService;

import static com.affymetrix.igb.shared.Selections.*;

/**
 * Puts all selected graphs in the same tier. Current glyph factories do not
 * support floating the combined graphs.
 */
public class CombineGraphsAction extends GenericAction {

    private static final long serialVersionUID = 1l;

    public CombineGraphsAction(IgbService igbService) {
        super("Join", null, null);
        this.igbService = igbService;
    }

    private final IgbService igbService;

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        int gcount = rootSyms.size();
        float height = 0;

        // Note that the combo_style does not implement IFloatableTierStyle
        // because the glyph factory doesn't support floating combo graphs anyway.
        ITrackStyleExtended combo_style = null;

        Map<Color, Integer> colorMap = new HashMap<>();
        // If any of them already has a combo style, use that one
        for (int i = 0; i < gcount && combo_style == null; i++) {
            if (rootSyms.get(i) instanceof GraphSym) {
                GraphSym gsym = (GraphSym) rootSyms.get(i);
                gsym.getGraphState().getTierStyle().setJoin(true);
                combo_style = gsym.getGraphState().getComboStyle();
                Color col = gsym.getGraphState().getTierStyle().getBackground();
                int c = 0;
                if (colorMap.containsKey(col)) {
                    c = colorMap.get(col) + 1;
                }
                colorMap.put(col, c);
            }
        }

        // otherwise, construct a new combo style
        if (combo_style == null) {
            combo_style = createComboStyle(igbService, colorMap);
        }

        // Now apply that combo style to all the selected graphs
        int i = 0;
        for (SeqSymmetry sym : rootSyms) {
            if (sym instanceof GraphSym) {
                GraphSym gsym = (GraphSym) sym;
                GraphState gstate = gsym.getGraphState();
                gstate.setComboStyle(combo_style, i++);
                gstate.getTierStyle().setFloatTier(false); // ignored since combo_style is set
                height += gsym.getGraphState().getTierStyle().getHeight();
            }
        }
        combo_style.setHeight(height);
        updateDisplay();
    }

    private void updateDisplay() {
        ThreadUtils.runOnEventQueue(() -> {
//				igbService.getSeqMap().updateWidget();
//				igbService.getSeqMapView().setTierStyles();
//				igbService.getSeqMapView().repackTheTiers(true, true);
            igbService.getSeqMapView().updatePanel(true, true);
        });
    }

    private static ITrackStyleExtended createComboStyle(IgbService igbService, Map<Color, Integer> colorMap) {
        ITrackStyleExtended combo_style = new SimpleTrackStyle("Joined Graphs", true);
        combo_style.setTrackName("Joined Graphs");
        combo_style.setExpandable(true);
        //	combo_style.setCollapsed(true);
        //combo_style.setLabelForeground(igbService.getDefaultForegroundColor());
        combo_style.setForeground(igbService.getDefaultForegroundColor());
        Color background = igbService.getDefaultBackgroundColor();
        int c = -1;
        for (Entry<Color, Integer> color : colorMap.entrySet()) {
            if (color.getValue() > c) {
                background = color.getKey();
                c = color.getValue();
            }
        }
        //combo_style.setLabelBackground(background);
        combo_style.setBackground(background);
        combo_style.setTrackNameSize(igbService.getDefaultTrackSize());

        return combo_style;
    }
}
