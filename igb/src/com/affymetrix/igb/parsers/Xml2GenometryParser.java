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

package com.affymetrix.igb.parsers;

import java.io.*;
import java.util.*;

import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.apache.xerces.parsers.DOMParser;

import com.affymetrix.genoviz.util.Timer;
import com.affymetrix.genoviz.util.Memer;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.das.DasLoader;

import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SeqSymStartComparator;

import com.affymetrix.igb.util.GenometryViewer;  // for testing main

public class Xml2GenometryParser {
  // optionally skip over populating descriptors -- checking to see how this affects speed
  public static boolean POPULATE_DESCRIPTORS = true;
  public static boolean USE_INTERNING = false;
  public static boolean USE_TIMER = true;
  public static boolean USE_MEMER = true;

  boolean DEBUG = false;
  HashMap mrna_hash;
  HashMap prot_hash;
  //  MutableAnnotatedBioSeq genomic;

  MutableSeqSymmetry all_gene_searches = null;
  // hash of method name to genesearch method symmetry (a SymWithProps whose child
  //    symmetries are all genesearch syms that use the same "method" --
  //    est, rsmrna, sanger, etc.)
  Hashtable method_to_sym = new Hashtable();

  public Xml2GenometryParser() { }

  public MutableAnnotatedBioSeq parse(InputStream istr) {
    return parse(istr, null);
  }

  /**
   * Create a new BioSeq and add annotations to it.
   * @param insource  the InputSource which supplies data for the parser
   */
  public MutableAnnotatedBioSeq parse(InputSource insource) {
    return parse(insource, null);
  }

