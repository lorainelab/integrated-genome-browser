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
public class DocumentationView {
  public DocumentationView() {
  }
  
  public static String getDocumentationText() {
    StringBuffer sb = new StringBuffer();
    sb.append("\n");
    sb.append("Documentation and user forums for IGB can be found at several places online.\n");
    sb.append("\n");
    sb.append("\n");
    sb.append("IGB AT AFFYMETRIX\n");
    sb.append("\n");
    sb.append("The main download page at Affymetrix.com contains links to the user manual, \n");
    sb.append("recent release notes, buttons to run IGB using Java WebStart, and a \n");
    sb.append("public discussion forum, primarily for Affymetrix customers. \n");
    sb.append("The release notes contain recent changes not yet incorporated in the manual.\n");
    sb.append("\n");
    sb.append("Main: http://www.affymetrix.com/support/developer/tools/download_igb.affx\n");
    sb.append("User's Guide (PDF): \n  http://www.affymetrix.com/support/developer/tools/IGB_User_Guide.pdf\n");    
    sb.append("Release Notes: \n  http://www.affymetrix.com/support/developer/tools/igb/release_notes/igb_release_4.html");
    sb.append("\n");
    sb.append("\n");
    sb.append("IGB AT SOURCEFORGE\n");
    sb.append("\n");
    sb.append("The source code is hosted at SourceForge.net as part of the GenoViz project. \n");
    sb.append("There you can find downloads of source code, pre-compiled executables, \n");
    sb.append("extra documentation, and a place to report bugs or feature requests.\n");
    sb.append("\n");
    sb.append("Introduction Page: http://genoviz.sourceforge.net/\n");
    sb.append("Downloads: http://sourceforge.net/project/showfiles.php?group_id=129420\n");
    sb.append("Documentation: http://sourceforge.net/docman/?group_id=129420\n");
    sb.append("Bug Reports: http://sourceforge.net/tracker/?group_id=129420&atid=714744\n");
    sb.append("\n");
    
    return sb.toString();
  }
  
}
