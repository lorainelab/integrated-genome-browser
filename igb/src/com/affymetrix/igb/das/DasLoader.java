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

package com.affymetrix.igb.das;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.util.LocalUrlCacher;

/**
 * A class to help load and parse documents from a DAS server.
 */
public abstract class DasLoader {
  final static boolean DEBUG = false;

  /** Creates a new DOMParser that has validation features turned off.
   *  The parser returned is not specifically set-up for DAS, and can be
   *  used in any case where you want a non-validating parser.
   */
  public static DOMParser nonValidatingParser() {
    if (DEBUG) System.out.println("========== Getting a nonValidatingParser!");
    DOMParser parser = new DOMParser();

    // validation and validation/dynamic should default to false, but just to make sure...
    try {
      parser.setFeature("http://xml.org/sax/features/validation", false);
      parser.setFeature("http://apache.org/xml/features/validation/dynamic", false);
      parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }
    catch (org.xml.sax.SAXNotSupportedException e) {}
    catch (org.xml.sax.SAXNotRecognizedException e) {}
    return parser;
  }

  /**
   *  Set parser to _not_ defer node expansion (thus forcing full expansion of DOM when
   *    loaded). This slows down "loading" of DOM significantly (~2-3x), but also significantly
   *    speeds up later access of the document, since that does not
   *    trigger any node expansions.
   *  Saves a lot of memory because it eliminates deferred-node objects that xerces-j uses, which
   *     seem to elude the garbage collector and just keep accumulating....
   */
  public static void doNotDeferNodeExpansion(DOMParser parser) {
    try {
      parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
    }
    catch (org.xml.sax.SAXNotSupportedException e) {}
    catch (org.xml.sax.SAXNotRecognizedException e) {}
  }

  /** Opens an XML document, using {@link #nonValidatingParser()}. */
  public static Document getDocument(String url)
  throws java.net.MalformedURLException, java.io.IOException, org.xml.sax.SAXException {
    if (DEBUG) System.out.println("=========== Getting a Document from URL: "+url);

    Document doc = null;
    URL request_url = new URL(url);
    URLConnection request_con = request_url.openConnection();
    doc = getDocument(request_con);
    return doc;
  }

  /** Opens an XML document, using {@link #nonValidatingParser()}. */
  public static Document getDocument(URLConnection request_con)
  throws java.io.IOException, org.xml.sax.SAXException {
    if (DEBUG) System.out.println("=========== Getting a Document from connection: "+request_con.getURL().toExternalForm());

    if (DEBUG) { LocalUrlCacher.reportHeaders(request_con); }

    InputStream result_stream = null;
    Document doc = null;
    try {
      result_stream = new BufferedInputStream(request_con.getInputStream());
      doc = getDocument(result_stream);
    } finally {
      if (result_stream != null) try {result_stream.close();} catch (Exception e) {}
    }
    return doc;
  }

  /** Opens an XML document, using {@link #nonValidatingParser()}. */
  public static Document getDocument(InputStream str)
  throws java.io.IOException, org.xml.sax.SAXException {
    Document doc = null;
    InputSource isrc = new InputSource(str);
    DOMParser parser = nonValidatingParser();
    parser.parse(isrc);
    doc = parser.getDocument();
    return doc;
  }

