package com.affymetrix.igb.prefs;

import com.affymetrix.genometry.parsers.FileTypeHolder;
import com.affymetrix.genometry.style.PropertyConstants;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.stylesheet.AssociationElement;
import com.affymetrix.igb.stylesheet.PropertyMap;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.jide.StyledJTable;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TrackStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

public class TrackDefaultView extends TrackPreferences implements ListSelectionListener {

    private static final String FILE_TYPE = "File Type";
    private static final String FOREGROUND = "FG";
    private static final String BACKGROUND = "BG";
    private static final String TRACK_NAME_SIZE = "Size";
    private final static String[] col_headings = {
        FILE_TYPE,
        BACKGROUND, FOREGROUND,
        TRACK_NAME_SIZE,};
    public static final int COL_TRACK_DEFAULT = 0;
    private static TrackStyle defaultStyle = IGBStateProvider.getDefaultTrackStyle();
    private static TrackDefaultView singleton;

    public static synchronized TrackDefaultView getSingleton() {
        if (singleton == null) {
            singleton = new TrackDefaultView();
        }

        return singleton;
    }
    private Object[] temp;
    private String[] trackDefaults;
    private String[] graphFormats = {"bar", "bgr", "egr", "egr.txt", "sin", "gr", "sgr", "useq", "wig"};
    private String[] sequenceFormats = {"2bit", "fa", "fasta", "bnib"};
    private ArrayList<String> list = new ArrayList<>();
    private String allowedTrackDefaults;
    private String AddButtonTitle = "Add Track Default";
    private String selectedTrackDefaultType;
    private TrackStyle selectedStyle;
    private JRPButton addTrackDefaultButton;
    private JRPButton removeTrackDefaultButton;
    private JTextField trackDefaultTextField;

    /**
     * Creates new form FileTypeViewNew
     */
    private TrackDefaultView() {
        model = new TrackDefaultPrefTableModel();
        title = "Track Default List";
        b1Text = "Add";
        b2Text = "Delete";
        track = "Track Type";
        ((TrackDefaultPrefTableModel) model).setElements(XmlStylesheetParser.getUserFileTypeAssociation());
        initializeFileTypes();
        initCommonComponents();
        initComponents();
        table.setRowSelectionInterval(0, 0);
    }

    private void initializeFileTypes() {
        list.clear();
        temp = FileTypeHolder.getInstance().getNameToExtensionMap(null).values().toArray();
        for (Object temp1 : temp) {
            StringTokenizer tokens = new StringTokenizer(temp1.toString(), ",");
            while (tokens.hasMoreElements()) {
                allowedTrackDefaults = tokens.nextToken();
                allowedTrackDefaults = allowedTrackDefaults.replace("[", "");
                allowedTrackDefaults = allowedTrackDefaults.replace("]", "");
                allowedTrackDefaults = allowedTrackDefaults.trim();
                if (!Arrays.asList(sequenceFormats).contains(allowedTrackDefaults)) {
                    list.add(allowedTrackDefaults);
                }
            }
        }
        Collections.sort(list);
        trackDefaults = list.toArray(new String[list.size()]);
    }

    private void initComponents() {
        trackDefaultTextField = new JTextField();
        trackDefaultTextField.setEnabled(false);
        table = new StyledJTable();
        table.setCellSelectionEnabled(false);
        TableCellEditor editor = table.getDefaultEditor(String.class);
        ((DefaultCellEditor) editor).setClickCountToStart(2);
        table.setDefaultEditor(String.class, editor);

        table.list.add(TierPrefsView.COL_BACKGROUND);
        table.list.add(TierPrefsView.COL_FOREGROUND);
        table.list.add(TierPrefsView.COL_LABEL_COLOR);

        addTrackDefaultButton = new JRPButton("TrackDefaultView_addTrackDefaultButton");
        removeTrackDefaultButton = new JRPButton("TrackDefaultView_removeTrackDefaultButton");
        initTable();
        addTrackDefaultButton.setText("Add");
        removeTrackDefaultButton.setText("Remove");
    }

    @Override
    public void trackNameSizeComboBox() {
        if (applyChanged) {
            trackNameSize = Float.parseFloat(trackNameSizeComboBox.getSelectedItem().toString());
            model.setValueAt(trackNameSize, selectedRows[0], COL_TRACK_NAME_SIZE);
        }
    }

