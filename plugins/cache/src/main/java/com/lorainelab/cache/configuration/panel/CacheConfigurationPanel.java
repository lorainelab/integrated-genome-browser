/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.cache.configuration.panel;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Reference;
import com.lorainelab.cache.api.RemoteFileCacheService;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.igb.swing.JRPJPanel;
import com.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.math.BigInteger;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
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
    private JButton removeBtn;
    private JButton clearAllBtn;
    private JButton refreshBtn;
    private JTable cacheDataTable;
    private JPanel cacheDataPanel;
    public JTextField maxCacheSize;
    public JLabel maxCacheSizeLabel;
    public JLabel maxCacheSizeUnits;
    public JTextField minFileSize;
    public JLabel minFileSizeLabel;
    public JLabel minFileSizeUnits;
    public JTextField cacheExpire;
    public JLabel cacheExpireLabel;
    public JLabel cacheExpireUnits;
    private JButton cacheSettingsApply;
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
        setLayout(new MigLayout("fill"));
        cachePrefsNode = PreferenceUtils.getCachePrefsNode();

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
    }

    @Activate
    public void activate() {
        cachePanel = initilizeCacheSettingsPanel();
        cacheDataTable = getStyledJTable();
        cacheDataPanel = initilizeCacheDataPanel();
        initilizeLayout();

    }

    private void initilizeLayout() {
        add(cacheDataPanel, "grow, wrap");
        add(cachePanel, "grow, wrap");
    }

    private JPanel initilizeCacheSettingsPanel() {
        JPanel panel = new JPanel(new MigLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Cache Settings"));
        initCacheExpire();
        initCacheSettingsApply();
        initMaxCacheSize();
        initMinFileSize();
        //TODO:Add buttons to panel
        JPanel cacheExpirePanel = new JPanel(new MigLayout());
        cacheExpirePanel.add(cacheExpireLabel, "width :125:");
        cacheExpirePanel.add(cacheExpire, "width :75:");
        cacheExpirePanel.add(cacheExpireUnits, "width :100:");

        JPanel maxCacheSizePanel = new JPanel(new MigLayout());
        maxCacheSizePanel.add(maxCacheSizeLabel, "width :125:");
        maxCacheSizePanel.add(maxCacheSize, "width :75:");
        maxCacheSizePanel.add(maxCacheSizeUnits, "width :100:");

        JPanel minFileSizePanel = new JPanel(new MigLayout());
        minFileSizePanel.add(minFileSizeLabel, "width :125:");
        minFileSizePanel.add(minFileSize, "width :75:");
        minFileSizePanel.add(minFileSizeUnits, "width :100:");

        JPanel cacheSettingsApplyPanel = new JPanel(new MigLayout());
        cacheSettingsApplyPanel.add(cacheSettingsApply, "width :100:");

        panel.add(cacheExpirePanel, "wrap");
        panel.add(maxCacheSizePanel, "wrap");
        panel.add(minFileSizePanel, "wrap");
        panel.add(cacheSettingsApplyPanel, "wrap");
        return panel;
    }

    private JPanel initilizeCacheDataPanel() {
        JPanel panel = new JPanel(new MigLayout("", "[grow]", "[grow][grow]"));
        panel.setBorder(BorderFactory.createTitledBorder("Data Sources"));
        JScrollPane sourcesScrollPane = new JScrollPane(cacheDataTable);

        initRemoveBtn();
        initClearAllBtn();
        initRefreshBtn();
        JPanel btnPanel = new JPanel(new MigLayout());
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

    private void initCacheExpire() {
        cacheExpire = new JTextField();
        initCacheExpireValue();
        cacheExpire.setInputVerifier(new InputVerifier() {

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
        cacheExpireLabel = new JLabel("Cache Expire:");
        cacheExpireUnits = new JLabel("minutes");
    }

    private void initCacheExpireValue() {
        cacheExpire.setText(remoteFileCacheService.getCacheExpireMin().toString());
        cacheExpire.setBackground(Color.WHITE);
    }

    private void initCacheSettingsApply() {
        cacheSettingsApply = new JButton("Apply");
        cacheSettingsApply.addActionListener((ActionEvent e) -> {
            try {
                BigInteger maxCacheSizeValue = new BigInteger(maxCacheSize.getText());
                remoteFileCacheService.setMaxCacheSizeMB(maxCacheSizeValue);
            } catch (Exception ex) {
                //TODO: Add warning
            }
            try {
                BigInteger minFileSizeValue = new BigInteger(minFileSize.getText());
                remoteFileCacheService.setMinFileSizeBytes(minFileSizeValue.multiply(new BigInteger("1000")));
            } catch (Exception ex) {
                //TODO: Add warning
            }
            try {
                BigInteger cacheExpireValue = new BigInteger(cacheExpire.getText());
                remoteFileCacheService.setCacheExpireMin(cacheExpireValue);
            } catch (Exception ex) {
                //TODO: Add warning
            }
            initMaxCacheSizeValue();
            initMinFileSizeValue();
            initCacheExpireValue();
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
            cacheTableModel.removeRow(cacheDataTable.getSelectedRow());
            refresh();
        });
        removeBtn.setEnabled(false);
    }

    private void initClearAllBtn() {
        clearAllBtn = new JButton("Clear All");
        clearAllBtn.addActionListener((ActionEvent e) -> {
            remoteFileCacheService.clearAllCaches();
            refresh();
        });
        clearAllBtn.setEnabled(true);
    }

    private JTable getStyledJTable() {
        JTable table = new JTable(cacheTableModel) {//data, columnNames){

            @Override
            public Component prepareRenderer(
                    TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                JComponent jc = (JComponent) c;

                return c;
            }
        };
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                removeBtn.setEnabled(true);
            }
        });
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
