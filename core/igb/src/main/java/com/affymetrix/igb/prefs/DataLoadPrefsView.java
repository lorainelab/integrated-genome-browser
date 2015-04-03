/**
 * Copyright (c) 2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.prefs;

import com.affymetrix.genometry.general.GenericServer;
import static com.affymetrix.genometry.general.GenericServerPrefKeys.SERVER_ORDER;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.LocalUrlCacher.CacheUsage;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.ServerTypeI;
import com.affymetrix.genometry.util.SynonymLookup;
import com.affymetrix.igb.action.AutoLoadFeatureAction;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.JRPJPanel;
import com.affymetrix.igb.swing.JRPTextField;
import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.util.IGBAuthenticator;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.lorainelab.igb.services.window.HtmlHelpProvider;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.Authenticator.RequestorType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import javax.swing.GroupLayout.Group;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import static javax.swing.JFileChooser.FILES_AND_DIRECTORIES;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sgblanch
 * @version $Id: DataLoadPrefsView.java 9956 2012-01-25 16:46:38Z dcnorris $
 */
public final class DataLoadPrefsView extends ServerPrefsView implements HtmlHelpProvider {

    private static final long serialVersionUID = 1L;
    private static final String PREF_VSYN_FILE_URL = "Version Synonyms File URL";
    private static final String PREF_CSYN_FILE_URL = "Chromosome Synonyms File URL";
    private static DataLoadPrefsView singleton;
    private static final JCheckBox autoload = AutoLoadFeatureAction.getActionCB();
    protected JRPButton editSourceButton;
    protected JRPButton editAuthButton;
    protected JRPButton rankUpButton;
    protected JRPButton rankDownButton;
    public static final int TAB_POSITION = 3;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DataLoadPrefsView.class);

    public static synchronized DataLoadPrefsView getSingleton() {
        if (singleton == null) {
            singleton = new DataLoadPrefsView();
        }
        return singleton;
    }

    public DataLoadPrefsView() {
        super(ServerList.getServerInstance());
        final JPanel synonymsPanel = initSynonymsPanel(this.getPanel());
        final JPanel cachePanel = initCachePanel();

        layout.setHorizontalGroup(layout.createParallelGroup().addComponent(sourcePanel).addComponent(synonymsPanel).addComponent(cachePanel));
        layout.setVerticalGroup(layout.createSequentialGroup().addComponent(sourcePanel).addComponent(synonymsPanel).addComponent(cachePanel));
    }

    @Override
    public int getWeight() {
        return TAB_POSITION;
    }

    @Override
    protected JPanel initSourcePanel(String viewName) {
        editAuthButton = createButton("DataLoadPrefsView_editAuthButton", "Authenticate\u2026", e -> {
            sourcesTable.stopCellEditing();
            Object url = sourcesTable.getModel().getValueAt(
                    sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()),
                    ((SourceTableModel) sourcesTable.getModel()).getColumnIndex(SourceTableModel.SourceColumn.URL));
            try {
                URL u = new URL((String) url);
                IGBAuthenticator.resetAuth((String) url);
                Authenticator.requestPasswordAuthentication(
                        u.getHost(),
                        null,
                        u.getPort(),
                        u.getProtocol(),
                        "Server Credentials",
                        null,
                        u,
                        RequestorType.SERVER);
            } catch (MalformedURLException ex) {
                Logger.getLogger(ServerPrefsView.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        editAuthButton.setEnabled(false);

        editSourceButton = createButton("DataLoadPrefsView_editAuthButton", "Edit\u2026", e -> {
            sourcesTable.stopCellEditing();

            Object url = sourcesTable.getModel().getValueAt(
                    sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()),
                    ((SourceTableModel) sourcesTable.getModel()).getColumnIndex(SourceTableModel.SourceColumn.URL));
            GenericServer server = ServerList.getServerInstance().getServer((String) url);

            AddSource.getSingleton().init(true, true, "Edit Data Source", server, (String) url, server.getMirrorUrl());
        });
        editSourceButton.setEnabled(false);
        ImageIcon up_icon = MenuUtil.getIcon("16x16/actions/up.png");
        rankUpButton = new JRPButton("DataLoadPrefsView_rankUpButton", up_icon);
        rankUpButton.setToolTipText("Increase sequence server priority");
        rankUpButton.addActionListener(
                e -> {
                    sourcesTable.stopCellEditing();
                    int row = sourcesTable.getSelectedRow();
                    if (row >= 1 && row < sourcesTable.getRowCount()) {
                        ((SourceTableModel) sourcesTable.getModel()).switchRows(row - 1);
                        sourcesTable.getSelectionModel().setSelectionInterval(row - 1, row - 1);
                        int column = ((SourceTableModel) sourcesTable.getModel()).getColumnIndex(SourceTableModel.SourceColumn.URL);
                        String URL = sourcesTable.getModel().getValueAt(row - 1, column).toString();
                        serverList.setServerOrder(URL, row - 1);
                        URL = sourcesTable.getModel().getValueAt(row, column).toString();
                        serverList.setServerOrder(URL, row);
                    }
                });
        rankUpButton.setEnabled(false);
        ImageIcon down_icon = MenuUtil.getIcon("16x16/actions/down.png");
        rankDownButton = new JRPButton("DataLoadPrefsView_rankDownButton", down_icon);
        rankDownButton.setToolTipText("Decrease sequence server priority");
        rankDownButton.addActionListener(
                e -> {
                    sourcesTable.stopCellEditing();
                    int row = sourcesTable.getSelectedRow();
                    if (row >= 0 && row < sourcesTable.getRowCount() - 1) {
                        ((SourceTableModel) sourcesTable.getModel()).switchRows(row);
                        sourcesTable.getSelectionModel().setSelectionInterval(row + 1, row + 1);
                        int column = ((SourceTableModel) sourcesTable.getModel()).getColumnIndex(SourceTableModel.SourceColumn.URL);
                        String URL = sourcesTable.getModel().getValueAt(row, column).toString();
                        serverList.setServerOrder(URL, row);
                        URL = sourcesTable.getModel().getValueAt(row + 1, column).toString();
                        serverList.setServerOrder(URL, row + 1);
                    }
                });
        rankDownButton.setEnabled(false);
        return super.initSourcePanel(viewName);
    }

    @Override
    protected void enableServerButtons(boolean enable) {
        super.enableServerButtons(enable);
        rankUpButton.setEnabled(sourcesTable.getSelectedRow() > 0);
        rankDownButton.setEnabled(sourcesTable.getSelectedRow() < sourcesTable.getRowCount() - 1);
        editAuthButton.setEnabled(sourcesTable.getSelectedRowCount() == 1);
        editSourceButton.setEnabled(enable);
    }

    @Override
    protected boolean isSortable() {
        return false;
    }

    @Override
    protected Group addServerComponents(Group group1, Group group2) {
        return group1.addComponent(sourcesScrollPane).addComponent(autoload).addGroup(group2.addComponent(rankUpButton).addComponent(rankDownButton).addComponent(addServerButton).addComponent(editSourceButton).addComponent(editAuthButton).addComponent(removeServerButton));
    }

    @Override
    protected Group getServerButtons(Group group) {
        return group.addComponent(rankUpButton).addComponent(rankDownButton).addComponent(addServerButton).addComponent(editSourceButton).addComponent(editAuthButton).addComponent(removeServerButton);
    }

    private static JPanel initSynonymsPanel(final JPanel parent) {
        final JPanel synonymsPanel = new JPanel();
        final GroupLayout layout = new GroupLayout(synonymsPanel);
        final JLabel vsynonymsLabel = new JLabel("Version Synonyms File");
        final JLabel csynonymsLabel = new JLabel("Chromosome Synonyms File");
        final JRPTextField vsynonymFile = new JRPTextField("DataLoadPrefsView_vsynonymFile", PreferenceUtils.getLocationsNode().get(PREF_VSYN_FILE_URL, ""));
        final JRPTextField csynonymFile = new JRPTextField("DataLoadPrefsView_csynonymFile", PreferenceUtils.getLocationsNode().get(PREF_CSYN_FILE_URL, ""));
        final JRPButton vopenFile = new JRPButton("DataLoadPrefsView_vopenFile", "\u2026");
        final JRPButton copenFile = new JRPButton("DataLoadPrefsView_copenFile", "\u2026");

        final ActionListener vlistener = e -> {
            if (e.getSource() == vopenFile) {
                File file = fileChooser(FILES_AND_DIRECTORIES, parent);
                try {
                    if (file != null) {
                        vsynonymFile.setText(file.getCanonicalPath());
                    }
                } catch (IOException ex) {
                    Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (vsynonymFile.getText().isEmpty() || loadSynonymFile(SynonymLookup.getDefaultLookup(), vsynonymFile)) {
                PreferenceUtils.getLocationsNode().put(PREF_VSYN_FILE_URL, vsynonymFile.getText());
            } else {
                ErrorHandler.errorPanel(
                        "Unable to Load Version Synonyms",
                        "Unable to load personal synonyms from " + vsynonymFile.getText() + ".", Level.SEVERE);
            }
        };

        final ActionListener clistener = e -> {
            if (e.getSource() == copenFile) {
                File file = fileChooser(FILES_AND_DIRECTORIES, parent);
                try {
                    if (file != null) {
                        csynonymFile.setText(file.getCanonicalPath());
                    }
                } catch (IOException ex) {
                    Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (csynonymFile.getText().isEmpty() || loadSynonymFile(SynonymLookup.getChromosomeLookup(), csynonymFile)) {
                PreferenceUtils.getLocationsNode().put(PREF_CSYN_FILE_URL, csynonymFile.getText());
            } else {
                ErrorHandler.errorPanel(
                        "Unable to Load Chromosome Synonyms",
                        "Unable to load personal synonyms from " + csynonymFile.getText() + ".", Level.SEVERE);
            }
        };

        vopenFile.setToolTipText("Open Local Directory");
        vopenFile.addActionListener(vlistener);
        vsynonymFile.addActionListener(vlistener);

        copenFile.setToolTipText("Open Local Directory");
        copenFile.addActionListener(clistener);
        csynonymFile.addActionListener(clistener);

        synonymsPanel.setLayout(layout);
        synonymsPanel.setBorder(new TitledBorder("Personal Synonyms"));
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createParallelGroup(LEADING).addGroup(layout.createSequentialGroup().addComponent(vsynonymsLabel).addComponent(vsynonymFile).addComponent(vopenFile)).addGroup(layout.createSequentialGroup().addComponent(csynonymsLabel).addComponent(csynonymFile).addComponent(copenFile)));

        layout.setVerticalGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(BASELINE).addComponent(vsynonymsLabel).addComponent(vsynonymFile).addComponent(vopenFile)).addGroup(layout.createParallelGroup(BASELINE).addComponent(csynonymsLabel).addComponent(csynonymFile).addComponent(copenFile)));

        /*
         * Load the synonym file from preferences on startup
         */
        loadSynonymFile(SynonymLookup.getDefaultLookup(), vsynonymFile);
        loadSynonymFile(SynonymLookup.getChromosomeLookup(), csynonymFile);

        return synonymsPanel;
    }

    private static JPanel initCachePanel() {

        final JLabel usageLabel = new JLabel("Cache Behavior");
        final JLabel emptyLabel = new JLabel();
        final JLabel cacheCleared = new JLabel("Cache Cleared");
        final JComboBox cacheUsage = new JComboBox(CacheUsage.values());
        final JRPButton clearCache = new JRPButton("DataLoadPrefsView_clearCache", "Empty Cache");
        cacheCleared.setVisible(false);
        cacheCleared.setForeground(Color.RED);
        clearCache.addActionListener(e -> {
            System.out.println("Action performed :" + Thread.currentThread().getId());
            clearCache.setEnabled(false);
            LocalUrlCacher.clearCache();
            clearCache.setEnabled(true);
            cacheCleared.setVisible(true);

            CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>("clear cache") {

                @Override
                protected Object runInBackground() {
                    System.out.println("Runnable :" + Thread.currentThread().getId());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException x) {
                    }
                    return null;
                }

                @Override
                public void finished() {
                    cacheCleared.setVisible(false);
                }
            };
            CThreadHolder.getInstance().execute(cacheCleared, worker);
        });

        cacheUsage.setSelectedItem(LocalUrlCacher.getCacheUsage(LocalUrlCacher.getPreferredCacheUsage()));
        cacheUsage.addActionListener(e -> LocalUrlCacher.setPreferredCacheUsage(((CacheUsage) cacheUsage.getSelectedItem()).usage));

        final JPanel cachePanel = new JPanel();
        final GroupLayout layout = new GroupLayout(cachePanel);
        cachePanel.setLayout(layout);
        cachePanel.setBorder(new TitledBorder("Cache Settings"));
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.linkSize(usageLabel, emptyLabel);

        layout.setHorizontalGroup(layout.createParallelGroup(LEADING).addGroup(layout.createSequentialGroup().addComponent(usageLabel).addComponent(cacheUsage)).addGroup(layout.createSequentialGroup().addComponent(emptyLabel).addComponent(clearCache).addComponent(cacheCleared)));

        layout.setVerticalGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(BASELINE).addComponent(usageLabel).addComponent(cacheUsage)).addGroup(layout.createParallelGroup(BASELINE).addComponent(emptyLabel).addComponent(clearCache).addComponent(cacheCleared)));

        return cachePanel;
    }

    private static boolean loadSynonymFile(SynonymLookup lookup, JRPTextField synonymFile) {
        File file = new File(synonymFile.getText());

        if (!file.isFile() || !file.canRead()) {
            return false;
        }

        FileInputStream fis = null;
        try {
            synonymFile.setText(file.getCanonicalPath());
            fis = new FileInputStream(file);
            lookup.loadSynonyms(fis);
        } catch (IOException ex) {
            return false;
        } finally {
            GeneralUtils.safeClose(fis);
        }

        return true;
    }

    @Override
    protected String getViewName() {
        return "Data Sources";
    }

    @Override
    protected String getToolTip() {
        return "Edit data sources and preferences";
    }

    @Override
    protected boolean enableCombo() {
        return true;
    }

    @Override
    protected void updateSource(GenericServer server) {
        ServerList.getServerInstance().removeServer(server.getUrlString());
        addDataSource(server);
    }

    @Override
    protected void updateSource(String url, ServerTypeI type, String name, String newUrl) {
        Preferences node = PreferenceUtils.getServersNode().node(GenericServer.getHash(url));
        int order = node.getInt(SERVER_ORDER, -1);
        boolean isDefault = ServerList.getServerInstance().getServer(url).isDefault();
        ServerList.getServerInstance().removeServer(url);
        addDataSource(type, name, newUrl, order, isDefault);
    }

    @Override
    public JRPJPanel getPanel() {
        return this;
    }

    @Override
    public String getHelpHtml() {
        String htmlText = null;
        try {
            htmlText = Resources.toString(DataLoadPrefsView.class.getResource("/help/com.affymetrix.igb.prefs.DataLoadPrefsView.html"), Charsets.UTF_8);
        } catch (IOException ex) {
            logger.error("Help file not found " , ex);
        }
        return htmlText;
    }
}
