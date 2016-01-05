/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.cache.configuration.panel;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.igb.swing.JRPJPanel;
import com.affymetrix.igb.swing.jide.JRPStyledTable;
import com.google.common.eventbus.Subscribe;
import org.lorainelab.igb.cache.api.ChangeEvent;
import org.lorainelab.igb.cache.api.RemoteFileCacheService;
import org.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
@aQute.bnd.annotation.component.Component(name = CacheConfigurationPanel.COMPONENT_NAME, immediate = true, provide = PreferencesPanelProvider.class)
public class CacheConfigurationPanel extends JRPJPanel implements PreferencesPanelProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CacheConfigurationPanel.class);

    public static final String COMPONENT_NAME = "CacheConfigurationPanel";
    private static final String TAB_NAME = "Cache";
    private static final int TAB_POSITION = 10;
    private JPanel cachePanel;
    private JCheckBox cacheEnable;
    private JButton removeBtn;
    private JButton clearAllBtn;
    private JButton refreshBtn;
    private JTable cacheDataTable;
    private JPanel cacheDataPanel;
    public JLabel currentCacheSizeLabel;
    public JTextField maxCacheSize;
    public JLabel maxCacheSizeLabel;
    public JLabel maxCacheSizeUnits;
    public JTextField minFileSize;
    public JLabel minFileSizeLabel;
    public JLabel minFileSizeUnits;
    private JButton cacheSettingsApply;
    private JButton cacheResetPref;
    private RemoteFileCacheService remoteFileCacheService;
    private CacheTableModel cacheTableModel;
    private final Preferences cachePrefsNode;

    String[] columnNames = {"Source",
        "Last Modified",
        "Cached On",
        "Size",
        "Action"};

    public CacheConfigurationPanel() {
        super(COMPONENT_NAME);
        setLayout(new MigLayout(new LC().fill().insetsAll("0")));
        cachePrefsNode = PreferenceUtils.getCachePrefsNode();

    }

    @Subscribe
    public void subscribeToChange(ChangeEvent e) {
        refresh();
    }

    @Override
    public String getName() {
        return TAB_NAME;
    }

    @Override
    public int getWeight() {
        return TAB_POSITION;
    }

    @Override
    public JRPJPanel getPanel() {
        return this;
    }

    @Override
    public void refresh() {
        cacheTableModel.refresh();
        cacheTableModel.fireTableDataChanged();
        initMaxCacheSizeValue();
        initMinFileSizeValue();
        initCacheEnableValue();
        initCacheSizeValue();
    }

    @Activate
    public void activate() {
        cachePanel = initilizeCacheSettingsPanel();
        cacheDataTable = getStyledJTable();
        cacheDataPanel = initilizeCacheDataPanel();
        initilizeLayout();
        remoteFileCacheService.registerEventListener(this);

    }

    private void initilizeLayout() {
        add(cacheDataPanel, "grow, wrap");
        add(cachePanel, "grow, wrap");
    }

    private JPanel initilizeCacheSettingsPanel() {
        JPanel panel = new JPanel(new MigLayout(new LC().insetsAll("0")));
        panel.setBorder(BorderFactory.createTitledBorder("Cache Settings"));
        initCacheSizeLabel();
        initCacheSettingsApply();
        initResetCachePref();
        initMaxCacheSize();
        initMinFileSize();
        initCacheEnable();

        JPanel currentCacheSizePanel = new JPanel(new MigLayout(new LC().insetsAll("2")));
        currentCacheSizePanel.add(currentCacheSizeLabel);

        JPanel cacheEnablePanel = new JPanel(new MigLayout(new LC().insetsAll("2")));
        cacheEnablePanel.add(cacheEnable);

        JPanel cacheResetPrefPanel = new JPanel(new MigLayout(new LC().insetsAll("2")));
        cacheResetPrefPanel.add(cacheResetPref, "width :100:");

        JPanel maxCacheSizePanel = new JPanel(new MigLayout(new LC().insetsAll("2")));
        maxCacheSizePanel.add(maxCacheSizeLabel, "width :125:");
        maxCacheSizePanel.add(maxCacheSize, "width :75:");
        maxCacheSizePanel.add(maxCacheSizeUnits, "width :100:");

        JPanel minFileSizePanel = new JPanel(new MigLayout(new LC().insetsAll("2")));
        minFileSizePanel.add(minFileSizeLabel, "width :125:");
        minFileSizePanel.add(minFileSize, "width :75:");
        minFileSizePanel.add(minFileSizeUnits, "width :100:");

        JPanel cacheSettingsApplyPanel = new JPanel(new MigLayout(new LC().insetsAll("2")));
        cacheSettingsApplyPanel.add(cacheSettingsApply, "width :100:");

        panel.add(currentCacheSizePanel, "wrap");
        panel.add(cacheEnablePanel, "wrap");
        panel.add(cacheResetPrefPanel, "wrap");
        panel.add(maxCacheSizePanel, "wrap");
        panel.add(minFileSizePanel, "wrap");
        panel.add(cacheSettingsApplyPanel, "wrap");
        return panel;
    }

    private JPanel initilizeCacheDataPanel() {
        JPanel panel = new JPanel(new MigLayout("ins 0", "[grow]", "[grow][grow]"));
        panel.setBorder(BorderFactory.createTitledBorder("Cache Entries"));
        JScrollPane sourcesScrollPane = new JScrollPane(cacheDataTable);

        initRemoveBtn();
        initClearAllBtn();
        initRefreshBtn();
        JPanel btnPanel = new JPanel(new MigLayout(new LC().insetsAll("0")));
        btnPanel.add(clearAllBtn, "right");
        btnPanel.add(removeBtn, "right");
        btnPanel.add(refreshBtn, "right");
        //
        panel.add(sourcesScrollPane, "grow, wrap");
        panel.add(btnPanel, "right");
        return panel;
    }

    private void initMaxCacheSize() {
        maxCacheSize = new JTextField();
        initMaxCacheSizeValue();
        maxCacheSize.setInputVerifier(new InputVerifier() {

            @Override
            public boolean verify(JComponent input) {
                JTextField tf = (JTextField) input;
                try {
                    BigInteger value = new BigInteger(tf.getText());
                    tf.setBackground(Color.WHITE);
                    return true;
                } catch (Exception e) {
                    tf.setBackground(Color.red);
                    return false;
                }
            }
        });
        maxCacheSizeLabel = new JLabel("Max Cache Size:");
        maxCacheSizeUnits = new JLabel("MB");

    }

    private void initMaxCacheSizeValue() {
        maxCacheSize.setText(remoteFileCacheService.getMaxCacheSizeMB().toString());
        maxCacheSize.setBackground(Color.WHITE);
    }

    private void initMinFileSize() {
        minFileSize = new JTextField();
        initMinFileSizeValue();
        minFileSize.setInputVerifier(new InputVerifier() {

            @Override
            public boolean verify(JComponent input) {
                JTextField tf = (JTextField) input;
                try {
                    BigInteger value = new BigInteger(tf.getText());
                    tf.setBackground(Color.WHITE);
                    return true;
                } catch (Exception e) {
                    tf.setBackground(Color.red);
                    return false;
                }
            }
        });
        minFileSizeLabel = new JLabel("Min File Size:");
        minFileSizeUnits = new JLabel("MB");
    }

    private void initMinFileSizeValue() {
        minFileSize.setText(remoteFileCacheService.getMinFileSizeBytes().divide(new BigInteger("1000")).toString());
        minFileSize.setBackground(Color.WHITE);
    }

    private void initCacheEnable() {
        cacheEnable = new JCheckBox("Enable Cache");
        initCacheEnableValue();
        cacheEnable.addActionListener((ActionEvent e) -> {
            remoteFileCacheService.setCacheEnabled(cacheEnable.isSelected());
            if (cacheEnable.isSelected()) {
                enableCacheSettings();
            } else {
                disableCacheSettings();
            }
        });
    }

    private void disableCacheSettings() {
        maxCacheSize.setEnabled(false);
        minFileSize.setEnabled(false);
        cacheSettingsApply.setEnabled(false);
    }

    private void enableCacheSettings() {
        maxCacheSize.setEnabled(true);
        minFileSize.setEnabled(true);
        cacheSettingsApply.setEnabled(true);
    }

    private void initCacheEnableValue() {
        boolean cacheEnabled = remoteFileCacheService.getCacheEnabled();
        cacheEnable.setSelected(cacheEnabled);
        if (cacheEnabled) {
            enableCacheSettings();
        } else {
            disableCacheSettings();
        }
    }

    private void initResetCachePref() {
        cacheResetPref = new JButton("Reset Cache Preferences");
        cacheResetPref.addActionListener((ActionEvent e) -> {
            try {
                cachePrefsNode.clear();
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
            }
        });
    }

    private void initCacheSizeLabel() {
        currentCacheSizeLabel = new JLabel();
        initCacheSizeValue();
    }

    private void initCacheSizeValue() {
        BigInteger currentCacheSize = remoteFileCacheService.getCacheSizeInMB();
        currentCacheSizeLabel.setText("Current Cache Size: " + currentCacheSize + " MB");
    }

    private void initCacheSettingsApply() {
        cacheSettingsApply = new JButton("Apply");
        cacheSettingsApply.addActionListener((ActionEvent e) -> {
            try {

                BigInteger maxCacheSizeValue = new BigInteger(maxCacheSize.getText());
                BigInteger minFileSizeValue = new BigInteger(minFileSize.getText());
                BigInteger currentCacheSize = remoteFileCacheService.getCacheSizeInMB();

                if (currentCacheSize.compareTo(maxCacheSizeValue) > 0) {
                    JPanel parentPanel = new JPanel(new MigLayout());
                    parentPanel.add(new JLabel("The max cache size is less than the current cache size."));
                    final JComponent[] inputs = new JComponent[]{
                        parentPanel
                    };
                    Object[] options = {"OK"};

                    int optionChosen = JOptionPane.showOptionDialog(null, inputs, "Cache Settings", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE,
                            null,
                            options,
                            options[0]);
                    return;
                }
                remoteFileCacheService.setMaxCacheSizeMB(maxCacheSizeValue);

                remoteFileCacheService.setMinFileSizeBytes(minFileSizeValue.multiply(new BigInteger("1000")));
            } catch (Exception ex) {
                //TODO: Add warning
            }

            initMaxCacheSizeValue();
            initMinFileSizeValue();
        });
    }

    private void initRefreshBtn() {
        refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener((ActionEvent e) -> {
            refresh();
        });
    }

    private void initRemoveBtn() {
        removeBtn = new JButton("Remove");
        removeBtn.addActionListener((ActionEvent e) -> {
            int rows[] = cacheDataTable.getSelectedRows();
            for (int i = 0; i < cacheDataTable.getSelectedRowCount(); i++) {
                cacheTableModel.removeRow(cacheDataTable.convertRowIndexToModel(rows[i]));
            }
            refresh();
        });
        removeBtn.setEnabled(false);
    }

    private void initClearAllBtn() {
        clearAllBtn = new JButton("Clear All");
        clearAllBtn.addActionListener((ActionEvent e) -> {
            remoteFileCacheService.clearAllCaches();
            //Remove this once igb is purged of the LocalUrlCacher
            LocalUrlCacher.clearCache();
            refresh();
        });
        clearAllBtn.setEnabled(true);
    }

    private JTable getStyledJTable() {
        JTable table = new JRPStyledTable("cache configuration table", cacheTableModel) {

            @Override
            public Component prepareRenderer(
                    TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                JComponent jc = (JComponent) c;

                return c;
            }
        };

        Collections.list(table.getColumnModel().getColumns()).forEach(column -> {

            switch (column.getModelIndex()) {
                case 0:
                    //column.setMaxWidth(20);
                    column.setPreferredWidth(200);
                    DefaultTableCellRenderer cr = new DefaultTableCellRenderer() {
                        @Override
                        public int getHorizontalAlignment() {
                            return SwingConstants.LEFT;
                        }
                    };

                    column.setCellRenderer(cr);
                    break;
                case 1:
                    column.setPreferredWidth(100);
                    break;
                case 2:
                    column.setPreferredWidth(100);
                    break;
                case 3:
                    column.setPreferredWidth(100);
                    break;
                case 4:
                    column.setPreferredWidth(40);
                    break;
            }
        });
        table.setRowSelectionAllowed(true);
        table.setShowGrid(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                removeBtn.setEnabled(true);
            }
        });

        TableCellRenderer tableCellRenderer = new DefaultTableCellRenderer() {

            SimpleDateFormat f = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                if (value instanceof Date) {
                    value = f.format(value);
                }
                return super.getTableCellRendererComponent(table, value, isSelected,
                        hasFocus, row, column);
            }
        };

        table.getColumnModel().getColumn(1).setCellRenderer(tableCellRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(tableCellRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(tableCellRenderer);

        table.setAutoCreateRowSorter(true);
        return table;
    }

    @Reference
    public void setRemoteFileCacheService(RemoteFileCacheService remoteFileCacheService) {
        this.remoteFileCacheService = remoteFileCacheService;
    }

    @Reference
    public void setCacheTableModel(CacheTableModel cacheTableModel) {
        this.cacheTableModel = cacheTableModel;
    }

}
