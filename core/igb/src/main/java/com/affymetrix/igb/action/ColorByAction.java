package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.color.ColorProviderI;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.general.SupportsFileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.ConfigureOptionsPanel;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.igb.util.ConfigureOptionsDialog;
import java.util.Optional;

/**
 *
 * @author hiralv
 */
public class ColorByAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static final ColorByAction ACTION = new ColorByAction("colorByAction");

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

        ConfigureOptionsDialog<ColorProviderI> colorByDialog = new ConfigureOptionsDialog<>(ColorProviderI.class, "Color By", configureFilter);
        colorByDialog.setTitle("Color By");
        colorByDialog.setLocationRelativeTo(getSeqMapView());
        colorByDialog.setInitialValue(cp);
        cp = colorByDialog.showDialog();

        style.setColorProvider(cp);
        refreshMap(false, false);
        //TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }
}