  /**
   *  Returns a Map where keys are String labels and values are SeqSpan's.
   *  Looks for <gff><segment id="..."> where the id's are in the given seq_group.
   */
  public static Map parseTermQuery(Document doc, AnnotatedSeqGroup seq_group) {
    if (DEBUG) System.out.println("========= Parsing term query");
    Map segment_hash = new HashMap();

    Element top_element = doc.getDocumentElement();
    String name = top_element.getTagName();
    //      System.out.println("top element: " + name);
    NodeList children = top_element.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String cname = child.getNodeName();
      if (cname != null && cname.equalsIgnoreCase("gff")) {
        NodeList gff_children = child.getChildNodes();
        for (int k=0; k<gff_children.getLength(); k++) {
          Node gff_child = gff_children.item(k);
          if (gff_child.getNodeName().equalsIgnoreCase("segment"))  {
            Element seg_elem = (Element)gff_child;
            String id = seg_elem.getAttribute("id");
            if (id != null) {
              BioSeq segmentseq = seq_group.getSeq(id);
              if (segmentseq != null) {
                int start = Integer.parseInt(seg_elem.getAttribute("start"));
                int end = Integer.parseInt(seg_elem.getAttribute("end"));
                SeqSpan segment_span = new SimpleSeqSpan(start, end, segmentseq);
                String das_label = seg_elem.getAttribute("label");
                String label = id + " : " + start + ", " + end + " ==> " + das_label;

                segment_hash.put(label, segment_span);
                if (DEBUG)  {
                  System.out.println("segment: id = " + id + ", start = " + start + ", end = " + end);
                }
              }
            }
          }
        }
      }
    }
    return segment_hash;
  }

  /**
   *  Returns a List of String's which are the id's of the segments.
   *  From <entry_points><segment id="...">.
   */
  public static List parseSegmentsFromEntryPoints(Document doc) {
    List seqs = new Vector();
    if (DEBUG) System.out.println("========= Parsing Segments from Entry Points");
    Element top_element = doc.getDocumentElement();
    String name = top_element.getTagName();
    //        System.out.println("top element: " + name);
    NodeList children = top_element.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String cname = child.getNodeName();
      if (cname != null && cname.equalsIgnoreCase("entry_points")) {
        NodeList entry_children = child.getChildNodes();
        for (int k=0; k<entry_children.getLength(); k++) {
          Node entry_child = entry_children.item(k);
          String gcname = entry_child.getNodeName();
          if (gcname != null && gcname.equalsIgnoreCase("segment")) {
            Element segment_elem = (Element)entry_child;
            String id = segment_elem.getAttribute("id");
            seqs.add(id);
          }
        }
      }
    }
    return seqs;
  }

  /** Returns a list of source id Strings.
   *  From  <dsn><source id="...">.
   */
  public static List parseSourceList(Document doc) {
    List ids = new ArrayList();
    if (DEBUG) System.out.println("========= Parsing Source List");
    String matching_id = null;
    Element top_element = doc.getDocumentElement();
    String name = top_element.getTagName();
    NodeList children = top_element.getChildNodes();

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String cname = child.getNodeName();
      // System.out.println(name);
      if (cname != null && cname.equalsIgnoreCase("dsn")) {
        NodeList dsn_children = child.getChildNodes();
        for (int k=0; k<dsn_children.getLength(); k++) {
          Node dsn_child = dsn_children.item(k);
          String gcname = dsn_child.getNodeName();
          if (gcname != null && gcname.equalsIgnoreCase("source")) {
            Element source_elem = (Element)dsn_child;
            String id = source_elem.getAttribute("id");
            ids.add(id);
          }
        }
      }
    }
    return ids;
  }

  /** FIXME: This is test code, functionality will be rolled into
   *  or replaced by com.affymetrix.igb.util.LocalUrlCacher.
   *  A method that checks the URL requested against a local
   *  document store and returns the local document URL if the
   *  server doc hasn't changed. Otherwise it just returns
   *  the URL argument.
   */
  public static String getCachedDocumentURL(String url)
    throws java.net.MalformedURLException, java.io.IOException {

    // fetch info about this URL
    URL request_url = new URL(url);
    URLConnection request_con = request_url.openConnection();

    // figure out hashcode
    int hashcode = url.hashCode();

    // find and create cache dir
    String home = System.getProperty("user.home");
    String sep = System.getProperty("file.separator");
    String cacheDir = home+sep+".igb"+sep+"cache";
    new File(cacheDir).mkdirs();
    File cacheFile = new File(cacheDir+sep+hashcode+".cache");
    long cacheDate = cacheFile.lastModified();

    /* HACK: I'm forcing the use of the cache docs
     * FIXME: if I to a getLastModified on a non-cached DAS/2 server (biopackages)
     * it actually causes the complete request to go through!!  So this call takes
     * forever and really makes the local caceh pointless.
     */
    long date = 0;// HACK: request_con.getLastModified();
    // try "Age" header if it exists
    /*if (date == 0) {
      String age_in_sec = request_con.getHeaderField("Age");
      if (age_in_sec != null) {
        long age = Long.parseLong(age_in_sec);
        age = age * 1000;
        date = (new Date().getTime()) - age;
        //System.out.println("AGE: "+age);
      }
    }
      HACK */

    // If the server doesn't report a lastModified then flush the cache every month (86400000 is milliseconds in a day)
    // FIXME: this should be configurable
    double cacheAge = new Date().getTime() - cacheDate;
    double maxAge = 30*8.64e+07;
    if (cacheDate < date || cacheDate == 0 || (date == 0 && cacheAge > maxAge)) {
      BufferedReader in = new BufferedReader(
                          new InputStreamReader(
                          request_con.getInputStream()));
      BufferedWriter out = new BufferedWriter(
                           new OutputStreamWriter(
                           new FileOutputStream(cacheFile)));
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
          out.write(inputLine);
          out.newLine();
      }
      in.close();
      out.close();
    }

    // Finally, set the mod time of the cache file if the server reports it
    if (date > 0) cacheFile.setLastModified(date);

    String returnString = "file:///"+cacheFile.getAbsolutePath();
    //the following is needed on Windows to make the path a URL (instead of a Windows path)
    try {
        Pattern p = Pattern.compile("\\\\");
        Matcher m = p.matcher(returnString);
        returnString = m.replaceAll("/");
    }
    catch(PatternSyntaxException e){
        System.out.println(e.getMessage());
    }
    return returnString;

  }
}
