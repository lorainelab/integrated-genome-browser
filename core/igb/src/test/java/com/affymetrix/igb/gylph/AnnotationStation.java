/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.gylph;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.MapGlyphFactory;
import com.affymetrix.genoviz.bioviews.SceneI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.glyph.EfficientLabelledLineGlyph;
import com.affymetrix.genoviz.glyph.EfficientPointedGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import static com.affymetrix.genoviz.util.NeoConstants.NORTH;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.Shadow;
import com.affymetrix.genoviz.widget.VisibleRange;
import com.affymetrix.igb.glyph.CharSeqGlyph;
import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.CoordinateStyle;
import com.affymetrix.igb.tiers.CustomLabelledTierMap;
import com.affymetrix.igb.tiers.TrackStyle;
import static com.affymetrix.igb.view.SeqMapView.axisFont;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;
import com.affymetrix.igb.view.factories.TransformTierGlyph;
import com.google.common.math.IntMath;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.RoundingMode;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author dcnorris
 */
public class AnnotationStation extends javax.swing.JFrame {

    private final VisibleRange zoomPoint = new VisibleRange();
    private static final Font max_zoom_font = NeoConstants.default_bold_font.deriveFont(30.0f);
    private CustomLabelledTierMap tieredMap;
    private boolean stretchXTofit, stretchYToFit = false;
    private DefaultTierGlyph annotationTierGlyph;
    private static final int DEFAULT_ANNOTATION_TRACK_HEIGHT = 200;

    public AnnotationStation() {
        setTitle("Annotation Station 2.0");
        tieredMap = createAffyTieredMap();
        setUpCoordinateTier();
        addAnnotationTier();
        initComponents();
        setupZoomStripe();
    }

    private void setupZoomStripe() {
        NeoRangeListener zoomAdjuster = new NeoRangeListener() {
            @Override
            public void rangeChanged(NeoRangeEvent e) {
                double midPoint = (e.getVisibleEnd() + e.getVisibleStart()) / 2.0f;
                tieredMap.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD, midPoint);
                tieredMap.updateWidget();
            }
        };

        this.zoomPoint.addListener(zoomAdjuster);

        Shadow hairline = new Shadow(tieredMap);

        hairline.setSelectable(false);