  /**
   *  This may look a little strange, because the MutableAnnotatedBioSeq returned is
   *     usually going to be the same object as the MutableAnnotatedBioSeq (seq) passed in as
   *     an argument, if seq is non-null.  The intent is that if a new AnnotatedBioSeq is being
   *     read in, then should pass in null for seq argument.  And if results from an AXML file
   *     are being merged with an existing seq, should pass in that seq as argument.  These are
   *     rolled into one method to avoid duplication of code.  This would be cleaner, except
   *     that currently BioSeq ids are immutable.
   */
  public MutableAnnotatedBioSeq parse(InputStream istr, MutableAnnotatedBioSeq seq) {
    MutableAnnotatedBioSeq result = null;
    mrna_hash = new HashMap();
    prot_hash = new HashMap();
    InputSource insrc = null;
    try {
      insrc = new InputSource(istr);
      result = parse(insrc, seq);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return result;
  }

  public MutableAnnotatedBioSeq parse(InputSource insource, MutableAnnotatedBioSeq seq) {
    MutableAnnotatedBioSeq result = null;
    Timer tim = null;
    Memer mem = null;
    System.gc();
    //    try  { Thread.currentThread().sleep(1000); } catch (Exception ex) { }
    if (USE_MEMER)  { mem = new Memer(); }
    if (USE_TIMER)  { tim = new Timer(); }
    boolean success = false;
    try {
      if (USE_TIMER) { tim.start(); }
      if (USE_MEMER)  { mem.printMemory(); }
      System.out.println("converting XML to DOM");
      DOMParser parser = DasLoader.nonValidatingParser();
      DasLoader.doNotDeferNodeExpansion(parser);
      parser.parse(insource);
      Document seqdoc = parser.getDocument();
      System.gc();
      if (USE_MEMER)  { mem.printMemory(); }
      if (USE_TIMER)  {
	System.out.println("Time to convert XML to DOM: " + tim.read()/1000f);
	tim.start();
      }
      System.out.println("converting DOM to genometry");
      result = processDocument(seqdoc, seq);
      seqdoc = null;
      parser = null;
      System.gc();
      if (USE_MEMER) { mem.printMemory(); }
      if (USE_TIMER)  {
	System.out.println("Time to convert DOM to genometry: " + tim.read()/1000f);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    if (result != null) {
      int[] counts = SeqUtils.collectCounts(result);
      System.out.println("counts: syms = " + counts[0] + ", spans = " + counts[1]);
    }
    System.gc();
    return result;
  }

  /*
     <dnaseq>
        <genesearch>
	   <gene>
	      <primarytranscript>
	         <mrna>
		    <exon />
		    <exon />
		    <cds>
		       <cdsseg />
		       <cdsseg />
		    </cds>
		 </mrna>
	      </primarytranscript>
	   </gene>
	</genesearch>
     </dnaseq>
  */

  /**
   *  Process the document.
   *  The document must refer to the "same" sequence as the genomic arg.
   */
  public MutableAnnotatedBioSeq processDocument(Document seqdoc, MutableAnnotatedBioSeq seq) {
    MutableAnnotatedBioSeq genomic = seq;
    String seqid = null;
    String residues = null;
    Element top_element = seqdoc.getDocumentElement();
    int glength = 0;
    String name = top_element.getTagName();
    // REALLY NEED TO ADD CHECKS IN HERE TO VERIFY THAT SEQ AND DOCUMENT REFER TO "SAME"
    //     SEQUENCE (IF SEQ != NULL)
    if (name.equalsIgnoreCase("dnaseq")) {
      if (DEBUG)  { System.out.println("processing dna seq"); }

      /** if have residues, then ignore the dnaseq's length attribute */
      NodeList children = top_element.getChildNodes();
      for (int i=0; i<children.getLength(); i++) {
	Node child = children.item(i);
	String cname = child.getNodeName();
	// System.out.println(name);
	if (cname != null && cname.equalsIgnoreCase("residues")) {
	  Text resnode = (Text)child.getFirstChild();
	  String temp_residues = resnode.getData();
	  int length = temp_residues.length();
	  System.out.println("seq length: " + length);
	  // looks weird, but side effect is to trim new String's internal char array --
	  //  then can garbage collect temp_residues later, and hopefully will save space...
	  residues = new String(temp_residues);
	  temp_residues = null;
	}
	if (cname != null && cname.equalsIgnoreCase("identifier")) {
	  //	  System.out.println("TESTING IDENTIFIER");
	  Element idelem = (Element)child;
	  if (idelem.getAttribute("preferred") == null ||
	      idelem.getAttribute("preferred").equalsIgnoreCase("TRUE")) {
	    seqid = idelem.getAttribute("name");
	    System.out.println("id: " + seqid);
	  }
	}
      }
      try {
	glength = Integer.parseInt(top_element.getAttribute("length"));
	System.out.println("no residues, but length specified: " + glength);
      }
      catch (Exception ex) {
	System.out.println("problem with dnaseq length attribute, arbitrarily assigning 200000");
	glength = 0;
      }

      MutableAnnotatedBioSeq current_seq = null;

      if (genomic == null) {
	//	genomic = new SimpleAnnotatedBioSeq("genome", glength);
	if (seqid == null) { seqid = "unknown"; }
	if (residues == null) {
	  // assign arbitrary length to sequence if no length or residues
	  if (glength == 0) { glength = 200000; }
	  genomic = new SimpleAnnotatedBioSeq(seqid, glength);
	}
	else {
	  genomic = new SimpleAnnotatedBioSeq(seqid, residues);
	}
	current_seq = genomic;
      }
      else {
	// should maybe check for sequence id matches here???
	if (seqid.equals(genomic.getID())) {
	  current_seq = genomic;
	}
	else {
	  System.out.println("trying to merge but seq ids don't match, checking composition: " +
			     "current id = " + seqid + ", previous id = " + genomic.getID());
	  if (genomic instanceof CompositeBioSeq) {
	    CompositeBioSeq cgenomic = (CompositeBioSeq)genomic;
	    SeqSymmetry comp = cgenomic.getComposition();
	    // need to switch this to a full recursion...
	    int scount = comp.getChildCount();
	    for (int i=0; i<scount; i++) {
	      SeqSymmetry csym = comp.getChild(i);
	      // return seq in a symmetry span that _doesn't_ match genomic
	      BioSeq cseq = SeqUtils.getOtherSeq(csym, genomic);
	      if (cseq.getID().equals(seqid) && (cseq instanceof MutableAnnotatedBioSeq)) {
		System.out.println("got a composition match, resetting seq to annotate to: " + seqid);
		current_seq = (MutableAnnotatedBioSeq)cseq;
		break;
	      }
	    }
	  }
	}
      }
      if (current_seq == null) {
	System.out.println("couldn't perform merge, no seq id match");
	return null;
      }
      processDNASeq(current_seq, top_element);
    }
    return genomic;
  }


  public void processDNASeq(MutableAnnotatedBioSeq genomic, Element elem) {
    // clearing method_to_sym tabel (just in case, this might
    //  help with multiple DNAs in same XML doc?
    method_to_sym = new Hashtable();
    NodeList children = elem.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      // System.out.println(name);
      if (name != null) {
	if (name.equalsIgnoreCase("genesearch")) {
	  if (all_gene_searches == null) {
	    all_gene_searches = new SimpleSymWithProps();
	  }
	  processGeneSearch(genomic, (Element)child);
	}
	// residues are now dealt with directly in processDocument() method
	//	else if (name.equalsIgnoreCase("residues")) {
	//	  processResidues(genomic, (Element)child);
	//	}
	else if (name.equalsIgnoreCase("repeatsearch")) {
	  // not processing repeats yet
	  //	  System.out.println(
	  processRepeatSearch(genomic, (Element)child);
	}
	else if (name.equalsIgnoreCase("adhocanalysis")) {
	  processAdHocAnalysis(genomic, (Element)child);
	}
      }
    }
    if (all_gene_searches != null) {
      // add genesearch-by-method symmetries to all_gene_searches
      Enumeration method_syms = method_to_sym.elements();
      while (method_syms.hasMoreElements()) {
	MutableSeqSymmetry method_sym = (MutableSeqSymmetry)method_syms.nextElement();
	if (method_sym.getChildCount() <= 0) { continue; }
	SeqSpan methSpan = SeqUtils.getChildBounds(method_sym, genomic);
	method_sym.addSpan(methSpan);
	all_gene_searches.addChild(method_sym);
      }

      SeqSpan searchSpan = SeqUtils.getChildBounds(all_gene_searches, genomic);
      all_gene_searches.addSpan(searchSpan);

      genomic.addAnnotation(all_gene_searches);
    }

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      // System.out.println(name);
      if (name != null && name.equalsIgnoreCase("aaseq")) {
	processProtein(genomic, (Element)child);
      }
    }

    // adjusting length of genomic if annotations extend beyond genomic range...
    // WARNING: if residues have already been set, and an annotation forces genomic to
    //     extend beyond residues, residues will be nulled out!
    int annotCount = genomic.getAnnotationCount();
    for (int i=0; i<annotCount; i++) {
      SeqSymmetry sym = genomic.getAnnotation(i);
      SeqSpan span = sym.getSpan(genomic);
      int max = span.getMax();
      if (max > genomic.getLength()) {
	genomic.setLength(max);
      }
    }
  }

  public void processProtein(MutableAnnotatedBioSeq genomic, Element elem) {
    String pid = elem.getAttribute("id");
    MutableAnnotatedBioSeq protein = (MutableAnnotatedBioSeq)prot_hash.get(pid);
    if (DEBUG)  { System.out.println("aaseq: id = " + pid + ",  " + protein); }

    NodeList children = elem.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (name != null && name.equalsIgnoreCase("simsearch")) {
	processSimSearch(protein, (Element)child);
      }
    }
  }

