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

import java.io.*;
import java.util.*;

import affymetrix.fusion.chp.*;
import affymetrix.calvin.data.*;
import affymetrix.calvin.parsers.*;
import affymetrix.calvin.utils.*;
import affymetrix.calvin.parameter.ParameterNameValue;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.GraphSymUtils;

public class ChpParser {
  static boolean reader_registered = false;
  //  public static void parse(InputStream str, AnnotatedSeqGroup seq_group, String stream_name) {
  // }

  public static List parse(String file_name) throws IOException {
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
    System.out.println("Array type: " + chp_type_name);


    // The following function will determine if the CHP file read contains "legacy" format data. This
    // can be either a GCOS/XDA file or a Command Console file. The "legacy" format data is that
    // which contains a signal, detection call, detection p-value, probe pairs, probe pairs used and
    // comparison results if a comparison analysis was performed. This will be from the MAS5 algorithm.
    // Note: The file may also contain genotyping results from the GTYPE software. The ExtractData function
    // will perform a check to ensure it is an expression CHP file.
    boolean success = false;

    FusionCHPQuantificationData qchp;
    FusionCHPQuantificationDetectionData qdchp;
    FusionCHPTilingData tilechp;
    FusionCHPLegacyData legchp;
    FusionCHPGenericData genchp;

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
    }
    /** legacy data */
    else if ((legchp = FusionCHPLegacyData.fromBase(chp)) != null) {
      System.out.println("CHP file is for legacty data: " + legchp);
      results = parseLegacyChp(legchp);
    }
    else if ((genchp = FusionCHPGenericData.fromBase(chp))  != null) {
      System.out.println("CHP file is generic: " + genchp);
      //      results = parseGenericChp(genchp);
    }
    else {
      System.out.println("WARNING: not parsing file, CHP file type not recognized: " + chp);
    }
    return results;
  }

  public static List parseTilingChp(FusionCHPTilingData tchp)  {
    return parseTilingChp(tchp, true);
  }

  public static List parseTilingChp(FusionCHPTilingData tchp, boolean annotate_seq) {
    return parseTilingChp(tchp, annotate_seq, true);
  }

  public static List parseQuantDetectChp(FusionCHPQuantificationDetectionData chp) {
    ArrayList results = new ArrayList();
    return results;
  }

  /**
   * 
   *  The FusionCHPQuantificationData class provides parsing capabilities for both 3' IVT (RMA and PLIER) 
   *  CHP files and Exon CHP files. The difference between the two are that the probe set result (ProbeSetQuantificationData 
   *  class) stores an "id" for Exon results and "name" for 3' IVT results. The "name" property will be 
   *  empty for Exon results.
   */
  public static List parseQuantChp(FusionCHPQuantificationData chp) {
    ArrayList results = new ArrayList();

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
      if (name == null || name.equals("")) {
	is_exon_chp = true;
	System.out.println("Exon CHP file");
      }
      else {
	System.out.println("3' IVT CHP file");
      }
    }
    if (is_exon_chp) {  // exon results, so try to match up prefixed ids with ids already seen?
      float[] vals = new float[ps_count];
      for (int i=0; i<ps_count; i++) {
	psqData = chp.getQuantificationEntry(i);
	vals[i] = psqData.getQuantification();
	int id = psqData.getId();
	if (i<10)  { System.out.println("probeset id: " + id + ", val: " + vals[i]); }
	// try to match up id to one previously seen
      }
    }
    else {  // 3' IVT results, so try to match up names of probesets with ids already seen
      float[] vals = new float[ps_count];
      for (int i=0; i<ps_count; i++) {
	psqData = chp.getQuantificationEntry(i);
	vals[i] = psqData.getQuantification();
	String name = psqData.getName();
	if (i<10)  { System.out.println("probeset name: " + name + ", val: " + vals[i]); }
	// try to match up id to one previously seen
      }
    }
    System.out.println("done parsing quantification data CHP file");
    return results;
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
      /**
      assertEquals(exp.getDetectionPValue(), (float)(0.05 - (i / 1000.0)), 0.00001f);
      assertEquals(exp.getSignal(), (float)(1.1 + i), 0.00001f);
      assertEquals(exp.getNumPairs(), 3 + i);
      assertEquals(exp.getNumUsedPairs(), 2 + i);
      assertEquals(exp.getDetection(), (i % 4));
      switch (exp.getDetection())
	{
	case (FusionExpressionProbeSetResults.ABS_PRESENT_CALL):
	  assertEquals(exp.getDetectionString(), "P");
	  break;
	case (FusionExpressionProbeSetResults.ABS_MARGINAL_CALL):
	  assertEquals(exp.getDetectionString(), "M");
	  break;
	case (FusionExpressionProbeSetResults.ABS_ABSENT_CALL):
	  assertEquals(exp.getDetectionString(), "A");
	  break;
	case (FusionExpressionProbeSetResults.ABS_NO_CALL):
	  assertEquals(exp.getDetectionString(), "No Call");
	  break;
	default:
	  assertEquals(exp.getDetectionString(), "");
	  break;
	}
      assertEquals(exp.hasCompResults(), true);
      assertEquals(exp.getChangePValue(), (float)(0.04 - (i / 1000.0)), 0.0000001f);
      assertEquals(exp.getSignalLogRatio(), (float)(1.1 + i), 0.0000001f);
      assertEquals(exp.getSignalLogRatioLow(), (float)(-1.1 + i), 0.0000001f);
      assertEquals(exp.getSignalLogRatioHigh(), (float)(10.1 + i), 0.0000001f);
      assertEquals(exp.getNumCommonPairs(), 2 + i);
      assertEquals(exp.getChange(), (i % 6 + 1));
      switch (exp.getChange())
	{
	case (FusionExpressionProbeSetResults.COMP_INCREASE_CALL):
	  assertEquals(exp.getChangeString(), "I");
	  break;
	case (FusionExpressionProbeSetResults.COMP_DECREASE_CALL):
	  assertEquals(exp.getChangeString(), "D");
	  break;
	case (FusionExpressionProbeSetResults.COMP_MOD_INCREASE_CALL):
	  assertEquals(exp.getChangeString(), "MI");
	  break;
	case (FusionExpressionProbeSetResults.COMP_MOD_DECREASE_CALL):
	  assertEquals(exp.getChangeString(), "MD");
	  break;
	case (FusionExpressionProbeSetResults.COMP_NO_CHANGE_CALL):
	  assertEquals(exp.getChangeString(), "NC");
	  break;
	case (FusionExpressionProbeSetResults.COMP_NO_CALL):
	  assertEquals(exp.getChangeString(), "No Call");
	  break;
	default:
	  assertEquals(exp.getChangeString(), "");
	  break;
	}
      */
      exp.clear();
    }
    System.out.println("done parsing legacy CHP data");
    return results;
  }

  public static List parseTilingChp(FusionCHPTilingData tchp, boolean annotate_seq, boolean ensure_unique_id) {
    SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    ArrayList results = new ArrayList();
    int seq_count = tchp.getNumberSequences();
    String alg_name = tchp.getAlgName();
    String alg_vers = tchp.getAlgVersion();

    System.out.println("seq_count = " + seq_count + ", alg_name = " + alg_name + ", alg_vers = " + alg_vers);

    Map file_prop_hash = new LinkedHashMap();
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

      // try and match up chp seq to a BioSeq and AnnotatedSeqGroup in SingletonGenometryModel
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
      String graph_id = tchp.getFileName();
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

  public static void main(String[] args) throws IOException {
    String infile = "c:/data/chp_test_data/from_Luis_Mar2006/4009028_37_D6_Hela_1st_A_signal.chp";
    List results = ChpParser.parse(infile);
    System.out.println("graphs parsed: " + results.size());
    for (int i=0; i<results.size(); i++)  {
      GraphSym gsym = (GraphSym)results.get(i);
      System.out.println(gsym.getGraphName() + ",  " + gsym.getID() + ",  points = " + gsym.getPointCount());
    }
  }

}
