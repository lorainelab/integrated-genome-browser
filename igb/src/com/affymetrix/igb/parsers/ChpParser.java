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
package com.affymetrix.igb.parsers;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import java.io.*;
import java.util.*;
import java.util.List;

import affymetrix.fusion.chp.*;
import affymetrix.calvin.data.*;
import affymetrix.calvin.utils.*;
import affymetrix.calvin.parameter.ParameterNameValue;

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.ScoredContainerSym;
import com.affymetrix.genometryImpl.IndexedSingletonSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;

import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.das2.Das2Region;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.das2.Das2VersionedSource;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.genometry.LazyChpSym;
import com.affymetrix.igb.menuitem.OpenGraphAction;
import com.affymetrix.igb.util.QuantByIntIdComparator;
import com.affymetrix.igb.util.QuantDetectByIntIdComparator;


public final class ChpParser {
  static boolean DEBUG = false;
  static boolean reader_registered = false;
  //  public static void parse(InputStream str, AnnotatedSeqGroup seq_group, String stream_name) {
  // }

  public static List parse(String file_name) throws IOException {
    //    Timer tim = new Timer();
    //    tim.start();
    List results = null;
    if (! (reader_registered)) {
      //    FusionCHPLegacyData.registerReader();
      //    FusionCHPGenericData.registerReader();
      FusionCHPTilingData.registerReader();
      FusionCHPQuantificationData.registerReader();
      FusionCHPQuantificationDetectionData.registerReader();
      FusionCHPLegacyData.registerReader();
      reader_registered = true;
    }

    File testfil = new File(file_name);
    System.out.println("Parsing CHP file: " + file_name);

    FusionCHPData chp = FusionCHPDataReg.read(file_name);
    if (chp == null) { return null; }

    AffymetrixGuidType chp_type =  chp.getFileTypeIdentifier();
    String chp_type_name = chp_type.getGuid();
    System.out.println("CHP file type guid: " + chp_type_name);

    // The following function will determine if the CHP file read contains "legacy" format data. This
    // can be either a GCOS/XDA file or a Command Console file. The "legacy" format data is that
    // which contains a signal, detection call, detection p-value, probe pairs, probe pairs used and
    // comparison results if a comparison analysis was performed. This will be from the MAS5 algorithm.
    // Note: The file may also contain genotyping results from the GTYPE software. The ExtractData function
    // will perform a check to ensure it is an expression CHP file.

    FusionCHPQuantificationData qchp;
    FusionCHPQuantificationDetectionData qdchp;
    FusionCHPTilingData tilechp;
    FusionCHPLegacyData legchp;
    FusionCHPGenericData genchp;
    boolean has_coord_data = false;

    /** For all chips other than tiling (and potentially resequencing?), the genomic location of the
     *   probesets is not specified in the CHP file.  Therefore it needs to be obtained from another
     *   source, based on info that _is_ in the CHP file and the current genome/AnnotatedSeqGroup (or
     *   most up-to-date genome for the organism if current genome is not for same organism as CHP file data
     *
     *  Plan is to get this data from DAS/2 server in as optimized a format as possible -- for instance,
     *     "bp2" format for exon chips.  It may be possible to optimize formats even further if parser
     *     can assume a particular ordering of data in CHP file will always be followed for a particular
     *     Affy chip, but I don't think we can make that assumption...
     *  Therefore will have to join location info with CHP info based on shared probeset IDs.
     */
    /** expression CHP file (gene or WTA), without detection */
    if ((qchp = FusionCHPQuantificationData.fromBase(chp))  != null) {
      System.out.println("CHP file is for expression array, without detection info: " + qchp);
      results = parseQuantChp(qchp);
    }
    /** expression CHP file (gene or WTA), with detection */
    else if ((qdchp = FusionCHPQuantificationDetectionData.fromBase(chp))  != null) {
      System.out.println("CHP file is for expression array, with detection info: " + qdchp);
      results = parseQuantDetectChp(qdchp);
    }
    /** tiling CHP file */
    else if ((tilechp = FusionCHPTilingData.fromBase(chp)) != null) {
      System.out.println("CHP file is for tiling array: " + tilechp);
      results = parseTilingChp(tilechp);
      has_coord_data = true;
    }
    /** legacy data */
    else if ((legchp = FusionCHPLegacyData.fromBase(chp)) != null) {
      System.out.println("CHP file is for legacy data: " + legchp);
      results = parseLegacyChp(legchp);
    }
    else if ((genchp = FusionCHPGenericData.fromBase(chp))  != null) {
      System.out.println("CHP file is generic: " + genchp);
      //      results = parseGenericChp(genchp);
      System.out.println("WARNING: generic CHP files currently not supported in IGB");
      Application.errorPanel("CHP file is in generic format, cannot be loaded");
    }
    else {
      System.out.println("WARNING: not parsing file, CHP file type not recognized: " + chp);
      Application.errorPanel("CHP file type not recognized, cannot be loaded");
    }
    if (! has_coord_data) {
      /**
       *  make lazy stub annotations for each sequence in genome
       *
       */
    }

    //    System.out.println("Time to load CHP file (etc.): " + tim.read()/1000f);
    return results;
  }

