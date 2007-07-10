/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.swing.ColorIcon;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;

/**
 *  Helps to save and load preferences such as locations of windows.
 *  The {@link Preferences} class is pretty easy to use, so the real utility of
 *  this class is to make sure that preference names and values follow
 *  some conventions and take valid values.
 */
 public abstract class UnibrowPrefsUtil {

  public static final String CONTROL_GRAPH_DIRECTORY = "control graph directory";
  public static final String DATA_DIRECTORY = "data directory";
  public static final String OUTPUT_DIRECTORY = "output directory";

  public static final String COMPONENT_STATE_TAB = "TAB";
  public static final String COMPONENT_STATE_WINDOW = "WINDOW";

   public static String SLASH_STANDIN = "%";


  /** The name of a boolean preference. */
  public static final String ASK_BEFORE_EXITING = "Ask before exiting";
  public static final boolean default_ask_before_exiting = false;

  private static Vector FILENAMES;
  static {
    FILENAMES = new Vector();
    FILENAMES.add(CONTROL_GRAPH_DIRECTORY);
    FILENAMES.add(DATA_DIRECTORY);
    FILENAMES.add(OUTPUT_DIRECTORY);
  }

  /**
   *  Returns the top preferences node for the "com/affymetrix/igb" package.
   *  Warning: do not try to store a preference node in a
   *  static variable.  This method will re-create the
   *  node even if it has been deleted with Preferences.removeNode(),
   *  whereas trying to use a static variable could lead to
   *  IllegalStateException's.
   */
  public static Preferences getTopNode() {
    return Preferences.userRoot().node("/com/affymetrix/igb");
  }

   public static void saveIntParam(String param_name, int param) {
     try {
       getTopNode().putInt(param_name, param);
     } catch (Exception e) {
       e.printStackTrace(System.out);
     }
   }

   public static int getIntParam(String param_name, int def) {
     return getTopNode().getInt(param_name, def);
   }

   public static void saveBooleanParam(String param_name, boolean param) {
     try {
       getTopNode().putBoolean(param_name, param);
     } catch (Exception e) {
       e.printStackTrace(System.out);
     }
   }

   public static boolean getBooleanParam(String param_name, boolean def) {
     return getTopNode().getBoolean(param_name, def);
   }

  /** Saves a preferred file, for example the directory the user keeps files in.
   *  If the given file is null or doesn't exist the routine will do nothing.
   *  @param name  Only acceptable value is {@link #DATA_DIRECTORY}
   *    or {@link #CONTROL_GRAPH_DIRECTORY}
   *  @throws IllegalArgumentException if name is not one of the pre-defined names.
   */
  public static void saveFilename(String name, File f) {
    if (! FILENAMES.contains(name)) {
      throw new IllegalArgumentException("'"+name+"' is not a known name for a file preference");
    }
    if (f == null || ! f.exists()) return;
    try {
      String path = f.getCanonicalPath();
      getTopNode().put(name, path);
    } catch (IOException ioe) {
      ErrorHandler.errorPanel("Can't resolve file path", ioe);
    } catch (Exception e) {
      e.printStackTrace(System.out);
    }
    try {getTopNode().flush();} catch (BackingStoreException bse) {}
  }

  /** Gets a preferred directory. If no directory can be found
   *  in the user's preferences for the given name, returns
   *  the user's home directory as a default.
   *  @param name  One of {@link #CONTROL_GRAPH_DIRECTORY} or {@link #DATA_DIRECTORY}
   *  @throws IllegalArgumentException if name is not one of the pre-defined names.
   */
  public static File getFilename(String name) {
    if (! FILENAMES.contains(name)) {
      throw new IllegalArgumentException("'"+name+"' is not a known name for a file preference");
    }
    try {getTopNode().sync();} catch (BackingStoreException bse) {}
    String path = getTopNode().get(name, System.getProperty("user.dir"));
    File f = new File(path);
    if (! f.exists()) {
      f = new File(System.getProperty("user.dir"));
    }
    return f;
  }

  /** Saves the current location of a window to the user's preferences.
   *  @param w     the window, in the desired location
   *  @param name  a unique identifying name
   */
  public static void saveWindowLocation(Window w, String name) {
    Rectangle r = w.getBounds();
    //System.out.println("saving window '"+name+"' location: "+r);
    try {
      Preferences p = getTopNode();
      p.putInt(name+" x", r.x);
      p.putInt(name+" y", r.y);
      p.putInt(name+" width", r.width);
      p.putInt(name+" height", r.height);
    } catch (Exception e) {
      e.printStackTrace(System.out);
    }
  }

  /** Retrieves the previously-stored preferred location of a window.
   *  Since this data may not have been saved, you must supply a default value.
   *  @param name   a unique identifying name
   *  @param def    a default location
   */
  public static Rectangle retrieveWindowLocation(String name, Rectangle def) {
    Rectangle r = new Rectangle();
    Preferences p = getTopNode();
    r.x = p.getInt(name+" x", def.x);
    r.y = p.getInt(name+" y", def.y);
    r.width = p.getInt(name+" width", def.width);
    r.height = p.getInt(name+" height", def.height);
    //System.out.println("Found window location for '"+name+"': "+r);
    return r;
  }

  /** Sets the size of a window, making sure the window does not
   *  start at a negative coordinate, nor have a size larger than 99% of the
   *  current screen size.
   *  (You are responsible for calling doLayout() yourself.)
   */
  public static void setWindowSize(Window w, Rectangle r) {
    Toolkit kit = Toolkit.getDefaultToolkit();
    Dimension dim = kit.getScreenSize();
    if (r.x < 0 || r.x > dim.width) {r.x = 0;}
    if (r.y < 0 || r.y > dim.height) {r.y = 0;}
    if (r.width < 50) r.width = 50;
    if (r.height < 50) r.height = 50;
    r.width = Math.min(r.width, (int)(dim.width * 0.99));
    r.height = Math.min(r.height, (int)(dim.height * 0.99));

    w.setBounds(r);
    if (w instanceof Frame) {((Frame) w).setState(Frame.NORMAL);}
  }

  /** Saves the state of a component.
   *  @param state must be either {@link #COMPONENT_STATE_TAB}
   *    or {@link #COMPONENT_STATE_WINDOW}.
   *  @throws IllegalArgumentException if the given value of state is not one
   *          of the acceptable values
   */
  public static void saveComponentState(String name, String state) {
    if ((state == COMPONENT_STATE_TAB) || (state == COMPONENT_STATE_WINDOW)) {
      getTopNode().put(name+" state", state);
    } else {
      throw new IllegalArgumentException();
    }
  }

  /** Returns the previously-stored state of a component.
   *  @return one of {@link #COMPONENT_STATE_TAB}
   *  or {@link #COMPONENT_STATE_WINDOW}
   */
  public static String getComponentState(String name) {
    return getTopNode().get(name+" state", COMPONENT_STATE_TAB);
  }

  static JFileChooser static_chooser = null;

  /** Gets a static re-usable file chooser that prefers "xml" files. */
  static JFileChooser getJFileChooser() {
    if (static_chooser == null) {
      static_chooser = new UniFileChooser("XML File", "xml");
    }
    static_chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    //static_chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
    static_chooser.rescanCurrentDirectory();
    return static_chooser;
  }

  /** Brings up a JFileChooser to pick the name of the file to
   *  export to, and then calls {@link #exportPreferences(Preferences, File)}
   *  with the Preferences node from {@link #getTopNode()}.
   *  @param parent_comp  the parent component for the JFileChooser, null is ok
   */
  public static void exportPreferences(Component parent_comp) {
    JFileChooser chooser = getJFileChooser();
    int option = chooser.showSaveDialog(parent_comp);
    if (option == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      try {
        Preferences prefs = getTopNode();
        exportPreferences(prefs, f);
      } catch (Exception e) {
        ErrorHandler.errorPanel("ERROR", "Error saving preferences to file", e);
      }
    }
  }

  /** Exports the preferences subtree to a file.
   *  Calls {@link Preferences#exportSubtree(OutputStream)}.
   */
  public static void exportPreferences(Preferences prefs, File f)
  throws IOException, BackingStoreException {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(f);
      prefs.exportSubtree(fos);
    } finally {
      try { fos.close(); } catch (Exception e) {}
    }
  }

  /** Brings up a JFileChooser to pick the name of the file to
   *  import from, and then calls {@link #importPreferences(File)}.
   *  @param parent_comp  the parent component for the JFileChooser, null is ok
   */
  public static void importPreferences(Component parent_comp) {
    JFileChooser chooser = getJFileChooser();
    int option = chooser.showOpenDialog(parent_comp);
    if (option == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      try {
        importPreferences(f);
      } catch (InvalidPreferencesFormatException ipfe) {
        ErrorHandler.errorPanel("ERROR", "Invalid preferences format:\n"+ipfe.getMessage()
        +"\n\nYou can only IMPORT preferences from a file that was created with EXPORT.  "+
        "In particular, you cannot import the file 'igb_prefs.xml' that was "+
        "used in earlier versions of this program.");
      } catch (Exception e) {
        ErrorHandler.errorPanel("ERROR", "Error importing preferences from file", e);
      }
    }
  }

  /**
   *  Imports and merges preferences from a file.
   *  It is not possible to limit the effects of this action
   *  to only preferences in the "com.affymetrix.igb" subtree.
   *  Any preferences in the file will be read and merged into the
   *  user's preferences.
   *  @see Preferences#importPreferences(InputStream)
   */
  public static void importPreferences(File f)
  throws IOException, InvalidPreferencesFormatException {
    FileInputStream fis = null;
    try {
      Preferences prefs = getTopNode();
      fis = new FileInputStream(f);
      prefs.importPreferences(fis);
    } finally {
      try { fis.close(); } catch (Exception e) {}
    }
  }

 
   public static void main(String[] args) {
     clearPreferences(null);
     System.exit(0);
   }


  /**
   *  Clears ALL stored preferences under the top node of {@link #getTopNode()}.
   *  Since this could have serious consequences, first asks for confirmation
   *  from the user via a JOptionPane.
   *  (The most significant negative consequence is that any PreferenceChangeListener's
   *  on any of the preference nodes will stop getting messages.)
   *  @param parent_comp  the parent component for the JOptionPane, null is ok
   */
  public static void clearPreferences(Component parent_comp) {
    // The option pane used differs from the confirmDialog only in
    // that "No" is the default choice.
    String[] options = {"Yes", "No"};
    if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
      parent_comp, "Really clear all preferences?", "Clear preferences?",
      JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
      options, options[1])) {

      try {
        getTopNode().removeNode();
      } catch (Exception e) {
        ErrorHandler.errorPanel("ERROR", "Error clearing preferences", e);
      }
    }
  }

  public static Preferences getKeystrokesNode() {
    return UnibrowPrefsUtil.getTopNode().node("keystrokes");
  }

  static SortedSet keystroke_node_names = new TreeSet();

  public static Collection getKeystrokesNodeNames() {
    return Collections.unmodifiableSet(new TreeSet(keystroke_node_names));
  }

  /** Finds the KeyStroke that was specified in the preferences
   *  for the given action_command String.
   *  @param action_command  a String used to uniquely identify an action
   *    both in the program and in the preferences file;  these names
   *    need to be globably unique within the application
   *  @return null if no preference was set or the given String is null
   */
  public static KeyStroke getAccelerator(String action_command) {
    if (action_command == null) {return null;}
    String str = getKeystrokesNode().get(action_command, "");
    KeyStroke ks = KeyStroke.getKeyStroke(str);

    keystroke_node_names.add(action_command);

    if (ks == null) {
      if ("".equals(str)) {
        //System.out.println("No accelerator set for '"+ action_command +"'");
      } else {
        System.out.println("Bad format accelerator set for '"+ action_command +"':");
        System.out.println("  invalid '"+ str +"'");
      }
      // put a blank value in the keystroke so that the user will be able to
      // see which preferences are settable
      // (actually, this is no longer necessary now that the keystroke_node_names
      // Set is being used to keep track of these.)
      getKeystrokesNode().put(action_command, "");
    }

    return ks;
  }

  public static Preferences getLocationsNode() {
    return UnibrowPrefsUtil.getTopNode().node("locations");
  }

  public static Preferences getGenomesNode() {
    return UnibrowPrefsUtil.getTopNode().node("genomes");
  }

  public static String getLocation(String name, String default_value) {

    if (name == null) {return null;}
    String str = getLocationsNode().get(name, null);

    if (str == null && default_value != null) {
      // store the default value so that the user will be able to
      // see which preferences are settable
      //System.out.println("Storing default value for location '"+name+"' --> '"+default_value+"'");
      getLocationsNode().put(name, default_value);
      str = default_value;
    }

    return str;
  }

  static String app_dir = null;

  /** Returns the location of the application data directory.
   *  The String will always end with "/".
   */
  public static String getAppDataDirectory() {
    if (app_dir == null) {
      String home = System.getProperty("user.home");
      String app_data = home + "/Application Data";
      File app_data_dir = new File(app_data);
      if (app_data_dir.exists() && app_data_dir.isDirectory()) {
        app_dir = app_data + "/IGB/";
      }
      else {
        app_dir = home + "/.igb/";
      }
    }
    if (! app_dir.endsWith("/")) {app_dir = app_dir + "/";}
    return app_dir;
  }

  static boolean bse_already_warned_once = false;

  /** Will issue a stern warning message the first time a BackingStoreException
   *  is passed to this method, but will be silent about all future ones.
   */
  public static void handleBSE(Component parent, BackingStoreException bse) {
    if (bse_already_warned_once) {
      return;
    } else {
      JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, parent);
      ErrorHandler.errorPanel(frame, "BackingStoreException",
        "Cannot communicate with the preference storage system.  \n" +
        "Changes to preferences may not become permanent.  \n" +
        "It may be a good idea to restart the program.  \n", null);
      bse_already_warned_once = true;
    }
    System.out.println("BackingStoreException: "+bse.getMessage());
  }

  /**
   *  Stores a color preference, encoded as a String.
   */
  public static void putColor(Preferences node, String key, Color c) {
    int i = c.getRGB() & 0xFFFFFF;
    String s = Integer.toHexString(i).toUpperCase();
    while (s.length() < 6) {
      s = "0"+s;
    }
    s = "0x"+s;
    node.put(key, s);
  }

  /**
   *  Retrieves a color preference that was stored with {@link #putColor(Preferences, String, Color)}.
   */
  public static Color getColor(Preferences node, String key, Color default_color) {
    Color result = default_color;
    String value = node.get(key, "unknown");
    if (value != "unknown") {
      try {
        result = Color.decode(value);
      } catch (Exception e) {
        System.out.println("Couldn't decode color preference for '"+key+"' from '"+value+"'");
      }
    }
    return result;
  }

  /**
   *  Creates a JCheckBox associated with a boolean preference.
   *  Will initialize itself with the value of the given
   *  preference and will update itself, via a PreferenceChangeListener,
   *  if the preference value changes.
   */
  public static JCheckBox createCheckBox(String title, final Preferences node,
    final String pref_name, boolean default_val) {
    final JCheckBox check_box = new JCheckBox(title);
    check_box.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        node.putBoolean(pref_name, check_box.isSelected());
      }
    });
    check_box.setSelected(node.getBoolean(pref_name, default_val));
    node.addPreferenceChangeListener(new PreferenceChangeListener() {
      public void preferenceChange(PreferenceChangeEvent evt) {
        if (evt.getNode().equals(node) && evt.getKey().equals(pref_name)) {
          check_box.setSelected(Boolean.valueOf(evt.getNewValue()).booleanValue());
        }
      }
    });
    return check_box;
  }

  /**
   *  Creates a JTextField associated with a String preference.
   *  Can also be used with Integer, Boolean, etc., preferences,
   *  since they can be inter-converted with Strings.
   *  Will initialize itself with the value of the given
   *  preference and will update itself, via a PreferenceChangeListener,
   *  if the preference value changes.
   */
  public static JTextField createTextField(final Preferences node,
    final String pref_name, String default_val) {
    String initial_value = node.get(pref_name, default_val);
    final JTextField text_box = new JTextField(initial_value);
    text_box.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String current_value = text_box.getText();
        node.put(pref_name, current_value);
      }
    });
    node.addPreferenceChangeListener(new PreferenceChangeListener() {
      public void preferenceChange(PreferenceChangeEvent evt) {
        if (evt.getNode().equals(node) && evt.getKey().equals(pref_name)) {
          text_box.setText(evt.getNewValue());
        }
      }
    });
    text_box.addFocusListener(new java.awt.event.FocusListener() {
      public void focusGained(java.awt.event.FocusEvent e) {}
      public void focusLost(java.awt.event.FocusEvent e) {
        text_box.postActionEvent();
      }
    });
    return text_box;
  }

  /**
   *  Creates a JTextField associated with a String preference for a number, where
   *  you want to ensure use of a particular type of number (Integer, Float, etc.).
   *  Will not allow the value to be set to anything that isn't parseable as
   *  the given type.
   *  Will initialize itself with the value of the given
   *  preference and will update itself, via a PreferenceChangeListener,
   *  if the preference value changes.
   *  @param class_type one of Double, Long, Short, Integer, or Float
   */
  public static JTextField createNumberTextField(final Preferences node,
    final String pref_name, final String default_val, final Class class_type) {

    String initial_value = node.get(pref_name, default_val);
    final JTextField text_box = new JTextField(initial_value);
    text_box.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String previous_value = node.get(pref_name, default_val);
        String typed_value = text_box.getText();
        String new_value = normalizeStringToNumber(typed_value, previous_value, class_type);
        node.put(pref_name, new_value);
      }
    });
    node.addPreferenceChangeListener(new PreferenceChangeListener() {
      public void preferenceChange(PreferenceChangeEvent evt) {
        if (evt.getNode().equals(node) && evt.getKey().equals(pref_name)) {
          text_box.setText(evt.getNewValue());
        }
      }
    });
    text_box.addFocusListener(new java.awt.event.FocusListener() {
      public void focusGained(java.awt.event.FocusEvent e) {}
      public void focusLost(java.awt.event.FocusEvent e) {
        text_box.postActionEvent();
      }
    });
    return text_box;
  }

  /**
   *  Makes sure that a given String is parseable as a particular Class of Number.
   *  @param new_val  The value you to be tested.
   *  @param fallback  The value to use if the given value isn't parseable as the
   *    given class type
   *  @param type one of Double, Long, Short, Integer, or Float
   *  @return a String representing the value given by new_val, if it was
   *    parseable as the requested class type, or else the fallback value.
   *    (Note that the value of the fallback string is never tested and might not
   *    itself be a valid number.)
   */
  static String normalizeStringToNumber(String new_val, String fallback, Class type) {
    String result = fallback;
    try {
      Double d = new Double(new_val);
      if (Integer.class.equals(type)) {
        result = Integer.toString(d.intValue());
      }
      else if (Double.class.equals(type)) {
        result = d.toString();
      }
      else if (Long.class.equals(type)) {
        result = Long.toString(d.longValue());
      }
      else if (Float.class.equals(type)) {
        result = Float.toString(d.floatValue());
      }
      else if (Short.class.equals(type)) {
        result = Short.toString(d.shortValue());
      }
      else if (String.class.equals(type)) {
        /* Do nothing special for Strings. */
      }
      else {
        throw new IllegalArgumentException("Class '"+type+"' not accepted by this routine.");
      }
    } catch (NumberFormatException nfe) {
      Toolkit.getDefaultToolkit().beep();
      result = fallback;
    }
    return result;
  }

  /**
   *  Creates a JComboBox associated with a String preference.
   *  Will initialize itself with the value of the given
   *  preference and will update itself, via a PreferenceChangeListener,
   *  if the preference value changes.
   */
  public static JComboBox createComboBox(final Preferences node,
    final String pref_name, String[] options, String default_value) {

    final String[] interned_options = new String[options.length];
    for (int i=0; i<options.length; i++) {
      interned_options[i] = options[i].intern();
    }
    default_value.intern();

    final JComboBox combo_box = new JComboBox(interned_options);

    // Note that no check is made that the given default_value is
    // actually one of the given options.  The combo_box will ignore
    // an attempt to set itself to a value that isn't in its option list.
    String current_stored_value = node.get(pref_name, default_value).intern();
    combo_box.setSelectedItem(current_stored_value);

    combo_box.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String selection = (String) combo_box.getSelectedItem();
        if (selection != null) { // selection == null is probably impossible
          node.put(pref_name, selection);
        }
      }
    });

    node.addPreferenceChangeListener(new PreferenceChangeListener() {
      public void preferenceChange(PreferenceChangeEvent evt) {
        if (evt.getNode().equals(node) && evt.getKey().equals(pref_name)) {
          if (! combo_box.getSelectedItem().equals(evt.getNewValue())) {
            // Note: checking that selection differs from new value prevents infinite loop.
            combo_box.setSelectedItem( ((String) evt.getNewValue()).intern() );
          }
        }
      }
    });
    return combo_box;
  }

  /**
   *  Creates a JButton associated with a Color preference.
   *  Will initialize itself with the value of the given
   *  preference and will update itself, via a PreferenceChangeListener,
   *  if the preference value changes.
   *  @param title  The title of the JButton and of the JColorChooser that will
   *    be opened when the button is pressed.  This is optional, null is ok.
   */
  public static JButton createColorButton(String title, final Preferences node,
    final String pref_name, final Color default_val) {

    Color initial_color = getColor(node, pref_name, default_val);
    final ColorIcon icon = new ColorIcon(11, initial_color);
    final String panel_title = (title == null ? "Choose a color" : title);

    final JButton button = new JButton(title, icon);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        Color c = JColorChooser.showDialog(button, panel_title, getColor(node, pref_name, default_val));
        if (c != null) {
          putColor(node, pref_name, c);
        }
      }
    });
    node.addPreferenceChangeListener(new PreferenceChangeListener() {
      public void preferenceChange(PreferenceChangeEvent evt) {
        if (evt.getNode().equals(node) && evt.getKey().equals(pref_name)) {
          Color c = getColor(node, pref_name, default_val);
          icon.setColor(c);
          button.repaint();
        }
      }
    });
    return button;
  }

  /** Convert a String of arbitrary length into one that is short enough to
   *  be used as a key name or node name.
   */
  public static String shortKeyName(String s) {
    String short_s;
    if (s.length() >= Preferences.MAX_KEY_LENGTH) {
      short_s = UrlToFileName.toMd5(s);
    } else {
      short_s = s;
    }
    return short_s;
  }



   public static String shortNodeName(String s)  {
     return shortNodeName(s, false);
   }

   public static String shortNodeName(String s, boolean remove_slash)  {
    String short_s;
    if (s.length() >= Preferences.MAX_NAME_LENGTH) {
      short_s = UrlToFileName.toMd5(s);
    } else {
      short_s = s;
    }
    if (remove_slash) {
      short_s = short_s.replaceAll("/", SLASH_STANDIN);
    }
    return short_s;
   }


  /** Create a subnode, making sure to shorten the name if necessary. */
  public static Preferences getSubnode(Preferences parent, String name) {
    return getSubnode(parent, name, false);
  }

  /** Create a subnode, making sure to shorten the name if necessary. */
  public static Preferences getSubnode(Preferences parent, String name, boolean remove_slash) {
    String short_name = shortNodeName(name, remove_slash);
    return parent.node(short_name);
  }

  public static JFrame createFrame(String name, JPanel panel) {
    final JFrame frame;

    if (name.length() > 70) {
      throw new IllegalArgumentException("Title of the frame must be less than 70 chars.");
    }

    // If not already open in a new window, make a new window
    frame = new JFrame(name);
    frame.setName(name);

    frame.getContentPane().add(panel);
    frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

    panel.setVisible(true);
    frame.pack(); // pack() to set frame to its preferred size

    Rectangle pos = UnibrowPrefsUtil.retrieveWindowLocation(frame.getTitle(), frame.getBounds());
    if (pos != null) {
      UnibrowPrefsUtil.setWindowSize(frame, pos);
    }

    frame.setVisible(true);
    frame.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        // save the current size into the preferences, so the window
        // will re-open with this size next time
        UnibrowPrefsUtil.saveWindowLocation(frame, frame.getTitle());
      }
    });

    // window already exists, but may not be visible
    return frame;
  }
 }
