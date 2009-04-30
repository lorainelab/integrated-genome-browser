package com.affymetrix.genoviz.parser;

import com.affymetrix.genoviz.datamodel.BaseConfidence;
import com.affymetrix.genoviz.datamodel.ReadConfidence;

import com.affymetrix.genoviz.util.GeneralUtils;
import java.net.*;
import java.io.*;


/**
 * parses output from FASTQ.
 *
 * <p> It will look something like this:
 * <pre>
 * @EAS54_6_R1_2_1_413_324
 * CCCTTCTTGTCTTCAGCGTTTCTCC
 * +
 * ;;3;;;;;;;;;;;;7;;;;;;;88
 * @EAS54_6_R1_2_1_540_792
 * TTGGCAGGCCAAGGCCGATGGATCA
 * +
 * ;;;;;;;;;;;7;;;;;-;;;3;83
 * @EAS54_6_R1_2_1_443_348
 * GTTGCTTCTGGCGTGGGTGGGGGGG
 * +EAS54_6_R1_2_1_443_348
 * ;;;;;;;;;;;9;7;;.7;393333
 * 
 * </pre>
 *
 * @author John Nicol
 */
public class FASTQParser
{

	/**
	 * constructs a ReadConfidence data model
	 *
	 * @param fastqURL
	 */
	public static ReadConfidence parseFiles ( URL fastqURL) {
		InputStream fastqIn = null;
		BufferedReader fastqDataIn = null;

		try {
			fastqIn = fastqURL.openStream();
			fastqDataIn = new BufferedReader( new InputStreamReader( fastqIn ) );
		} catch (MalformedURLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		ReadConfidence readConf = new ReadConfidence();

		boolean FASTAline = true;	// The line is either FASTA or confidence scores.
		try {

			String bases = null;
			String confidence = null;
			String fastqLine;
			while ((fastqLine = fastqDataIn.readLine()) != null) {
				// Skip comment lines
				if (fastqLine.startsWith("@") || fastqLine.startsWith("+")) {
					continue;
				}
				if (FASTAline) {
					bases = fastqLine;
				} else {
					confidence = fastqLine;

					if (bases == null) {
						System.out.println("Couldn't find bases for confidence -- stopping parsing.");
						break;
					}
					
					if (bases.length() != confidence.length()) {
						System.out.println("Bases length was not equal to confidence length -- stopping parsing.");
						break;
					}

					int baseLength = bases.length();	// for performance
					for (int i=0;i<baseLength;i++) {
						char base = bases.charAt(i);
						char conf = confidence.charAt(i);
						BaseConfidence baseConf = new BaseConfidence( base, (int)conf );
						readConf.addBaseConfidence( baseConf );
					}
					bases = null;	// Sanity check
				}
				FASTAline = !FASTAline;
			}
		}
		catch ( IOException e ) {
			e.printStackTrace();
		} finally {
			GeneralUtils.safeClose(fastqIn);
			GeneralUtils.safeClose(fastqDataIn);
		}

		return readConf;

	}

}
