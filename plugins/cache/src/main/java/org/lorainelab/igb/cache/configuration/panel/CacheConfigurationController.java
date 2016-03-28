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

import com.google.common.eventbus.Subscribe;

import java.io.IOException;
import java.math.BigInteger;

import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.net.URL;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import org.lorainelab.igb.cache.api.ChangeEvent;
import org.lorainelab.igb.cache.api.RemoteFileCacheService;
import org.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dfreese
 */

@aQute.bnd.annotation.component.Component(name = CacheConfigurationPanel.COMPONENT_NAME, immediate = true, provide = PreferencesPanelProvider.class)
public class CacheConfigurationController extends JRPJPanel implements PreferencesPanelProvider {
    private static final Logger logger = LoggerFactory.getLogger(CacheConfigurationController.class);

    public static final String COMPONENT_NAME = "CacheConfigurationPanel";
    private static final String TAB_NAME = "Cache";
    private static final int TAB_POSITION = 10;
    JFXPanel fxPanel = new JFXPanel();

    @FXML
    private Button clearBtn;
    @FXML
    private Button removeBtn;
    @FXML
    private Button refreshBtn;
    @FXML
    private Button resetCachePreferencesBtn;
    @FXML
    private CheckBox enableCache;
    @FXML
    private Button applyBtn;
    @FXML
    private TextField maxCacheSize;
    @FXML
    private TextField minFileSize;
    @FXML
    private TableView<CacheEntry> table;
    @FXML
    private Label CacheSizeLabel;
    @FXML
    private TableColumn<CacheEntry, String> sourceCol;
    @FXML
    private TableColumn<CacheEntry, String> lastModifiedCol;
    @FXML
    private TableColumn<CacheEntry, String> cacheCol;
    @FXML
    private TableColumn<CacheEntry, String> lastAccessCol;
    @FXML
    private TableColumn<CacheEntry, String> sizeCol;
    private Pane pane;
    private Scene scene;

    private RemoteFileCacheService remoteFileCacheService;
    private CacheTableModelFx cacheTableModelFx;
    private final Preferences cachePrefsNode;
    
    private ListProperty<CacheEntry> cacheEntries;

    public CacheConfigurationController() {
        super(COMPONENT_NAME);
        cacheEntries = new SimpleListProperty<CacheEntry>(FXCollections.observableArrayList());
        setLayout(new MigLayout(new LC().fill().insetsAll("0")));
        cachePrefsNode = PreferenceUtils.getCachePrefsNode();

        Platform.runLater(() -> {
            init();
        });

    }

    @Activate
    public void activate() {
        remoteFileCacheService.registerEventListener(this);
        initializeLayout();
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
        cacheTableModelFx.refresh();
        initMaxCacheSizeValue();
        initMinFileSizeValue();
        initCacheEnableValue();
        initCacheSizeValue();

        Platform.runLater(() -> {
            populateTable();
        });

    }

    @Subscribe
    public void subscribeToChange(ChangeEvent e) {
        refresh();
    }

    private void initializeLayout() {
        add(fxPanel, "grow, wrap");
 
    }

