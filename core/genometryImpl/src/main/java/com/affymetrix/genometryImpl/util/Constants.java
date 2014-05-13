package com.affymetrix.genometryImpl.util;

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
	public static final String contentsTxt = "contents.txt";
	public static final String annotsTxt = "annots.txt";
	public static final String annotsXml = "annots.xml";
	public static final String liftAllLft = "liftAll.lft";
	public static final String modChromInfoTxt = "mod_chromInfo.txt";
	public static final String genomeTxt = "genome.txt";

	//Cached Server filenames
	public static final String serverMapping = "serverMapping.txt";
	public static final String xml_ext = ".xml";
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
	public static final String speciesTxt = "species.txt";

	// Synonym filenames
	public static final String chromosomesTxt = "chromosomes.txt";
	public static final String synonymsTxt = "synonyms.txt";
}
