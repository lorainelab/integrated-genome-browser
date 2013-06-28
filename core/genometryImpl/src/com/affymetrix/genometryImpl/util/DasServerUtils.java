package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.comparator.MatchToListComparator;
import com.affymetrix.genometryImpl.comparator.GenomeVersionDateComparator;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.parsers.LiftParser;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.symloader.*;
import com.affymetrix.genometryImpl.symmetry.SearchableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.IndexingUtils.IndexedSyms;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utils for DAS/2 and other servers.
 */
public abstract class DasServerUtils {
	private static final boolean SORT_SOURCES_BY_ORGANISM = true;
	private static final boolean SORT_VERSIONS_BY_DATE_CONVENTION = true;
	private static final Pattern interval_splitter = Pattern.compile(":");
	
	private static final String modChromInfo = "mod_chromInfo.txt";
	private static final String liftAll = "liftAll.lft";
	public static final List<String> BAR_FORMATS = new ArrayList<String>();

	static {
		BAR_FORMATS.add("bar");
	}

	public static void parseChromosomeData(File genome_directory, AnnotatedSeqGroup genome) throws IOException {
		String genome_version = genome.getID();
		File chrom_info_file = new File(genome_directory, modChromInfo);
		if (chrom_info_file.exists()) {
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.INFO,
					"parsing {0} for: {1}", new Object[]{modChromInfo,genome_version});
			InputStream chromstream = new FileInputStream(chrom_info_file);
			try {
				ChromInfoParser.parse(chromstream, genome);
			} finally {
				GeneralUtils.safeClose(chromstream);
			}
		} else {
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.INFO,
					"couldn't find {0} for: {1}", new Object[]{modChromInfo,genome_version});
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.INFO,
					"looking for {0} instead", liftAll);
			File lift_file = new File(genome_directory, "liftAll.lft");
			if (lift_file.exists()) {
				Logger.getLogger(DasServerUtils.class.getName()).log(Level.INFO,
						"parsing {0} for: {1}", new Object[]{liftAll,genome_version});
				InputStream liftstream = new FileInputStream(lift_file);
				try {
					LiftParser.parse(liftstream, genome);
				} finally {
					GeneralUtils.safeClose(liftstream);
				}
			} else {
				Logger.getLogger(DasServerUtils.class.getName()).log(Level.SEVERE,
						"couldn't find {0} or {1} for genome!!! {2}", new Object[]{modChromInfo, liftAll, genome_version});
			}
		}
	}

	/**
	 * Loads a originalFile's lines into a hash.
	 * The first column is the key, second the value.
	 * Skips blank lines and those starting with a '#'
	 * @return null if an exception in thrown
	 * */
	public static HashMap<String, String> loadFileIntoHashMap(File file) {
		BufferedReader in = null;
		HashMap<String, String> names = new HashMap<String, String>();
		try {
			in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				String[] keyValue = line.split("\\s+");
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

	/**
	 * Load synonyms from file into lookup.
	 */
	public static void loadSynonyms(File synfile, SynonymLookup lookup) {
		if (synfile.exists()) {
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.INFO,
					"Synonym file {0} found, loading synonyms", synfile.getName());
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(synfile);
				lookup.loadSynonyms(fis);
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				GeneralUtils.safeClose(fis);
			}
		} else {
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.INFO,
					"Synonym file {0} not found, therefore not using synonyms", synfile.getName());
		}
	}

	/** sorts genomes and versions within genomes
	 *  sort genomes based on "organism_order.txt" config originalFile if present
	 * @param organisms
	 * @param org_order_filename
	 */
	public static void sortGenomes(Map<String, List<AnnotatedSeqGroup>> organisms, String org_order_filename) {
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

	public static void loadGenoPubAnnotFromDir(String type_name,
			String file_path,
			AnnotatedSeqGroup genome,
			File current_file,
			Integer annot_id,
			Map<String,String> graph_name2dir) {
		// each file in directory is same annotation type, but for a single seq?
		// assuming bar files for now, each with starting with seq id?
		//  String graph_name = file_name.substring(0, file_name.length() - graph_dir_suffix.length());
		Logger.getLogger(DasServerUtils.class.getName()).log(Level.FINE,
				"@@@ adding graph directory to types: {0}, path: {1}", new Object[]{type_name, file_path});
		graph_name2dir.put(type_name, file_path);
		genome.addType(type_name, annot_id);

	}

	public static void unloadGenoPubAnnot(String type_name,
			AnnotatedSeqGroup genome,
			Map<String,String> graph_name2dir) {
		
		
		if (graph_name2dir != null && graph_name2dir.containsKey(type_name)) {
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.FINE,
					"@@@ removing graph directory to types: {0}", type_name);
			graph_name2dir.remove(type_name);
			
		}  else {
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.FINE,
					"@@@ removing annotation {0}", type_name);
			List<BioSeq> seqList = genome.getSeqList();
			for (BioSeq aseq : seqList) {
				SymWithProps tannot = aseq.getAnnotation(type_name);			
				if (tannot != null) {
					aseq.unloadAnnotation(tannot);
					tannot = null;
				} else {
					IndexedSyms iSyms = aseq.getIndexedSym(type_name);
					if (iSyms != null) {
						if (!aseq.removeIndexedSym(type_name)) {
							Logger.getLogger(DasServerUtils.class.getName()).log(
									Level.WARNING, "Unable to remove indexed annotation {0}", type_name);
						}
						iSyms = null;
					}
				}
			}
		}
		genome.removeType(type_name);

	}
	
	/**
	 *  Differs from Das2FeatureSaxParser.getLocationSpan():
	 *     Won't add unrecognized seqids or null groups
	 *     If rng is null or "", will set to span to [0, originalSeq.getLength()]
	 */
	public static SeqSpan getLocationSpan(String seqid, String rng, AnnotatedSeqGroup group) {
		if (seqid == null || group == null) {
			return null;
		}
		BioSeq seq = group.getSeq(seqid);
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
				Logger.getLogger(DasServerUtils.class.getName()).log(Level.INFO,
						"Problem parsing a query parameter range filter: {0}", rng);
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

	/**
	 * Retrieve symmetries meeting query constraints.
	 * @param overlap_span
	 * @param query_type - annotation "type", which is feature name.
	 * @param inside_span
	 */
	public static List<SeqSymmetry> getIntersectedSymmetries(SeqSpan overlap_span, String query_type, SeqSpan inside_span) {
		List<SeqSymmetry> result =
				DasServerUtils.getOverlappedSymmetries(overlap_span, query_type);
		if (result == null) {
			result = Collections.<SeqSymmetry>emptyList();
		}
		if (inside_span != null) {
			result = DasServerUtils.specifiedInsideSpan(inside_span, result);
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
	public static List<SeqSymmetry> getOverlappedSymmetries(SeqSpan query_span, String annot_type) {
		BioSeq seq = query_span.getBioSeq();
		SymWithProps container = seq.getAnnotation(annot_type);
		if (container != null) {
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.FINE,
					"non-indexed request for {0}", annot_type);
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
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.FINE,
					"indexed request for {0}", annot_type);
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
	//    don't return symmetries with a min < inside_span.min() or max > inside_span.max()  (even if they overlap query interval)
	public static <SymExtended extends SeqSymmetry> List<SymExtended> specifiedInsideSpan(
			SeqSpan inside_span, List<SymExtended> result) {
		int inside_min = inside_span.getMin();
		int inside_max = inside_span.getMax();
		BioSeq iseq = inside_span.getBioSeq();
		MutableSeqSpan testspan = new SimpleMutableSeqSpan();
		List<SymExtended> orig_result = result;
		result = new ArrayList<SymExtended>(orig_result.size());
		for (SymExtended sym : orig_result) {
			// fill in testspan with span values for sym (on aseq)
			sym.getSpan(iseq, testspan);
			if ((testspan.getMin() >= inside_min) && (testspan.getMax() <= inside_max)) {
				result.add(sym);
			}
		}
		Logger.getLogger(DasServerUtils.class.getName()).log(Level.FINE,
				"  overlapping annotations that passed inside_span constraints: {0}", result.size());
		return result;
	}

	
	/**
	 * Get the list of symmetries.
	 * @param overlap_span
	 * @param iSyms
	 * @param annot_type
	 * @param group
	 * @return list of indexed overlapped symmetries
	 */
	public static List<SeqSymmetry> getIndexedOverlappedSymmetries(
			SeqSpan overlap_span,
			IndexedSyms iSyms,
			String annot_type,
			AnnotatedSeqGroup group) {

		List<? extends SeqSymmetry> symList = getIndexedSymmetries(overlap_span,iSyms,annot_type,group);

		// We need to filter this list to only return overlaps.
		// Due to the way indexing is implemented, there may have been additional symmetries outside of the specified interval.
		// This violates the DAS/2 specification, but more importantly, IGB gets confused.
		return SeqUtils.filterForOverlappingSymmetries(overlap_span,symList);
	}

	/**
	 * Get the list of symmetries
	 * @param overlap_span
	 * @param iSyms
	 * @param annot_type
	 * @param group
	 * @return list of indexed seq symmetries
	 */
	private static List<? extends SeqSymmetry> getIndexedSymmetries(
			SeqSpan overlap_span,
			IndexedSyms iSyms,
			String annot_type,
			AnnotatedSeqGroup group) {

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

			if (minPos >= maxPos) {
				// Nothing found, or invalid values passed in.
				return Collections.<SeqSymmetry>emptyList();
			}
			byte[] bytes = IndexingUtils.readBytesFromFile(
					iSyms.file, iSyms.filePos[minPos], (int) (iSyms.filePos[maxPos] - iSyms.filePos[minPos]));

			if ((iSyms.iWriter instanceof PSLParser || iSyms.iWriter instanceof PSL) && iSyms.ext.equalsIgnoreCase("link.psl")) {
				String indexesFileName = iSyms.file.getAbsolutePath();
				newIstr = IndexingUtils.readAdditionalLinkPSLIndex(indexesFileName, annot_type, bytes);
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
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(newIstr);
		}
	}

	// Print out the genomes
	public static void printGenomes(Map<String, List<AnnotatedSeqGroup>> organisms) {
		for (Map.Entry<String, List<AnnotatedSeqGroup>> ent : organisms.entrySet()) {
			String org = ent.getKey();
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.INFO, "Organism: {0}", org);
			for (AnnotatedSeqGroup version : ent.getValue()) {
				Logger.getLogger(DasServerUtils.class.getName()).log(Level.INFO, 
						"    Genome version: {0}, organism: {1}, seq count: {2}",
						new Object[]{version.getID(), version.getOrganism(), version.getSeqCount()});
			}
		}
	}

}
