package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.MutableSeqSpan;
import com.affymetrix.genometry.SearchableSeqSymmetry;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.parsers.AnnotsParser;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.parsers.LiftParser;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utils for DAS/2 and other servers.
 */
public abstract class ServerUtils {
	private static final String annots_filename = "annots.xml"; // potential file for annots parsing
	private static final String graph_dir_suffix = ".graphs.seqs";
	private static final boolean SORT_SOURCES_BY_ORGANISM = true;
	private static final boolean SORT_VERSIONS_BY_DATE_CONVENTION = true;
	private static final Pattern interval_splitter = Pattern.compile(":");

	private static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

	private static Map<String,String> annots_map = new LinkedHashMap<String,String>();    // hash of file names and titles


	public static final void parseChromosomeData(File genome_directory, String genome_version) throws IOException {
		File chrom_info_file = new File(genome_directory, "mod_chromInfo.txt");
		if (chrom_info_file.exists()) {
			System.out.println("parsing in chromosome data from mod_chromInfo file for genome: " + genome_version);
			InputStream chromstream = new FileInputStream(chrom_info_file);
			ChromInfoParser.parse(chromstream, gmodel, genome_version);
			GeneralUtils.safeClose(chromstream);
		} else {
			System.out.println("couldn't find mod_chromInfo file for genome: " + genome_version);
			System.out.println("looking for lift file instead");
			File lift_file = new File(genome_directory, "liftAll.lft");
			if (lift_file.exists()) {
				System.out.println("parsing in chromosome data from liftAll file for genome: " + genome_version);
				InputStream liftstream = new FileInputStream(lift_file);
				LiftParser.parse(liftstream, gmodel, genome_version);
				GeneralUtils.safeClose(liftstream);
			} else {
				System.out.println("couldn't find liftAll or mod_chromInfo file for genome!!! " + genome_version);
			}
		}
	}

