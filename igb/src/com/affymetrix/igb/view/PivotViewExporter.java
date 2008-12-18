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
import com.affymetrix.genoviz.util.ErrorHandler;
import java.io.*;
import java.text.*;
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
          theDest.print( format(theTable.getValueAt( i, j )) + "\t" );
        }
        if ( 0 < cols ) {
          theDest.print( format(theTable.getValueAt( i, lastCol )) );
        }
        theDest.println();
      }
    }
  };

  
  /**
   * Convert the table to "cdt" format.
   */
  private TableFilter cdtFilter = new TableFilter() {
    {
      this.validSuffixes.add( "cdt" );
      this.description = "Clustered Data Table";
    }
    /**
     * Quote the source using Microsoft style quote mark stuffing.
     */

    public void write( TableModel theTable, PrintWriter theDest ) {
      int rows = theTable.getRowCount();
      int cols = theTable.getColumnCount();

      theDest.print("UID\tNAME\tGWEIGHT");
      for ( int i = 3; i < cols; i++ ) {
        theDest.print('\t');
        theDest.print( theTable.getColumnName( i ));
      }
      theDest.println();
      
      theDest.print("EWEIGHT\t\t");
      for ( int j = 3; j < cols; j++ ) {
        theDest.print("\t1");
      }
      theDest.println();
      
      for ( int i = 0; i < rows; i++ ) {
        theDest.print(Integer.toString(i));
        theDest.print('\t');
        theDest.print(theTable.getValueAt(i, 0));
        theDest.print('\t');
        theDest.print('1');
        for ( int j = 3; j < cols; j++ ) {
          theDest.print('\t');
          String cell = "";
          Object o = theTable.getValueAt( i, j );
          if ( null != o ) cell = format(o);
          else cell = "0";
          theDest.print(cell);
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
          if ( null != o ) cell = format(o);
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
        if ( null != o ) cell = quote( format(o) );
        theDest.print( "<th>" + cell + "</th>" );
      }
      theDest.println( "</tr>" );
      for ( int i = 0; i < rows; i++ ) {
        theDest.print( "<tr>" );
        for ( int j = 0; j < cols; j++ ) {
          String cell = "";
          Object o = theTable.getValueAt( i, j );
          if ( null != o ) cell = quote( format(o) );
          theDest.print( "<td>" + cell + "</td>" );
        }
        theDest.println( "</tr>" );
      }
      theDest.println( "</table>" );
    }
  };

  NumberFormat nf = NumberFormat.getInstance();
  
  /**
   *  Convert an object to a String; if it is a number, then use
   *  a NumberFormat to do so.
   */
  String format(Object o) {
    String s;
    if (o==null) {
      s = "";
    }
    else if (o instanceof Number) {
      s = nf.format(o);
    } else {
      s = o.toString();
    }
    
    return s;
  }

  private ExperimentPivotView pivotView;
  private JFileChooser dialog = new JFileChooser();
  {
    dialog.addChoosableFileFilter( tsvFilter );
    dialog.addChoosableFileFilter( csvFilter );
    /* I created this CDT format exporter, but am not sure yet if it is useful
     * to add it to IGB.
    dialog.addChoosableFileFilter( cdtFilter );
     */
    dialog.addChoosableFileFilter( htmlFilter );
    dialog.setFileFilter( tsvFilter );
  }

  public PivotViewExporter( ExperimentPivotView theView ) {
    super( "Export..." );
    this.pivotView = theView;
    nf.setGroupingUsed(false); // no commas
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
          ErrorHandler.errorPanel("Problem saving file", ioe);
      }
      finally {
        if (out != null) try {out.close();} catch (Exception ex) {}
      }
    }
  }

}
