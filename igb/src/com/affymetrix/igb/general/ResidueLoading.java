package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.FastaParser;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.das.DasLoader;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import com.affymetrix.genometryImpl.symloader.BNIB;
import com.affymetrix.genometryImpl.symloader.TwoBit;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.igb.Application;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @version $Id$
 */
public final class ResidueLoading {

	enum FORMAT {
		BNIB,
		RAW,
		FASTA
	};

	enum QFORMAT{
		BNIB,
		VTWOBIT,
		TWOBIT,
		FA
	};

	/**
	 * Get residues from servers: DAS/2, Quickload, or DAS/1.
	 * Also gets partial residues.
	 * @param genomeVersionName -- name of the genome.
	 * @param seq_name -- sequence (chromosome) name
	 * @param span	-- May be null.  If not, then it's used for partial loading.
	 * @return boolean
	 */
	// Most confusing thing here -- certain parsers update the composition, and certain ones do not.
	// DAS/1 and partial loading in DAS/2 do not update the composition, so it's done separately.
	public static boolean getResidues(Set<GenericVersion> versionsWithChrom, String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span) {
		if (span == null) {
			span = new SimpleSeqSpan(min, max, aseq);
		}
		List<GenericVersion> versions = new ArrayList<GenericVersion>(versionsWithChrom);
		String seq_name = aseq.getID();
		boolean residuesLoaded = false;
		for (GenericServer server : ServerList.getServerInstance().getAllServers()) {
			if (!server.isEnabled()) {
				continue;
			}
			String serverDescription = server.serverName + " " + server.serverType;
			String msg = "Loading sequence for "+seq_name+" from "+serverDescription;
			Application.getSingleton().addNotLockedUpMsg(msg);
			switch (server.serverType) {
			case DAS2:
				if (getDAS2Residues(server, versions, genomeVersionName, aseq, min, max, span)) {
					residuesLoaded = true;
				}
				break;
			case QuickLoad:
				if (getQuickLoadResidues(server, versions, genomeVersionName, aseq, min, max, span)) {
					residuesLoaded = true;
				}
				break;
			case DAS:
				if (getDASResidues(server, versions, genomeVersionName, aseq, min, max, span)) {
					residuesLoaded = true;
				}
				break;
			}
			Application.getSingleton().removeNotLockedUpMsg(msg);
			if (residuesLoaded) {
				Application.getSingleton().setStatus(MessageFormat.format(
						"Completed loading sequence for {0} : {1} - {2} from {3}", 
						new Object[]{seq_name,min,max,serverDescription}));
				return true;
			}
		}
		Application.getSingleton().setStatus("");
		return false;

	}

	private static boolean loadDAS2Residues(BioSeq aseq, String uri, SeqSpan span, boolean partial_load) {
		AnnotatedSeqGroup seq_group = aseq.getSeqGroup();
		if (partial_load) {
			String residues = GetPartialFASTADas2Residues(uri);
			if (residues != null) {
				BioSeq.addResiduesToComposition(aseq, residues, span);
				return true;
			}
		}
		else {
			if (LoadResiduesFromDAS2(aseq, seq_group, uri)) {
				BioSeq.addResiduesToComposition(aseq);
				return true;
			}
		}
		return false;
	}

