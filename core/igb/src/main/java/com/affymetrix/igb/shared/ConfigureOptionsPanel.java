package com.affymetrix.igb.shared;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometry.color.ColorProviderI;
import com.affymetrix.genometry.filter.PropertyFilter;
import com.affymetrix.genometry.filter.SAMtagsFilter;
import com.affymetrix.genometry.general.ID;
import com.affymetrix.genometry.general.IParameters;
import com.affymetrix.genometry.general.NewInstance;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.operator.service.OperatorServiceRegistry;
import com.affymetrix.genometry.style.HeatMap;
import com.affymetrix.genometry.style.HeatMapExtended;
import com.affymetrix.genometry.tooltip.ToolTipConstants;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.IDComparator;
import com.affymetrix.genoviz.swing.NumericFilter;
import com.affymetrix.igb.colorproviders.Property;
import com.affymetrix.igb.colorproviders.SAMtagsColor;
import com.affymetrix.igb.colorproviders.SAMtagsTable;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.jidesoft.combobox.ColorComboBox;
import cytoscape.visual.ui.editors.continuous.ColorInterpolator;
import cytoscape.visual.ui.editors.continuous.GradientColorInterpolator;
import cytoscape.visual.ui.editors.continuous.GradientEditorPanel;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 *
 * @author hiralv
 */
