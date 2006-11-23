package com.affymetrix.igb.genometry;

import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.das2.*;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.util.IntList;
import com.affymetrix.igb.util.FloatList;

import affymetrix.calvin.data.*;


/**
 *  Want to automatically load location data for probesets on chip
 *
 *  Needed for:
 *      3' IVT Expression chips
 *      Exon chips and other expression that is non-3' IVT
 *      [Genotyping chips?  Not yet implemented]
 *
 *  Not necessary for tiling array chips
 *  Probably not necessary for sequencing chips (but those aren't supported yet)
 *
 *  Basic strategy is to retrieve probeset id and location data from the main public Affymetrix DAS/2 server,
 *     and match by id to associate locations with the probeset results.
 *
 *  Used for sequence annotations based on Affymetrix CHP file data when
 *  coords aren't included in the CHP files
 *
 *  Might get further optimizations by not extending from ScoreContainerSym,
 *     but for now want to leverage off use of ScoredContainerGlyphFactory to render as graphs
 *
 *  For every CHP file that needs coord resolution there should be a LazyChpSym for each sequence in the genome
 */
public class LazyChpSym extends ScoredContainerSym {

  //  static String PROBESET_SERVER_NAME = "localhost";
  public static String PROBESET_SERVER_NAME = "NetAffx";

  //  static Map genome2chp;
  SmartAnnotBioSeq aseq;

  /**
   *  map of probeset integer IDs to probeset result data, for probesets whose name/id can be
   *   represented as an integer
   */
  Map probeset_id2data = null;

  /**
   *  map of probeset name Strings to probeset result data, for probesets whose name/id can _NOT_ be
   *   represented as an integer
   *  NOT CURRENTLY USED (ALWAYS NULL)
   */
  Map probeset_name2data = null;

  /** in Affy Fusion SDK this is called "CHP array type", for example "HuEx-1_0-st-v2" */
  String chp_array_type = null;

  /**
   *  Class used to represent experimental data for a single probeset
   *
   *  Possible values:  all from package affymetrix.calvin.data in Affy Fusion SDK
   *     exon and gene chips:
   *         ProbeSetQuantificationData
   *         ProbeSetQuantificationDetectionData
   *         ProbeSetMultiDataExpressionData ???
   *     genotyping:
   *         ProbeSetMultiDataGenotypeData
   *
   *  For ProbeSetQuantificatonData and ProbeSetQuantificationDetectionData,
   *     if getID() >= 0, then Integer(id) should be used for hashing
   */
  Class probeset_data_class;

  /**
   *  Convenience field to gather all probeset syms on this seq out of the annotation hierarchy
   *    (otherwise would have to traverse "chp_array_type" annotation branch on seq every time)
   *    (hmmm -- tree traversal is actually probably fine...)
   */
  List probesets_on_seq = null;

  public LazyChpSym(SmartAnnotBioSeq seq, String array_type, Map id2data, Map name2data) {
    this.aseq = seq;
    this.chp_array_type = array_type;
    this.probeset_id2data = id2data;
    this.probeset_name2data = name2data;
    // this.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq));
  }

  /**
   *  Pointer to set of scored results + ids (probably as a ScoreEntry (see ChpParser))

   *
   *
   *
   *  Coords & ids are retrieved on a per-seq basis via a DAS/2 server, preferably in an optimized binary format
   *  [server_root]/[genomeid]/features?segment=[seqid];
   */
  protected boolean coords_loaded = false;

  public int getChildCount() {
    if (! coords_loaded) { loadCoords(); }
    return super.getChildCount();
  }

  public SeqSymmetry getChild(int index) {
    if (! coords_loaded) { loadCoords(); }
    return super.getChild(index);
  }

  public float[] getChildScores(IndexedSym child, java.util.List scorelist) {
    if (! coords_loaded) { loadCoords(); }
    return super.getChildScores(child, scorelist);
  }

