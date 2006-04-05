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

  public static List parse(String file_name) {
    List results = null;
    if (! (reader_registered)) {
      //    FusionCHPLegacyData.registerReader();
      //    FusionCHPGenericData.registerReader();
      FusionCHPTilingData.registerReader();
      reader_registered = true;
    }

    File testfil = new File(file_name);
    System.out.println("file exists: " + testfil.exists());
    System.out.println("reading in CHP file: " + file_name);

    FusionCHPData chp = FusionCHPDataReg.read(file_name);
    AffymetrixGuidType chp_type =  chp.getFileTypeIdentifier();
    String chp_type_name = chp_type.getGuid();
    System.out.println("chp: " + chp);
    System.out.println("chp type: " + chp_type_name);

    if (chp_type_name.equals(CHPTilingData.CHP_TILING_TYPE)) {
      FusionCHPTilingData tile_chp = FusionCHPTilingData.fromBase(chp);
      System.out.println("tilechp: " + tile_chp);
      results = parseTilingChp(tile_chp);
    }
    else {
      System.out.println("WARNING: not parsing file, CHP file type not recognized: " + chp_type_name);
    }
    return results;
  }

  public static List parseTilingChp(FusionCHPTilingData tchp)  {
    return parseTilingChp(tchp, true);
  }

  public static List parseTilingChp(FusionCHPTilingData tchp, boolean annotate_seq) {
    return parseTilingChp(tchp, annotate_seq, true);
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
      //      System.out.println("   param:  name = " + pname + ", val = " + pval);
      file_prop_hash.put(pname, pval);
    }

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
      AnnotatedSeqGroup group = gmodel.getSeqGroup(groupid);
      if (group == null) {
	group = gmodel.getSeqGroup(seq_group_name);
      }
      if (group == null) {
	group = gmodel.getSeqGroup(seq_vers);
      }
      // if no AnnotatedSeqGroup matches found, create a new one
      if (group == null) {
	System.out.println("adding new seq group to genometry model: " + groupid);
	group = gmodel.addSeqGroup(groupid);
      }

      MutableAnnotatedBioSeq aseq = group.getSeq(seq_name);
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
    return results;
  }

  public static void main(String[] args) {
    String infile = "c:/data/chp_test_data/from_Luis_Mar2006/4009028_37_D6_Hela_1st_A_signal.chp";
    List results = ChpParser.parse(infile);
    System.out.println("graphs parsed: " + results.size());
    for (int i=0; i<results.size(); i++)  {
      GraphSym gsym = (GraphSym)results.get(i);
      System.out.println(gsym.getGraphName() + ",  " + gsym.getID() + ",  points = " + gsym.getPointCount());
    }
  }

}