	/**Loads a file's lines into a hash first column is the key, second the value.
	 * Skips blank lines and those starting with a '#'
	 * @return null if an exception in thrown
	 * */
	public static final HashMap<String, String> loadFileIntoHashMap(File file) {
		BufferedReader in = null;
		HashMap<String, String> names = null;
		try {
			names = new HashMap<String, String>();
			in = new BufferedReader(new FileReader(file));
			String line;
			String[] keyValue;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				keyValue = line.split("\\s+");
				if (keyValue.length < 2) {
					continue;
				}
				names.put(keyValue[0], keyValue[1]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			GeneralUtils.safeClose(in);
		}
		return names;
	}

	public static final void loadSynonyms(String synonym_file) {
		File synfile = new File(synonym_file);
		if (synfile.exists()) {
			System.out.println("Synonym file found, loading synonyms");
			SynonymLookup lookup = SynonymLookup.getDefaultLookup();
			try {
				lookup.loadSynonyms(new FileInputStream(synfile));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			System.out.println("Synonym file not found, therefore not using synonyms");
		}
	}


	/** sorts genomes and versions within genomes
	 *  sort genomes based on "organism_order.txt" config file if present
	 * @param organisms
	 * @param org_order_filename
	 */
	public static final void sortGenomes(Map<String,List<AnnotatedSeqGroup>> organisms, String org_order_filename) {
		// sort genomes based on "organism_order.txt" config file if present
		// get Map.Entry for organism, sort based on order in organism_order.txt,
		//    put in order in new LinkedHashMap(), then replace as organisms field
		File org_order_file = new File(org_order_filename);
		if (SORT_SOURCES_BY_ORGANISM && org_order_file.exists()) {
			Comparator<String> org_comp = new MatchToListComparator(org_order_filename);
			List<String> orglist = new ArrayList<String>(organisms.keySet());
			Collections.sort(orglist, org_comp);
			Map<String,List<AnnotatedSeqGroup>> sorted_organisms = new LinkedHashMap<String,List<AnnotatedSeqGroup>>();
			for (String org: orglist) {
				//	System.out.println("add organism to sorted list: " + org + ",   " + organisms.get(org));
				sorted_organisms.put(org, organisms.get(org));
			}
			organisms = sorted_organisms;
		}
		if (SORT_VERSIONS_BY_DATE_CONVENTION) {
			Comparator<AnnotatedSeqGroup> date_comp = new GenomeVersionDateComparator();
			for (List<AnnotatedSeqGroup> versions : organisms.values()) {
				Collections.sort(versions, date_comp);
			}
		}
	}

	private static final void loadAnnotsFromStream(InputStream istr, String stream_name, AnnotatedSeqGroup genome) {
		ParserController.parse(istr, annots_map, stream_name, gmodel, genome);
	}


	/**
	 *   If current_file is directory:
	 *       if ".seqs" suffix, then handle as graphs
	 *       otherwise recursively call on each child files;
	 *   if not directory, see if can parse as annotation file.
	 *   if type prefix is null, then at top level of genome directory, so make type_prefix = "" when recursing down
	 */
	public static final void loadAnnotsFromFile(File current_file, AnnotatedSeqGroup genome, String type_prefix,
							Map<String,String> graph_name2dir,
							Map<String,String> graph_name2file) {
		String file_name = current_file.getName();
		String file_path = current_file.getPath();

		String type_name;
		String new_type_prefix;
		if (type_prefix == null) {  // special-casing for top level genome directory, don't want genome name added to type name path
			type_name = file_name;
			new_type_prefix = "";
		} else {
			type_name = type_prefix + file_name;
			new_type_prefix = type_name + "/";
		}

		// if current file is directory, then descend down into child files
		if (current_file.isDirectory()) {
			loadAnnotsFromDir(type_name, file_path, genome, current_file, new_type_prefix, graph_name2dir, graph_name2file);
			return;
		}

		if (type_name.endsWith(".bar")) {
			// String file_path = current_file.getPath();
			// special casing so bar files are seen in types request, but not parsed in on startup
			//    (because using graph slicing so don't have to pull all bar file graphs into memory)
			System.out.println("@@@ adding graph file to types: " + type_name + ", path: " + file_path);
			graph_name2file.put(type_name, file_path);
			return;
		}

		if (!annots_map.isEmpty() && !annots_map.containsKey(file_name)) {
			// we have loaded in an annots.xml file, but yet this file is not in it and should be ignored.
			return;
		}

		if (file_name.equals("mod_chromInfo.txt") || file_name.equals("liftAll.lft")) {
			// for loading annotations, ignore the genome sequence data files
			return;
		}

		// current file is not a directory, so try and recognize as annotation file
		InputStream istr = null;
		try {
			istr = new BufferedInputStream(new FileInputStream(current_file));
			loadAnnotsFromStream(istr, type_name, genome);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(istr);
		}
	}



	public static final void loadAnnotsFromDir(
					String type_name,
					String file_path,
					AnnotatedSeqGroup genome,
					File current_file,
					String new_type_prefix,
					Map<String,String> graph_name2dir,
					Map<String,String> graph_name2file) {
		File annot = new File(current_file, annots_filename);
		if (annot.exists()) {
			FileInputStream istr = null;
			try {
				istr = new FileInputStream(annot);
				AnnotsParser.parseAnnotsXml(istr, annots_map);
			} catch (FileNotFoundException ex) {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				GeneralUtils.safeClose(istr);
			}
		}


		if (type_name.endsWith(graph_dir_suffix)) {
			// each file in directory is same annotation type, but for a single seq?
			// assuming bar files for now, each with starting with seq id?
			//	String graph_name = file_name.substring(0, file_name.length() - graph_dir_suffix.length());
			String graph_name = type_name.substring(0, type_name.length() - graph_dir_suffix.length());
			System.out.println("@@@ adding graph directory to types: " + graph_name + ", path: " + file_path);
			graph_name2dir.put(graph_name, file_path);
		} else {
			//System.out.println("checking for annotations in directory: " + current_file);
			File[] child_files = current_file.listFiles(new HiddenFileFilter());
			Arrays.sort(child_files);
			for (File child_file : child_files) {
				loadAnnotsFromFile(child_file, genome, new_type_prefix, graph_name2dir, graph_name2file);
			}
		}
	}

	public static final List<SeqSymmetry> FindNameInGenome(String name, AnnotatedSeqGroup genome) {
			// GAH 11-2006
			//   need to enhance this to support multiple name parameters OR'd together
			//   DAS/2 specification defines glob-style searches:
			//   The string searches may be exact matches, substring, prefix or suffix searches.
			//   The query type depends on if the search value starts and/or ends with a '*'.
			//
			//    ABC -- field exactly matches "ABC"
			//    *ABC -- field ends with "ABC"
			//    ABC* -- field starts with "ABC"
			//    *ABC* -- field contains the substring "ABC"
			boolean glob_start = name.startsWith("*");
			boolean glob_end = name.endsWith("*");

			List<SeqSymmetry> result = null;
			Pattern name_pattern = null;
			if (glob_start || glob_end) {
				String name_regex = name.toLowerCase();
				if (glob_start) {
					// do replacement of first "*" with ".*" ?
					name_regex = ".*" + name_regex.substring(1);
				}
				if (glob_end) {
					// do replacement of last "*" with ".*" ?
					name_regex = name_regex.substring(0, name_regex.length() - 1) + ".*";
				}
				System.out.println("!!!! name arg: " + name + ",  regex to use for pattern-matching: " + name_regex);
				name_pattern = Pattern.compile(name_regex);
				result = genome.findSyms(name_pattern);
				//	   Collections.sort(result, new SeqSymIdComparator());
				System.out.println("!!!! regex matches: " + result.size());
			} else {
				// ABC -- field exactly matches "ABC"
				result = genome.findSyms(name);
			}
			return result;
		}


		/**
		 *  Differs from Das2FeatureSaxParser.getLocationSpan():
		 *     Won't add unrecognized seqids or null groups
		 *     If rng is null or "", will set to span to [0, seq.getLength()]
		 */
		public static final SeqSpan getLocationSpan(String seqid, String rng, AnnotatedSeqGroup group) {
			if (seqid == null || group == null) {
				return null;
			}
			MutableAnnotatedBioSeq seq = group.getSeq(seqid);
			if (seq == null) {
				return null;
			}
			int min;
			int max;
			boolean forward = true;
			if (rng == null) {
				min = 0;
				max = seq.getLength();
			} else {
				try {
					String[] subfields = interval_splitter.split(rng);
					min = Integer.parseInt(subfields[0]);
					max = Integer.parseInt(subfields[1]);
					if (subfields.length >= 3) {  // in DAS/2 strandedness is not allowed for range query params, but accepting it here
						if (subfields[2].equals("-1")) {
							forward = false;
						}
					}
				} catch (Exception ex) {
					System.out.println("Problem parsing a query parameter range filter: " + rng);
					return null;
				}
			}
			SeqSpan span;
			if (forward) {
				span = new SimpleSeqSpan(min, max, seq);
			} else {
				span = new SimpleSeqSpan(max, min, seq);
			}
			return span;
		}

			// if an inside_span specified, then filter out intersected symmetries based on this:
		//    don't return symmetries with a min < inside_span.min() or max > inside_span.max()  (even if they overlap query interval)s
		public static final List<SeqSymmetry> SpecifiedInsideSpan(SeqSpan inside_span, List<SeqSymmetry> result, String query_type) {
			int inside_min = inside_span.getMin();
			int inside_max = inside_span.getMax();
			MutableAnnotatedBioSeq iseq = inside_span.getBioSeq();
			/*System.out.println("*** trying to apply inside_span constraints ***");
			if (iseq != oseq) {
				System.out.println("Problem with applying inside_span constraint, different seqs: iseq = " + iseq.getID() + ", oseq = " + oseq.getID());
				// if different seqs, then no feature can pass constraint...
				//   hmm, this might not strictly be true based on genometry...
				result = Collections.<SeqSymmetry>emptyList();
			} else {
				Timer timecheck = new Timer();
				timecheck.start();
				*/
			MutableSeqSpan testspan = new SimpleMutableSeqSpan();
				List<SeqSymmetry> orig_result = result;
				int rcount = orig_result.size();
				result = new ArrayList<SeqSymmetry>(rcount);
				for (SeqSymmetry sym : orig_result) {
				//for (int i = 0; i < rcount; i++) {
				//	SeqSymmetry sym = (SeqSymmetry) orig_result.get(i);
					// fill in testspan with span values for sym (on aseq)
					sym.getSpan(iseq, testspan);
					if ((testspan.getMin() >= inside_min) && (testspan.getMax() <= inside_max)) {
						result.add(sym);
					}
				}
				System.out.println("  overlapping annotations of type " + query_type + " that passed inside_span constraints: " + result.size());
				//System.out.println("  time for inside_span filtering: " + (timecheck.read()) / 1000f);
			//}
			return result;
		}

	/**
		 *
		 *  Currently assumes:
		 *    query_span's seq is a BioSeq (which implies top-level annots are TypeContainerAnnots)
		 *    only one IntervalSearchSym child for each TypeContainerAnnot
		 *  Should expand soon so results can be returned from multiple IntervalSearchSyms children
		 *      of the TypeContainerAnnot
		 */
		public static final List<SeqSymmetry> getIntersectedSymmetries(SeqSpan query_span, String annot_type) {
			BioSeq seq = (BioSeq) query_span.getBioSeq();
			SymWithProps container = seq.getAnnotation(annot_type);
			if (container != null) {
				int annot_count = container.getChildCount();
				for (int i = 0; i < annot_count; i++) {
					SeqSymmetry sym = container.getChild(i);
					if (sym instanceof SearchableSeqSymmetry) {
						SearchableSeqSymmetry target_sym = (SearchableSeqSymmetry) sym;
						return target_sym.getOverlappingChildren(query_span);
					}
				}
			}
			return Collections.<SeqSymmetry>emptyList();
		}

	// Print out the genomes
		public static final void printGenomes(Map<String,List<AnnotatedSeqGroup>> organisms) {
			for (Map.Entry<String, List<AnnotatedSeqGroup>> ent : organisms.entrySet()) {
				String org = ent.getKey();
				System.out.println("Organism: " + org);
				for (AnnotatedSeqGroup version : ent.getValue()) {
					System.out.println("    Genome version: " + version.getID()
							+ ", organism: " + version.getOrganism()
							+ ", seq count: " + version.getSeqCount());
				}
			}
		}

		/**
	 *  Gets the list of types of annotations for a given genome version.
	 *  Assuming top-level annotations hold type info in property "method" or "meth".
	 *  @return a Map where keys are feature type Strings and values
	 *    are non-null Lists of preferred format Strings
	 *
	 *  may want to cache this info (per versioned source) at some point...
	 */
	public static final Map<String, List<String>> getTypes(
					AnnotatedSeqGroup genome,
					Map<String, String> graph_name2file,
					Map<String, String> graph_name2dir,
					ArrayList<String> graph_formats) {
		Map<String, List<String>> genome_types = getGenomeTypes(genome.getSeqList());

		// adding in any graph files as additional types (with type id = file name)
		// this is temporary, need a better solution soon -- should probably add empty graphs to seqs to have graphs
		//    show up in seq.getTypes(), but without actually being loaded??
		for (String gname : graph_name2file.keySet()) {
			genome_types.put(gname, graph_formats);  // should probably get formats instead from "preferred_formats"?
		}

		for (String gname : graph_name2dir.keySet()) {
			genome_types.put(gname, graph_formats);  // should probably get formats instead from "preferred_formats"?
		}

		return genome_types;
	}

	// iterate over seqs to collect annotation types
	private static final Map<String,List<String>> getGenomeTypes(List<BioSeq> seqList) {
		Map<String,List<String>> genome_types = new LinkedHashMap<String,List<String>>();
		for (BioSeq aseq : seqList) {
			for (String type : aseq.getTypeList()) {
				if (genome_types.get(type) != null) {
					continue;
				}
				List<String> flist = Collections.<String>emptyList();
				SymWithProps tannot = aseq.getAnnotation(type);
				SymWithProps first_child = (SymWithProps) tannot.getChild(0);
				if (first_child != null) {
					List formats = (List) first_child.getProperty("preferred_formats");
					if (formats != null) {
						flist = formats;
					}
				}
				genome_types.put(type, flist);
			}
		}
		return genome_types;
	}

}
