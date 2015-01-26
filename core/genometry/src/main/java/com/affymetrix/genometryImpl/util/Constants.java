package com.affymetrix.genometry.util;

/**
 *
 * @author jnicol
 */
public class Constants {

	private Constants() {
		//private construtor to prevent instantiation
	}
	public static final String UTF8 = "UTF-8";
	public final static String GENOME_SEQ_ID = "genome";

	//QuickLoad filenames
	public static final String CONTENTS_TXT = "contents.txt";
	public static final String ANNOTS_TXT = "annots.txt";
	public static final String ANNOTS_XML = "annots.xml";
	public static final String LIFT_ALL_LFT = "liftAll.lft";
	public static final String MOD_CHROM_INFO_TXT = "mod_chromInfo.txt";
	public static final String GENOME_TXT = "genome.txt";

	//Cached Server filenames
	public static final String SERVER_MAPPING = "serverMapping.txt";
	public static final String XML_EXTENSION = ".xml";
	/**
	 * The species.txt file maps the scientific name to the data set name and
	 * other data set synonyms<br>
	 *
	 * It handles species name to common names and also subspecies <br>
	 *
	 * When present in a quickload archive, the information is appended to the
	 * SpeciesLookup map data structure. <br>
	 * <br>
	 * Example:<br>
	 * [Arabidopsis thaliana]->[Thale cress] -> A_thaliana
	 */
	public static final String SPECIES_TXT = "species.txt";

	// Synonym filenames
	public static final String CHROMOSOMES_TXT = "chromosomes.txt";
	public static final String SYNONYMS_TXT = "synonyms.txt";
}