    private void init() {
        final URL resource = CacheConfigurationController.class.getClassLoader().getResource("CacheConfigurationPanelFx.fxml");
        FXMLLoader loader = new FXMLLoader(resource);
        loader.setController(this);
        try {
            pane = (Pane) loader.load();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        Platform.runLater(() -> {
            this.createScene();
            initializeTable();
            clearBtn.disableProperty().setValue(cacheEntries.isEmpty());
            removeBtn.disableProperty().setValue(cacheEntries.isEmpty());
            cacheEntries.addListener((ObservableValue<? extends ObservableList<CacheEntry>> observable, ObservableList<CacheEntry> oldValue, ObservableList<CacheEntry> newValue) -> {
                clearBtn.disableProperty().setValue(cacheEntries.isEmpty());
                removeBtn.disableProperty().set(cacheEntries.isEmpty());
            });
        });

    }

    private void createScene() {
        Scene scene = new Scene(pane);
        fxPanel.setScene(scene);
    }

    @FXML
    private void clearBtnAction(ActionEvent ae) {
        remoteFileCacheService.clearAllCaches();
        LocalUrlCacher.clearCache();
        refresh();
    }

    @FXML
    private void removeBtnAction(ActionEvent ae) {
        ObservableList<CacheEntry> selectedRows = table.getSelectionModel().getSelectedItems();
        for (int i = 0; i < selectedRows.size(); i++) {
            cacheTableModelFx.removeRow(i);  
        }
        refresh();
    }

    @FXML
    private void refreshBtnAction(ActionEvent ae) {
        refresh(); 
    }

    @FXML
    private void resetCachePreferencesBtnAction(ActionEvent ae) {
        try {
            cachePrefsNode.clear();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @FXML
    private void applyBtnAction(ActionEvent ae) {

        try {
            BigInteger maxCacheSizeValue = new BigInteger(maxCacheSize.getText());
            BigInteger minFileSizeValue = new BigInteger(minFileSize.getText());
            BigInteger currentCacheSize = remoteFileCacheService.getCacheSizeInMB();

            if (currentCacheSize.compareTo(maxCacheSizeValue) > 0) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error Dialog");
                alert.setHeaderText("The max cache size is less than the current cache size.");

                alert.showAndWait();
                return;
            }
            remoteFileCacheService.setMaxCacheSizeMB(maxCacheSizeValue);

            remoteFileCacheService.setMinFileSizeBytes(minFileSizeValue.multiply(new BigInteger("1000")));
        } catch (Exception ex) {
            //TODO: Add warning
        }
        initMaxCacheSizeValue();
        initMinFileSizeValue();
    }

    @FXML
    private void enableCacheAction(ActionEvent ae) {
        initCacheEnableValue();
            remoteFileCacheService.setCacheEnabled(enableCache.isSelected());
            if (enableCache.isSelected()) {
                enableCacheSettings();
            } else {
                disableCacheSettings();
            }

    }

    private void initializeTable() {
        sourceCol.setCellValueFactory(cellData -> cellData.getValue().getUrlStringProperty());
        lastModifiedCol.setCellValueFactory(cellData -> cellData.getValue().getLastModifiedStringProperty());
        cacheCol.setCellValueFactory(cellData -> cellData.getValue().getCacheUpdateStringProperty());
        lastAccessCol.setCellValueFactory(cellData -> cellData.getValue().getLastAccessedStringProperty());
        sizeCol.setCellValueFactory(cellData -> cellData.getValue().getCacheSizeStringProperty());

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setPlaceholder(new Label(""));

    }

    private void populateTable() {
        cacheEntries.clear();
        cacheTableModelFx.refresh();
        
        CacheEntry cacheEntry;
        int rowCount = cacheTableModelFx.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            String url = cacheTableModelFx.getValueAt(i, 0).toString();
            String lastModified = cacheTableModelFx.getValueAt(i, 1).toString();
            String cacheUpdate = cacheTableModelFx.getValueAt(i, 2).toString();
            String lastAccessed = cacheTableModelFx.getValueAt(i, 3).toString();
            String cacheSize = cacheTableModelFx.getValueAt(i, 4).toString();
            cacheEntry = new CacheEntry(url, lastModified, cacheUpdate, lastAccessed, cacheSize);
            cacheEntries.add(cacheEntry);
        }

        table.setItems(cacheEntries);

    }

    private void initMaxCacheSizeValue() {
        maxCacheSize.setText(remoteFileCacheService.getMaxCacheSizeMB().toString());
    }

    private void initMinFileSizeValue() {
        minFileSize.setText(remoteFileCacheService.getMinFileSizeBytes().divide(new BigInteger("1000")).toString());
    }

    private void initCacheEnableValue() {
        boolean cacheEnabled = remoteFileCacheService.getCacheEnabled();
        enableCache.setSelected(cacheEnabled);
        if (cacheEnabled) {
            enableCacheSettings();
        } else {
            disableCacheSettings();
        }
    }

    private void enableCacheSettings() {
        maxCacheSize.setDisable(false);
        minFileSize.setDisable(false);
        applyBtn.setDisable(false);

    }

    private void disableCacheSettings() {
        maxCacheSize.setDisable(true);
        minFileSize.setDisable(true);
        applyBtn.setDisable(true);
    }

    private void initCacheSizeValue() {
        BigInteger currentCacheSize = remoteFileCacheService.getCacheSizeInMB();
        Platform.runLater(() -> {
            CacheSizeLabel.setText(currentCacheSize + " MB");
        });
    }

    @Reference
    public void setRemoteFileCacheService(RemoteFileCacheService remoteFileCacheService) {
        this.remoteFileCacheService = remoteFileCacheService;
    }

    @Reference
    public void setCacheTableModelFx(CacheTableModelFx cacheTableModel) {
        this.cacheTableModelFx = cacheTableModel;
    }

    
}