  public void processSimSearch(MutableAnnotatedBioSeq query_seq, Element elem) {
    NodeList children = elem.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (name != null && name.equalsIgnoreCase("simhit")) {
	processSimHit(query_seq, (Element)child);
      }
    }
  }

  public void processSimHit(MutableAnnotatedBioSeq query_seq, Element elem) {
    SimpleSymWithProps hitSym = new SimpleSymWithProps();
    addDescriptors(elem, hitSym);

    hitSym.setProperty("name", elem.getAttribute("name"));
    hitSym.setProperty("description", elem.getAttribute("desc"));
    /*
        String name = elem.getAttribute("name");
	if (name != null & name.length() > 0) {
          hitSym.setProperty("name", name);
        }
    }
    */

    SeqSpan hitSpan = null;
    NodeList children = elem.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();

      if (child instanceof Element)  {
	//	System.out.println(name);
	Element chelem = (Element)child;
	if (name.equalsIgnoreCase("simspan")) {
	  SeqSymmetry spanSym = processSimSpan(query_seq, chelem);
	  hitSym.addChild(spanSym);
	  SeqSpan spanSpan = spanSym.getSpan(query_seq);
	  if (hitSpan == null) {
	    hitSpan = new SimpleSeqSpan(spanSpan.getStart(), spanSpan.getEnd(), query_seq);
	  }
	  else {
	    hitSpan = SeqUtils.encompass(hitSpan, spanSpan);
	  }
	}
      }
    }

    hitSym.addSpan(hitSpan);
    query_seq.addAnnotation(hitSym);
    //    SeqUtils.printSymmetry(hitSym);
  }

  /**
   *  Handles both <descriptor> and <identifier> tags.
   *  Currently puts both in sym's properties.
   *  may at some point want to change so id is set for mapped sequence rather
   *    than the sym itself.
   */
  public void addDescriptors(Element elem, Propertied sym) {
    if (! POPULATE_DESCRIPTORS) { return; }
    NodeList children = elem.getChildNodes();
    int idcount = 0;
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();

      if (child instanceof Element)  {
	//	System.out.println(name);
	Element chelem = (Element)child;
    	if (name.equalsIgnoreCase("descriptor")) {
	  String desc_name = chelem.getAttribute("type");
	  Text tnode = (Text)chelem.getFirstChild();
	  if (tnode != null) {
	    String desc_text = tnode.getData();
	    sym.setProperty(desc_name, desc_text);
	  }
	}
	else if (name.equalsIgnoreCase("identifier")) {
	  String id_name = chelem.getAttribute("name");
	  String pref = chelem.getAttribute("preferred");
	  if (pref == null || pref.equalsIgnoreCase("TRUE")) {
	    sym.setProperty("id", id_name);
	  }
	  else {
	    String prop_name = "alt_id" + idcount;
	    sym.setProperty(prop_name, id_name);
	    idcount++;
	  }
	}
      }
    }
  }

  public SeqSymmetry processSimSpan(MutableAnnotatedBioSeq query_seq, Element elem) {
    int start = Integer.parseInt(elem.getAttribute("query_start")) - 1;
    int end;
    // blech!  need to standardize on which tag to use!
    // also, should be able to use Element.hasAttribute() to test here, but
    //  compiler is saying this method doesn't exist -- there may be some disagreement
    //  between DOM API I'm referring to and DOM implementation I'm actually using
    //  (the docs say Element.hasAttribute() is part of "DOM level 2" interface)
    try {
      end = Integer.parseInt(elem.getAttribute("query_end")) - 1;
    } catch (Exception ex) {
      end = Integer.parseInt(elem.getAttribute("query_stop")) - 1;
    }
    if (start < end) { end++; } else { start++; }
    //    System.out.println("simspan:  start = " + start + "  end = " + end);
    SimpleSymWithProps spanSym = new SimpleSymWithProps();
    addDescriptors(elem, spanSym);

    SeqSpan qspan = new SimpleSeqSpan(start, end, query_seq);
    spanSym.addSpan(qspan);
    return spanSym;
  }

  public void processAdHocAnalysis(MutableAnnotatedBioSeq genomic, Element elem) {
    SimpleSymWithProps analysis = new SimpleSymWithProps();
    String meth = elem.getAttribute("method");
    if (meth == null) { meth = "adhoc unknown"; }
    if (USE_INTERNING) { analysis.setProperty("method", meth.intern()); }
    else  { analysis.setProperty("method", meth); }

    analysis.setProperty("type", "adhocanalysis");
    NodeList children = elem.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String nodename = child.getNodeName();
      if (null != nodename && nodename.equalsIgnoreCase("adhocfeature")) {
	processAdHocFeature(genomic, analysis, (Element)child);
      }
    }
    SeqSpan analysis_span = SeqUtils.getChildBounds(analysis, genomic);
    analysis.addSpan(analysis_span);
    genomic.addAnnotation(analysis);
    System.out.println("adhoc features: " + analysis.getChildCount());
  }

  public void processRepeatSearch(MutableAnnotatedBioSeq genomic, Element elem) {
    SimpleSymWithProps repeat_search = new SimpleSymWithProps();
    repeat_search.setProperty("method", "repeats");
    NodeList children = elem.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String nodename = child.getNodeName();
      if (null != nodename && nodename.equalsIgnoreCase("repeat")) {
	processRepeat(genomic, repeat_search, (Element)child);
      }
    }
    SeqSpan rs_span = SeqUtils.getChildBounds(repeat_search, genomic);
    repeat_search.addSpan(rs_span);
    genomic.addAnnotation(repeat_search);
    System.out.println("repeats: " + repeat_search.getChildCount());
  }

  public void processAdHocFeature(MutableAnnotatedBioSeq genomic, MutableSeqSymmetry parentSym,
			    Element elem) {
    int start = Integer.parseInt(elem.getAttribute("start")) - 1;
    int end = Integer.parseInt(elem.getAttribute("end")) - 1;
    if (start < end) { end++; } else { start++; }
    SeqSymmetry featSym = new SingletonSeqSymmetry(start, end, genomic);
    parentSym.addChild(featSym);
  }

  public void processRepeat(MutableAnnotatedBioSeq genomic, MutableSeqSymmetry parentSym,
			    Element elem) {
    int start = Integer.parseInt(elem.getAttribute("start")) - 1;
    int end = Integer.parseInt(elem.getAttribute("end")) - 1;
    if (start < end) { end++; } else { start++; }
    SeqSymmetry repeatSym = new SingletonSeqSymmetry(start, end, genomic);
    parentSym.addChild(repeatSym);
  }

  public void processGeneSearch(MutableAnnotatedBioSeq genomic, Element elem) {

    //    System.out.println("processing gene search");
    //    Integer id = getInteger(n.getAttribute("id"));
    //    String basis = n.getAttribute("basis");
    //    String method = n.getAttribute("method");
    SimpleSymWithProps gene_search = new SimpleSymWithProps();
    String meth = elem.getAttribute("method");
    if (USE_INTERNING) { gene_search.setProperty("method", meth.intern()); }
    else  { gene_search.setProperty("method", meth); }

    NodeList children = elem.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String nodename = child.getNodeName();
      if (null != nodename && nodename.equalsIgnoreCase("gene")) {
	processGene(genomic, gene_search, (Element)child);
      }
    }

    SeqSpan searchSpan = SeqUtils.getChildBounds(gene_search, genomic);
    gene_search.addSpan(searchSpan);

    // trying to put genesearch data into symmetries as
    //     children of a symmetry that encompasses all genesearches
    //     done with same method (which in turn is held by a
    //     parent symmetry that represents _all_ gene searches)
    SimpleSymWithProps method_sym;
    if (method_to_sym.containsKey(meth)) {
      method_sym = (SimpleSymWithProps)method_to_sym.get(meth);
    }
    else {
      method_sym = new SimpleSymWithProps();
      method_to_sym.put(meth, method_sym);
    }
    method_sym.addChild(gene_search);
    //    all_gene_searches.addChild(gene_search);
    //    genomic.addAnnotation(gene_search);
  }

  public void processGene(MutableAnnotatedBioSeq genomic, MutableSeqSymmetry parentSym, Element elem) {
    int start = Integer.parseInt(elem.getAttribute("start")) - 1;
    int end = Integer.parseInt(elem.getAttribute("end")) - 1;
    if (start < end) { end++; } else { start++; }
    if (DEBUG)  { System.out.println("gene:  start = " + start + "  end = " + end); }

    MutableSeqSymmetry gene_sym = new SimpleMutableSeqSymmetry();

    NodeList children = elem.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String nodename = child.getNodeName();
      if (null != nodename && nodename.equalsIgnoreCase("primarytranscript")) {
	//	processTranscript(genomic, parentSym, (Element)child);
	processTranscript(genomic, gene_sym, (Element)child);
      }
    }
    gene_sym.addSpan(SeqUtils.getChildBounds(gene_sym, genomic));
    parentSym.addChild(gene_sym);
  }

  public void processTranscript(MutableAnnotatedBioSeq genomic, MutableSeqSymmetry parentSym, Element elem) {
    int start = Integer.parseInt(elem.getAttribute("start")) - 1;
    int end = Integer.parseInt(elem.getAttribute("end")) - 1;
    if (start < end) { end++; } else { start++; }
    if (DEBUG)  { System.out.println("transcript:  start = " + start + "  end = " + end); }
    NodeList children = elem.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String nodename = child.getNodeName();
      if (null != nodename && nodename.equalsIgnoreCase("mrna")) {
	processMRNA(genomic, parentSym, (Element)child);
      }
    }
  }

  public void processMRNA(MutableAnnotatedBioSeq genomic, MutableSeqSymmetry parentSym, Element elem) {
    int start = Integer.parseInt(elem.getAttribute("start")) - 1;
    int end = Integer.parseInt(elem.getAttribute("end")) - 1;
    if (start < end) { end++; } else { start++; }
    if (DEBUG)  { System.out.println("mrna:  start = " + start + "  end = " + end); }
    NodeList children = elem.getChildNodes();
    SeqSpan span = new SimpleSeqSpan(start, end, genomic);

    SimpleSymWithProps m2gSym = new SimpleSymWithProps();
    addDescriptors(elem, m2gSym);
    m2gSym.addSpan(span);
    boolean forward = (span.isForward());


    List exon_list = new ArrayList();
    List exon_insert_list = new ArrayList();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String nodename = child.getNodeName();
       if (nodename != null) {
	if (nodename.equalsIgnoreCase("exon")) {
	  SeqSymmetry exSym = processExon(genomic, (Element)child);
	  exon_list.add(exSym);
	}
	else if (nodename.equalsIgnoreCase("exon_insert")) {
	  exon_insert_list.add(child);
	}
	//	else if (nodename.equalsIgnoreCase("cds")) {
	//	  processCDS(seq, (Element)child, m2gSym);
	//	}
      }
    }

    // NEED TO SORT EXON_INSERTS!!!
    //    5' TO 3' ALONG TRANSCRIPT.  OTHERWISE TRYING TO INSERT A 5' ONE
    //    AFTER A 3' ONE HAS ALREADY BEEN INSERTED WILL MESS UP COORDINATES OF 3' ONE
    // NOT YET IMPLEMENTED -- ASSUMING FOR NOW THAT EXON_INSERTS ARE ALREADY ORDERED IN THE XML

    // sorting exons, so that later position calculations are accurate
    Collections.sort(exon_list, new SeqSymStartComparator(genomic, forward));

    //    System.out.println("=======================");
    for (int i=0; i<exon_list.size(); i++) {
      SeqSymmetry esym = (SeqSymmetry)exon_list.get(i);
      //      SeqUtils.printSymmetry(esym);
      m2gSym.addChild(esym);
    }
    //    System.out.println("=======================");

    int exoncount = m2gSym.getChildCount();
    int mrnalength = 0;
    for (int i=0; i<exoncount; i++) {
      SeqSymmetry esym = m2gSym.getChild(i);
      SeqSpan gspan = esym.getSpan(genomic);
      mrnalength += gspan.getLength();
    }
    //    System.out.println("mrna length, pre-insert: " + mrnalength);

    if (exon_insert_list.size() > 0) {
      for (int i=0; i<exon_insert_list.size(); i++) {
	Element iel = (Element)exon_insert_list.get(i);
	//	int istart = Integer.parseInt(iel.getAttribute("insert_at"));
	int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
	mrnalength += ilength;
      }
    }
    //    System.out.println("mrna length, post-insert: " + mrnalength);
    //    System.out.println("spliced mrna length: " + mrnalength);

    start = 0;
    end = 0;
    String mrna_id = "mrna";
    MutableAnnotatedBioSeq mrna = new SimpleAnnotatedBioSeq(mrna_id, mrnalength);
    mrna_hash.put(mrna_id, mrna);
    SeqSpan mrna_span = new SimpleSeqSpan(0, mrnalength, mrna);
    m2gSym.addSpan(mrna_span);

    for (int i=0; i<exoncount; i++) {
      SimpleSymWithProps esym = (SimpleSymWithProps)m2gSym.getChild(i);
      SeqSpan gspan = esym.getSpan(genomic);
      end = start + gspan.getLength();
      Vector hit_inserts = new Vector();

      /** check each exon_insert, figure out which (if any) exons it overlaps */
      if (exon_insert_list.size() > 0) {
	for (int insert_index=0; insert_index<exon_insert_list.size(); insert_index++) {
	  Element iel = (Element)exon_insert_list.get(insert_index);
	  int istart = Integer.parseInt(iel.getAttribute("insert_at"));
	  int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
	  if (SeqUtils.contains(gspan, istart)) {
	    // need to add children to this exon symmetry to indicate an insertion
	    //   (or possibly deletion?) of bases in the transcript relative to the genomic
	    //	    processExonInsert(esym, istart, ilength);
	    System.out.println("insert: insertion_start = " + istart + ", length = " + ilength);
	    // remove this exon_insert from list to consider in future passes
	    //    need to also decrement the insert_index to make sure removal doesn't cause
	    //    next exon_insert to not be considered...
	    exon_insert_list.remove(insert_index);
	    hit_inserts.add(iel);
	    insert_index--;
	    end += ilength;
	  }

	  //	MutableSeqSymmetry isym = new SimpleMutableSeqSymmetry();
	  //	SeqSpan ispan = new SimpleSeqSpan(istart, istart + ilength, mrna);
	  //	isym.addSpan(ispan);
	}
      }

      SeqSpan tspan = new SimpleSeqSpan(start, end, mrna);
      esym.addSpan(tspan);

      //      System.out.println("hit inserts: " + hit_inserts.size());
      if (hit_inserts.size() > 0)  {
	processExonInsert(esym, hit_inserts, genomic, mrna);
      }

      start = end;
    }

    String protein_id = null;

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String nodename = child.getNodeName();
      if (nodename != null && nodename.equalsIgnoreCase("descriptor")) {
	Element el = (Element)child;
	String type = el.getAttribute("type");
	if (type != null && type.equalsIgnoreCase("protein_product_id")) {
	  Text tnode = (Text)el.getFirstChild();
	  protein_id = tnode.getData();
	  break;
	}
      }
    }

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String nodename = child.getNodeName();
      if (nodename != null) {
	if (nodename.equalsIgnoreCase("cds")) {
	  processCDS(genomic, (Element)child, m2gSym, mrna, protein_id);
	}
      }
    }

    //    genomic.addAnnotation(m2gSym);
    parentSym.addChild(m2gSym);
    mrna.addAnnotation(m2gSym);

    //    SeqUtils.printSymmetry(m2gSym);
  }


  public void processExonInsert(MutableSeqSymmetry exonSym, Vector hit_inserts,
				BioSeq genomic, BioSeq mrna) {
    // assumes that hit_inserts are in order 5' to 3' along transcript
    // assumes that each exon_insert in hit_inserts actually is contained in the exon
    // assumes that the genomic and transcript spans of the exon are already
    //       part of the exonSym and that the transcript span already correctly takes into account
    //       the additional bases introduced by the exon inserts

    //   map from genomic coords over to transcript coords to figure out where to "split" the
    //       exonSym into children

    SeqSpan egSpan = exonSym.getSpan(genomic);
    SeqSpan etSpan = exonSym.getSpan(mrna);

    int genStart = egSpan.getStart();
    int transStart = etSpan.getStart();

    for (int insert_index=0; insert_index<hit_inserts.size(); insert_index++) {
      Element iel = (Element)hit_inserts.get(insert_index);
      int istart = Integer.parseInt(iel.getAttribute("insert_at"));
      int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
      int genLength = Math.abs(istart - genStart);
      int transEnd = transStart + genLength;

      // split out exon seg between last insert (or start of exon) and current insert
      //   [unless start of exon and the insert is actually at exact beginning of exon]
      if (istart != genStart) {
	MutableSeqSymmetry segSym = new SimpleMutableSeqSymmetry();
	SeqSpan gSpan = new SimpleSeqSpan(genStart, istart, genomic);  // start of insert is end of exon seg
	SeqSpan tSpan = new SimpleSeqSpan(transStart, transEnd, mrna);
	segSym.addSpan(gSpan);
	segSym.addSpan(tSpan);
	exonSym.addChild(segSym);
      }
      // now add exon seg for the current insert
      transStart = transEnd;
      transEnd += ilength;
      SeqSpan insert_tspan = new SimpleSeqSpan(transStart, transEnd, mrna);
      SeqSpan insert_gspan = new SimpleSeqSpan(istart, istart, genomic);
      MutableSeqSymmetry isegSym = new SimpleMutableSeqSymmetry();
      isegSym.addSpan(insert_tspan);
      // experimenting with adding a zero-length placeholder for exon insert relative to genomic
      isegSym.addSpan(insert_gspan);
      exonSym.addChild(isegSym);

      // set current genomic start point for next loop to location of current insert
      genStart = istart;
      transStart = transEnd;

    }

    // if last insert is not _exactly_ at end of exon, then need to add last exon seg
    //   after finished looping through inserts
    if (genStart != egSpan.getEnd()) {
      SeqSpan gSpan = new SimpleSeqSpan(genStart, egSpan.getEnd(), genomic);
      SeqSpan tSpan = new SimpleSeqSpan(transStart, etSpan.getEnd(), mrna);
      MutableSeqSymmetry endSym = new SimpleMutableSeqSymmetry();
      endSym.addSpan(gSpan);
      endSym.addSpan(tSpan);
      exonSym.addChild(endSym);
    }
  }


  public SimpleSymWithProps processExon(MutableAnnotatedBioSeq genomic, Element elem)  {
    // should not be any nodes underneath exon tags (at least in current pseudo-DTD)
    int start = Integer.parseInt(elem.getAttribute("start")) - 1;
    int end = Integer.parseInt(elem.getAttribute("end")) - 1;
    if (start < end) { end++; } else { start++; }

    //    System.out.println("exon:  start = " + start + "  end = " + end);
    SeqSpan span = new SimpleSeqSpan(start, end, genomic);
    SimpleSymWithProps exonsym = new SimpleSymWithProps();
    addDescriptors(elem, exonsym);

    exonsym.addSpan(span);
    return exonsym;
    //    m2gSym.addChild(exonsym);
  }

  public void processCDS(MutableAnnotatedBioSeq genomic, Element elem, SeqSymmetry m2gSym,
			 MutableAnnotatedBioSeq mrna, String protein_id) {
    int start = Integer.parseInt(elem.getAttribute("transstart")) - 1;
    /*
       because transstop currently indicates first base of stop codon (rather than
       last base of actual translation), forgoing the usual +1 addition...
     */
    int end = Integer.parseInt(elem.getAttribute("transstop")) - 1;
    if (end < start)  { start++; end++; }

    // could just do this as a single seq span (start, end, seq), but then would end up recreating
    //   the cds segments, which will get ignored afterwards...
    SeqSpan gstart_point = new SimpleSeqSpan(start, start, genomic);
    SeqSpan gend_point = new SimpleSeqSpan(end, end, genomic);
    SimpleSymWithProps result = new SimpleSymWithProps();

    result.addSpan(gstart_point);
    SeqSymmetry[] m2gPath = new SeqSymmetry[] { m2gSym };

    SeqUtils.transformSymmetry(result, m2gPath);
    SeqSpan mstart_point = result.getSpan(mrna);

    result = new SimpleSymWithProps();
    result.addSpan(gend_point);
    SeqUtils.transformSymmetry(result, m2gPath);
    SeqSpan mend_point = result.getSpan(mrna);

    SimpleSymWithProps m2pSym = new SimpleSymWithProps();
    SeqSpan mspan = new SimpleSeqSpan(mstart_point.getStart(), mend_point.getEnd(), mrna);

    MutableAnnotatedBioSeq protein = new SimpleAnnotatedBioSeq(protein_id, mspan.getLength()/3);
    prot_hash.put(protein_id, protein);
    SeqSpan pspan = new SimpleSeqSpan(0, protein.getLength(), protein);
    if (DEBUG)  { System.out.println("protein: length = " + pspan.getLength()); }
    m2pSym.addSpan(mspan);
    m2pSym.addSpan(pspan);

    protein.addAnnotation(m2pSym);
    mrna.addAnnotation(m2pSym);

  }

  public static void main(String[] args) {
    Xml2GenometryParser test = new Xml2GenometryParser();
    String file_name = args[0];
    try {
      File fl = new File(file_name);
      FileInputStream fistr = new FileInputStream(fl);
      MutableAnnotatedBioSeq seq = test.parse(fistr, null);
      int acount = seq.getAnnotationCount();
      System.out.println("Annotation count: " + acount);
      GenometryViewer viewer = GenometryViewer.displaySeq(seq, false);
      viewer.setPrintSelection(false);
      viewer.setAnnotatedSeq(seq);
    }
    catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
    System.exit(0);
  }


}
