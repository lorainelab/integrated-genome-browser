package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.comparator.MatchToListComparator;
import com.affymetrix.genometryImpl.comparator.GenomeVersionDateComparator;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.SearchableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.parsers.AnnotsParser;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.parsers.LiftParser;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.parsers.ProbeSetDisplayPlugin;
import com.affymetrix.genometryImpl.util.IndexingUtils.IndexedSyms;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
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
	private static final boolean DEBUG = false;
	private static final String annots_filename = "annots.xml"; // potential originalFile for annots parsing
	private static final String graph_dir_suffix = ".graphs.seqs";
	private static final boolean SORT_SOURCES_BY_ORGANISM = true;
	private static final boolean SORT_VERSIONS_BY_DATE_CONVENTION = true;
	private static final Pattern interval_splitter = Pattern.compile(":");
	private static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	private static Map<String, String> annots_map = new LinkedHashMap<String, String>();    // hash of originalFile names and titles

	private static final String modChromInfo = "mod_chromInfo.txt";
	private static final String liftAll = "liftAll.lft";
	public static final void parseChromosomeData(File genome_directory, String genome_version) throws IOException {
		File chrom_info_file = new File(genome_directory, modChromInfo);
		if (chrom_info_file.exists()) {
			System.out.println("parsing " + modChromInfo + " for: " + genome_version);
			InputStream chromstream = new FileInputStream(chrom_info_file);
			ChromInfoParser.parse(chromstream, gmodel, genome_version);
			GeneralUtils.safeClose(chromstream);
		} else {
			System.out.println("couldn't find " + modChromInfo + "  for genome: " + genome_version);
			System.out.println("looking for " + liftAll + " instead");
			File lift_file = new File(genome_directory, "liftAll.lft");
			if (lift_file.exists()) {
				System.out.println("parsing " + liftAll + " for: " + genome_version);
				InputStream liftstream = new FileInputStream(lift_file);
				LiftParser.parse(liftstream, gmodel, genome_version);
				GeneralUtils.safeClose(liftstream);
			} else {
				System.out.println("couldn't find " + modChromInfo + " or " + liftAll + " for genome!!! " + genome_version);
			}
		}
	}

	/**Loads a originalFile's lines into a hash first column is the key, second the value.
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
	 *  sort genomes based on "organism_order.txt" config originalFile if present
	 * @param organisms
	 * @param org_order_filename
	 */
	public static final void sortGenomes(Map<String, List<AnnotatedSeqGroup>> organisms, String org_order_filename) {
		// sort genomes based on "organism_order.txt" config originalFile if present
		// get Map.Entry for organism, sort based on order in organism_order.txt,
		//    put in order in new LinkedHashMap(), then replace as organisms field
		File org_order_file = new File(org_order_filename);
		if (SORT_SOURCES_BY_ORGANISM && org_order_file.exists()) {
			Comparator<String> org_comp = new MatchToListComparator(org_order_filename);
			List<String> orglist = new ArrayList<String>(organisms.keySet());
			Collections.sort(orglist, org_comp);
			Map<String, List<AnnotatedSeqGroup>> sorted_organisms = new LinkedHashMap<String, List<AnnotatedSeqGroup>>();
			for (String org : orglist) {
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

	/**
	 *   Recursively call on each child file;
	 *   if not directory, see if can parse as annotation originalFile.
	 *   if type prefix is null, then at top level of genome directory, so make type_prefix = "" when recursing down
	 */
	public static final void loadAnnotsFromFile(
			File genome_directory,
			AnnotatedSeqGroup genome,
			Map<String, String> graph_name2dir,
			Map<String, String> graph_name2file,
			String dataRoot) {

		try {
		ServerUtils.loadAnnotsFromFileRecurse(genome_directory, genome, "", graph_name2dir, graph_name2file, dataRoot);
		}
catch (Exception ex) {
	ex.printStackTrace();
}
		// Index the IDs in the symmetries for speed and memory usage in name search.
		//IndexingUtils.writeIndexedIDs(genome, results);
	}

		/**
	 *   If current_file is directory:
	 *       if ".seqs" suffix, then handle as graphs
	 *       otherwise recursively call on each child files;
	 *   if not directory, see if can parse as annotation originalFile.
	 */
	private static final void loadAnnotsFromFileRecurse(File current_file, AnnotatedSeqGroup genome, String type_prefix,
			Map<String, String> graph_name2dir,
			Map<String, String> graph_name2file,
			String dataRoot) {
		String file_name = current_file.getName();
		String file_path = current_file.getPath();

		String type_name = type_prefix + file_name;
		String new_type_prefix = type_name + "/";
		

		// if current originalFile is directory, then descend down into child files
		if (current_file.isDirectory()) {
			loadAnnotsFromDir(type_name, file_path, genome, current_file, new_type_prefix, graph_name2dir, graph_name2file, dataRoot);
			return;
		}

		if (type_name.endsWith(".bar")) {
			// String file_path = current_file.getPath();
			// special casing so bar files are seen in types request, but not parsed in on startup
			//    (because using graph slicing so don't have to pull all bar originalFile graphs into memory)
			System.out.println("@@@ adding graph file to types: " + type_name + ", path: " + file_path);
			graph_name2file.put(type_name, file_path);
			return;
		}

		if (!annots_map.isEmpty() && !annots_map.containsKey(file_name)) {
			// we have loaded in an annots.xml originalFile, but yet this originalFile is not in it and should be ignored.
			return;
		}

		if (file_name.equals("mod_chromInfo.txt") || file_name.equals("liftAll.lft")) {
			// for loading annotations, ignore the genome sequence data files
			return;
		}

		// current originalFile is not a directory, so try and recognize as annotation originalFile


		//System.out.println("loading annotations of " + current_file.getName());

		indexOrLoadFile(dataRoot, current_file, type_name, genome);
	}


	/**
	 * Index the file, if possible.
	 * @param dataRoot
	 * @param file
	 * @param genome
	 * @param loadedSyms
	 */
	private static void indexOrLoadFile(String dataRoot, File file, String stream_name, AnnotatedSeqGroup genome) {

		String originalFileName = file.getName();

		IndexWriter iWriter = ParserController.getIndexWriter(originalFileName);

		if (iWriter == null) {
			loadAnnotFile(file, stream_name, genome, false);
			//System.out.println("Type " + typeName + " is not optimizable");
			// Not yet indexable
			return;
		}

		AnnotatedSeqGroup tempGenome = tempGenome(genome);
		List loadedSyms = loadAnnotFile(file, stream_name, tempGenome, true);
		System.out.println("Indexing " + originalFileName);
		determineIndexes(genome,
				tempGenome, dataRoot, file, loadedSyms, iWriter, stream_name);
	}

	/**
	 * Create a temporary shallow-copy genome, to avoid any side-effects.
	 * @param oldGenome
	 * @return
	 */
	private static AnnotatedSeqGroup tempGenome(AnnotatedSeqGroup oldGenome) {
		AnnotatedSeqGroup tempGenome = new AnnotatedSeqGroup(oldGenome.getID());
		tempGenome.setOrganism(oldGenome.getOrganism());
		if (oldGenome == null) {
			return tempGenome;
		}
		for (BioSeq seq : oldGenome.getSeqList()) {
			tempGenome.addSeq(seq.getID(), seq.getLength());
		}
		return tempGenome;
	}

	/**
	 * Generate indexes.
	 * @param genome
	 * @param dataRoot
	 * @param file
	 * @param originalFileName
	 * @param loadedSyms
	 * @param iWriter
	 * @param typeName
	 */
	private static void determineIndexes(
			AnnotatedSeqGroup originalGenome, AnnotatedSeqGroup tempGenome, 
			String dataRoot, File file, List loadedSyms, IndexWriter iWriter, String stream_name) {

		String extension = "";
		if (stream_name.endsWith(".link.psl")) {
			extension = stream_name.substring(stream_name.lastIndexOf(".link.psl"),
				stream_name.length());
		} else {
			extension = stream_name.substring(stream_name.lastIndexOf("."),
				stream_name.length());
		}
		String typeName = ParserController.GetAnnotType(annots_map, stream_name, extension);
		String returnTypeName = typeName;
		if (stream_name.endsWith(".link.psl")) {
			// Nasty hack necessary to add "netaffx consensus" to type names returned by GetGenomeType
			returnTypeName = typeName + " " + ProbeSetDisplayPlugin.CONSENSUS_TYPE;
		}

		for (BioSeq originalSeq : originalGenome.getSeqList()) {
			BioSeq tempSeq = tempGenome.getSeq(originalSeq.getID());
			if (tempSeq == null) {
				continue;	// ignore; this is a seq that was added during parsing.
			}

			IndexedSyms iSyms = null;
			String dirName = indexedDirName(dataRoot, tempGenome, tempSeq);
			String indexedAnnotationsFileName = indexedFileName(dataRoot, file.getName(), tempGenome, tempSeq);
			File indexedAnnotationsFile = new File(indexedAnnotationsFileName);

			createDirIfNecessary(dirName);

			// Sort symmetries for this specific chromosome.
			List<SeqSymmetry> sortedSyms =
					IndexingUtils.getSortedAnnotationsForChrom(loadedSyms, tempSeq, iWriter.getComparator(tempSeq));
			iSyms = new IndexedSyms(sortedSyms.size(), indexedAnnotationsFile, typeName, iWriter);
			// add symmetries to the chromosome (used by types request)
			// TODO: use this for names request
			originalSeq.addIndexedSyms(returnTypeName, iSyms);
			// Write the annotations out to a file.
			IndexingUtils.writeIndexedAnnotations(sortedSyms, tempSeq, iSyms, indexedAnnotationsFileName);
		}
	}


	// filename of indexed annotations.
	private static String indexedFileName(String dataRoot, String fileName, AnnotatedSeqGroup genome, BioSeq seq) {
		return indexedDirName(dataRoot, genome, seq) + "/" + fileName;
	}
	private static String indexedDirName(String dataRoot, AnnotatedSeqGroup genome, BioSeq seq) {
		String optimizedDirectory = dataRoot + ".indexed";
		return optimizedDirectory + "/" + genome.getOrganism() + "/" + genome.getID() + "/" + seq.getID();
	}

	private static void createDirIfNecessary(String dirName) {
		// Make sure the appropriate .indexed/species/version/chr directory exists.
		// If not, create it.
		File newFile = new File(dirName);
		if (!newFile.exists()) {
			if (!new File(dirName).mkdirs()) {
				System.out.println("ERROR: Couldn't create directory: " + dirName);
				System.exit(-1);
			} else {
				System.out.println("Created new directory: " + dirName);
			}
		}
	}


	private static List loadAnnotFile(File current_file, String stream_name, AnnotatedSeqGroup genome, boolean isIndexed) {
		InputStream istr = null;
		List results = null;
		try {
			istr = new BufferedInputStream(new FileInputStream(current_file));
			if (!isIndexed) {
				results = ParserController.parse(istr, annots_map, stream_name, gmodel, genome);
			} else {
				results = ParserController.parseIndexed(istr, annots_map, stream_name, genome);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(istr);
		}
		return results;
	}

	

	public static final void loadAnnotsFromDir(
			String type_name,
			String file_path,
			AnnotatedSeqGroup genome,
			File current_file,
			String new_type_prefix,
			Map<String, String> graph_name2dir,
			Map<String, String> graph_name2file,
			String dataRoot) {
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
			// each originalFile in directory is same annotation type, but for a single originalSeq?
			// assuming bar files for now, each with starting with originalSeq id?
			//	String graph_name = file_name.substring(0, file_name.length() - graph_dir_suffix.length());
			String graph_name = type_name.substring(0, type_name.length() - graph_dir_suffix.length());
			System.out.println("@@@ adding graph directory to types: " + graph_name + ", path: " + file_path);
			graph_name2dir.put(graph_name, file_path);
		} else {
			//System.out.println("checking for annotations in directory: " + current_file);
			File[] child_files = current_file.listFiles(new HiddenFileFilter());
			Arrays.sort(child_files);
			for (File child_file : child_files) {
				loadAnnotsFromFileRecurse(child_file, genome, new_type_prefix, graph_name2dir, graph_name2file, dataRoot);
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
			//	   Collections.sort(sortedSyms, new SeqSymIdComparator());
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
	 *     If rng is null or "", will set to span to [0, originalSeq.getLength()]
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

	/** this is the main call to retrieve symmetries meeting query constraints */
	public static List<SeqSymmetry> getIntersectedSymmetries(SeqSpan overlap_span, String query_type, SeqSpan inside_span) {
		List<SeqSymmetry> result =
				ServerUtils.getOverlappedSymmetries(overlap_span, query_type);
		if (result == null) {
			result = Collections.<SeqSymmetry>emptyList();
		}
		if (inside_span != null) {
			result = ServerUtils.specifiedInsideSpan(inside_span, result);
		}
		return result;
	}

	/**
	 *
	 *  Currently assumes:
	 *    query_span's originalSeq is a BioSeq (which implies top-level annots are TypeContainerAnnots)
	 *    only one IntervalSearchSym child for each TypeContainerAnnot
	 *  Should expand soon so loadedSyms can be returned from multiple IntervalSearchSyms children
	 *      of the TypeContainerAnnot
	 */
	public static final List<SeqSymmetry> getOverlappedSymmetries(SeqSpan query_span, String annot_type) {
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
		} else {
			// Couldn't find it.  See if it's been indexed.
			IndexedSyms iSyms = seq.getIndexedSym(annot_type);
			if (iSyms != null) {
				return getIndexedOverlappedSymmetries(
						query_span,
						iSyms,
						annot_type,
						seq.getSeqGroup());
			}
		}
		return Collections.<SeqSymmetry>emptyList();
	}

	// if an inside_span specified, then filter out intersected symmetries based on this:
	//    don't return symmetries with a min < inside_span.min() or max > inside_span.max()  (even if they overlap query interval)s
	public static final <SymExtended extends SeqSymmetry> List<SymExtended> specifiedInsideSpan(
			SeqSpan inside_span, List<SymExtended> result) {
		int inside_min = inside_span.getMin();
		int inside_max = inside_span.getMax();
		MutableAnnotatedBioSeq iseq = inside_span.getBioSeq();
		MutableSeqSpan testspan = new SimpleMutableSeqSpan();
		List<SymExtended> orig_result = result;
		int rcount = orig_result.size();
		result = new ArrayList<SymExtended>(rcount);
		for (SymExtended sym : orig_result) {
			// fill in testspan with span values for sym (on aseq)
			sym.getSpan(iseq, testspan);
			if ((testspan.getMin() >= inside_min) && (testspan.getMax() <= inside_max)) {
				result.add(sym);
			}
		}
		System.out.println("  overlapping annotations that passed inside_span constraints: " + result.size());
		return result;
	}

	/**
	 * Get the list of symmetries.
	 * @param overlap_span
	 * @param min - array of min values
	 * @param max - array of max values
	 * @param indexedFile - indexed file to read from
	 * @param filePos - array of indexed file positions
	 * @param group
	 * @return
	 */
	public static List getIndexedOverlappedSymmetries(
			SeqSpan overlap_span,
			IndexedSyms iSyms,
			String annot_type,
			AnnotatedSeqGroup group) {
		FileInputStream fis = null;
		InputStream newIstr = null;
		DataInputStream dis = null;
		try {
			int[] overlapRange = new int[2];
			int[] outputRange = new int[2];
			overlapRange[0] = overlap_span.getMin();
			overlapRange[1] = overlap_span.getMax();
			IndexingUtils.findMaxOverlap(overlapRange, outputRange, iSyms.min, iSyms.max);
			int minPos = outputRange[0];
			// We add 1 to the maxPos index.
			// Since filePos is recorded at the *beginning* of each line, this allows us to read the last element.
			int maxPos = outputRange[1] + 1;
			fis = new FileInputStream(iSyms.file);
			byte[] bytes = IndexingUtils.readBytesFromFile(
					fis, iSyms.filePos[minPos], (int) (iSyms.filePos[maxPos] - iSyms.filePos[minPos]));

			if (iSyms.iWriter instanceof PSLParser && iSyms.file.getName().endsWith(".link.psl")) {
				String indexesFileName = iSyms.file.getAbsolutePath();
				newIstr = readAdditionalLinkPSLIndex(indexesFileName, annot_type, bytes);
			} else {
				newIstr = new ByteArrayInputStream(bytes);
			}
			dis = new DataInputStream(newIstr);

			return iSyms.iWriter.parse(dis, annot_type, group);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		finally {
			GeneralUtils.safeClose(fis);
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(newIstr);
		}
	}


	// special case for link.psl files
	// we need to append the track name, and the probesets
	private static ByteArrayInputStream readAdditionalLinkPSLIndex(
			String indexesFileName, String annot_type, byte[] bytes1) throws IOException {
		String secondIndexesFileName = indexesFileName.substring(0, indexesFileName.lastIndexOf(".link.psl"));
		secondIndexesFileName += ".link2.psl";
		
		File secondIndexesFile = new File(secondIndexesFileName);
		int bytes2Len = (int) secondIndexesFile.length();
		byte[] bytes0 = PSLParser.trackLine(annot_type, "Consensus Sequences").getBytes();
		// Determine overall length
		int bytes0Len = bytes0.length;
		int bytes1Len = bytes1.length;
		byte[] combinedByteArr = new byte[bytes0Len + bytes1Len + bytes2Len];

		// Copy in arrays.
		// copy 0th byte array (trackLine)
		System.arraycopy(bytes0, 0, combinedByteArr, 0, bytes0Len);
		bytes0 = null;	// now unused

		// copy 1st byte array (consensus syms)
		System.arraycopy(bytes1, 0, combinedByteArr, bytes0Len, bytes1Len);
		bytes1 = null;	// now unused

		// copy 2nd byte array (probeset syms)
		FileInputStream fis = null;
		byte[] bytes2 = null;
		try {
			fis = new FileInputStream(secondIndexesFileName);
			bytes2 = IndexingUtils.readBytesFromFile(
					fis, 0, bytes2Len);
		} finally {
			GeneralUtils.safeClose(fis);
		}
		System.arraycopy(bytes2, 0, combinedByteArr, bytes0Len + bytes1Len, bytes2Len);
		bytes2 = null;	// now unused

		return new ByteArrayInputStream(combinedByteArr);
	}


	// Print out the genomes
	public static final void printGenomes(Map<String, List<AnnotatedSeqGroup>> organisms) {
		for (Map.Entry<String, List<AnnotatedSeqGroup>> ent : organisms.entrySet()) {
			String org = ent.getKey();
			System.out.println("Organism: " + org);
			for (AnnotatedSeqGroup version : ent.getValue()) {
				System.out.println("    Genome version: " + version.getID() + ", organism: " + version.getOrganism() + ", seq count: " + version.getSeqCount());
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

		// adding in any graph files as additional types (with type id = originalFile name)
		// this is temporary, need a better solution soon -- should probably add empty graphs to seqs to have graphs
		//    show up in originalSeq.getTypes(), but without actually being loaded??
		for (String gname : graph_name2file.keySet()) {
			genome_types.put(gname, graph_formats);  // should probably get formats instead from "preferred_formats"?
		}

		for (String gname : graph_name2dir.keySet()) {
			genome_types.put(gname, graph_formats);  // should probably get formats instead from "preferred_formats"?
		}

		return genome_types;
	}

	// iterate over seqs to collect annotation types
	private static final Map<String, List<String>> getGenomeTypes(List<BioSeq> seqList) {
		Map<String, List<String>> genome_types = new LinkedHashMap<String, List<String>>();
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
			for (String type : aseq.getIndexedTypeList()) {
				if (genome_types.get(type) != null) {
					continue;
				}
				IndexedSyms iSyms = aseq.getIndexedSym(type);
				List<String> flist = new ArrayList<String>();
				flist.addAll(iSyms.iWriter.getFormatPrefList());
				genome_types.put(type, flist);
			}
		}
		return genome_types;
	}
}
