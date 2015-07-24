/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.common.CommonUtils;
import com.google.common.base.Strings;
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
@Component(name = StatusBar.COMPONENT_NAME, immediate = true, provide = StatusBar.class)
public class StatusBar extends JPanel {

    public static final String COMPONENT_NAME = "StatusBar";
    private final JProgressBar progressBar;
    private final JLabel statusMessage;
    private final JLabel messageIcon;
    private static StatusBar statusBar;

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

    public StatusBar() {
        statusMessage = new JLabel();
        statusMessage.setHorizontalAlignment(SwingConstants.LEFT);
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setMaximumSize(new Dimension(150, 16));
        messageIcon = new JLabel();
        statusBar = this;
        setBorder(new BevelBorder(BevelBorder.LOWERED));
        setPreferredSize(new Dimension(Integer.MAX_VALUE, 16));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        statusBar.add(messageIcon);
        statusBar.add(progressBar);
        statusBar.add(statusMessage);

        disableAllComponents();
    }

    public void setProgressStatus(ICONS icon, String message) {
        statusMessage.setText(message);
        messageIcon.setIcon(icon.getIcon());
        enableAllComponents();
    }

    public void clearStatusBar() {
        disableAllComponents();
    }

    public void setMessage(String message) {
        setMessage(ICONS.NO_ICON, message);
    }

    public void setMessage(ICONS icon, String message) {
        messageIcon.setIcon(icon.getIcon());
        statusMessage.setText(message);
        enableMessageComponents();
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
