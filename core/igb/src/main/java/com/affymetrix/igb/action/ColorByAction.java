package com.affymetrix.igb.action;

import com.affymetrix.genometry.color.ColorProviderI;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.general.SupportsFileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.ConfigureOptionsPanel;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import com.affymetrix.igb.util.ConfigureOptionsDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 *
 * @author hiralv
 */
public class ColorByAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static final ColorByAction ACTION = new ColorByAction("colorByAction");
    private static String colorsRootNodeName = "";

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ColorByAction getAction() {
        return ACTION;
    }

    private ColorByAction(String transKey) {
        super(BUNDLE.getString(transKey), "16x16/actions/blank_placeholder.png", null);
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);

        final TierGlyph tg = getTierManager().getSelectedTiers().get(0);
        ITrackStyleExtended style = tg.getAnnotStyle();

        //this is to get all the preferences nodes and pass them to ConfigureOptionsPanel so that preferences related to colorproviders of all tracks can be updated
        //to store history in heatmap. Not good design and need to find better solution.
        List<Preferences> trackroots = new ArrayList<>();
        getTierManager().getSelectedTiers().stream().
                forEach(glyph -> glyph.getAnnotStyle().getPreferenceChildForProperty(colorsRootNodeName).ifPresent(root -> trackroots.add(root)));
        ColorProviderI cp = style.getColorProvider();

        ConfigureOptionsPanel.Filter<ColorProviderI> configureFilter = colorProvider -> {
            if (colorProvider instanceof SupportsFileTypeCategory) {
                Optional<FileTypeCategory> category = tg.getFileTypeCategory();
                if (category.isPresent()) {
                    return colorProvider.isFileTypeCategorySupported(category.get());
                } else {
                    return false;
                }
            }
            return true;
        };

        ConfigureOptionsDialog<ColorProviderI> colorByDialog = new ConfigureOptionsDialog<>(ColorProviderI.class, "Color By", configureFilter, trackroots);
        colorByDialog.setTitle("Color By");
        colorByDialog.setLocationRelativeTo(getSeqMapView());
        colorByDialog.setInitialValue(cp);
        ColorProviderI newCp = colorByDialog.showDialog();

        //set color provider to all selected tiers only if it is changed..
        //We do not know if user pressed cancel or okay on selection dialog. Hence need to see if CP changed or not.
        if (!newCp.equals(cp)) {
            getTierManager().getSelectedTiers().forEach(tm -> tm.getAnnotStyle().setColorProvider(newCp));
        }
//        style.setColorProvider(cp);
        refreshMap(false, false);
        //TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }
}
