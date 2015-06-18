package com.affymetrix.igb.prefs;

import aQute.bnd.annotation.component.Component;
import static com.affymetrix.genometry.util.PreferenceUtils.getAnnotationLabelPrefsNode;
import com.affymetrix.genoviz.glyph.EfficientLabelledLineGlyph;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.swing.JRPJPanel;
import com.affymetrix.igb.tiers.TrackConstants;
import com.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author dcnorris
 */
@Component(name = AnnotationLabelOptions.COMPONENT_NAME, immediate = true, provide = PreferencesPanelProvider.class)
public final class AnnotationLabelOptions extends JRPJPanel implements PreferencesPanelProvider {

    public static final String COMPONENT_NAME = "AnnotationLabelOptions";
    private static final int TAB_POSITION = 6;

    private enum PREF_KEYS {

        SELECTED_BTN("selectedBtn"),
        SELECTED_LABEL_SIZE("labelSize");

        private final String keyValue;

        private PREF_KEYS(final String keyValue) {
            this.keyValue = keyValue;
        }

    }

    private final int defaultFixedFontSize;
    private final Preferences annotationLabelPrefsNode;
    private JComboBox annotationLabelSizeComboBox;
    private JLabel fixedAnnotaionSizeLabel;
    private ButtonGroup labelOptionBtnGroup;
    private JRadioButton fixedSizeBtn;
    private JRadioButton autoSizeBtn;
    private JRadioButton variableSizeBtn;

    public AnnotationLabelOptions() {
        super(COMPONENT_NAME);
        defaultFixedFontSize = 12;
        annotationLabelPrefsNode = getAnnotationLabelPrefsNode();
        initComponents();
        initializeComponentState();
        initializeLayout();
    }

    @Override
    public String getName() {
        return "Annotation Label Options";
    }

    private void initComponents() {
        fixedAnnotaionSizeLabel = new JLabel("Fixed Annotation Label Size");
        annotationLabelSizeComboBox = new AnnotationLabelCombobox();
        float previouslySelectedLabelSize = annotationLabelPrefsNode.getFloat(PREF_KEYS.SELECTED_LABEL_SIZE.keyValue, defaultFixedFontSize);
        EfficientLabelledLineGlyph.OVERRIDE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, Math.round(previouslySelectedLabelSize));
        annotationLabelSizeComboBox.setModel(new DefaultComboBoxModel(TrackConstants.SUPPORTED_SIZE));
        annotationLabelSizeComboBox.setSelectedItem(previouslySelectedLabelSize);
        annotationLabelSizeComboBox.addActionListener((ActionEvent e) -> {
            annotationLabelSizeComboBoxActionPerformed();
        });
        labelOptionBtnGroup = new ButtonGroup();
        autoSizeBtn = new JRadioButton("Auto Sized Labels");
        autoSizeBtn.setToolTipText("All labels will use the same dynamically choosen font size based on available space.");
        autoSizeBtn.addActionListener((ActionEvent e) -> {
            EfficientLabelledLineGlyph.DYNAMICALLY_SIZE_LABELS = false;
            EfficientLabelledLineGlyph.AUTO_SIZE_LABELS = true;
            annotationLabelSizeComboBox.setEnabled(false);
            annotationLabelPrefsNode.put(PREF_KEYS.SELECTED_BTN.keyValue, "autoSizeBtn");
            IGB.getInstance().getMapView().getSeqMap().updateWidget();
        });
        fixedSizeBtn = new JRadioButton("Fixed Sized Labels");
        fixedSizeBtn.setToolTipText("All labels will use a configurable fixed font size which does not change based on the available space.");
        fixedSizeBtn.addActionListener((ActionEvent e) -> {
            EfficientLabelledLineGlyph.DYNAMICALLY_SIZE_LABELS = false;
            EfficientLabelledLineGlyph.AUTO_SIZE_LABELS = false;
            annotationLabelSizeComboBox.setEnabled(true);
            annotationLabelPrefsNode.put(PREF_KEYS.SELECTED_BTN.keyValue, "fixedSizeBtn");
            IGB.getInstance().getMapView().getSeqMap().updateWidget();
        });
        variableSizeBtn = new JRadioButton("Variably Sized Labels (IGB Classic)");
        variableSizeBtn.setToolTipText("Label font size will be derrived from the relative size of the annotation.");
        variableSizeBtn.addActionListener((ActionEvent e) -> {
            EfficientLabelledLineGlyph.DYNAMICALLY_SIZE_LABELS = true;
            EfficientLabelledLineGlyph.AUTO_SIZE_LABELS = false;
            annotationLabelSizeComboBox.setEnabled(false);
            annotationLabelPrefsNode.put(PREF_KEYS.SELECTED_BTN.keyValue, "variableSizeBtn");
            IGB.getInstance().getMapView().getSeqMap().updateWidget();
        });
        labelOptionBtnGroup.add(autoSizeBtn);
        labelOptionBtnGroup.add(fixedSizeBtn);
        labelOptionBtnGroup.add(variableSizeBtn);
    }

    private void initializeComponentState() {
        switch (annotationLabelPrefsNode.get(PREF_KEYS.SELECTED_BTN.keyValue, "autoSizeBtn")) {
            case "fixedSizeBtn":
                fixedSizeBtn.doClick();
                break;
            case "variableSizeBtn":
                variableSizeBtn.doClick();
                break;
            default:
                autoSizeBtn.doClick();
                break;
        }
    }

    private void initializeLayout() {
        setLayout(new MigLayout("fillx"));
        JPanel panel = new JPanel(new MigLayout("", "[]rel[]", "[][][]"));
        panel.setBorder(BorderFactory.createTitledBorder("Global Annotation Font Settings"));
        panel.add(autoSizeBtn, "wrap");
        panel.add(fixedSizeBtn, "");
        panel.add(fixedAnnotaionSizeLabel, "gap rel");
        panel.add(annotationLabelSizeComboBox, "wrap");
        panel.add(variableSizeBtn, "");
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

    private void annotationLabelSizeComboBoxActionPerformed() {
        float annotationLabelSize = Float.parseFloat(annotationLabelSizeComboBox.getSelectedItem().toString());
        EfficientLabelledLineGlyph.OVERRIDE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, Math.round(annotationLabelSize));
        annotationLabelPrefsNode.putFloat(PREF_KEYS.SELECTED_LABEL_SIZE.keyValue, annotationLabelSize);
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