        MouseListener zoomPointAdjuster = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                double focus = ((NeoMouseEvent) e).getCoordX();
                zoomPoint.setSpot(focus);
            }

        };

        tieredMap.addMouseListener(zoomPointAdjuster);
        this.zoomPoint.addListener(hairline);
    }

    private CustomLabelledTierMap createAffyTieredMap() {
        CustomLabelledTierMap customLabelMap = new CustomLabelledTierMap(true, true);
        customLabelMap.enableDragScrolling(true);
        customLabelMap.getLabelMap().enableMouseWheelAction(false);

        customLabelMap.setMaxZoomToFont(max_zoom_font);
        customLabelMap.setMapColor(Color.WHITE);
        customLabelMap.setScrollIncrementBehavior(AffyTieredMap.X, AffyTieredMap.AUTO_SCROLL_HALF_PAGE);
        customLabelMap.setReshapeBehavior(NeoAbstractWidget.X, NeoConstants.NONE);
        customLabelMap.setReshapeBehavior(NeoAbstractWidget.Y, NeoConstants.NONE);

        customLabelMap.setExpansionBehavior(NeoMap.X, NeoMap.EXPAND);
        // make sure map expands to encompass all glyph y coords
        customLabelMap.setExpansionBehavior(NeoMap.Y, NeoMap.EXPAND);
        customLabelMap.setSelectionAppearance(SceneI.SELECT_OUTLINE);
        customLabelMap.setMapRange(0, 500);
        customLabelMap.setMapOffset(0, 100);
        NeoMap lableMap = customLabelMap.getLabelMap();
        lableMap.setSelectionAppearance(SceneI.SELECT_OUTLINE);
        lableMap.setReshapeBehavior(NeoAbstractWidget.Y, NeoConstants.NONE);
        lableMap.setMaxZoomToFont(max_zoom_font);

        customLabelMap.stretchToFit(true, true);
        customLabelMap.updateWidget();
        return customLabelMap;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        startFieldLabel = new javax.swing.JLabel();
        stopFieldLabel = new javax.swing.JLabel();
        stopTextField = new javax.swing.JTextField();
        startTextField = new javax.swing.JTextField();
        String[] glyphtypes = {
            "com.affymetrix.genoviz.glyph.FillRectGlyph",
            "com.affymetrix.genoviz.glyph.OutlineRectGlyph",
            "com.affymetrix.genoviz.glyph.FillOvalGlyph",
            "com.affymetrix.genoviz.glyph.ArrowGlyph",
            "com.affymetrix.genoviz.glyph.LabelGlyph",
            "com.affymetrix.genoviz.glyph.PointedGlyph",
            "com.affymetrix.genoviz.glyph.FloaterGlyph",
            "com.affymetrix.genoviz.glyph.FlyweightPointGlyph",
            "com.affymetrix.genoviz.glyph.InsertionSeqGlyph",
            "com.affymetrix.genoviz.glyph.SquiggleGlyph",
            "com.affymetrix.genoviz.glyph.AlignedProteinGlyph",
            "com.affymetrix.genoviz.glyph.EfficientSolidGlyph",
            "com.affymetrix.genoviz.glyph.EfficientPointedGlyph",
            "com.affymetrix.genoviz.glyph.RoundRectGlyph",
            "com.affymetrix.genoviz.glyph.AlignedResiduesGlyph",
            "com.affymetrix.genoviz.glyph.GapGlyph",
            "com.affymetrix.genoviz.glyph.DirectedGlyph",
            "com.affymetrix.genoviz.glyph.PixelFloaterGlyph",
            "com.affymetrix.genoviz.glyph.BridgeGlyph",
            "com.affymetrix.genoviz.glyph.CenteredCircleGlyph",
            "com.affymetrix.genoviz.glyph.TriBarGlyph",
            "com.affymetrix.genoviz.glyph.CoordFloaterGlyph",
            "com.affymetrix.genoviz.glyph.BasicImageGlyph",
            "com.affymetrix.genoviz.glyph.ThreshGlyph",
            "com.affymetrix.genoviz.glyph.ColorSepGlyph",
            "com.affymetrix.genoviz.glyph.AlignmentGlyph",
            "com.affymetrix.genoviz.glyph.FillOvalGlyph",
            "com.affymetrix.genoviz.glyph.EfficientLabelledGlyph",
            "com.affymetrix.genoviz.glyph.EfficientOutlineContGlyph",
            "com.affymetrix.genoviz.glyph.FlyPointLinkerGlyph",
            "com.affymetrix.genoviz.glyph.GlyphStyle",
            "com.affymetrix.genoviz.glyph.EfficientLineContGlyph",
            "com.affymetrix.genoviz.glyph.AxisGlyph",
            "com.affymetrix.genoviz.glyph.OutlineRectGlyph",
            "com.affymetrix.genoviz.glyph.RoundRectMaskGlyph",
            "com.affymetrix.genoviz.glyph.StretchContainerGlyph",
            "com.affymetrix.genoviz.glyph.EfficientOutlinedRectGlyph",
            "com.affymetrix.genoviz.glyph.LabelledRectGlyph",
            "com.affymetrix.genoviz.glyph.LineContainerGlyph",
            "com.affymetrix.genoviz.glyph.BasicGraphGlyph",
            "com.affymetrix.genoviz.glyph.InvisibleBoxGlyph",
            "com.affymetrix.genoviz.glyph.TransientGlyph",
            "com.affymetrix.genoviz.glyph.SequenceGlyph",
            "com.affymetrix.genoviz.glyph.BoundedPointGlyph",
            "com.affymetrix.genoviz.glyph.TriangleGlyph",
            "com.affymetrix.genoviz.glyph.SolidGlyph",
            "com.affymetrix.genoviz.glyph.PointedOutlinedGlyph",
            "com.affymetrix.genoviz.glyph.GlyphStyleFactory",
            "com.affymetrix.genoviz.glyph.OutlinedPointedGlyph",
            "com.affymetrix.genoviz.glyph.EfficientPaintRectGlyph",
            "com.affymetrix.genoviz.glyph.EfficientLabelledLineGlyph",
            "com.affymetrix.genoviz.glyph.AlignedDNAGlyph",
            "com.affymetrix.genoviz.glyph.StringGlyph",
            "com.affymetrix.genoviz.glyph.ColoredResiduesGlyph",
            "com.affymetrix.genoviz.glyph.LineStretchContainerGlyph",
            "com.affymetrix.igb.shared.DeletionGlyph",
            "com.affymetrix.igb.shared.AlignedResidueGlyph",
            "com.affymetrix.igb.shared.CodonGlyph",
            "com.affymetrix.igb.shared.WrappedStringGlyph",
            "com.affymetrix.igb.shared.StyledGlyph"
        };
        typeComboBox = new javax.swing.JComboBox(glyphtypes);
        addButton = new javax.swing.JButton();
        clearButton = new javax.swing.JButton();
        String[] colors = {"blue", "red", "green", "black", "yellow"};
        colorComboBox = new javax.swing.JComboBox(colors);
        jLabel1 = new javax.swing.JLabel();
        rowTextLabel = new javax.swing.JLabel();
        glyphRowTextField = new javax.swing.JTextField();
        autoLayoutCheckBox = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        neoMap2 = tieredMap;
        jSlider1 = tieredMap.getXzoomer();
        yslider = tieredMap.getYzoomer();
        jPanel2 = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        addAnnotationTier = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(1100, 720));
        setMinimumSize(new java.awt.Dimension(1100, 0));
        setPreferredSize(new Rectangle(0, 0, 1100, 720).getSize());
        setResizable(false);

        startFieldLabel.setText("Start");

        stopFieldLabel.setText("Stop");

        stopTextField.setText("255");

        startTextField.setText("245");

        addButton.setText("Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        clearButton.setText("Clear Map");
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Color");

        rowTextLabel.setText("Row");

        autoLayoutCheckBox.setText("AutoLayoutRow");
        autoLayoutCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoLayoutCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(startFieldLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(startTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(autoLayoutCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rowTextLabel)
                    .addComponent(stopFieldLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(glyphRowTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stopTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(113, 113, 113)
                .addComponent(typeComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(addButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(colorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(clearButton)
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {startFieldLabel, stopFieldLabel});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {startTextField, stopTextField});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1)
                    .addComponent(colorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearButton)
                    .addComponent(typeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addButton)
                    .addComponent(stopTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stopFieldLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(startTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(startFieldLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(rowTextLabel)
                    .addComponent(glyphRowTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(autoLayoutCheckBox))
                .addContainerGap(68, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {startFieldLabel, stopFieldLabel});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {startTextField, stopTextField});

        jPanel3.setLayout(new java.awt.BorderLayout());
        jPanel3.add(neoMap2, java.awt.BorderLayout.CENTER);

        jSlider1.setMaximum(1000);
        jSlider1.setValue(0);
        jPanel3.add(jSlider1, java.awt.BorderLayout.PAGE_START);

        yslider.setOrientation(javax.swing.JSlider.VERTICAL);
        yslider.setValue(0);
        jPanel3.add(yslider, java.awt.BorderLayout.LINE_START);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jCheckBox1.setText("stretchXToFit");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });
        jPanel2.add(jCheckBox1, new java.awt.GridBagConstraints());

        jCheckBox2.setText("stretchYToFit");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });
        jPanel2.add(jCheckBox2, new java.awt.GridBagConstraints());

        jPanel3.add(jPanel2, java.awt.BorderLayout.PAGE_END);

        fileMenu.setText("File");

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        jMenuBar1.add(fileMenu);

        editMenu.setText("edit");

        addAnnotationTier.setText("addAnnotationTierGlyph");
        addAnnotationTier.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAnnotationTierActionPerformed(evt);
            }
        });
        editMenu.add(addAnnotationTier);

        jMenuBar1.add(editMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        resetMap();
    }//GEN-LAST:event_clearButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        // TODO add your handling code here:
        String glyph_name = (String) typeComboBox.getSelectedItem();
        String color_name = (String) colorComboBox.getSelectedItem();
        Color col = NeoMap.getColor(color_name);
        MapGlyphFactory fac = tieredMap.getFactory();
        try {
            int start = Integer.parseInt(startTextField.getText());
            int end = Integer.parseInt(stopTextField.getText());
            int height = -1;

            if (!autoLayoutCheckBox.isSelected()) {
                height = Integer.parseInt(glyphRowTextField.getText());
            }
            Class glyph_class = Class.forName(glyph_name);
            fac.setGlyphtype(glyph_class);
            fac.setBackgroundColor(col);
            fac.setForegroundColor(col);
            addItemToAnnotationTrack(start, end, glyph_class);
        } catch (ClassNotFoundException ex) {
            System.err.println("could not find class: " + glyph_name);
        } catch (NumberFormatException ex) {
            System.err.println("could not parse numeric text field");
        }
    }//GEN-LAST:event_addButtonActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        // TODO add your handling code here:
        stretchYToFit = jCheckBox2.isSelected();
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        // TODO add your handling code here:
        stretchXTofit = jCheckBox1.isSelected();
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void addAnnotationTierActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAnnotationTierActionPerformed
        // TODO add your handling code here:
        //add annotation tier
    }//GEN-LAST:event_addAnnotationTierActionPerformed

    private void autoLayoutCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoLayoutCheckBoxActionPerformed
        glyphRowTextField.setEnabled(!autoLayoutCheckBox.isSelected());
    }//GEN-LAST:event_autoLayoutCheckBoxActionPerformed

    private void addItemToAnnotationTrack(int start, int end, Class glyph_class) {
        addPairedEndStyleGlyphSet(start, end);
        tieredMap.stretchToFit(stretchXTofit, stretchYToFit);
        tieredMap.updateWidget();
    }

    private void resetMap() {
        tieredMap.clearWidget();
        addAnnotationTier();
        setUpCoordinateTier();
        setupZoomStripe();
        tieredMap.stretchToFit(true, true);
        tieredMap.updateWidget();

    }

    public static void main(String args[]) {
        final AnnotationStation a = new AnnotationStation();
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                a.pack();
                a.setVisible(true);
            }
        });
        //hack to fix xslider position
        Runnable r = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    //do nothing
                }
                a.resetMap();
            }
        };
        new Thread(r).start();

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem addAnnotationTier;
    private javax.swing.JButton addButton;
    private javax.swing.JCheckBox autoLayoutCheckBox;
    private javax.swing.JButton clearButton;
    private javax.swing.JComboBox colorComboBox;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JTextField glyphRowTextField;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JSlider jSlider1;
    private com.affymetrix.genoviz.widget.NeoMap neoMap2;
    private javax.swing.JLabel rowTextLabel;
    private javax.swing.JLabel startFieldLabel;
    private javax.swing.JTextField startTextField;
    private javax.swing.JLabel stopFieldLabel;
    private javax.swing.JTextField stopTextField;
    private javax.swing.JComboBox typeComboBox;
    private javax.swing.JSlider yslider;
    // End of variables declaration//GEN-END:variables

    private static void print(int s) {
        System.out.println(s);
    }

    private static void print(String s) {
        System.out.println(s);
    }

    private void setUpCoordinateTier() {
        TransformTierGlyph resultAxisTier = new TransformTierGlyph(CoordinateStyle.coordinate_annot_style);
        resultAxisTier.setInfo(new RootSeqSymmetry() {
            @Override
            public FileTypeCategory getCategory() {
                return FileTypeCategory.Axis;
            }

            @Override
            public void search(Set<SeqSymmetry> results, String id) {
            }

            @Override
            public void searchHints(Set<String> results, Pattern regex, int limit) {
            }

            @Override
            public void search(Set<SeqSymmetry> result, Pattern regex, int limit) {
            }

            @Override
            public void searchProperties(Set<SeqSymmetry> results, Pattern regex, int limit) {
            }
        });

        int TIER_SIZE = 54;
        int AXIS_SIZE = 27;
        resultAxisTier.setPacker(null);
        resultAxisTier.setFixedPixHeight(TIER_SIZE);
        resultAxisTier.setDirection(TierGlyph.Direction.AXIS);

        AxisGlyph axis_glyph = tieredMap.addAxis(AXIS_SIZE);
        axis_glyph.setHitable(true);
        axis_glyph.setFont(axisFont);

        axis_glyph.setBackgroundColor(resultAxisTier.getBackgroundColor());
        axis_glyph.setForegroundColor(resultAxisTier.getForegroundColor());

        resultAxisTier.addChild(axis_glyph);

        tieredMap.addTier(resultAxisTier, false);
        BioSeq bioSeq = new BioSeq("ch1", 500);
        bioSeq.setResidues(getResidues(500));
        bioSeq.setBounds(0, 500);
        CharSeqGlyph seq_glyph = CharSeqGlyph.initSeqGlyph(bioSeq, axis_glyph);
        resultAxisTier.addChild(seq_glyph);
        resultAxisTier.setCoords(0, 0, tieredMap.getScene().getCoordBox().getWidth(), TIER_SIZE);
    }

    private void addAnnotationTier() {
        annotationTierGlyph = new DefaultTierGlyph(defautltTrackStyle);
        annotationTierGlyph.setInfo(new RootSeqSymmetry() {
            @Override
            public FileTypeCategory getCategory() {
                return FileTypeCategory.Annotation;
            }

            @Override
            public void search(Set<SeqSymmetry> results, String id) {
            }

            @Override
            public void searchHints(Set<String> results, Pattern regex, int limit) {
            }

            @Override
            public void search(Set<SeqSymmetry> result, Pattern regex, int limit) {
            }

            @Override
            public void searchProperties(Set<SeqSymmetry> results, Pattern regex, int limit) {
            }
        });

        int TIER_SIZE = 54;
        int AXIS_SIZE = 27;
        annotationTierGlyph.setDirection(StyledGlyph.Direction.BOTH);
        addSomeGlyphs();

        annotationTierGlyph.setCoords(0, 0, tieredMap.getScene().getCoordBox().getWidth(), DEFAULT_ANNOTATION_TRACK_HEIGHT);

        tieredMap.addTier(annotationTierGlyph, true);

    }

    private void addSomeGlyphs() {
        addPairedEndStyleGlyphSet(100, 200);
        addPairedEndStyleGlyphSet(100, 250);
        addPairedEndStyleGlyphSet(50, 90);
        addPairedEndStyleGlyphSet(100, 150);
    }

    private void addPairedEndStyleGlyphSet(int start, int end) {
        addPairedEndStyleGlyphSet(start, end, -1);
    }

    //assumes forwardness for the moment
    private void addPairedEndStyleGlyphSet(int start, int end, int offsetRows) {
        if (offsetRows == -1) {
            offsetRows = getOffset(start, end);
        }

        int width = end - start;
        int quarterWidth = IntMath.divide(width, 4, RoundingMode.UP);
        int halfWidth = IntMath.divide(width, 2, RoundingMode.UP);
        tieredMap.getFactory().setGlyphtype(EfficientLabelledLineGlyph.class);
        EfficientLabelledLineGlyph labelGlyph = (EfficientLabelledLineGlyph) tieredMap.getFactory().makeGlyph(start, end);
        labelGlyph.setLabel("Label");
        labelGlyph.setLabelLocation(NORTH);
        offsetRows = 25 + (25 * offsetRows);
        labelGlyph.setCoords(start, DEFAULT_ANNOTATION_TRACK_HEIGHT - offsetRows, width, annotationTierGlyph.getChildHeight());
        annotationTierGlyph.addChild(labelGlyph);

        tieredMap.getFactory().setGlyphtype(com.affymetrix.genoviz.glyph.EfficientPointedGlyph.class);
        EfficientPointedGlyph point = (EfficientPointedGlyph) tieredMap.getFactory().makeGlyph(start, start + quarterWidth);
        point.setForward(true);
        labelGlyph.addChild(point);

        tieredMap.getFactory().setGlyphtype(com.affymetrix.genoviz.glyph.EfficientLineContGlyph.class);
        GlyphI glyph = tieredMap.getFactory().makeGlyph(start + quarterWidth, start + halfWidth + quarterWidth + 1);
        labelGlyph.addChild(glyph);

        tieredMap.getFactory().setGlyphtype(com.affymetrix.genoviz.glyph.EfficientPointedGlyph.class);

        EfficientPointedGlyph efPGlyph = (EfficientPointedGlyph) tieredMap.getFactory().makeGlyph(start + halfWidth + quarterWidth, end);
        efPGlyph.setForward(false);
        labelGlyph.addChild(efPGlyph);

    }

    private int getOffset(int start, int end) {
        int offset = 0;
        SeqSpan span = new SimpleSeqSpan(start, end, null);
        if (annotationTierGlyph.getChildren() != null) {
            for (GlyphI glyph : annotationTierGlyph.getChildren()) {
                int glyphStart = (int) glyph.getCoordBox().getX();
                SeqSpan glyphSpan = new SimpleSeqSpan(glyphStart, glyphStart + (int) glyph.getCoordBox().getWidth(), null);
                if (SeqUtils.overlap(span, glyphSpan)) {
                    offset++;
                }
            }
        }
        return offset;
    }

    private String getResidues(int size) {
        String residueString = "ACAAGATGCCATTGTCCCCCGGCCTCCTGCTGCTGCTGCTCTCCGGGGC"
                + "CACGGCCACCGCTGCCCTGCCCCTGGAGGGTGGCCCCACCGGCCGAG"
                + "ACAGCGAGCATATGCAGGAAGCGGCAGGAATAAGGAAAAGCAGCCTCC"
                + "TGACTTTCCTCGCTTGGTGGTTTGAGTGGACCTCCCAGGCCAGTGCCG"
                + "GGCCCCTCATAGGAGAGGAAGCTCGGGAGGTGGCCAGGCGGCAGGAAG"
                + "GCGCACCCCCCCAGCAATCCGCGCGCCGGGACAGAATGCCCTGCAGGA"
                + "ACTTCTTCTGGAAGACCTTCTCCTCCTGCAAATAAAACCTCACCCATG"
                + "AATGCTCACGCAAGTTTAATTACAGACCTGAATTTCCTCGCTTGGTGG"
                + "TTTGAGTGGACCTCCCAGGCCAGTGCCGGGCCCCTCATAGGAGAGGAA"
                + "GCTCGGGAGGTGGCCAGGCGGCAGGAAGGCGCACCCTTTGAGTGGACC"
                + "TCCCAGGCCAGTGCTGAGTG";
        return residueString.substring(0, Math.min(residueString.length(), size));
    }

    static final TrackStyle defautltTrackStyle = new TrackStyle() {
        private Color foreground, background;

        { // a non-static initializer block
            setTrackName("Annotation Track");
            foreground = Color.black;
            background = Color.white;
        }

        @Override
        public boolean getSeparate() {
            return false;
        }

        @Override
        public boolean getCollapsed() {
            return false;
        }

        @Override
        public boolean getExpandable() {
            return false;
        }

        @Override
        public void setForeground(Color c) {
            foreground = c;
        }

        @Override
        public Color getForeground() {
            return foreground;
        }

        @Override
        public void setBackground(Color c) {
            background = c;
        }

        @Override
        public Color getBackground() {
            return background;
        }

        @Override
        public Color getLabelForeground() {
            return getForeground();
        }

        @Override
        public Color getLabelBackground() {
            return getBackground();
        }

        @Override
        public void restoreToDefault() {
            foreground = Color.black;
            background = Color.white;
        }
    };
}
