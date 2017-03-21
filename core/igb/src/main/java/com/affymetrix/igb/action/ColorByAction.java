package com.affymetrix.igb.action;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.color.ColorProviderI;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.general.SupportsFileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.HeatMapExtended;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.ConfigureOptionsPanel;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import com.affymetrix.igb.util.ConfigureOptionsDialog;
import com.google.gson.Gson;
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

        Preferences trackroot = null;
        if (style.getPreferenceChildForProperty(colorsRootNodeName).isPresent()) {
            trackroot = style.getPreferenceChildForProperty(colorsRootNodeName).get();
        }
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

        ConfigureOptionsDialog<ColorProviderI> colorByDialog = new ConfigureOptionsDialog<>(ColorProviderI.class, "Color By", configureFilter, trackroot);
        colorByDialog.setTitle("Color By");
        colorByDialog.setLocationRelativeTo(getSeqMapView());
        colorByDialog.setInitialValue(cp);
        cp = colorByDialog.showDialog();

        style.setColorProvider(cp);
        refreshMap(false, false);
        //TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }
}