  public float[] getChildScores(IndexedSym child) {
    if (! coords_loaded) { loadCoords(); }
    return super.getChildScores(child);
  }

  public int getScoreCount() {
    if (! coords_loaded) { loadCoords(); }
    return super.getScoreCount();
  }

  public float[] getScores(String name) {
    if (! coords_loaded) { loadCoords(); }
    return super.getScores(name);
  }

  public float[] getScores(int index) {
    if (! coords_loaded) { loadCoords(); }
    return super.getScores(index);
  }

  public String getScoreName(int index)  {
    if (! coords_loaded) { loadCoords(); }
    return super.getScoreName(index);
  }


  public void loadCoords() {
    coords_loaded = true;
    /**
     *  First check and see if probeset locations are already present as an annotation on seq
     *  But what if _some_ but not all of the probeset locations were already loaded via a
     *     different route (NetAffx page?  DAS/2 GUI?) -- this is a problem
     *  If only other way to get probeset data with same type is through DAS/2 range query, then
     *    DAS/2 optimizations _might_ deal with this -- if there already is an annotation on seq,
     *    make the request anyway but run it through the optimizer -- if entire seq already covered,
     *    optimizer will eliminate entire request, if only partial then optimizer will trim out
     *    parts already covered and request the rest.
     *
     *  Coords & ids are retrieved on a per-seq basis via a DAS/2 server, preferably in an optimized binary format
     *      [server_root]/[genomeid]/features?segment=[seqid];
     */
    // ScoredContainerSym should have one and only one span/seq and it should be a SmartAnnotBioSeq
    //   aseq is now a field, set in constructor
    //    SmartAnnotBioSeq aseq = (SmartAnnotBioSeq)this.getSpanSeq(0);

    Map das_servers = Das2Discovery.getDas2Servers();
    Das2ServerInfo server = (Das2ServerInfo)das_servers.get(PROBESET_SERVER_NAME);
    // server and vsource should already be checked before making this LazyChpSym, but checking again
    //     in case connection can no longer be established
    if (server == null) {
      IGB.errorPanel("Couldn't find server to retrieve location data for CHP file, server = " + PROBESET_SERVER_NAME);
      return;
    }
    Das2VersionedSource vsource = server.getVersionedSource(aseq.getSeqGroup());
    if (vsource == null) {
      IGB.errorPanel("Couldn't find genome data on server for CHP file, genome = " + aseq.getSeqGroup().getID());
      return;
    }
    Das2Region das_segment = vsource.getSegment(aseq);
    if (das_segment == null) {
      IGB.errorPanel("Couldn't find sequence data on server for CHP file, seq = " + aseq.getID());
      return;
    }
    List typelist = (List)vsource.getTypesByName(chp_array_type);
    if (typelist == null || typelist.size() < 1) {
      // try again with synonyms?
      SynonymLookup lookup = SynonymLookup.getDefaultLookup();
      List synonyms = lookup.getSynonyms(chp_array_type);
      if (synonyms != null)  {
	System.out.println("synonym count: " + synonyms.size());
	for (int i=0; i<synonyms.size(); i++) {
	  String syn = (String)synonyms.get(i);
	  System.out.println("synonym " + i + ": " + syn);
	  typelist = (List)vsource.getTypesByName(syn);
	  if ((typelist != null) && (typelist.size() > 0)) {
	    break;
	  }
	}
      }
    }
    if (typelist == null || typelist.size() < 1) {
      // no DAS/2 type found for the CHP!
      System.out.println("****** WARNING: could not find location data for CHP array type: " + chp_array_type);
      return;
    }

    Das2Type das_type = (Das2Type)typelist.get(0);
    System.out.println("found DAS/2 type: " + das_type.getName() + ", for CHP array type: " + chp_array_type);

    SeqSymmetry typesym = aseq.getAnnotation(chp_array_type);
    SeqSpan whole_span = new SimpleSeqSpan(0, aseq.getLength(), aseq);
    // to get Das2Region and Das2Type, need to intialize DAS/2 versioned source
    //    (but can do this once per versioned source, rather than once per LazyChpSym?
    //    (YES -- once DAS/2 versioned source is initialized, model doesn't have to
    //       make additional queries to
    Das2FeatureRequestSym request_sym = new Das2FeatureRequestSym(das_type, das_segment, whole_span, null);
    System.out.println("request: " + das_type.getName() + ", seq = " + aseq.getID() + ", length = " + aseq);

    // if already retrieved chp_array_type coord annotations for this whole sequence (for example
    //   due to a previously loaded CHP file with same "array_type", then optimizer
    //   will figure this out and not make any queries --
    //   so if load multiple chps of same array type, actual feature query to DAS/2 server only happens once (per seq)
    // optimizer should also figure out (based on Das2Type info) an optimized format to load data with
    //   (for example "bp2" for
    Das2ClientOptimizer.loadFeatures(request_sym);

    TypeContainerAnnot container = (TypeContainerAnnot)aseq.getAnnotation(das_type.getID()); // should be a TypeContainerAnnot
    List symlist = new ArrayList(10000);
    List id_data_hits = new ArrayList(10000);
    List id_name_hits = new ArrayList(10000);
    List id_sym_hits = new ArrayList(10000);
    int id_hit_count = 0;

    // collect probeset annotations for given chp type
    //     (probesets should be at 3rd level down in annotation hierarchy)
    for (int i=0; i<container.getChildCount(); i++) {
      Das2FeatureRequestSym req = (Das2FeatureRequestSym)container.getChild(i);
      int pset_count = req.getChildCount();
      for (int k=0; k<pset_count; k++) {
	// probeset should be one of:
	//    EfficientProbesetSymA (for exon chips)
	//    ??? (for gene chips)
	//    ??? (for genotyping chips)
	SeqSymmetry probeset = req.getChild(k);
	symlist.add(probeset);
      }
    }
    int symcount = symlist.size();

    // should the syms be sorted here??
    Collections.sort(symlist, new SeqSymMinComparator(aseq, true));

    // Iterate through probeset annotations, try hashing each one to chp data, collect hits
    for (int i=0; i<symcount; i++) {
      SeqSymmetry probeset = (SeqSymmetry)symlist.get(i);
      if (probeset instanceof EfficientProbesetSymA) {
	// want to use integer id to avoid lots of String churn
	EfficientProbesetSymA psym = (EfficientProbesetSymA)probeset;
	int nid = psym.getIntID();
	Integer pid = new Integer(nid);
	// look for a match in ID-to-ScoreEntry
	Object data = probeset_id2data.get(pid);
	// if get a match, add as child sym, keep track of score(s)
	if (data != null) {
	  id_hit_count++;
	  id_data_hits.add(data);
	  id_sym_hits.add(psym);
	}
      }
    }

    // now see what was found
    float[] quants = new float[id_hit_count];
    float[] pvals = new float[id_hit_count];
    for (int i=0; i<id_hit_count; i++) {
      Object data = id_data_hits.get(i);
      SeqSymmetry sym = (SeqSymmetry)id_sym_hits.get(i);
      SeqSpan span = sym.getSpan(0);
      IndexedSingletonSym isym = new IndexedSingletonSym(span.getStart(), span.getEnd(), span.getBioSeq());
      this.addChild(isym);

      //      if (data instanceof ProbeSetQuantificationData) {
      //      }
      //      else if (data instanceof ProbeSetQuantificationDetectionData) {
      ProbeSetQuantificationDetectionData pdata = (ProbeSetQuantificationDetectionData)data;
      quants[i] = pdata.getQuantification();
      pvals[i] = pdata.getPValue();
      //      }

    }
    //    this.addScores("score: " + this.getID(), quants);
    //    this.addScores("pval: " + this.getID(), pvals);
    this.addScores("score", quants);
    this.addScores("pval", pvals);

    System.out.println("Matching probeset integer IDs with CHP data, matches: " + id_hit_count);
  }



}
