/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import com.affymetrix.igb.das.DasDiscovery;
import com.affymetrix.igb.util.ErrorHandler;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.prefs.*;
import javax.swing.*;

public class DasServerInfoEditor extends JPanel {
  
  public static final String TITLE = "DAS Server Info Editor";
  
  JLabel url_label = new JLabel("URL:", JLabel.TRAILING);
  JTextField url_field = new JTextField(30);

  JLabel name_label = new JLabel("Name:", JLabel.TRAILING);
  JTextField name_field = new JTextField(30);
  
  JCheckBox enabled_box = new JCheckBox("Enabled");

  JButton submit_button;
  JButton cancel_button;
  
  JDialog dialog = null;
  
  Preferences original_node = null;
  
  public DasServerInfoEditor(Component the_parent) {
    
    name_label.setLabelFor(name_field);
    url_label.setLabelFor(url_field);

    Box line1 = new Box(BoxLayout.X_AXIS);
    line1.add(url_label);
    line1.add(Box.createHorizontalStrut(5));
    line1.add(url_field);

    Box line2 = new Box(BoxLayout.X_AXIS);
    line2.add(name_label);
    line2.add(Box.createHorizontalStrut(5));
    line2.add(name_field);
    
    Box line3 = new Box(BoxLayout.X_AXIS);
    enabled_box.setHorizontalTextPosition(JCheckBox.LEADING);
    line3.add(enabled_box);
    line3.add(Box.createHorizontalGlue());
    
    Action cancel_action = new AbstractAction("Cancel") {
      public void actionPerformed(ActionEvent e) {
        dialog.hide();
      }
    };
    cancel_action.setEnabled(true);

    Action submit_action = new AbstractAction("Apply Changes") {
      public void actionPerformed(ActionEvent e) {
        boolean success = DasServerInfoEditor.this.applyChanges();
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
      url_field.setText("");
      url_field.setEnabled(true);
      name_field.setText("");
      name_field.setEnabled(true);
      enabled_box.setSelected(true);
    } else {
      // neither URL nor name is expected to be null, if so, there isn't anything that can be done
      String uuu = node.get(DasDiscovery.KEY_URL, null);
      if (uuu == null) {
        ErrorHandler.errorPanel("ERROR", "Empty URL in Das Server preferences!");
        return;
      }
      url_field.setText(uuu);
      url_field.setEnabled(false);
      name_field.setText(node.get(DasDiscovery.KEY_NAME, ""));
      name_field.setEnabled(true);
      enabled_box.setSelected(node.getBoolean(DasDiscovery.KEY_ENABLED, true));
    }
  }

  JDialog createDialog(Component parent) {
    JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, parent);
    dialog = new JDialog(frame, TITLE, true);
    dialog.getContentPane().add(this);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
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
    String url_string = url_field.getText();
    String name_string = name_field.getText();
    if (name_string == null || name_string.trim().equals("")) {
      ErrorHandler.errorPanel("No name", "You must supply a name for the server");
    }
    else if (this.original_node == null) {
      //System.out.println("Adding a new node for url: "+url_string);
      URL new_url = null;
      try {
        new_url = new URL(url_string);
        Preferences node = DasDiscovery.getNodeForURL(url_string, true);
        node.put(DasDiscovery.KEY_URL, url_string);
        node.put(DasDiscovery.KEY_NAME, name_string);
        node.putBoolean(DasDiscovery.KEY_ENABLED, enabled_box.isSelected());
        ok = true;
      } catch (MalformedURLException e) {
        ErrorHandler.errorPanel("Invalid URL", "The given URL is not valid");
      }
    } else {
      //System.out.println("Modifying existing node. ");
      this.original_node.put(DasDiscovery.KEY_NAME, name_string);
      this.original_node.putBoolean(DasDiscovery.KEY_ENABLED, enabled_box.isSelected());
      ok = true;
    }
    return ok;
  }
}
