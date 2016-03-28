/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.cache.configuration.panel;

import aQute.bnd.annotation.component.Activate;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.igb.swing.JRPJPanel;
import java.util.prefs.Preferences;

import javafx.scene.Scene; 
import javafx.scene.control.ScrollPane; 
import javafx.embed.swing.JFXPanel; 

import javafx.scene.Group;
import javafx.scene.paint.Color;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.lorainelab.igb.cache.api.RemoteFileCacheService;
import org.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class
 *
 * @author dfreese
 */

//@aQute.bnd.annotation.component.Component(name = CacheConfigurationPanel.COMPONENT_NAME, immediate = true, provide = PreferencesPanelProvider.class)
public class CacheConfigurationPanelJavaFxController extends JRPJPanel implements PreferencesPanelProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CacheConfigurationPanel.class);

    public static final String COMPONENT_NAME = "CacheConfigurationPanel";
    private static final String TAB_NAME = "Cache";
    private static final int TAB_POSITION = 10;
    private RemoteFileCacheService remoteFileCacheService;
    private CacheTableModel cacheTableModel;
    private final Preferences cachePrefsNode;
    
    final JFXPanel fxCacheDataPanel = new JFXPanel(); 
    final JFXPanel fxCachePanel = new JFXPanel(); 
    
    String[] columnNames = {"Source",
        "Last Modified",
        "Cached On",
        "Size",
        "Action"};
    
    public CacheConfigurationPanelJavaFxController() {
        super(COMPONENT_NAME); 
        setLayout(new MigLayout(new LC().fill().insetsAll("0")));
        cachePrefsNode = PreferenceUtils.getCachePrefsNode();
        
        
    }    
    
    @Override
    public int getWeight() {
        return TAB_POSITION; 
    }
    
    @Override
    public String getName() {
        return TAB_NAME;
    }


    @Override
    public JRPJPanel getPanel() {
        return this; 
    }

    @Override
    public void refresh() {
        cacheTableModel.refresh();
        cacheTableModel.fireTableDataChanged();
//        initMaxCacheSizeValue(); 
//        initMinFileSizeValue();
//        initCacheEnableValue();
//        initCacheSizeValue();
    }
    
    @Activate
    public void activate(){
        fxCachePanel.setScene(initializeCacheSettingsScene()); 
        initializeLayout(); 
        
    }
    
    public void initializeLayout(){
        add(fxCacheDataPanel);
        add(fxCachePanel); 
    }
    
    public Scene initializeCacheSettingsScene(){
        Group root = new Group(); 
        ScrollPane scrollPane = new ScrollPane(); 
        Scene scene = new Scene(root); 
        
        root.getChildren().add(scrollPane); 
        
        
        return scene; 
    }
    
    

 
    
}
