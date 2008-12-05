package com.affymetrix.igb.genometry;

import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import java.util.*;
import java.util.List;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometryImpl.*;
import com.affymetrix.genometryImpl.util.SynonymLookup;
//import com.affymetrix.igb.das2.*;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.util.QuantByIntIdComparator;
import com.affymetrix.igb.util.QuantDetectByIntIdComparator;
import com.affymetrix.genometryImpl.util.StringUtils;

import affymetrix.calvin.data.*;
import com.affymetrix.igb.das2.Das2ClientOptimizer;
import com.affymetrix.igb.das2.Das2Discovery;
import com.affymetrix.igb.das2.Das2FeatureRequestSym;
import com.affymetrix.igb.das2.Das2Region;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.das2.Das2Type;
import com.affymetrix.igb.das2.Das2VersionedSource;


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

  public static String PROBESET_SERVER_NAME = "NetAffx";
  // public static String PROBESET_SERVER_NAME = "localhost";  // for debugging

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

  /**
   *  list of probeset result data for probesets whose name/id can be
   *   represented as an integer
   *  list should be sorted by integer id
   */
  List int_entries = null;

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

  /**
   *  Assumes entries_with_int_id is already sorted by int id
   */
  public LazyChpSym(SmartAnnotBioSeq seq, String array_type, Map id2data, Map name2data, List entries_with_int_id) {
    this.aseq = seq;
    this.chp_array_type = array_type;
    this.probeset_id2data = id2data;
    this.probeset_name2data = name2data;
    this.int_entries = entries_with_int_id;
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

  public float[] getChildScores(IndexedSym child, List scorelist) {
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
    //    Timer tim = new Timer();
    //    tim.start();
    coords_loaded = true;
    /**
     *  Coords & ids are retrieved on a per-seq basis via a DAS/2 server, preferably in an optimized binary format
     *      [server_root]/[genomeid]/features?segment=[seqid];
     *  DAS/2 query is run through Das2ClientOptimizer, so only regions that haven't been retrieved yet are queried for
     *  If features have already been retrieved for entire seq, then optimizer won't make any feature query calls
     */
    Map das_servers = Das2Discovery.getDas2Servers();
    Das2ServerInfo server = (Das2ServerInfo)das_servers.get(PROBESET_SERVER_NAME);
    // server and vsource should already be checked before making this LazyChpSym, but checking again
    //     in case connection can no longer be established
    if (server == null) {
      Application.errorPanel("Couldn't find server to retrieve location data for CHP file, server = " + PROBESET_SERVER_NAME);
      return;
    }
    Das2VersionedSource vsource = server.getVersionedSource(aseq.getSeqGroup());
    if (vsource == null) {
      Application.errorPanel("Couldn't find genome data on server for CHP file, genome = " + aseq.getSeqGroup().getID());
      return;
    }
    Das2Region das_segment = vsource.getSegment(aseq);
    if (das_segment == null) {
      Application.errorPanel("Couldn't find sequence data on server for CHP file, seq = " + aseq.getID());
      return;
    }

    ProbeSetQuantificationDetectionData quant_detect = new ProbeSetQuantificationDetectionData();
    ProbeSetQuantificationData quant = new ProbeSetQuantificationData();
    QuantDetectByIntIdComparator quant_detect_comp = null;
    QuantByIntIdComparator quant_comp = null;
    if (int_entries != null && int_entries.size() > 0) {
      Object data = int_entries.get(0);
      if (data instanceof ProbeSetQuantificationDetectionData)  {
	quant_detect_comp = new QuantDetectByIntIdComparator();
      }
      else if (data instanceof ProbeSetQuantificationData)  {
	quant_comp = new QuantByIntIdComparator();
      }
    }

    /**
     *  May need to load multiple annotations from DAS/2 server --
     *     for instance, for exon arrays both probesets (in bp2 format) and transcripts (in bgn format)
     *     [moving towards single load, for example probesets/transcripts/etc. all in "ead" format]
     *  Assume that any annotation type whose name starts with chp_array_type,
     *     or starts with any synonym of chp_array_type, need to be loaded...-st
     *     (caveat -- need to remove any path prefix from type name first) -- GAH
     */
    SynonymLookup lookup = SynonymLookup.getDefaultLookup();
    Map types = vsource.getTypes();
    Map matched_types = new HashMap();
    ArrayList chp_array_syns = lookup.getSynonyms(chp_array_type);
    if (chp_array_syns == null) { chp_array_syns = new ArrayList(); chp_array_syns.add(chp_array_type); }
    for (int i=0; i<chp_array_syns.size(); i++) {
      //      String synonym = (String)chp_array_syns.get(i);
      String synonym = (String)chp_array_syns.get(i);
      String lcsyn = synonym.toLowerCase();
      //      System.out.println("   synonym: " + synonym);
      Iterator titer = types.entrySet().iterator();
      while (titer.hasNext())  {
	Map.Entry ent = (Map.Entry)titer.next();
	Das2Type type = (Das2Type)ent.getValue();
	//	String tname = type.getName();
        //  Switched to type.getShortName() to fix problem with name matching when name has a path prefix...
	String tname = type.getShortName();
	if ((tname.startsWith(synonym) || tname.startsWith(lcsyn))
	    && (matched_types.get(type) == null)) {
	  matched_types.put(type, type);
	}
      }
    }

    if (matched_types.size() < 1) {
      // no DAS/2 type found for the CHP!
      System.out.println("****** WARNING: could not find location data for CHP array type: " + chp_array_type);
      return;
    }

    List symlist = new ArrayList(10000);
    List id_data_hits = new ArrayList(10000);
    List id_sym_hits = new ArrayList(10000);
    int id_hit_count = 0;
    int str_hit_count = 0;
    int all_digit_not_int = 0;

    Iterator titer = matched_types.entrySet().iterator();
    while (titer.hasNext()) {
      Map.Entry ent = (Map.Entry)titer.next();
      Das2Type das_type = (Das2Type)ent.getValue();
      System.out.println("#### found DAS/2 type: " + das_type.getName() + ", for CHP array type: " + chp_array_type);

      // Set the human name on the tier to the short type name, not the long URL ID
      DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(das_type.getID()).setHumanName(das_type.getName());

      SeqSpan whole_span = new SimpleSeqSpan(0, aseq.getLength(), aseq);
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
      // TypeContainerAnnot container = (TypeContainerAnnot)aseq.getAnnotation(das_type.getName());

      // collect probeset annotations for given chp type
      //     (probesets should be at 3rd level down in annotation hierarchy)
      for (int i=0; i<container.getChildCount(); i++) {
	Das2FeatureRequestSym req = (Das2FeatureRequestSym)container.getChild(i);
	int pset_count = req.getChildCount();
	for (int k=0; k<pset_count; k++) {
	  SeqSymmetry sym = req.getChild(k);
	  addIdSyms(sym, symlist);
	}
      }
    }

    int symcount = symlist.size();
    // should the syms be sorted here??
    Collections.sort(symlist, new SeqSymMinComparator(aseq, true));

    // Iterate through probeset annotations, if possible do integer id binary search,
    //     otherwise do hash for string ID
    for (int i=0; i<symcount; i++) {
      SeqSymmetry annot = (SeqSymmetry)symlist.get(i);
      Object data = null;
      if (annot instanceof IntId) {
	// want to use integer id to avoid lots of String churn
	IntId isym = (IntId)annot;
	int nid = isym.getIntID();
	int index = -1;
	if (quant_detect_comp != null) {
	  quant_detect.setId(nid);
	  index = Collections.binarySearch(int_entries, quant_detect, quant_detect_comp);
	}
	else if (quant_comp != null) {
	  quant.setId(nid);
	  index = Collections.binarySearch(int_entries, quant, quant_comp);
	}
	if (index >= 0) {  // if index >= 0 then found entry at that index
	  data = int_entries.get(index);
	  id_hit_count++;
	}
      }
      else {  //  annot is not an IntId, try string ID
	String id = annot.getID();
	// try making id an integer and hashing to probeset_id2data
	// if not an integer, try id as string and hashing to probeset_name2data
	// [ what if can make it an integer, but no hit in probeset_id2data -- should also try probeset_name2data?
	//     NO, for now consider that a miss -- if id in CHP file _can_ be an integer,
	//     should have been converted in ChpParser to an Integer and populated in probeset_id2data ]
	if (id != null) {
	  if (probeset_name2data != null)  {
	    data = probeset_name2data.get(id);
	    if (data != null) {
	      str_hit_count++;
	    }
	  }
	  if (data == null && StringUtils.isAllDigits(id)) {
	    // using a simple isAllDigits() method here, which will miss some
	    //    want to avoid needing try/catch unless most likely can parse as integer
	    try {
	      int nid = Integer.parseInt(id);
	      int index = -1;
	      if (quant_detect_comp != null) {
		quant_detect.setId(nid);
		index = Collections.binarySearch(int_entries, quant_detect, quant_detect_comp);
	      }
	      else if (quant_comp != null)  {
		quant.setId(nid);
		index = Collections.binarySearch(int_entries, quant, quant_comp);
	      }
	      if (index >= 0) {  // if index >= 0 then found entry at that index
		data = int_entries.get(index);
		id_hit_count++;
	      }
	    }
	    catch (Exception ex) { // can't parse as an integer (even though all chars are digits)
	      all_digit_not_int++;
	    }
	  }
	}
      }  // end non-IntId conditional
      if (data != null) {
	id_data_hits.add(data);
	id_sym_hits.add(annot);
      }
    }

    // now see what was found
    float[] quants = new float[id_hit_count];
    float[] pvals = new float[id_hit_count];
    boolean has_pvals = false;
    for (int i=0; i<id_hit_count; i++) {
      Object data = id_data_hits.get(i);
      SeqSymmetry sym = (SeqSymmetry)id_sym_hits.get(i);
      SeqSpan span = sym.getSpan(0);
      IndexedSingletonSym isym = new IndexedSingletonSym(span.getStart(), span.getEnd(), span.getBioSeq());
      this.addChild(isym);

      if (data instanceof ProbeSetQuantificationData) {
	ProbeSetQuantificationData pdata = (ProbeSetQuantificationData)data;
	quants[i] = pdata.getQuantification();
	pvals[i] = 0;
      }
      else if (data instanceof ProbeSetQuantificationDetectionData) {
	ProbeSetQuantificationDetectionData pdata = (ProbeSetQuantificationDetectionData)data;
	quants[i] = pdata.getQuantification();
	pvals[i] = pdata.getPValue();
	has_pvals = true;
      }
      else {
	quants[i] = 0;
	pvals[i] = 0;
      }
    }
    //    this.addScores("score: " + this.getID(), quants);
    //    this.addScores("pval: " + this.getID(), pvals);
    this.addScores("score", quants);
    if (has_pvals)  {
      this.addScores("pval", pvals);
    }
    //    System.out.println("Time to load and merge coords from DAS for CHP file: " + tim.read()/1000f);
    System.out.println("Matching probeset integer IDs with CHP data, matches: " + id_hit_count);
    System.out.println("Matching non-integer string IDs with CHP data, matches: " + str_hit_count);
  }


  /**
   *  syms should be one of:
   *     EfficientProbesetSymA (for exon array probesets)
   *     SingletonSymWithIntId (for exon array transcript_clusters, exon_clusters, PSRs)
   *     ??? (for gene chips)
   *     ??? (for genotyping chips)
   */
  protected void addIdSyms(SeqSymmetry sym, List symlist) {
    if (sym instanceof IntId) {
      symlist.add(sym);
    }
    else if (sym.getID() != null)  {
      symlist.add(sym);
    }
    // if SingletonSymWithIntId, recursively descend through children and add those with IDs
    if ((sym.getChildCount() > 0) && (sym instanceof SingletonSymWithIntId))  {
      for (int i=0; i<sym.getChildCount(); i++) {
	SeqSymmetry child = sym.getChild(i);
	addIdSyms(child, symlist);
      }
    }
  }

}
