/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.parser;

import java.net.ContentHandler;
import java.net.URLConnection;
import java.io.IOException;

/**
 * a {@link ContentHandler} that uses a genoviz {@link ContentParser}.
 *
 * @author Cyrus Harmon
 */
public class NeoContentHandler extends ContentHandler {

  ContentParser parser;

  /**
   * constructs a handler using the given parser.
   *
   * @param parser the parser that will actually create the Object
   *               to be returned.
   */
  public NeoContentHandler(ContentParser parser) {
    this.parser = parser;
  }

  /**
   * delegates the getting of content
   * to the parser.
   *
   * @param urlc from whence the data come.
   * @return an Object modeling the data.
   */
  public Object getContent( URLConnection urlc ) throws IOException {
    Object o = parser.importContent( urlc.getInputStream() );
    return o;
  }

}
