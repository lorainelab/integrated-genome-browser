/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.  
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.igb.prefs;

import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.view.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.*;
import javax.swing.*;

public class PluginEditor extends JPanel {
  
  public static final String TITLE = "Plugin Editor";
  
  JLabel class_label = new JLabel("Class:", JLabel.TRAILING);
  JTextField class_field = new JTextField(30);

  JLabel name_label = new JLabel("Name:", JLabel.TRAILING);
  JTextField name_field = new JTextField(30);
  
  JCheckBox enabled_box = new JCheckBox("Enabled");
  
  //String[] placements = new String[] {PluginInfo.PLACEMENT_TAB, PluginInfo.PLACEMENT_WINDOW};
  //JComboBox placement_chooser = new JComboBox(placements);

  JButton submit_button;
  JButton cancel_button;
  
  JDialog dialog = null;
  
  Preferences original_node = null;
  
  public PluginEditor(Component the_parent) {
    
    name_label.setLabelFor(name_field);
    class_label.setLabelFor(class_field);

    Box line1 = new Box(BoxLayout.X_AXIS);
    line1.add(class_label);
    line1.add(Box.createHorizontalStrut(5));
    line1.add(class_field);

    Box line2 = new Box(BoxLayout.X_AXIS);
    line2.add(name_label);
    line2.add(Box.createHorizontalStrut(5));
    line2.add(name_field);
    
    Box line3 = new Box(BoxLayout.X_AXIS);
    enabled_box.setHorizontalTextPosition(JCheckBox.LEADING);
    //line3.add(placement_chooser);
    //line3.add(Box.createHorizontalStrut(5));
    line3.add(enabled_box);
    line3.add(Box.createHorizontalGlue());
    
    Action cancel_action = new AbstractAction("Cancel") {
      public void actionPerformed(ActionEvent e) {
        dialog.hide();
      }
    };
    cancel_action.setEnabled(true);

    Action submit_action = new AbstractAction("OK") {
      public void actionPerformed(ActionEvent e) {
        boolean success = PluginEditor.this.applyChanges();
        if (success) {
          dialog.hide();
        }
      }
    };
    submit_action.setEnabled(true);
    
    cancel_button = new JButton(cancel_action);
    submit_button = new JButton(submit_action);
    
    Box top_box = new Box(BoxLayout.Y_AXIS);
    top_box.add(line1);
    top_box.add(line2);
    top_box.add(line3);

    Box line4 = new Box(BoxLayout.X_AXIS);
    line4.add(Box.createHorizontalGlue());
    line4.add(submit_button);
    line4.add(Box.createHorizontalStrut(5));
    line4.add(cancel_button);
    line4.add(Box.createHorizontalGlue());
    
    JPanel main_box = (JPanel) this;
    main_box.setLayout(new BorderLayout());
    main_box.add(top_box, BorderLayout.NORTH);
    main_box.add(line4, BorderLayout.SOUTH);
    
    createDialog(the_parent);
  }
  
  void setNode(Preferences node) {
    this.original_node = node;
    if (node == null) {
      class_field.setText("");
      class_field.setEnabled(true);
      name_field.setText("");
      name_field.setEnabled(true);
      enabled_box.setSelected(true);
      //placement_chooser.setSelectedItem(PluginInfo.PLACEMENT_TAB);
    } else {
      String cl = node.get(PluginInfo.KEY_CLASS, null);
      if (cl == null) {
        ErrorHandler.errorPanel("ERROR", "Empty class in plugin preferences!");
      }
      class_field.setText(cl);
      class_field.setEnabled(false);
      name_field.setText(node.name());
      name_field.setEnabled(false);
      enabled_box.setSelected(node.getBoolean(PluginInfo.KEY_LOAD, true));
      //placement_chooser.setSelectedItem(node.get(PluginInfo.KEY_PLACEMENT, PluginInfo.PLACEMENT_TAB));
    }
  }

  JDialog createDialog(Component parent) {
    JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, parent);
    dialog = new JDialog(frame, TITLE, true);
    dialog.getContentPane().add(this);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    return dialog;
  }
  
  public void showDialog(Preferences node) {
    setNode(node);
    dialog.show();
  }
  
  /** Tries to create or modify the Preferences node based on the user's input.
   *  @return true for sucess, false otherwise.
   */
  boolean applyChanges() {
    boolean ok = false;
    String class_string = class_field.getText();
    String name_string = name_field.getText();
    if (name_string == null || name_string.trim().equals("") || name_string.indexOf('/') >= 0 || name_string.length() > Preferences.MAX_NAME_LENGTH) {
      ErrorHandler.errorPanel("Invalid name", "You must supply a valid name for the plugin");
    }
    else if (this.original_node == null) {
      System.out.println("Adding a new plugin with name: "+name_string);
      try {
        Object plugin = PluginInfo.instantiatePlugin(class_string);
        if (plugin != null) {
          Preferences node = PluginInfo.getNodeForName(name_string);
          node.put(PluginInfo.KEY_CLASS, class_string);
          node.putBoolean(PluginInfo.KEY_LOAD, this.enabled_box.isSelected());
          //node.put(PluginInfo.KEY_PLACEMENT, (String) this.placement_chooser.getSelectedItem());
          ok = true;
        }
      } catch (Exception e) {
        ErrorHandler.errorPanel("ERROR", "Could not apply changes", this, e);
      }
    } else {
      //this.original_node.put(PluginInfo.KEY_CLASS, name_string);
      this.original_node.putBoolean(PluginInfo.KEY_LOAD, this.enabled_box.isSelected());
      //this.original_node.put(PluginInfo.KEY_PLACEMENT, (String) this.placement_chooser.getSelectedItem());
      ok = true;
    }
    return ok;
  }
}
