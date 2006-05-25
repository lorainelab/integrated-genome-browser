/*
 * Das2OntologySource.java
 *
 * Created on November 8, 2005, 1:29 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.affymetrix.igb.das2.ontology;

import com.affymetrix.igb.das2.*;

import java.io.*;
import java.net.*;
import java.util.*;
import org.xml.sax.*;
import org.w3c.dom.*;

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.DasUtils;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.util.ErrorHandler;

/**
 *
 * @author Marc Carlson
 *
 */
public class Das2OntologySource extends Das2Source {

  boolean init;
    /** Creates a new instance of Das2OntologySource */
    public Das2OntologySource(Das2OntologyServerInfo source_server, URI src_uri, boolean init) {
      super(source_server, src_uri, null, null, null, null);
      this.init = init;
    }

    //overridden methods
    public void addVersion(Das2OntologyVersionedSource version) {
        this.versions.put(version.getID(), version);
    }

}
