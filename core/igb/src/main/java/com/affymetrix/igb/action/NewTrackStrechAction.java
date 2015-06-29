package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import static com.affymetrix.igb.view.factories.AbstractTierGlyph.DEFAULT_TIER_GLYPH_HEIGHT;
import com.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class NewTrackStrechAction extends SeqMapViewActionA {

    private static final Logger logger = LoggerFactory.getLogger(NewTrackStrechAction.class);
    private static final NewTrackStrechAction ACTION = new NewTrackStrechAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static NewTrackStrechAction getAction() {
        return ACTION;
    }

    public NewTrackStrechAction() {
        super("Stretch Main View to fit new track", null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AffyTieredMap affyTieredMap = getSeqMapView().getSeqMap();

        ((AffyLabelledTierMap) affyTieredMap).packTiers(false, true, true);

        double[] pixelsPerCoord = affyTieredMap.getPixelsPerCoord();
        double pixelsPerCoordY = pixelsPerCoord[NeoMap.Y];
        double minCoordHeight = DEFAULT_TIER_GLYPH_HEIGHT / pixelsPerCoordY;
        for (TierLabelGlyph label : ((AffyLabelledTierMap) affyTieredMap).getTierLabels()) {
            TierGlyph referenceTier = label.getReferenceTier();
            Rectangle2D.Double coordBox = referenceTier.getCoordBox();
            final Scene scene = referenceTier.getScene();
            Rectangle2D.Double sceneCoordBox = scene.getCoordBox();
            final double currentHeight = coordBox.height;
            if (currentHeight < minCoordHeight) {
                double coordDiff = minCoordHeight - currentHeight;
                referenceTier.setCoords(coordBox.x, coordBox.y, coordBox.width, minCoordHeight);
                scene.setCoords(sceneCoordBox.x, sceneCoordBox.y, sceneCoordBox.width, sceneCoordBox.height + coordDiff);
            }
        }
        final double newHeight = ((AffyLabelledTierMap) affyTieredMap).getTierLabels().stream().mapToDouble(label -> label.getCoordBox().height * pixelsPerCoordY).sum();
        final int availableHeight = ((AffyLabelledTierMap) affyTieredMap).getSplitPane().getRightComponent().getHeight();
        if (newHeight <= availableHeight) {
            ((AffyLabelledTierMap) affyTieredMap).packTiers(false, true, true);
//            affyTieredMap.stretchToFit(false, true);
        } else {
            ((AffyLabelledTierMap) affyTieredMap).packTiers(false, true, true);
        }

        affyTieredMap.updateWidget();
    }

}