	private static boolean getDAS2Residues(GenericServer server, List<GenericVersion> versions, String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span) {
		String seq_name = aseq.getID();
		boolean partial_load = (min > 0 || max < (aseq.getLength()-1));	// Are we only asking for part of the sequence?
		for (GenericVersion version : versions) {
			if (!server.equals(version.gServer)) {
				continue;
			}
			Das2VersionedSource das2version = (Das2VersionedSource) version.versionSourceObj;
			Set<String> format = das2version.getResidueFormat(seq_name);
			FORMAT[] formats = null;

			if (format != null && !format.isEmpty()) {
				//Try to check if format data is available from Das2
				if (format.contains("bnib")) {
					formats = new FORMAT[]{FORMAT.BNIB};
				} else if (format.contains("raw")) {
					formats = new FORMAT[]{FORMAT.RAW};
				} else if (format.contains("fasta") || format.contains("fa")) {
					formats = new FORMAT[]{FORMAT.FASTA};
				}
			}
			if (formats == null) {
				// If no format information is available then try all formats.
				// Try to load in raw format from DAS2 server.
				// Then try to load in fasta format from DAS2 server.
				formats = partial_load ? new FORMAT[]{FORMAT.RAW, FORMAT.FASTA} : new FORMAT[]{FORMAT.BNIB, FORMAT.RAW, FORMAT.FASTA};
			}
			for (FORMAT formatLoop : formats) {
				String uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, formatLoop);
				if (loadDAS2Residues(aseq, uri, span, partial_load)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean getQuickLoadResidues(GenericServer server, List<GenericVersion> versions, String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span) {
		String seq_name = aseq.getID();
		AnnotatedSeqGroup seq_group = aseq.getSeqGroup();
		for (GenericVersion version : versions) {
			if (!server.equals(version.gServer)) {
				continue;
			}
			String residues = GetQuickLoadResidues(server, version, seq_group, seq_name, server.URL, span, aseq);
			if (residues != null) {
				BioSeq.addResiduesToComposition(aseq, residues, span);
				return true;
			}
		}
		return false;
	}

	private static boolean getDASResidues(GenericServer server, List<GenericVersion> versions, String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span) {
		String seq_name = aseq.getID();
		for (GenericVersion version : versions) {
			if (!server.equals(version.gServer)) {
				continue;
			}
			String residues = DasLoader.getDasResidues(version, seq_name, min, max);
			if (residues != null) {
				BioSeq.addResiduesToComposition(aseq, residues, span);
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the partial residues from the specified QuickLoad server.
	 * @param seq_group
	 * @param path
	 * @param root_url
	 * @param span
	 * @return residue String.
	 */

	private static String GetQuickLoadResidues(
			GenericServer server, GenericVersion version, AnnotatedSeqGroup seq_group, String seq_name, String root_url, SeqSpan span, BioSeq aseq) {
		String common_url = "";
		String path = "";
		SymLoader symloader;
		try {
			URL quickloadURL = new URL((String) server.serverObj);
			QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
			path = quickloadServer.getPath(version.versionName, seq_name);
			common_url = root_url + path + ".";
			String vPath = root_url + quickloadServer.getPath(version.versionName, version.versionName) + ".2bit";

			symloader = determineLoader(common_url, vPath, seq_group, seq_name);

			if (symloader != null) {
				return symloader.getRegionResidues(span);
			}
		} catch (Exception ex) {
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	private static SymLoader determineLoader(String common_url, String vPath, AnnotatedSeqGroup seq_group, String seq_name){
		QFORMAT format = determineFormat(common_url, vPath);

		if(format == null)
			return null;

		URI uri = null;
		try {
			uri = new URI(generateQuickLoadURI(common_url, vPath, format));
		} catch (URISyntaxException ex) {
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.SEVERE, null, ex);
		}

		switch(format){
			case BNIB:
				return new BNIB(uri, "", seq_group);

			case VTWOBIT:
				return new TwoBit(uri, seq_group, seq_name);

			case TWOBIT:
				return new TwoBit(uri, "", seq_group);

//			case FA:
//				return new Fasta(uri, seq_group);
		}

		return null;
	}

	private static QFORMAT determineFormat(String common_url, String vPath){

		for(QFORMAT format : QFORMAT.values()){
			String url_path = generateQuickLoadURI(common_url,vPath,format);
			if(LocalUrlCacher.isValidURL(url_path)){
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE,
							"  Quickload location of bnib file: {0}", url_path);

				return format;
			}
		}

		return null;
	}

	// Generate URI (e.g., "http://www.bioviz.org/das2/genome/A_thaliana_TAIR8/chr1?range=0:1000")
	private static String generateDas2URI(String URL, String genomeVersionName,
			String segmentName, int min, int max, FORMAT Format) {
		Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "trying to load residues via DAS/2");
		String uri = URL + "/" + genomeVersionName + "/" + segmentName + "?format=";
		switch(Format)
		{
			case RAW:
				uri += "raw";
				break;

			case BNIB:
				uri += "bnib";
				break;

			case FASTA:
				uri += "fasta";
				break;
		}

		if (max > -1) {
			// ranged
			uri = uri + "&range=" + min + ":" + max;
		}

		Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "   request URI: {0}", uri);
		return uri;
	}

	// Generate URI (e.g., "http://www.bioviz.org/das2/genome/A_thaliana_TAIR8/chr1.bnib")
	private static String generateQuickLoadURI(String common_url, String vPath, QFORMAT Format) {
		Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "trying to load residues via Quickload");
		switch(Format)
		{
			case BNIB:
				common_url += "bnib";
				break;

			case FA:
				common_url += "fa";
				break;

			case VTWOBIT:
				common_url = vPath;
				break;

			case TWOBIT:
				common_url += "2bit";
				break;

		}

		return common_url;
	}

	// try loading via DAS/2 server that genome was originally modeled from
	private static boolean LoadResiduesFromDAS2(BioSeq aseq, AnnotatedSeqGroup seq_group, String uri) {
		InputStream istr = null;
		BufferedReader buff = null;
		Map<String, String> headers = new HashMap<String, String>();
		try {
			istr = LocalUrlCacher.getInputStream(uri, true, headers);
			String content_type = headers.get("content-type");
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE,
						"    response content-type: {0}", content_type);
			if (istr == null || content_type == null) {
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "  Improper response from DAS/2; aborting DAS/2 residues loading.");
				return false;
			}
			if(content_type.equals("text/raw"))
			{
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.INFO, "   response is in raw format, parsing...");
				buff = new BufferedReader(new InputStreamReader(istr));
				aseq.setResidues(buff.readLine());
				return true;
			}

			if (content_type.equals(NibbleResiduesParser.getMimeType())) {
				// check for bnib format
				// NibbleResiduesParser handles creating a BufferedInputStream from the input stream
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.INFO, "   response is in bnib format, parsing...");
				NibbleResiduesParser.parse(istr, seq_group);
				return true;
			}

			if (content_type.equals(FastaParser.getMimeType())) {
				// check for fasta format
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.INFO, "   response is in fasta format, parsing...");
				FastaParser.parseSingle(istr, seq_group);
				return true;
			}
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "   response is not in accepted format, aborting DAS/2 residues loading");
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(buff);
			GeneralUtils.safeClose(istr);
		}

		return false;
	}

	// try loading via DAS/2 server
	private static String GetPartialFASTADas2Residues(String uri) {
		InputStream istr = null;
		BufferedReader buff = null;
		Map<String, String> headers = new HashMap<String, String>();
		try {
			istr = LocalUrlCacher.getInputStream(uri, true, headers);
			// System.out.println(headers);
			String content_type = headers.get("content-type");
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE,
						"    response content-type: {0}", content_type);
			if (istr == null || content_type == null) {
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "  Didn't get a proper response from DAS/2; aborting DAS/2 residues loading.");
				return null;
			}

			if(content_type.equals("text/raw"))
			{
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.INFO, "   response is in raw format, parsing...");
				buff = new BufferedReader(new InputStreamReader(istr));
				return buff.readLine();
			}

			if (content_type.equals(FastaParser.getMimeType())) {
				// check for fasta format
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.INFO, "   response is in fasta format, parsing...");
				return FastaParser.parseResidues(istr);
			}

			Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "   response is not in accepted format, aborting DAS/2 residues loading");
			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(buff);
			GeneralUtils.safeClose(istr);
		}

		return null;
	}
}
