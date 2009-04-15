/**
 *   Copyright (c) 2006 Affymetrix, Inc.
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

/**
 *  A class that contains some basic documentation text.
 */
public final class DocumentationView {
  public DocumentationView() {
  }
  
  public static String getDocumentationText() {
    StringBuffer sb = new StringBuffer();
    sb.append("\n");
    sb.append("Documentation and user forums for IGB can be found at SourceForge.\n");
    sb.append("\n");
    sb.append("The source code is hosted at SourceForge.net as part of the GenoViz project. \n");
    sb.append("There you can find downloads of source code, pre-compiled executables, \n");
    sb.append("extra documentation, and a place to report bugs or feature requests.\n");
    sb.append("\n");
    sb.append("Introduction Page: http://genoviz.sourceforge.net/\n");
    sb.append("User's Guide (PDF): \n http://genoviz.sourceforge.net/IGB_User_Guide.pdf\n");
    sb.append("Release Notes: \n http://genoviz.sourceforge.net/release_notes/igb_release.html");
    sb.append("\n");
    sb.append("Downloads: \n http://sourceforge.net/project/showfiles.php?group_id=129420\n");
    sb.append("Documentation: \n http://sourceforge.net/docman/?group_id=129420\n");
    sb.append("Bug Reports: \n http://sourceforge.net/tracker/?group_id=129420&atid=714744\n");
    sb.append("\n");
    
    return sb.toString();
  }
  
}
