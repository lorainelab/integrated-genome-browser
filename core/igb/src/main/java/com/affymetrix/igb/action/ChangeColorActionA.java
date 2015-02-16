package com.affymetrix.igb.action;

import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.ParameteredAction;
import com.affymetrix.igb.shared.Selections;
import static com.affymetrix.igb.shared.Selections.allGlyphs;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.lorainelab.igb.genoviz.extensions.StyledGlyph;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JColorChooser;
import javax.swing.JDialog;

/**
 * note - this class contains an instance of SeqMapView. For now, there is just
 * one instance using the regular SeqMapView, no instance for AltSpliceView
 */
public abstract class ChangeColorActionA extends SeqMapViewActionA implements ParameteredAction {

    protected static final java.awt.Color DEFAULT_COLOR = javax.swing.UIManager.getColor("Button.background");
    private static final long serialVersionUID = 1L;
    private boolean iterateMultigraph = true;

    protected ChangeColorActionA(String text, String iconPath, String largeIconPath) {
        super(text, iconPath, largeIconPath);
        this.ordinal = -6008000;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        changeColor();
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    protected abstract void setChooserColor(JColorChooser chooser, ITrackStyleExtended style);

    protected abstract void setStyleColor(Color color, ITrackStyleExtended style);

    protected final void iterateMultiGraph(boolean iterate) {
        iterateMultigraph = iterate;
    }

    private void changeColor() {
        if (allGlyphs.isEmpty()) {
            return;
        }

        final JColorChooser chooser = new JColorChooser();

        ITrackStyleExtended style_0 = (allGlyphs.get(0)).getAnnotStyle();
        if (style_0 != null) {
            setChooserColor(chooser, style_0);
        }

        ActionListener al = e -> {
            performAction(chooser.getColor());
            getSeqMapView().updatePanel();
        };

        JDialog dialog = JColorChooser.createDialog(null, // parent
                "Pick a Color",
                true, //modal
                chooser,
                al, //OK button handler
                null); //no CANCEL button handler
        dialog.setVisible(true);

    }

    private void changeColor(Color color) {
        for (StyledGlyph vg : allGlyphs) {
            ITrackStyleExtended style;

            //If graphs is joined then apply color to combo style too.
            if (vg instanceof GraphGlyph) {
                style = ((GraphGlyph) vg).getGraphState().getComboStyle();
                if (style != null) {
                    setStyleColor(color, style);
                    if (!iterateMultigraph) {
                        continue;
                    }
                }
            }

            style = vg.getAnnotStyle();
            if (style != null) {
                setStyleColor(color, style);
            }
        }
    }

    @Override
    public void performAction(Object... parameters) {
        if (parameters != null) {
            if (parameters.length < 1 || parameters[0].getClass() != Color.class) {
                return;
            }

            changeColor((Color) parameters[0]);
        }
    }

    @Override
    public boolean isEnabled() {
        return Selections.allGlyphs.size() > 0;
    }
}
