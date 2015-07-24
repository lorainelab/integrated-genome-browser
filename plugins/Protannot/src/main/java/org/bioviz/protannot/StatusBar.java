/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot;

import com.affymetrix.common.CommonUtils;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

/**
 *
 * @author Tarun
 */
public class StatusBar extends JPanel {
    
    private final JProgressBar progressBar;
    private final JLabel statusMessage;
    private final JLabel messageIcon;
    EventBus eventBus;
    private String id;
    
    public enum ICONS {
        
        ERROR("16x16/actions/stop_hex.gif"), WARNING("16x16/actions/warning.png"), INFO("16x16/actions/info.png"), NO_ICON("");
        
        private ImageIcon icon;
        
        public ImageIcon getIcon() {
            return icon;
        }
        
        ICONS(String iconLocation) {
            if (!Strings.isNullOrEmpty(iconLocation)) {
                icon = CommonUtils.getInstance().getIcon(iconLocation);
            } else {
                icon = null;
            }
        }
        
    }
    
    public StatusBar(String id) {
        this.id = id;
        statusMessage = new JLabel();
        statusMessage.setHorizontalAlignment(SwingConstants.LEFT);
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setMaximumSize(new Dimension(150, 16));
        messageIcon = new JLabel();
        setBorder(new BevelBorder(BevelBorder.LOWERED));
        setPreferredSize(new Dimension(Integer.MAX_VALUE, 16));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
        add(messageIcon);
        add(progressBar);
        add(statusMessage);
        
        disableAllComponents();
        eventBus = ProtAnnotEventService.getModuleEventBus();
        eventBus.register(this);
    }
    
    @Subscribe
    public void setStatusEventHandler(StatusSetEvent e) {
        if (id.equals(e.getId())) {
            statusMessage.setText(e.getStatusMessage());
            messageIcon.setIcon(e.getMessageIcon().getIcon());
            if (e.isProgressBarRequired()) {
                enableAllComponents();
            } else {
                enableMessageComponents();
            }
        }
    }
    
    @Subscribe
    public void clearStatusEventHandler(StatusClearEvent e) {
        if (id.equals(e.getId())) {
            disableAllComponents();
        }
    }
    
    private void disableAllComponents() {
        messageIcon.setVisible(false);
        statusMessage.setVisible(false);
        progressBar.setVisible(false);
    }
    
    private void enableAllComponents() {
        messageIcon.setVisible(true);
        statusMessage.setVisible(true);
        progressBar.setVisible(true);
    }
    
    private void enableMessageComponents() {
        enableAllComponents();
        progressBar.setVisible(false);
    }
    
}