    @SuppressWarnings("unchecked")
    public void addTrackDefaultButton() {
        initializeFileTypes();
        selectedTrackDefaultType = (String) JOptionPane.showInputDialog(null, "Select file type:", AddButtonTitle,
                JOptionPane.PLAIN_MESSAGE, null, trackDefaults, trackDefaults[0]);

        if (selectedTrackDefaultType == null) {
            return;
        }

        if (XmlStylesheetParser.getUserFileTypeAssociation().get(selectedTrackDefaultType) != null) {
            ErrorHandler.errorPanel("Duplicate Entry", "File type " + selectedTrackDefaultType + " exists");
            return;
        }

        AssociationElement element = AssociationElement.getFileTypeAssocation(selectedTrackDefaultType);
        AssociationElement sysElement = XmlStylesheetParser.getSystemFileTypeAssociation().get(selectedTrackDefaultType);
        if (sysElement != null) {
            PropertyMap props = sysElement.getPropertyMap();
            element.getPropertyMap().putAll(props);
        }

        Map<String, AssociationElement> elements = XmlStylesheetParser.getUserFileTypeAssociation();
        elements.put(selectedTrackDefaultType, element);
        ((TrackDefaultPrefTableModel) model).file2types = elements.entrySet().toArray(new Entry[elements.size()]);
        ((TrackDefaultPrefTableModel) model).addElement(selectedTrackDefaultType, element);
        model.fireTableDataChanged();
    }

    public void deleteTrackDefaultButton() {
        if (table.getSelectedRow() != -1) {
            List<TrackStyle> styles = new ArrayList<>();
            for (int i : selectedRows) {
                styles.add(((TrackDefaultPrefTableModel) model).tier_styles.get(i));
            }

            styles.stream().filter(style -> style != defaultStyle).forEach(style -> {
                XmlStylesheetParser.getUserFileTypeAssociation().remove(style.getTrackName());
                ((TrackDefaultPrefTableModel) model).removeElement(style.getTrackName());
            });

            model.fireTableDataChanged();
        }
    }

    private void setEnabled(boolean b) {
        labelFieldComboBox.setEnabled(b);
        maxDepthTextField.setEnabled(b);
        show2TracksCheckBox.setEnabled(b);
        collapsedCheckBox.setEnabled(b);
        possitiveColorComboBox.setEnabled(b);
        negativeColorComboBox.setEnabled(b);
        arrowCheckBox.setEnabled(b);
        colorCheckBox.setEnabled(b);
    }

    /**
     * Called when the user selects a row of the table.
     *
     * @param evt
     */
    @Override
    public void valueChanged(ListSelectionEvent evt) {
        selectedRows = table.getSelectedRows();
        if (selectedRows.length > 0) {
            initializationDetector = true;
            applyChanged = false;
            setEnabled(true);
            selectedStyle = ((TrackDefaultPrefTableModel) model).getStyles().get(selectedRows[0]);
            if (selectedStyle == defaultStyle) {
                removeTrackDefaultButton.setEnabled(false);
            } else {
                removeTrackDefaultButton.setEnabled(true);
            }
            trackDefaultTextField.setText(selectedStyle.getTrackName());
            bgColorComboBox.setSelectedColor(selectedStyle.getBackground());
            fgColorComboBox.setSelectedColor(selectedStyle.getForeground());
            labelColorComboBox.setSelectedColor(selectedStyle.getLabelForeground());
            trackNameSizeComboBox.setSelectedItem(selectedStyle.getTrackNameSize());
            if (Arrays.asList(graphFormats).contains(selectedStyle.getTrackName())) {
                setEnabled(false);
                labelFieldComboBox.setSelectedItem(null);
                maxDepthTextField.setText("");
                show2TracksCheckBox.setSelected(false);
                collapsedCheckBox.setSelected(false);
                colorCheckBox.setSelected(false);
                arrowCheckBox.setSelected(false);
                possitiveColorComboBox.setSelectedColor(null);
                negativeColorComboBox.setSelectedColor(null);
            } else {
                possitiveColorComboBox.setSelectedColor(selectedStyle.getForwardColor());
                negativeColorComboBox.setSelectedColor(selectedStyle.getReverseColor());
                labelFieldComboBox.setSelectedItem(selectedStyle.getLabelField());
                maxDepthTextField.setText(String.valueOf(selectedStyle.getMaxDepth()));
                show2TracksCheckBox.setSelected(!selectedStyle.getSeparate());
                collapsedCheckBox.setSelected(selectedStyle.getCollapsed());
            }
            applyChanged = true;
            initializationDetector = false;
        }
    }

