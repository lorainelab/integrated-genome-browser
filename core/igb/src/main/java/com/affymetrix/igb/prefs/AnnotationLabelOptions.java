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
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author dcnorris
 */
@Component(name = AnnotationLabelOptions.COMPONENT_NAME, immediate = true, provide = PreferencesPanelProvider.class)
public final class AnnotationLabelOptions extends JRPJPanel implements PreferencesPanelProvider {

    public static final String COMPONENT_NAME = "AnnotationLabelOptions";
    private static final String PANEL_TITLE = "Annotation Label Font";
    private static final String INNER_PANEL_TITLE = "Font Settings";
    private static final String VARIABLE_SIZE_BTN_PREF = "variableSizeBtn";
    private static final String VARIABLY_SIZED_LABELS_BTN_TEXT = " Auto-size labels, variable (classic)";
    private static final String VARIABLY_SIZED_LABELS_BTN_TOOLTIP = "Annotation size and track height set the label font size. Different annotations can have different font sizes.";
    private static final String FIXED_SIZE_BTN_PREF = "fixedSizeBtn";
    private static final String FIXED_SIZED_LABELS_BTN_TEXT = "Fixed size labels, uniform";
    private static final String FIXED_SIZED_LABELS_BTN_TOOLTIP = "You set the label font size. All annotations in every track have the same font size. Partial labels with ... sometimes appear.";
    private static final String AUTO_SIZE_BTN_PREF = "autoSizeBtn";
    private static final String AUTO_SIZED_LABELS_BTN_TEXT = "Auto-size labels, uniform";
    private static final String AUTO_SIZED_BTN_TOOLTIP = "Track height sets the label font size. All annotations in a track have the same font size. Partial labels with ... sometimes appear.";
    private static final String HEADER_TEXT = "Settings apply to all annotation tracks.";
    private static final String BOTTOM_PANEL_MESSAGE = "Changes take effect immediately.";

    private static final int TAB_POSITION = 1;

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
    private JLabel titleLabel;
    private JLabel bottomPanelMessage;
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
        return PANEL_TITLE;
    }

    private void initComponents() {
        titleLabel = new JLabel(HEADER_TEXT);
        bottomPanelMessage = new JLabel(BOTTOM_PANEL_MESSAGE);
        fixedAnnotaionSizeLabel = new JLabel("Font Size");
        annotationLabelSizeComboBox = new AnnotationLabelCombobox();
        float previouslySelectedLabelSize = annotationLabelPrefsNode.getFloat(PREF_KEYS.SELECTED_LABEL_SIZE.keyValue, defaultFixedFontSize);
        EfficientLabelledLineGlyph.OVERRIDE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, Math.round(previouslySelectedLabelSize));
        annotationLabelSizeComboBox.setModel(new DefaultComboBoxModel(TrackConstants.SUPPORTED_SIZE));
        annotationLabelSizeComboBox.setSelectedItem(previouslySelectedLabelSize);
        annotationLabelSizeComboBox.addActionListener((ActionEvent e) -> {
            annotationLabelSizeComboBoxActionPerformed();
        });
        labelOptionBtnGroup = new ButtonGroup();
        autoSizeBtn = new JRadioButton(AUTO_SIZED_LABELS_BTN_TEXT);
        autoSizeBtn.setToolTipText(AUTO_SIZED_BTN_TOOLTIP);
        autoSizeBtn.addActionListener((ActionEvent e) -> {
            EfficientLabelledLineGlyph.DYNAMICALLY_SIZE_LABELS = false;
            EfficientLabelledLineGlyph.AUTO_SIZE_LABELS = true;
            annotationLabelSizeComboBox.setEnabled(false);
            annotationLabelPrefsNode.put(PREF_KEYS.SELECTED_BTN.keyValue, AUTO_SIZE_BTN_PREF);
            IGB.getInstance().getMapView().getSeqMap().updateWidget();
        });
        fixedSizeBtn = new JRadioButton(FIXED_SIZED_LABELS_BTN_TEXT);
        fixedSizeBtn.setToolTipText(FIXED_SIZED_LABELS_BTN_TOOLTIP);
        fixedSizeBtn.addActionListener((ActionEvent e) -> {
            EfficientLabelledLineGlyph.DYNAMICALLY_SIZE_LABELS = false;
            EfficientLabelledLineGlyph.AUTO_SIZE_LABELS = false;
            annotationLabelSizeComboBox.setEnabled(true);
            annotationLabelPrefsNode.put(PREF_KEYS.SELECTED_BTN.keyValue, FIXED_SIZE_BTN_PREF);
            IGB.getInstance().getMapView().getSeqMap().updateWidget();
        });
        variableSizeBtn = new JRadioButton(VARIABLY_SIZED_LABELS_BTN_TEXT);
        variableSizeBtn.setToolTipText(VARIABLY_SIZED_LABELS_BTN_TOOLTIP);
        variableSizeBtn.addActionListener((ActionEvent e) -> {
            EfficientLabelledLineGlyph.DYNAMICALLY_SIZE_LABELS = true;
            EfficientLabelledLineGlyph.AUTO_SIZE_LABELS = false;
            annotationLabelSizeComboBox.setEnabled(false);
            annotationLabelPrefsNode.put(PREF_KEYS.SELECTED_BTN.keyValue, VARIABLE_SIZE_BTN_PREF);
            IGB.getInstance().getMapView().getSeqMap().updateWidget();
        });
        labelOptionBtnGroup.add(autoSizeBtn);
        labelOptionBtnGroup.add(fixedSizeBtn);
        labelOptionBtnGroup.add(variableSizeBtn);
    }

    private void initializeComponentState() {
        switch (annotationLabelPrefsNode.get(PREF_KEYS.SELECTED_BTN.keyValue, AUTO_SIZE_BTN_PREF)) {
            case FIXED_SIZE_BTN_PREF:
                fixedSizeBtn.doClick();
                break;
            case VARIABLE_SIZE_BTN_PREF:
                variableSizeBtn.doClick();
                break;
            default:
                autoSizeBtn.doClick();
                break;
        }
    }

    private void initializeLayout() {
        setLayout(new MigLayout("left"));
        JPanel panel = new JPanel(new MigLayout());
        panel.setBorder(BorderFactory.createTitledBorder(INNER_PANEL_TITLE));
        panel.add(titleLabel, "span 3, center, wrap");
        panel.add(autoSizeBtn, "wrap");
        panel.add(fixedSizeBtn, new CC());
        panel.add(fixedAnnotaionSizeLabel, new CC().gap("rel"));
        panel.add(annotationLabelSizeComboBox, new CC().wrap());
        panel.add(variableSizeBtn, "wrap");
        panel.add(bottomPanelMessage, "span 3, center");
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
