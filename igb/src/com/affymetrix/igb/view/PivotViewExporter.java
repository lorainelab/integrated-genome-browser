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

package com.affymetrix.igb.view;

import com.affymetrix.igb.util.TableFilter;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Exports pivot view data to spread sheet formats.
 * The user can specify a file type
 * affecting the translation.
 * We support
 * tab separated values (tsv),
 * comma separated values (csv), and
 * hypertext (html).
 * The default is tsv if no file type is specified.
 * <p>Actually, this could be called TableExporter
 * as there is nothing specific to PivotView here.
 *
 * @author  Eric Blossom
 */
public class PivotViewExporter extends AbstractAction {

  /**
   * Convert the table to tab delimited format.
   * Note that if any of the cells contain a tab character
   * this will not work well.
   * Use csv or html in such a case.
   */
  private TableFilter tsvFilter = new TableFilter() {
    {
      this.validSuffixes.add( "tsv" );
      this.validSuffixes.add( "txt" );
      this.description = "Tab Separated Values";
    }
    public void write( TableModel theTable, PrintWriter theDest ) {
      int rows = theTable.getRowCount();
      int cols = theTable.getColumnCount();
      int lastCol = cols-1;
      for ( int i = 0; i < lastCol; i++ ) {
        theDest.print( theTable.getColumnName( i ) + "\t" );
      }
      if ( 0 < cols ) {
        theDest.print( theTable.getColumnName( lastCol ) );
      }
      theDest.println();
      for ( int i = 0; i < rows; i++ ) {
        for ( int j = 0; j < lastCol; j++ ) {
          theDest.print( theTable.getValueAt( i, j ) + "\t" );
        }
        if ( 0 < cols ) {
          theDest.print( theTable.getValueAt( i, lastCol ) );
        }
        theDest.println();
      }
    }
  };

  /**
   * Convert the table to comma separated values.
   */
  private TableFilter csvFilter = new TableFilter() {
    {
      this.validSuffixes.add( "csv" );
      this.validSuffixes.add( "txt" );
      this.description = "Comma Separated Values";
    }
    /**
     * Quote the source using Microsoft style quote mark stuffing.
     */
    private String quote( String theSource ) {
      if ( theSource.matches( "^[^, \t\"]*$" ) ) return theSource;
      StringBuffer answer = new StringBuffer();
      for ( int i = 0; i < theSource.length(); i++ ) {
        char c = theSource.charAt( i );
        if ( '"' == c ) answer.append( "\"\"" );
        else answer.append( c );
      }
      return '"' + answer.toString() + '"';
    }
    public void write( TableModel theTable, PrintWriter theDest ) {
      int rows = theTable.getRowCount();
      int cols = theTable.getColumnCount();
      for ( int i = 0; i < cols; i++ ) {
        theDest.print( quote( theTable.getColumnName( i ) ) + ", " );
      }
      theDest.println();
      for ( int i = 0; i < rows; i++ ) {
        for ( int j = 0; j < cols; j++ ) {
          String cell = "";
          Object o = theTable.getValueAt( i, j );
          if ( null != o ) cell = o.toString();
          theDest.print( quote( cell ) + ", " );
        }
        theDest.println();
      }
    }
  };

  private TableFilter htmlFilter = new TableFilter() {
    {
      this.validSuffixes.add( "html" );
      this.validSuffixes.add( "htm" );
      this.validSuffixes.add( "xml" );
      this.validSuffixes.add( "xhtml" );
      this.description = "Hypertext";
    }
    private String quote( String theSource ) {
      if ( theSource.matches( "^[^<>&]$ " ) ) return theSource;
      StringBuffer answer = new StringBuffer();
      for ( int i = 0; i < theSource.length(); i++ ) {
        char c = theSource.charAt( i );
        switch( c ) {
          case '&': answer.append( "&amp;" ); break;
          case '<': answer.append( "&lt;" ); break;
          case '>': answer.append( "&gt;" ); break;
          default: answer.append( c );
        }
      }
      return answer.toString();
    }
    public void write( TableModel theTable, PrintWriter theDest ) {
      int rows = theTable.getRowCount();
      int cols = theTable.getColumnCount();
      theDest.println( "<table>" );
      theDest.print( "<tr>");
      for ( int i = 0; i < cols; i++ ) {
        String cell = "";
        Object o = theTable.getColumnName( i );
        if ( null != o ) cell = quote( o.toString() );
        theDest.print( "<th>" + cell + "</th>" );
      }
      theDest.println( "</tr>" );
      for ( int i = 0; i < rows; i++ ) {
        theDest.print( "<tr>" );
        for ( int j = 0; j < cols; j++ ) {
          String cell = "";
          Object o = theTable.getValueAt( i, j );
          if ( null != o ) cell = quote( o.toString() );
          theDest.print( "<td>" + cell + "</td>" );
        }
        theDest.println( "</tr>" );
      }
      theDest.println( "</table>" );
    }
  };


  private ExperimentPivotView pivotView;
  private JFileChooser dialog = new JFileChooser();
  {
    dialog.addChoosableFileFilter( tsvFilter );
    dialog.addChoosableFileFilter( csvFilter );
    dialog.addChoosableFileFilter( htmlFilter );
    dialog.setFileFilter( tsvFilter );
  }

  public PivotViewExporter( ExperimentPivotView theView ) {
    super( "Export..." );
    this.pivotView = theView;
  }

  /**
   * Put up a save dialog and export to the specified file.
   */
  public void actionPerformed( java.awt.event.ActionEvent e ) {
    Object o = e.getSource();
    int state = dialog.showSaveDialog( this.pivotView );
    if (dialog.APPROVE_OPTION == state ) {
      TableModel tm = this.pivotView.getTable();
      File f = dialog.getSelectedFile();
      javax.swing.filechooser.FileFilter ff = dialog.getFileFilter();
      PrintWriter out = null;
      try {
        out = new PrintWriter( new FileWriter( f ) );
        TableFilter tout = this.tsvFilter;
        if ( ff instanceof TableFilter ) {
          tout = ( TableFilter ) ff;
        }
        tout.write( tm, out );
        out.flush();
      }
      catch ( IOException ioe ) {
        com.affymetrix.igb.util.ErrorHandler.errorPanel("Problem saving file", ioe);
      }
      finally {
        if (out != null) try {out.close();} catch (Exception ex) {}
      }
    }
  }

}
