package com.affymetrix.igb.view;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import com.affymetrix.igb.glyph.GraphVisibleBoundsSetter;

public class SimpleGraphTab extends JPanel  {
  public SimpleGraphTab() {
    Box overbox = Box.createHorizontalBox();
    this.setLayout(new BorderLayout());
    Box stylebox = Box.createVerticalBox();
    JRadioButton mmavgB = new JRadioButton("Min/Max/Avg");
    //    JRadioButton mmavgB = new JRadioButton("Standard");
    //    JRadioButton mmavgB = new JRadioButton("Normal");
    JRadioButton lineB = new JRadioButton("Line");
    JRadioButton barB = new JRadioButton("Bar");
    JRadioButton dotB = new JRadioButton("Dot");
    JRadioButton sstepB = new JRadioButton("Stairstep");
    JRadioButton hmapB = new JRadioButton("Heat Map");
    stylebox.add(barB);
    stylebox.add(dotB);
    stylebox.add(hmapB);
    stylebox.add(lineB);
    stylebox.add(mmavgB);
    stylebox.add(sstepB);

    ButtonGroup stylegroup = new ButtonGroup();
    stylegroup.add(mmavgB);
    stylegroup.add(lineB);
    stylegroup.add(barB);
    stylegroup.add(dotB);
    stylegroup.add(sstepB);
    stylegroup.add(hmapB);
    stylebox.setBorder(new TitledBorder("Style"));


    JSlider height_slider = new JSlider(JSlider.VERTICAL);
    height_slider.setMinimumSize(new Dimension(100, 100));
    JPanel heightP = new JPanel();
    heightP.setMinimumSize(new Dimension(100,100));
    heightP.setBorder(new TitledBorder("Height"));
    //    height_slider.setBorder(new TitledBorder("Height"));
    heightP.setLayout(new BorderLayout());
    heightP.add(height_slider, "West");

    Box scalebox = Box.createVerticalBox();
    //    scalebox.setBorder(new TitledBorder("Graph Scaling"));
    scalebox.setBorder(new TitledBorder("Y-axis Scale"));
    scalebox.add(new GraphVisibleBoundsSetter(null));
    JSlider height_sliderH = new JSlider(JSlider.HORIZONTAL);
    height_sliderH.setBorder(new TitledBorder("Height"));
    scalebox.add(height_sliderH);

    JButton selectB = new JButton("Select All Graphs");
    JButton resetB = new JButton("Reset Appearance");
    JButton advB = new JButton("Advanced...");
    Box butbox = Box.createHorizontalBox();
    butbox.add(selectB);
    butbox.add(resetB);
    butbox.add(advB);

    this.add(stylebox, "West");
    this.add(scalebox, "Center");
    //    this.add(heightP, "East");
    this.add(butbox, "South");
    //    this.add(height_slider, "East");


  }

  public static void main(String[] args) {
    SimpleGraphTab graph_tab = new SimpleGraphTab();
    JFrame fr = new JFrame();
    Container cpan = fr.getContentPane();
    cpan.add(graph_tab);
    fr.pack();
    fr.show();
  }
}
