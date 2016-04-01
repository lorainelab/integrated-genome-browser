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
import java.net.MalformedURLException;

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
import java.util.Date;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.When;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.lorainelab.igb.cache.api.CacheStatus;

import org.lorainelab.igb.cache.api.ChangeEvent;
import org.lorainelab.igb.cache.api.RemoteFileCacheService;
import org.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dfreese
 */

@aQute.bnd.annotation.component.Component(name = CacheConfigurationController.COMPONENT_NAME, immediate = true, provide = PreferencesPanelProvider.class)
public class CacheConfigurationController extends JRPJPanel implements PreferencesPanelProvider {
    private static final Logger logger = LoggerFactory.getLogger(CacheConfigurationController.class);

    public static final String COMPONENT_NAME = "CacheConfigurationPanel";
    private static final String TAB_NAME = "Cache";
    private static final int TAB_POSITION = 10;
    
    private final Preferences cachePrefsNode;
    private RemoteFileCacheService remoteFileCacheService;
    
    private JFXPanel fxPanel; 
    private Pane pane;
    private ListProperty<CacheEntry> cacheEntries;

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


    public CacheConfigurationController() {
        super(COMPONENT_NAME);
        fxPanel = new JFXPanel();
        cacheEntries = new SimpleListProperty<CacheEntry>(FXCollections.observableArrayList());
        setLayout(new MigLayout(new LC().fill().insetsAll("0")));
        cachePrefsNode = PreferenceUtils.getCachePrefsNode();
        init();
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
            
            ObservableValue<Boolean> cacheEntriesEmpty = cacheEntries.emptyProperty();             
            clearBtn.disableProperty().bind(cacheEntriesEmpty);
            removeBtn.disableProperty().bind(cacheEntriesEmpty);
            
            clearBtnAction(); 
            enableCacheAction(); 
            refreshBtnAction(); 
            removeBtnAction(); 
            applyAction();
            resetCachePreferencesAction(); 


        }); 

    }

    private void createScene() {
        Scene scene = new Scene(pane);
        fxPanel.setScene(scene);
    }
    
   

    private void resetCachePreferencesAction() {
        resetCachePreferencesBtn.setOnAction(event -> {
            try {
                cachePrefsNode.clear();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        });

    }
    
    private void clearBtnAction() {
        clearBtn.setOnAction((event) -> {
            remoteFileCacheService.clearAllCaches();
            LocalUrlCacher.clearCache();
            refresh();
        });
    }
    private void enableCacheAction() {
        enableCache.setOnAction(event -> {
            remoteFileCacheService.setCacheEnabled(enableCache.isSelected());
            initCacheEnableValue();
        });
    }
    
    private void refreshBtnAction() {
        refreshBtn.setOnAction(event -> {
            refresh();
        });
    }
    private void removeBtnAction() {
        removeBtn.setOnAction(event -> {
            ObservableList<CacheEntry> selectedRows = table.getSelectionModel().getSelectedItems();
            selectedRows.stream().forEach(cacheEntry -> {
                try {
                    remoteFileCacheService.clearCacheByUrl(new URL(cacheEntry.getUrl()));
                } catch (MalformedURLException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            });
            refresh();
        });
    }
    
    private void applyAction() {
        applyBtn.setOnAction(event -> {
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
            
        });
        

    }

    
    private void createCacheEntriesList() {
        List<CacheStatus> cacheStatusList = remoteFileCacheService.getCacheEntries();
        for (CacheStatus cacheStatus : cacheStatusList) {
            String url, lastModified, cacheUpdate, lastAccessed, cacheSize;
            try {
                url = cacheStatus.getUrl();
            } catch (Exception ex) {
                url = "";
            }
            try {
                lastModified = new Date(cacheStatus.getLastModified()).toString();
            } catch (Exception ex) {
                lastModified = new Date().toString();
            }
            try {
                cacheUpdate = new Date(cacheStatus.getCacheLastUpdate()).toString();
            } catch (Exception ex) {
                cacheUpdate = new Date().toString();
            }
            try {
                lastAccessed = remoteFileCacheService.getLastRequestDate(new URL(cacheStatus.getUrl())).toString();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                lastAccessed = new Date().toString();
            }
            try {
                BigInteger size = cacheStatus.getSize();
                if (size.compareTo(BigInteger.ZERO) <= 0) {
                    cacheSize = "<1";
                } else {
                    cacheSize = cacheStatus.getSize().toString();
                }
            } catch (Exception ex) {
                cacheSize = "0";
            }
            CacheEntry cacheEntry = new CacheEntry(url,lastModified,cacheUpdate,lastAccessed,cacheSize); 
            cacheEntries.add(cacheEntry); 
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
        createCacheEntriesList();
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

   
}