@SuppressWarnings("unchecked")
public class ConfigureOptionsPanel<T extends ID & NewInstance> extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ConfigureOptionsPanel.class);
    private static final long serialVersionUID = 1L;

    private T returnValue, selectedValue;
    private Map<String, Object> paramMap;
    private JComboBox comboBox;
    private JPanel paramsPanel;
    private List<SelectionChangeListener> tChangeListeners;
    private List<Preferences> preferenceNodes;
    private TierLabelManager tierLabelManager;
    // This is used to keep track of preferences update once result is accepted bu user,
    // ie. getReturnValue called with parameter true.
    private Runnable commitPreferences = null;
    private IParameters saved_IParameters = null;
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("igb");

    /**
     * Creates the reusable dialog.
     */
    public ConfigureOptionsPanel(Class clazz, Object label) {
        this(clazz, label, null);
    }

    public ConfigureOptionsPanel(Class clazz, Object label, Filter<T> filter) {
        this(clazz, label, filter, true);
    }

    public ConfigureOptionsPanel(Class clazz, Object label, Filter<T> filter, boolean includeNone) {
        this(clazz, label, filter, includeNone, null);
    }

    public ConfigureOptionsPanel(Class clazz, Object label, Filter<T> filter, boolean includeNone, List<Preferences> preferences) {
        preferenceNodes = preferences;
        tierLabelManager = null;
        init(clazz, label, filter, includeNone);
    }

    public ConfigureOptionsPanel(Class clazz, String label, Filter<T> filter, boolean includeNone, List<Preferences> preferences, TierLabelManager tierLabelManager) {
        preferenceNodes = preferences;
        this.tierLabelManager = tierLabelManager;
        init(clazz, label, filter, includeNone);
    }

    private void init(Class clazz, Object label, Filter<T> filter, boolean includeNone) throws SecurityException {
        setLayout(new MigLayout("fill"));

        comboBox = new JComboBox();
        comboBox.setRenderer(new IDListCellRenderer());

        if (includeNone) {
            comboBox.addItem(null);
        }
        TreeSet<T> tProviders = new TreeSet<>(new IDComparator());
        //exception for now
        if (clazz == Operator.class) {
            tProviders.addAll((Collection<? extends T>) OperatorServiceRegistry.getOperators());
        } else {
            tProviders.addAll(ExtensionPointHandler.getExtensionPoint(clazz).getExtensionPointImpls());
        }
        for (T cp : tProviders) {
            if (filter != null) {
                if (!filter.shouldInclude(cp)) {
                    continue;
                }
            }
            if(cp instanceof Property || cp instanceof PropertyFilter) {
                refreshProps(((IParameters) cp).getParametersPossibleValues("property"));
            }else if(cp instanceof SAMtagsFilter || cp instanceof SAMtagsColor){
                refreshSAMTAGS(((IParameters) cp).getParametersPossibleValues("tag"));
            }
            comboBox.addItem(cp);
        }
        if(tierLabelManager != null){
            try {
                List<TierGlyph> selectedTiers = tierLabelManager.getSelectedTiers();
                List<TierGlyph> ucscRestTiers = selectedTiers.stream().filter(tierGlyph -> tierGlyph.getAnnotStyle().getFeature().getDataContainer().getDataProvider().getName().equalsIgnoreCase("UCSC REST"))
                        .filter(ucscRestTierGlyph -> ucscRestTierGlyph.getAnnotStyle().getFeature().getProperties().containsKey("props"))
                        .toList();
                if (!ucscRestTiers.isEmpty() && ucscRestTiers.size() == selectedTiers.size()) {
                    List<Set<String>> propsList = ucscRestTiers.stream().map(ucscRestTier -> ucscRestTier.getAnnotStyle().getFeature().getProperties().get("props"))
                            .map(props -> props.split(","))
                            .map(propsArray -> Arrays.stream(propsArray).collect(Collectors.toUnmodifiableSet()))
                            .toList();
                    Set<String> commonProps = new HashSet<>(propsList.get(0));
                    for (Set<String> set : propsList) {
                        commonProps.retainAll(set);
                    }
                    tProviders.stream().filter(tProvider -> tProvider instanceof Property).forEach(tProvider -> {
                        List<Object> properties = ((Property) tProvider).getParametersPossibleValues("property");
                        properties.addAll(commonProps);
                    });
                    tProviders.stream().filter(tProvider -> tProvider instanceof PropertyFilter).forEach(tProvider -> {
                        List<Object> properties = ((PropertyFilter) tProvider).getParametersPossibleValues("property");
                        properties.addAll(commonProps);
                    });
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        if (includeNone) {
            comboBox.setSelectedItem(null);
        }

        JPanel optionsBox = new JPanel();
        optionsBox.setLayout(new MigLayout("fillx", "[]rel[grow]", "[]"));
        if (label instanceof JComponent) {
            optionsBox.add((JComponent) label, "right");
        } else if (label instanceof String) {
            optionsBox.add(new JLabel(label + ""), "right");
        }
        optionsBox.add(comboBox, "growx");

        paramsPanel = new JPanel(new MigLayout("fill"));

        add(optionsBox, "growx, wrap");
        add(paramsPanel, "growx");

        addListeners();

        //Initialized with first value
        if (selectedValue == null && comboBox.getItemCount() > 0) {
            T cp = (T) comboBox.getItemAt(0);
            returnValue = cp;
            setSelected(cp);
        }
    }

    private void refreshProps(List<Object> properties){
        properties.clear();
        properties.add(ToolTipConstants.ID);
        properties.add(ToolTipConstants.NAME);
        properties.add(ToolTipConstants.SCORE);
        properties.add(ToolTipConstants.TITLE);
    }
    private void refreshSAMTAGS(List<Object> tags){
        tags.clear();
        tags.add(ToolTipConstants.CR);
        tags.add(ToolTipConstants.CB);
        tags.add(ToolTipConstants.MI);
        tags.add(ToolTipConstants.UB);
        tags.add(ToolTipConstants.UR);
    }

    public IParameters getSaved_IParameters() {
        return saved_IParameters;
    }

    public void setSaved_IParameters(IParameters saved_IParameters) {
        this.saved_IParameters = saved_IParameters;
    }

    private void addOptions(final IParameters iParameters, final JPanel paramsPanel) {
        paramMap = new HashMap<>();
        JPanel panel = new JPanel(new MigLayout("fill"));

        paramsPanel.removeAll();
        if (iParameters != null && iParameters.getParametersType() != null) {
            for (Map.Entry<String, Class<?>> entry : iParameters.getParametersType().entrySet()) {
                final String label = entry.getKey();
                final Class<?> clazz = entry.getValue();
                final List<Object> possibleValues = iParameters.getParametersPossibleValues(label);
                JComponent component = null;

                if (possibleValues != null) {
                    final JComboBox cb = new JComboBox();
                    cb.setRenderer(new IDListCellRenderer());
                    possibleValues.forEach(cb::addItem);
                    cb.setSelectedItem(iParameters.getParameterValue(label));
                    cb.addItemListener(e -> ConfigureOptionsPanel.this.setParameter(iParameters, label, cb.getSelectedItem()));
                    component = cb;
                }else if(HashMap.class.isAssignableFrom(clazz)){
                    component = HashMapCondition(iParameters,label);
                } else if (Number.class.isAssignableFrom(clazz) || String.class.isAssignableFrom(clazz)) {
                    component = NumberOrStringCondition(iParameters,label,clazz);
                } else if (Color.class.isAssignableFrom(clazz)) {
                    component = ColorConditon(iParameters,label);
                } else if (HeatMapExtended.class.isAssignableFrom(clazz)) {
                    component = HeatMapCondition(iParameters,label);
                }

                if (component != null) {
                    panel.add(new JLabel(label), new CC().gap("rel").alignX("right"));
                    panel.add(component, new CC().growX().alignY("top"));
                }

            }
        }

        if (panel.getComponentCount() > 0) {
            paramsPanel.add(panel, "growx");
        }
    }
    private ColorComboBox ColorConditon(IParameters iParameters, String label){
        final ColorComboBox colorComboBox = new ColorComboBox();
        colorComboBox.setSelectedColor((Color) iParameters.getParameterValue(label));
        colorComboBox.addItemListener(e -> setParameter(iParameters, label, e.getItem()));
        colorComboBox.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
        colorComboBox.setButtonVisible(false);
        colorComboBox.setColorValueVisible(false);
        colorComboBox.setMaximumSize(new java.awt.Dimension(20, 20));
        colorComboBox.setPreferredSize(new java.awt.Dimension(20, 20));
        colorComboBox.setMinimumSize(new java.awt.Dimension(20, 20));
        //colorComboBox.setStretchToFit(true);
        return colorComboBox;
    }
    private JTextField NumberOrStringCondition(IParameters iParameters,String label,Class clazz){
        final JTextField tf;
        if (Number.class.isAssignableFrom(clazz)) {
            tf = new JTextField();
            if (Integer.class.isAssignableFrom(clazz)) {
                ((AbstractDocument) tf.getDocument()).setDocumentFilter(new NumericFilter.IntegerNumericFilter(Integer.MIN_VALUE, Integer.MAX_VALUE));
            } else {
                ((AbstractDocument) tf.getDocument()).setDocumentFilter(new NumericFilter.FloatNumericFilter());
            }

        } else {
            tf = new JTextField();
        }
        tf.setText(String.valueOf(iParameters.getParameterValue(label)));
        tf.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setParameter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setParameter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setParameter();
            }

            private void setParameter() {
                if (Number.class.isAssignableFrom(clazz)) {
                    if (tf.getText() != null && tf.getText().length() > 0) {
                        try {
                            if (Integer.class.isAssignableFrom(clazz)) {
                                int value = Integer.valueOf(tf.getText());
                                ConfigureOptionsPanel.this.setParameter(iParameters, label, value);
                            } else {
                                float value = Float.valueOf(tf.getText());
                                ConfigureOptionsPanel.this.setParameter(iParameters, label, value);
                            }
                        } catch (NumberFormatException ex) {
                        }
                    }
                } else {
                    ConfigureOptionsPanel.this.setParameter(iParameters, label, tf.getText().trim());
                }
            }
        });
        tf.setMinimumSize(new java.awt.Dimension(40, 20));
        return tf;
    }
    private JPanel HashMapCondition(IParameters iParameters,String label){
        JButton editTagsColor = new JButton("Edit Tags and Color");
        JDialog editor = new JDialog();
        JButton save_btn = new JButton("Save and Apply");
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener((ActionListener)e ->editor.setVisible(false));
        editor.setLayout(new MigLayout("insets 4 4 4 4",
                "[fill,30%][fill,40%][fill,30%]", "[fill,grow]"));
        SAMtagsTable table = createSAMtagsTable(getSaved_IParameters());
        save_btn.addActionListener((ActionListener) e ->{
            //Table cell editing mode needs to be stopped for the cell value to be available in getValueAt() fn
            if (table.isEditing())
                table.getCellEditor().stopCellEditing();
            Map<String, Object> savedColors = new HashMap<>();
            savedColors.put(label, table.saveAndApply());
            if (savedColors.get(label) != null)
                iParameters.setParametersValue(savedColors);
            setSaved_IParameters(iParameters);
            ConfigureOptionsPanel.this.setParameter(iParameters, label, savedColors.get(label));
            editor.setVisible(false);

        });
        JScrollPane table_pane = new JScrollPane();
        table.setMinimumSize(new Dimension(350,350));
        table_pane.getViewport().add(table);
        editor.add(table_pane, "spanx, grow, wrap");

        JPanel save_panel = new JPanel();
        save_panel.add(save_btn);
        save_panel.add(cancelBtn);
        editor.add(save_panel);
        save_panel.setMaximumSize(new Dimension(350,350));
        editor.setMinimumSize(new Dimension(350, 350));
        editor.setTitle("Edit Tags and Colors");
        editor.setModal(true);
        editor.setAlwaysOnTop(false);
        editor.setLocationRelativeTo(ConfigureOptionsPanel.this);
        editor.pack();
        editTagsColor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.setVisible(true);
            }
        });

        JButton importTagsColor = new JButton("Import...");
        HashMap<String, Object> file_color_map = new HashMap<>();
        importTagsColor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JFileChooser fileChooser = new JFileChooser();
                        int result = fileChooser.showOpenDialog(null);
                        File file = null;
                        if(result == JFileChooser.APPROVE_OPTION){
                            file = fileChooser.getSelectedFile();
                        }
                        if(file != null)
                        try {
                            BufferedReader reader = new BufferedReader(new FileReader(file.getAbsoluteFile()));
                            String[] headers = reader.readLine().split("\t");
                            String line;
                            while ((line = reader.readLine()) != null) {
                                file_color_map.put(line.split("\t")[0], Color.decode(line.split("\t")[1]));
                            }
                            table.populateImportUserData(file_color_map);
                            editor.setVisible(true);
                        } catch (FileNotFoundException ex) {
                            throw new RuntimeException(ex);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });
            }
        });
        JPanel editImportPane = new JPanel();
        editImportPane.add(editTagsColor);
        editImportPane.add(importTagsColor);
        return editImportPane;
    }

    private JButton HeatMapCondition(IParameters iParameters, String label) {
        final GradientEditorPanel editor = new GradientEditorPanel(null);
        Object hm = iParameters.getParameterValue(label);
        float[] positions;
        Color[] colorRanges;
        String colorByPropName = ((ColorProviderI) iParameters).getName();
        String[] heatMapTitle = {"HeatMapExtended"};
//                    Preferences[] currentLabelPreferences = new Preferences[]{null};
        if (hm instanceof HeatMapExtended) {
            positions = ((HeatMapExtended) hm).getValues();
            colorRanges = ((HeatMapExtended) hm).getRangeColors();
            heatMapTitle[0] = ((HeatMapExtended) hm).getName();
        } else {
            HeatMapExtended hme = (HeatMapExtended) iParameters.getParameterValue(label);
            positions = hme.getValues();
            colorRanges = hme.getColors();
            heatMapTitle[0] = hme.getName();
        }
        //Initial values of heatmap are set to the ones found in preferences of first selected track.
        List<String> previosPrefs = new ArrayList<>();
        if (preferenceNodes != null && preferenceNodes.size() > 0) {
            Preferences preferenceNode = preferenceNodes.get(0);
            try {
                previosPrefs = Arrays.asList(preferenceNode.childrenNames());
            } catch (BackingStoreException ex) {
                //Preferences store not available
            }
            if (previosPrefs.contains(colorByPropName)) {
                // read json from pref and create values and colors
                Preferences node = preferenceNode.node(colorByPropName);
                float[] pos = new Gson().fromJson(node.get("values", ""), float[].class);
                Color[] color = new GsonBuilder().registerTypeAdapter(Color.class, (InstanceCreator<Color>) (Type type) -> new Color(0)).create().fromJson(node.get("colors", ""), Color[].class);;
                if (pos != null && color != null) {
                    positions = pos;
                    colorRanges = color;
                }

            }
        }
        editor.setVirtualRange(positions, colorRanges);
        ColorInterpolator colorInterpolator1 = new GradientColorInterpolator(editor.getVirtualRange());
        setParameter(iParameters, label, new HeatMapExtended(heatMapTitle[0],
                colorInterpolator1.getColorRange(HeatMap.BINS),
                positions, colorRanges));

        final JButton editButton = new JButton("Edit");
        editButton.addActionListener(evt -> {
            editor.setTitle("Configure Heatmap");
            editor.setModal(true);
            editor.setAlwaysOnTop(false);
            editor.setLocationRelativeTo(ConfigureOptionsPanel.this);
            editor.setDefaultCloseOperation(GradientEditorPanel.DISPOSE_ON_CLOSE);
            Object value = editor.showDialog();
            if (value.equals(JOptionPane.OK_OPTION)) {
                ColorInterpolator colorInterpolator = new GradientColorInterpolator(editor.getVirtualRange());
                setParameter(iParameters, label, new HeatMapExtended(heatMapTitle[0],
                        colorInterpolator.getColorRange(HeatMap.BINS),
                        editor.getVirtualRange().getVirtualValues(),
                        editor.getVirtualRange().getColors()));
                // This is used to keep track of preferences update once result is accepted by user,
                // ie. getReturnValue called with parameter true.
                //Here we create a function that will save heatmap preferences if user clicks "Ok" on "color by" dialog.
                commitPreferences = () -> {
                    if (preferenceNodes != null) {
                        // Convert to JSON and save to preferences.
                        String values = new Gson().toJson(editor.getVirtualRange().getVirtualValues());
                        String colors = new Gson().toJson(editor.getVirtualRange().getColors());
                        preferenceNodes.forEach(node -> {
                            node = node.node(colorByPropName);
                            node.put("values", values);
                            node.put("colors", colors);
                        });
                    }
                };
            }
        });
        return editButton;
    }

    private SAMtagsTable createSAMtagsTable(IParameters iParameters) {
        String[] columns = {"Tag Value", "Color", ""};
        SAMtagsTable samtags_table = new SAMtagsTable();
        if (iParameters != null)
            samtags_table.populateUserData(iParameters);
        return samtags_table;
    }

    private void setParameter(IParameters cp, String key, Object value) {
//		boolean isValid = cp.setParameterValue(key, value);
//		okOption.setEnabled(isValid);
        paramMap.put(key, value);
    }

    private void initParamPanel(IParameters cp) {
        addOptions(cp, paramsPanel);
    }

    private void addListeners() {
        comboBox.addItemListener(e -> {
            T cp = (T) comboBox.getSelectedItem();
            // If a user selects same color provider as initial then reuses the same object
            // this if-statement was removed in IGBF-1129 and 
            // it was restored (with modification) in IGBF-1232
            // The condition: !(cp instanceof ColorProviderI)
            // was added to address the needs of IGBF-1129
            if (returnValue != null && cp != null 
                    && cp.getName().equals(returnValue.getName())
                    && !(cp instanceof ColorProviderI)) { 
                cp = returnValue;
            } else {
                cp = (cp == null) ? cp : (T) cp.newInstance();
            }
            setSelected(cp);
            if (tChangeListeners != null && !tChangeListeners.isEmpty() && cp != returnValue) {
                for (SelectionChangeListener tChangeListener : tChangeListeners) {
                    tChangeListener.selectionChanged(cp);
                }
            }
        });

    }

    private void setSelected(T value) {
        selectedValue = value;
        if (value instanceof IParameters) {
            initParamPanel((IParameters) value);
        } else {
            paramsPanel.removeAll();
        }
        paramsPanel.revalidate();
        revalidate();
    }

    public void addTChangeListner(SelectionChangeListener tcl) {
        if (tChangeListeners == null) {
            tChangeListeners = new ArrayList<>();
        }
        tChangeListeners.add(tcl);
    }

    public void removeTChangeListner(SelectionChangeListener tcl) {
        tChangeListeners.remove(tcl);
        if (tChangeListeners.isEmpty()) {
            tChangeListeners = null;
        }
    }

    @Override
    public void setEnabled(boolean b) {
        comboBox.setEnabled(b);
    }

    public void setInitialValue(T t) {
        returnValue = t;
        if (t == null) {
            comboBox.setSelectedItem(t);
        } else {
            for (int i = 0; i < comboBox.getItemCount(); i++) {
                T item = (T) comboBox.getItemAt(i);
                if (item != null && item.getName().equals(t.getName())) {
                    comboBox.setSelectedItem(item);
                    break;
                }
            }
        }
    }

    public T getReturnValue(boolean applyChanges) {
        if (applyChanges) {
            returnValue = selectedValue;
            if (returnValue instanceof IParameters) {
                boolean value = ((IParameters) returnValue).setParametersValue(paramMap);
                if (!value && paramMap.size() >= 1) {
                    String[] options = new String[] {"Ok", "Copy Error Message"};
                    String message = MessageFormat.format(BUNDLE.getString("trackOperationError"), paramMap.get(paramMap.keySet().toArray()[0].toString()));
                    int response = JOptionPane.showOptionDialog(this, message, "Invalid Value", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
                    if (response == 1) {
                        GeneralUtils.copyToClipboard(message);
                    }
                    returnValue = null;
                }
            }
            if (commitPreferences != null) {
                commitPreferences.run();
                commitPreferences = null;
            }
        }
        return returnValue;
    }

    public JComboBox getComboBox() {
        return comboBox;
    }

    /**
     * Interface to listen to combobox event change
     *
     * @param <T>
     */
    public static interface SelectionChangeListener<T> {

        public void selectionChanged(T t);
    }

    /**
     * Interface to filter out available
     *
     * @param <T>
     */
    public static interface Filter<T> {

        public boolean shouldInclude(T t);
    }

    private static class IDListCellRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            if (value == null) {
                return super.getListCellRendererComponent(list, "None", index, isSelected, cellHasFocus);
            }
            if (value instanceof ID) {
                return super.getListCellRendererComponent(list, ((ID) value).getDisplay(),
                        index, isSelected, cellHasFocus);
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }
}
