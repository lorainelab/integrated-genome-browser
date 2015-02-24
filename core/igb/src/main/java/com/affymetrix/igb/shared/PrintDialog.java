/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.shared;

import com.affymetrix.genometry.util.DisplayUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ComponentPagePrinter;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.shared.ExportDialog.FONT_SIZE;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 *
 * @author tkanapar
 */
public class PrintDialog {

    private static PrintDialog singleton;
    static final String TITLE = "Print Image";

    /**
     * @return PrintDialog instance
     */
    public static PrintDialog getSingleton() {
        if (singleton == null) {
            singleton = new PrintDialog();
        }
        
        return singleton;
    }
    private JFrame static_frame = null;
    protected BufferedImage exportImage;

    private AffyTieredMap seqMap;
    private AffyTieredMap svseqMap;

    private Component wholeFrame;
    private Component mainView;
    private Component mainViewWithLabels;
    private Component component; // Export component

    ButtonGroup buttonGroup = new ButtonGroup();
    JLabel previewLabel = new JLabel();
    JButton printButton = new JButton();
    JButton cancelButton = new JButton();
    JPanel buttonsPanel = new JPanel();

    JRadioButton mvRadioButton = new JRadioButton();
    JRadioButton mvlRadioButton = new JRadioButton();
    JRadioButton wholeFrameRB = new JRadioButton();

    public synchronized void display() {
        initRadioButton();
        initFrame();
        DisplayUtils.bringFrameToFront(static_frame);
        previewImage();
    }

    public void setComponent(Component c) {
        component = c;
    }

    public Component getComponent() {
        return component;
    }

    private void initRadioButton() {

        initView();

        if (mvRadioButton.isSelected()) {
            setComponent(mainView);
        } else if (mvlRadioButton.isSelected()) {
            setComponent(mainViewWithLabels);
        } else {
            setComponent(wholeFrame);
            wholeFrameRB.setSelected(true);
        }

        mvRadioButton.setEnabled(!seqMap.getTiers().isEmpty());
        mvlRadioButton.setEnabled(!seqMap.getTiers().isEmpty());
        buttonsPanel.setVisible(true);
    }

    private void initView() {
        if (seqMap == null) {
            seqMap = IGB.getSingleton().getMapView().getSeqMap();
            wholeFrame = IGB.getSingleton().getFrame();
            mainView = seqMap.getNeoCanvas();
            AffyLabelledTierMap tm = (AffyLabelledTierMap) seqMap;
            mainViewWithLabels = tm.getSplitPane();

        }
    }

    private void initFrame() {
        if (static_frame == null) {

            static_frame = PreferenceUtils.createFrame(TITLE, new PrintDialogGUI(this));
            static_frame.setLocationRelativeTo(IGB.getSingleton().getFrame());
            static_frame.getRootPane().setDefaultButton(printButton);
            static_frame.setResizable(false);
        }
    }

    private void drawTitleBar(Graphics2D g) {
        // Draw Background
        g.setColor(component.getBackground().darker());
        g.fillRect(0, 0, component.getWidth(), component.getHeight());

        // Draw Border
        g.setColor(Color.BLACK);
        g.fillRect(0, 20, component.getWidth(), 2);

        // Draw Title
        g.setFont(g.getFont().deriveFont(FONT_SIZE));
        int x_offset = (component.getWidth() - g.getFontMetrics().stringWidth(((JFrame) component).getTitle())) / 2;
        int y_offset = 14;
        g.drawString(((JFrame) component).getTitle(), x_offset, y_offset);
    }

    private void previewImage() {
        exportImage = GraphicsUtil.getDeviceCompatibleImage(
                component.getWidth(), component.getHeight());
        Graphics2D g = exportImage.createGraphics();
        if (component instanceof JFrame) {
            drawTitleBar(g);
        }
        component.printAll(g);

        Image previewImage = GraphicsUtil.resizeImage(exportImage,
                previewLabel.getWidth(), previewLabel.getHeight());

        previewLabel.setIcon(new ImageIcon(previewImage));
    }

    public void mvRadioButtonActionPerformed() {
        component = mainView;
        previewImage();
    }

    public void mvlRadioButtonActionPerformed() {
        component = mainViewWithLabels;
        previewImage();
    }

    public void wholeFrameRBActionPerformed() {
        component = wholeFrame;
        previewImage();
    }

    public void printButtonActionPerformed() {
        ComponentPagePrinter cprinter = new ComponentPagePrinter(this.getComponent());
        try {
            cprinter.print();
        } catch (Exception ex) {
            ErrorHandler.errorPanel("Problem trying to print.", ex);
        }
    }

    public void cancelButtonActionPerformed() {
        static_frame.setVisible(false);
        //static_frame = null;
    }

}