  public static List<GraphSym> parseTilingChp(FusionCHPTilingData tchp)  {
    return parseTilingChp(tchp, true);
  }

  public static List<GraphSym> parseTilingChp(FusionCHPTilingData tchp, boolean annotate_seq) {
    return parseTilingChp(tchp, annotate_seq, true);
  }

  /**
     *  Want to automatically load location data for probesets on chip
     *
     *  Needed for:
     *      3' IVT Expression chips
     *      Exon chips and other expression that is non-3' IVT
     *      Genotyping chips
     *
     *  Not necessary for tiling array chips
     *  Probably not necessary for sequencing chips (but those aren't supported yet)
     *
     *  Basic strategy is to retrieve probeset id and location data from the main public Affymetrix DAS/2 server,
     *     and match by id to associate locations with the probeset results.
     *
     */
  protected static List<LazyChpSym> makeLazyChpSyms(String file_name, String chp_array_type, Map id2data, Map name2data, List int_entries) {
    //    Timer tim2 = new Timer();
    //    tim2.start();
    SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();

		GenericServer gServer = ServerList.getServer(LazyChpSym.PROBESET_SERVER_NAME);
    // Don't make any LazyChpSyms if can't find the appropriate genome on the DAS/2 server
    if (gServer == null) {
      Application.errorPanel("Couldn't find server to retrieve location data for CHP file, server = " + LazyChpSym.PROBESET_SERVER_NAME);
      return null;
    }
		Das2ServerInfo server = (Das2ServerInfo)gServer.serverObj;
    
    // Don't make any LazyChpSyms if can't find the appropriate genome on the DAS/2 server
    if (server == null) {
      Application.errorPanel("Couldn't find server to retrieve location data for CHP file, server = " + LazyChpSym.PROBESET_SERVER_NAME);
      return null;
    }
    Das2VersionedSource vsource = server.getVersionedSource(group);
    if (vsource == null) {
      Application.errorPanel("Couldn't find genome data on server for CHP file, genome = " + group.getID());
      return null;
    }

    String type_name = OpenGraphAction.getGraphNameForFile(file_name);
    // Force the AnnotStyle for the container to have glyph depth of 1
    IAnnotStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(type_name);
    style.setGlyphDepth(1);

    List<LazyChpSym> results = new ArrayList<LazyChpSym>();
    int scount = group.getSeqCount();
    for (int i=0; i<scount; i++) {
      BioSeq aseq = group.getSeq(i);
      //String seqid = aseq.getID();
      // Don't make LazyChpSym if can't find sequence on DAS/2 server
      Das2Region das_segment = vsource.getSegment(aseq);
      // I think above test for presence of sequence on server will handle skipping the genome and encode regions
      //  (at least as long as the DAS/2 coord server does not serve virtual seqs for these)
      if (das_segment != null) {
	// LazyChpSym constructor handles adding span to itself for aseq
	LazyChpSym chp_sym = new LazyChpSym(aseq, chp_array_type, id2data, name2data, int_entries);
	chp_sym.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq));
	chp_sym.setProperty("method", type_name);
        chp_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
	chp_sym.setID(type_name);
	aseq.addAnnotation(chp_sym);
	results.add(chp_sym);
      }
    }
    //    System.out.println("Time to build LazyChpSyms: " + tim2.read()/1000f);
    return results;
  }


  /** same as parseQuantChp, but adding detection/pval */
  public static List<LazyChpSym> parseQuantDetectChp(FusionCHPQuantificationDetectionData chp) {
    //SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    //AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
    List<LazyChpSym> results = null;
    String file_name = chp.getFileName();
    String algName = chp.getAlgName();
    String algVersion = chp.getAlgVersion();
    String array_type = chp.getArrayType();
    int ps_count = chp.getEntryCount();
    //    Map name2data = new HashMap(ps_count);
    Map id2data = new HashMap(ps_count);
    List<ProbeSetQuantificationDetectionData> int_entries = new ArrayList<ProbeSetQuantificationDetectionData>(ps_count);
    int int_id_count = 0;
    int str_id_count = 0;
    System.out.println("array type: " + array_type + ", alg name = " + algName + ", version = " + algVersion);
    System.out.println("probeset count: " + ps_count);
    ProbeSetQuantificationDetectionData psqData;

    String type_name = OpenGraphAction.getGraphNameForFile(file_name);

    for (int i=0; i<ps_count; i++) {
      psqData = chp.getQuantificationDetectionEntry(i);
      float quant = psqData.getQuantification();
      float pval = psqData.getPValue();
      int intid = psqData.getId();
      String name = null;
      Integer nid = null;
      if (DEBUG && (i<4 || i>=(ps_count-4))) {
      	System.out.println("preprocessed, id: " + intid + ", name: " + psqData.getName() + ", quant: " + quant + ", pval: " + pval);
      }
      if (intid >= 0) {
	nid = new Integer(intid);
	psqData.setName(null);
	int_entries.add(psqData);
	//	id2data.put(nid, psqData);
	int_id_count++;
      }
      else {  // nid < 0, then nid field not being used, so name should be used instead
	name = psqData.getName();
	try {
	  nid = new Integer(name);
	  intid = nid.intValue();
	  psqData.setId(intid);
	  psqData.setName(null);
	  int_entries.add(psqData);
	  //	  id2data.put(nid, psqData);
	  int_id_count++;
	}
	catch (Exception ex) {
	  // can't parse as an integer
	  //	  name2data.put(name, psqData);
	  str_id_count++;
	}
      }
      if (DEBUG && (i<4 || i>=(ps_count-4)))  {
        System.out.println(" post, id: " + psqData.getId() + ", name: " + psqData.getName() + ", quant: " + quant + ", pval: " + pval);
      }
    }
    // sort by int_entries int id
    QuantDetectByIntIdComparator comp = new  QuantDetectByIntIdComparator();
    Collections.sort(int_entries, comp);
    if (int_id_count > 0) {
      System.out.println("Probsets with integer id: " + int_id_count);
      System.out.println("Probsets with string id: " + str_id_count);
      System.out.println("done parsing quantification + detection CHP file");
      results = ChpParser.makeLazyChpSyms(type_name, array_type, id2data, null, int_entries);
    }
    else {
      System.out.println("CHP quantification/detection data is not for exon chip, " +
			 "falling back on older method for handling expression CHP files");
      //      results = oldParseQuantDetectChp(chp);
    }
    return results;
  }


  public static List<LazyChpSym> parseQuantChp(FusionCHPQuantificationData chp) {
    //SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    //AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
    List<LazyChpSym> results = null;
    String file_name = chp.getFileName();
    String algName = chp.getAlgName();
    String algVersion = chp.getAlgVersion();
    String array_type = chp.getArrayType();
    int ps_count = chp.getEntryCount();
    //    Map name2data = new HashMap(ps_count);
    Map id2data = new HashMap(ps_count);
    List<ProbeSetQuantificationData> int_entries = new ArrayList<ProbeSetQuantificationData>(ps_count);
    int int_id_count = 0;
    int str_id_count = 0;
    System.out.println("array type: " + array_type + ", alg name = " + algName + ", version = " + algVersion);
    System.out.println("probeset count: " + ps_count);
    ProbeSetQuantificationData psqData;
    for (int i=0; i<ps_count; i++) {
      psqData = chp.getQuantificationEntry(i);
      float quant = psqData.getQuantification();
      int intid = psqData.getId();
      String name = null;
      Integer nid = null;
      if (DEBUG && (i<2 || i>=(ps_count-2)))  {
      	System.out.println("preprocessed, id: " + intid + ", name: " + psqData.getName() + ", quant: " + quant);
      }
      if (intid >= 0) {
	nid = new Integer(intid);
	psqData.setName(null);
	int_entries.add(psqData);
	//	id2data.put(nid, psqData);
	int_id_count++;
      }
      else {  // nid < 0, then nid field not being used, so name should be used instead
	name = psqData.getName();
	try {
	  nid = new Integer(name);
	  intid = nid.intValue();
	  psqData.setId(intid);
	  psqData.setName(null);
	  int_entries.add(psqData);
	  //	  id2data.put(nid, psqData);
	  int_id_count++;
	}
	catch (Exception ex) {
	  // can't parse as an integer
	  //	  name2data.put(name, psqData);
	  str_id_count++;
	}
      }
      if (DEBUG && (i<2 || i>=(ps_count-2)))  {
        System.out.println(" post, id: " + psqData.getId() + ", name: " + psqData.getName() + ", quant: " + quant);
      }
    }
    // sort by int_entries int id
    QuantByIntIdComparator comp = new  QuantByIntIdComparator();
    Collections.sort(int_entries, comp);
    if (int_id_count > 0) {
      //      results = ChpParser.makeLazyChpSyms(file_name, array_type, id2data, name2data);
      System.out.println("Probsets with integer id: " + int_id_count);
      System.out.println("Probsets with string id: " + str_id_count);
      System.out.println("done parsing quantification CHP file");
      results = ChpParser.makeLazyChpSyms(file_name, array_type, id2data, null, int_entries);
    }
    else {
      System.out.println("CHP quantification data is not for exon chip, " +
			 "falling back on older method for handling expression CHP files");
      results = oldParseQuantChp(chp);
    }
    return results;
  }


  /**
   *
   *  The FusionCHPQuantificationData class provides parsing capabilities for both 3' IVT (RMA and PLIER)
   *  CHP files and Exon CHP files. The difference between the two are that the probe set result (ProbeSetQuantificationData
   *  class) stores an "id" for Exon results and "name" for 3' IVT results. The "name" property will be
   *  empty for Exon results.
   */
  public static List<LazyChpSym> oldParseQuantChp(FusionCHPQuantificationData chp) {
    SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
    //ArrayList results = new ArrayList();

    String file_name = chp.getFileName();
    String algName = chp.getAlgName();
    String algVersion = chp.getAlgVersion();
    String array_type = chp.getArrayType();
    int ps_count = chp.getEntryCount();
    System.out.println("array type: " + array_type + ", alg name = " + algName + ", version = " + algVersion);
    System.out.println("probeset count: " + ps_count);
    ProbeSetQuantificationData psqData;
    boolean is_exon_chp = false;
    if (ps_count>0) {
      psqData = chp.getQuantificationEntry(0);
      String name = psqData.getName();
      // if name property is empty, then it's a CHP file for exon array results
      //    otherwise it's a CHP file for 3' IVT array results
      if (name == null || name.equals("")) {
	is_exon_chp = true;
	System.out.println("Exon CHP file");
      }
      else {
	System.out.println("3' IVT CHP file");
      }
    }
    Map<MutableAnnotatedBioSeq,List<OneScoreEntry>> seq2entries =
						new HashMap<MutableAnnotatedBioSeq,List<OneScoreEntry>>();
    int match_count = 0;

    if (is_exon_chp) {  // exon results, so try to match up prefixed ids with ids already seen?
      ArrayList<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
      for (int i=0; i<ps_count; i++) {
	psqData = chp.getQuantificationEntry(i);
	float val = psqData.getQuantification();
	int nid = psqData.getId();
	String id = array_type + ":" + nid;
	if (i<2 || i>=(ps_count-2))  {
	  System.out.println("full id: " + id + ", probeset id: " + nid + ", val: " + val);
	}
	// assumes no ".n" postfix (exon chip ids are truly unique and thus should none should get postfixed by duplication)...
	group.findSyms(id, syms, false);
	if (syms.size() > 0) {
	  // for exon chips, assume at most a single pre-existing sym for each probeset in CHP file
	  match_count++;
	  SeqSymmetry prev_sym = syms.get(0);
	  SeqSpan span = prev_sym.getSpan(0);
	  MutableAnnotatedBioSeq aseq = span.getBioSeq();
	  IndexedSingletonSym isym = new IndexedSingletonSym(span.getStart(), span.getEnd(), aseq);
	  isym.setID(id);
	  OneScoreEntry sentry = new OneScoreEntry(isym, val);
	  List<OneScoreEntry> sentries = seq2entries.get(aseq);
	  if (sentries == null) {
	    sentries = new ArrayList<OneScoreEntry>();
	    seq2entries.put(aseq, sentries);
	  }
	  sentries.add(sentry);
	}
	syms.clear();
      }
    }
    else {  // 3' IVT results, so try to match up names of probesets with ids already seen
      ArrayList<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
      for (int i=0; i<ps_count; i++) {
	psqData = chp.getQuantificationEntry(i);
	float val = psqData.getQuantification();
	String id = psqData.getName();
	// try just name as id, if no match, try full id (array_type:probeset_name)
	if (i<2 || i>=(ps_count-10))  {
	  System.out.println("probeset name: " + id + ", val: " + val);
	}
	// try to match up id to one previously seen
	// must deal with possibility that sym ids used ".n" postfixing to uniquify
	group.findSyms(id, syms, true); // not sure if should allow match to name, or always require full id match
	if (syms.size() == 0)  {
	  // couldn't find match with just name, so trying full id
	  id = array_type + ":" + id;
	  group.findSyms(id, syms, true);
	}
	int scount = syms.size();
	if (scount > 0) {
	  match_count++;
	  for (int k=0; k<scount; k++) {
	    SeqSymmetry prev_sym = syms.get(k);
	    SeqSpan span = prev_sym.getSpan(0);
	    MutableAnnotatedBioSeq aseq = span.getBioSeq();
	    IndexedSingletonSym isym = new IndexedSingletonSym(span.getStart(), span.getEnd(), aseq);
	    isym.setID(id);
	    OneScoreEntry sentry = new OneScoreEntry(isym, val);
	    List<OneScoreEntry> sentries = seq2entries.get(aseq);
	    if (sentries == null) {
	      sentries = new ArrayList<OneScoreEntry>();
	      seq2entries.put(aseq, sentries);
	    }
	    sentries.add(sentry);
	  }
	}
	syms.clear();
      }
    }  // end 3' IVT conditional  (! is_exon_chp)

    System.out.println("matching probeset ids found: " + match_count);
    if (match_count == 0) {
      System.out.println("WARNING: Could not automatically load location data for CHP file,\n " +
			 "  and could not find any previously loaded location data matching CHP file");
      Application.errorPanel("Could not automatically load location data for CHP file,\n " +
		     "  and could not find any previously loaded location data matching CHP file");
      return null;
    }

    String type_name = OpenGraphAction.getGraphNameForFile(file_name);
    // Force the AnnotStyle for the container to have glyph depth of 1
    IAnnotStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(type_name);
    style.setGlyphDepth(1);

    // now for each sequence seen, sort the SinEntry list by span min/max
    ScoreEntryComparator comp = new ScoreEntryComparator();
		for (Map.Entry<MutableAnnotatedBioSeq,List<OneScoreEntry>> ent : seq2entries.entrySet()) {
      MutableAnnotatedBioSeq aseq = ent.getKey();
      List<OneScoreEntry> entry_list = ent.getValue();
      Collections.sort(entry_list, comp);

      // now make the container syms
      ScoredContainerSym container = new ScoredContainerSym();
      container.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq));
      container.setProperty("method", type_name);

      int entry_count = entry_list.size();
      float[] scores = new float[entry_count];
      System.out.println("seq: " + aseq.getID() + ", entry list count: " + entry_count);
      for (int k=0; k<entry_count; k++) {
	OneScoreEntry sentry = entry_list.get(k);
	container.addChild(sentry.sym);
	scores[k] = sentry.score;
      }
      container.addScores("probeset scores: " + type_name, scores);
      container.setID(type_name);
      aseq.addAnnotation(container);
    }

    System.out.println("done parsing quantification data CHP file");
    return Collections.<LazyChpSym>emptyList();
  }



  public static List parseLegacyChp(FusionCHPLegacyData chp) {
      ArrayList results = new ArrayList();
    FusionCHPHeader header = chp.getHeader();
    System.out.println("Alg name: " + header.getAlgName());
    System.out.println("Alg version: " + header.getAlgVersion());
    //    System.out.println(header.getAlgorithmParameter("Alpha1"));
    System.out.println("Assay type: " + header.getAssayType());
    System.out.println("Chip type: " + header.getChipType());
    System.out.println("Rows: " + header.getRows() + ", Columns: " + header.getCols());
    System.out.println("Parent .cel file: " + header.getParentCellFile());
    System.out.println("Program ID: " + header.getProgID());
    //    System.out.println(header.getSummaryParameter("RawQ"));

    int probe_count = header.getNumProbeSets();
    System.out.println("number of probesets: " + probe_count);

    float[] pvals = new float[probe_count];
    float[] signals = new float[probe_count];
    FusionExpressionProbeSetResults exp = new FusionExpressionProbeSetResults();
    for (int i=0; i<probe_count; i++) {
      chp.getExpressionResults(i, exp);
      pvals[i] = exp.getDetectionPValue();
      signals[i] = exp.getSignal();
      exp.clear();
    }
    System.out.println("Stopped loading, parsing Legacy CHP data only partially implemented!");
    Application.errorPanel("CHP file is in legacy format, cannot be loaded");
    return results;
  }

  public static List<GraphSym> parseTilingChp(FusionCHPTilingData tchp, boolean annotate_seq, boolean ensure_unique_id) {
    SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    ArrayList<GraphSym> results = new ArrayList<GraphSym>();
    int seq_count = tchp.getNumberSequences();
    String alg_name = tchp.getAlgName();
    String alg_vers = tchp.getAlgVersion();

    System.out.println("seq_count = " + seq_count + ", alg_name = " + alg_name + ", alg_vers = " + alg_vers);

    Map<String,String> file_prop_hash = new LinkedHashMap<String,String>();
    List alg_params = tchp.getAlgParams();
    for (int i=0; i<alg_params.size(); i++) {
      ParameterNameValue param = (ParameterNameValue) alg_params.get(i);
      String pname = param.getName();
      String pval = param.getValueText();
      // unfortunately, param.getValueText() is NOT Ascii text, as it is supposed to be.
      // the character encoding is unknown, so the text looks like garbage (at least on Unix).
      //      System.out.println("   param:  name = " + pname + ", val = " + pval);
      file_prop_hash.put(pname, pval);
    }

    AnnotatedSeqGroup group = null;
    MutableAnnotatedBioSeq aseq = null;

    for (int i=0; i<seq_count; i++) {
      tchp.openTilingSequenceDataSet(i);
      TilingSequenceData seq = tchp.getTilingSequenceData();
      int entry_count = tchp.getTilingSequenceEntryCount(i);

      String seq_name = seq.getName();
      String seq_group_name = seq.getGroupName();
      String seq_vers = seq.getVersion();
      System.out.println("seq " + i + ", name = " + seq_name + ", group = " + seq_group_name +
			 ", version = " + seq_vers + ", datapoints = " + entry_count);

      // try and match up chp seq to a MutableAnnotatedBioSeq and AnnotatedSeqGroup in SingletonGenometryModel
      // if seq group can't be matched, make a new seq group
      // if seq can't be matched, make a new seq

      // trying three different ways of matching up to AnnotatedSeqGroup
      // 1. concatenation of seq_group_name and seq_vers (standard way of mapping CHP ids to AnnotatedSeqGroup)
      // 2. just seq_group_name
      // 3. just seq_vers
      String groupid = seq_group_name + ":" + seq_vers;
      group = gmodel.getSeqGroup(groupid);
      if (group == null) {
	group = gmodel.getSeqGroup(seq_group_name);
      }
      if (group == null) {
	group = gmodel.getSeqGroup(seq_vers);
      }
      // if no AnnotatedSeqGroup matches found, create a new one
      if (group == null) {
	System.out.println("adding new seq group: " + groupid);
	group = gmodel.addSeqGroup(groupid);
      }

      if (gmodel.getSelectedSeqGroup() != group) {
        // This is necessary to make sure new groups get added to the DataLoadView.
        // maybe need a SeqGroupModifiedEvent class instead.
        gmodel.setSelectedSeqGroup(group);
      }

      aseq = group.getSeq(seq_name);
      if (aseq == null) {
	System.out.println("adding new seq: id = " + seq_name + ", group = " + group.getID());
	aseq = group.addSeq(seq_name, 0);
      }

      int[] xcoords = new int[entry_count];
      float[] ycoords = new float[entry_count];
      CHPTilingEntry entry;
      for (int dindex = 0; dindex<entry_count; dindex++) {
	entry = tchp.getTilingSequenceEntry(dindex);
	int pos = entry.getPosition();
	float score = entry.getValue();
	xcoords[dindex] = pos;
	ycoords[dindex] = score;
      }
      int last_base_pos = xcoords[xcoords.length-1];
      if (aseq.getLength() < last_base_pos) {
	aseq.setLength(last_base_pos);
      }
      String graph_id = OpenGraphAction.getGraphNameForFile(tchp.getFileName());
      if (ensure_unique_id) { graph_id = GraphSymUtils.getUniqueGraphID(graph_id, aseq); }
      GraphSym gsym = new GraphSym(xcoords, ycoords, graph_id, aseq);

      Iterator fiter = file_prop_hash.entrySet().iterator();
      while (fiter.hasNext()) {
        Map.Entry ent = (Map.Entry)fiter.next();
        gsym.setProperty((String)ent.getKey(), ent.getValue());
      }

      List seq_params = seq.getParameters();
      for (int k=0; k<seq_params.size(); k++)  {
	ParameterNameValue param = (ParameterNameValue)seq_params.get(k);
	String pname = param.getName();
	String pval = param.getValueText();
	//	System.out.println("   param:  name = " + pname + ", val = " + pval);
	gsym.setProperty(pname, pval);
      }


      // add all seq_prop_hash entries as gsym properties
      results.add(gsym);
      if (annotate_seq) {
	aseq.addAnnotation(gsym);
      }
    }

    gmodel.setSelectedSeqGroup(group);
    gmodel.setSelectedSeq(aseq);
    return results;
  }

  /*public static void main(String[] args) throws IOException {
    //    String infile = "c:/data/chp_test_data/from_Luis_Mar2006/4009028_37_D6_Hela_1st_A_signal.chp";
    SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    AnnotatedSeqGroup group = gmodel.addSeqGroup("H_sapiens_Mar_2006");
    gmodel.setSelectedSeqGroup(group);
    String infile = "c:/data/chp_data_exon/exon_chp_results/HuEx-1_0-st-v2.colon-cancer-data-set/10_5N.rma-exon-all-dabg.chp";
    List results = ChpParser.parse(infile);
    if (results != null)  {
        System.out.println("graphs parsed: " + results.size());
        for (int i = 0; i < results.size(); i++) {
            GraphSymFloat gsym = (GraphSymFloat) results.get(i);
            System.out.println(gsym.getGraphName() + ",  " + gsym.getID() +
                               ",  points = " + gsym.getPointCount());
        }
    }
  }*/

}
/** For sorting single-score probeset entries */

  abstract class ScoreEntry {
    SeqSymmetry sym;
  }

  class OneScoreEntry extends ScoreEntry {
    float score;
    public OneScoreEntry(SeqSymmetry sym, float score) {
      this.sym = sym;
      this.score = score;
    }
  }

  /*
  class TwoScoreEntry extends ScoreEntry {
    float quant;
    float pval;
    public TwoScoreEntry(SeqSymmetry sym, float quant, float pval) {
      this.sym = sym;
      this.quant = quant;
      this.pval = pval;
    }
  }
   */

  /** For sorting single-score probeset entries */
class ScoreEntryComparator implements Comparator<ScoreEntry>  {
  public int compare(ScoreEntry seA, ScoreEntry seB) {
    SeqSpan symA = seA.sym.getSpan(0);
    SeqSpan symB = seB.sym.getSpan(0);
    if (symA.getMin() < symB.getMin()) { return -1; }
    else if (symA.getMin() > symB.getMin()) { return 1; }
    else {  // mins are equal, try maxes
      if (symA.getMax() < symB.getMax()) { return -1; }
      else if (symA.getMax() > symB.getMax()) { return 1; }
      else { return 0; }  // mins are equal and maxes are equal, so consider them equal
    }
  }
}