    public JRPButton getAddTrackDefaultButton() {
        return addTrackDefaultButton;
    }

    public JRPButton getRemoveTrackDefaultButton() {
        return removeTrackDefaultButton;
    }

    @Override
    public JTextField getTrackDefaultTextField() {
        return trackDefaultTextField;
    }

    class TrackDefaultPrefTableModel extends AbstractTableModel implements PropertyConstants {

        private static final long serialVersionUID = 1L;
        private List<TrackStyle> tier_styles;
        private AssociationElement element;
        private Entry<String, AssociationElement>[] file2types;
        private Entry<String, AssociationElement> entry;

        TrackDefaultPrefTableModel() {
            this.tier_styles = new ArrayList<>();
        }

        public void setStyles(List<TrackStyle> tier_styles) {
            this.tier_styles = tier_styles;
        }

        public List<TrackStyle> getStyles() {
            return this.tier_styles;
        }

        @SuppressWarnings("unchecked")
        private void setElements(Map<String, AssociationElement> elements) {
            file2types = elements.entrySet().toArray(new Entry[elements.size()]);
            tier_styles.add(defaultStyle);
            for (Entry<String, AssociationElement> entries : file2types) {
                element = entries.getValue();

                addElement(entries.getKey(), element);
            }
            fireTableDataChanged();
        }

        private void addElement(String filetype, AssociationElement element) {
            if (element.getPropertyMap() != null) {
                PropertyMap props = defaultStyle.getProperties();
                props.putAll(element.getPropertyMap());
                element.getPropertyMap().putAll(props);
                TrackStyle style = new TrackStyle(props);
                style.setTrackName(filetype);
                tier_styles.add(style);
            }
        }

        private void removeElement(String filetype) {
            Iterator<TrackStyle> iterator = tier_styles.iterator();
            TrackStyle style;
            while (iterator.hasNext()) {
                style = iterator.next();
                if (style.getTrackName().equals(filetype)) {
                    iterator.remove();
                }
            }
        }

        // Allow editing most fields in normal rows, but don't allow editing some
        // fields in the "default" style row.
        @Override
        public boolean isCellEditable(int row, int column) {
            if (column == COL_TRACK_DEFAULT) {
                return false;
            }

            return true;
        }

        @Override
        public int getRowCount() {
            return tier_styles.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return col_headings[columnIndex];
        }

        @Override
        public int getColumnCount() {
            return col_headings.length;
        }

        @Override
        public Object getValueAt(int row, int column) {
            TrackStyle style = tier_styles.get(row);
            switch (column) {
                case COL_TRACK_DEFAULT:
                    return style.getTrackName();
                case COL_FOREGROUND:
                    return style.getForeground();
                case COL_BACKGROUND:
                    return style.getBackground();
                case COL_LABEL_COLOR:
                    return style.getLabelForeground();
                case COL_TRACK_NAME_SIZE:
                    return style.getTrackNameSize();
                default:
                    return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            Object val = getValueAt(0, c);
            if (val == null) {
                return Object.class;
            } else {
                return val.getClass();
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            for (int i : selectedRows) {
                setValue(value, i, col);
            }
        }

        public void setValue(Object value, int row, int col) {
            applyChanged = false;
            if (value != null && !initializationDetector) {
                try {
                    TrackStyle style = tier_styles.get(row);
                    if (!style.equals(defaultStyle)) {
                        entry = file2types[row - 1];
                        element = entry.getValue();
                    }
                    fireTableCellUpdated(row, col);
                } catch (Exception e) {
                    // exceptions should not happen, but must be caught if they do

                }
            }
            applyChanged = true;
        }

        int parseInteger(String s, int empty_string, int fallback) {
            //System.out.println("Parsing string: '" + s + "'");
            int i = fallback;
            try {
                if ("".equals(s.trim())) {
                    i = empty_string;
                } else {
                    i = Integer.parseInt(s);
                }
            } catch (Exception e) {
                //System.out.println("Exception: " + e);
                // don't report the error, use the fallback value
            }
            return i;
        }
    }
}
