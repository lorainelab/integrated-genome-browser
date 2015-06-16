package com.affymetrix.igb.prefs;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genoviz.glyph.EfficientLabelledLineGlyph;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.swing.JRPJPanel;
import com.affymetrix.igb.tiers.TrackConstants;
import com.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.awt.Font;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author dcnorris
 */
@Component(name = AnnotationLabelOptions.COMPONENT_NAME, immediate = true, provide = PreferencesPanelProvider.class)
public class AnnotationLabelOptions extends JRPJPanel implements PreferencesPanelProvider {

    public static final String COMPONENT_NAME = "AnnotationLabelOptions";
    private static final int TAB_POSITION = 6;
    private JComboBox annotationLabelSizeComboBox;
    private JCheckBox annotationLabelAutoSizeCheckBox;
    private JLabel annotaionSizeLabel;
    private JLabel autoSizeLabel;

    public AnnotationLabelOptions() {
        super(COMPONENT_NAME);
        initComponents();
        initializeLayout();
    }

    @Override
    public String getName() {
        return "Annotation Label Options";
    }

    private void initComponents() {
        annotationLabelAutoSizeCheckBox = new JCheckBox();
        annotationLabelAutoSizeCheckBox.setSelected(EfficientLabelledLineGlyph.AUTO_SIZE_LABELS);
        annotationLabelAutoSizeCheckBox.addActionListener((ActionEvent e) -> {
            annotationLabelAutoSizeCheckBoxActionPerformed();
        });
        annotaionSizeLabel = new JLabel("Fixed Annotation Label Size");
        autoSizeLabel = new JLabel("Auto Size");
        annotationLabelSizeComboBox = new AnnotationLabelCombobox();
        annotationLabelSizeComboBox.setModel(new DefaultComboBoxModel(TrackConstants.SUPPORTED_SIZE));
        annotationLabelSizeComboBox.setEnabled(!EfficientLabelledLineGlyph.AUTO_SIZE_LABELS);
        annotationLabelSizeComboBox.addActionListener((ActionEvent e) -> {
            annotationLabelSizeComboBoxActionPerformed();
        });
    }

    public void initializeLayout() {
        setLayout(new MigLayout("fillx"));
        JPanel panel = new JPanel(new MigLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Global Annotation Font Settings"));
        panel.add(annotaionSizeLabel, "gap rel");
        panel.add(annotationLabelSizeComboBox, "gap rel");
        panel.add(annotationLabelAutoSizeCheckBox, "gap rel");
        panel.add(autoSizeLabel, "");
        add(panel, "growx");
    }

    @Override
    public JRPJPanel getPanel() {
        return this;
    }

    @Override
    public void refresh() {
    }

    @Override
    public int getWeight() {
        return TAB_POSITION;
    }

    private void annotationLabelAutoSizeCheckBoxActionPerformed() {
        EfficientLabelledLineGlyph.AUTO_SIZE_LABELS = !EfficientLabelledLineGlyph.AUTO_SIZE_LABELS;
        annotationLabelSizeComboBox.setEnabled(!EfficientLabelledLineGlyph.AUTO_SIZE_LABELS);
        IGB.getInstance().getMapView().getSeqMap().updateWidget();
    }

    private void annotationLabelSizeComboBoxActionPerformed() {
        float annotationLabelSize = Float.parseFloat(annotationLabelSizeComboBox.getSelectedItem().toString());
        EfficientLabelledLineGlyph.OVERRIDE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, Math.round(annotationLabelSize));
        IGB.getInstance().getMapView().getSeqMap().updateWidget();
    }

    class AnnotationLabelCombobox extends JComboBox {

        @Override
        public void setEnabled(boolean enabled) {
            if (enabled) {
                if (!EfficientLabelledLineGlyph.AUTO_SIZE_LABELS) {
                    super.setEnabled(enabled);
                }
            } else {
                super.setEnabled(enabled);
            }
        }
    }
}
