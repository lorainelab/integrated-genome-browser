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

package com.affymetrix.igb.util;

import java.io.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;


/**
 *  A user-friendly JFileChooser.  This is most useful when saving files,
 *  because it will warn the user if they try to over-write an existing file,
 *  and it both forces and helps the user to supply the correct file extension.
 *  For reading files, you probably need to set-up your own JFileChooser, but
 *  the UniFileFilter class can be helpful in that case.
 *
 */
public class UniFileChooser extends JFileChooser {

  /** 
   *  A singleton UniFileChooser, re-used when possible.
   */
  private static UniFileChooser static_file_chooser;

  private String description = "Any file (*.*)";
  private String extension = "";

  private FileFilter current_file_filter;

  /** 
   *  Creates and returns a JFileChooser which accepts only filenames 
   *  ending in period+extension when creating or writing to a file,
   *  it also prefers this ending when reading
   *  files, but will allow you to try to read an existing file
   *  with a different extension.
   *
   *  <p>Example: new UniFileChooser("AXML file (*.axml)", "axml");
   */
  public UniFileChooser(String description, String extension) {

    super(new File((String) System.getProperties().get("user.dir")));

    reinitialize(description, extension);
  }

  public static UniFileChooser getFileChooser(String description, String extension) {
    if (static_file_chooser == null) {
      static_file_chooser = new UniFileChooser(description, extension);
    }
    else static_file_chooser.reinitialize(description, extension);

    return static_file_chooser;
  }

  /** 
   *  Reinitializes a singleton JFileChooser to accept only an ".axml"
   *  filename  when creating a file, and to prefer ".axml" filenames
   *  when reading files, but will allow you to try to read an existing
   *  file with a different extension.
   */
  public static UniFileChooser getAXMLFileChooser() {
    return getFileChooser("AXML file (*.axml)", "axml");
  }

  /** 
   *  Reinitializes a singleton JFileChooser to accept only an ".xml"
   *  filename  when creating a file, and to prefer ".xml" filenames
   *  when reading files, but will allow you to try to read an existing
   *  file with a different extension.
   */
  public static UniFileChooser getXMLFileChooser() {
    return getFileChooser("XML file (*.xml)", "xml");
  }

  /** 
   *  Resets such that it will accept only filenames 
   *  ending in period+extension when creating or writing to a file,
   *  and will prefer this ending when reading
   *  files, but will allow opening an existing file
   *  with a different extension for reading.
   *
   *  <p>Example: reinitialize("AXML file (*.axml)", "axml");
   */
  protected void reinitialize(final String description, final String extension) {
    if (description==null || extension==null || "".equals(extension)) throw new 
      IllegalArgumentException("description and extension cannot be null");

    if (extension.indexOf('.') != -1) throw new
      IllegalArgumentException("extension should not contain '.'");

    if (this.description != description || this.extension != extension) {
      this.description = description;
      this.extension = extension;

      if (current_file_filter != null) {
        removeChoosableFileFilter(current_file_filter);
      }

      current_file_filter = new FileFilter() {
        public final boolean accept(File f) {
          return (f.isDirectory() || f.getName().endsWith("."+extension));
        }
        public final String getDescription() {return description;}
      };

      addChoosableFileFilter(current_file_filter);
    }

    addChoosableFileFilter(getAcceptAllFileFilter());
    setFileFilter(current_file_filter);
    setMultiSelectionEnabled(false);
    setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    rescanCurrentDirectory();
    setSelectedFile(null);  
  }


  /** 
   *  If the selected file looks like a reasonable choice, then open it. 
   *  Else suggest a new filename. 
   */
  public void approveSelection() {
    File f = getSelectedFile();

    if (f.isDirectory()) {
      setSelectedFile(null);  
      setCurrentDirectory(f);
      return;
    }

    if (getDialogType() == OPEN_DIALOG) {
      if (f.exists()) {
        super.approveSelection();
      }
      else { // if a similar filename with "."+extension exists, suggest that
        getToolkit().beep();
        File file2 = applyExtension(f, extension);
        if (file2.exists()) setSelectedFile(file2);
      }
    }

    else if (getDialogType() == SAVE_DIALOG) {
      if (f.getName().endsWith("."+extension)) {
        if (! f.exists()) {
          super.approveSelection();
        }
        else { // give the user the choice to overwrite the existing file or not
          // The option pane used differs from the confirmDialog only in
          // that "No" is the default choice.
          getToolkit().beep();
          String[] options = {"Yes", "No"};
          if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
            this, "Overwrite Existing File?", "File Exists",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            options, options[1])) super.approveSelection();
        }
      }
      else { // Suggest to the user a new filename ending with "."+extension
        getToolkit().beep();
        setSelectedFile(applyExtension(f, extension));
      }
    }
  }

  /** Return a new file with the given extension at the end of the name. */
  private File applyExtension(File f, String extension) {
    String name = f.getName();
    String dotExtension = "."+extension;

    if (name.endsWith(".")) {
      return new File(name+extension);
    }
    else if (! name.endsWith(dotExtension)) {
      return new File(name+dotExtension);
    }
    else return f;
  }
  
}
