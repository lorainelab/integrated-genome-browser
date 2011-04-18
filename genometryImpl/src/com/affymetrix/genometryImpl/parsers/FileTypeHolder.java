package com.affymetrix.genometryImpl.parsers;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.das.DASFeatureParser;
import com.affymetrix.genometryImpl.parsers.gchp.AffyCnChpParser;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.parsers.graph.BgrParser;
import com.affymetrix.genometryImpl.parsers.graph.CntParser;
import com.affymetrix.genometryImpl.parsers.graph.GrParser;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.genometryImpl.parsers.graph.ScoredMapParser;
import com.affymetrix.genometryImpl.parsers.graph.SgrParser;
import com.affymetrix.genometryImpl.parsers.graph.WiggleParser;
import com.affymetrix.genometryImpl.parsers.useq.USeqRegionParser;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symloader.BED;
import com.affymetrix.genometryImpl.symloader.BNIB;
import com.affymetrix.genometryImpl.symloader.Fasta;
import com.affymetrix.genometryImpl.symloader.GFF3;
import com.affymetrix.genometryImpl.symloader.Genbank;
import com.affymetrix.genometryImpl.symloader.Gr;
import com.affymetrix.genometryImpl.symloader.PSL;
import com.affymetrix.genometryImpl.symloader.Sgr;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoaderInst;
import com.affymetrix.genometryImpl.symloader.SymLoaderInstNC;
import com.affymetrix.genometryImpl.symloader.TwoBit;
import com.affymetrix.genometryImpl.symloader.USeq;
import com.affymetrix.genometryImpl.symloader.Wiggle;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ServerUtils;

/**
 * all the FileTypeHandler implementations are saved here, included dynamically
 * added FileTypeHandlers
 */
public class FileTypeHolder {
	private static final FileTypeHolder instance = new FileTypeHolder();
	private final Map<String, FileTypeHandler> fileTypeHandlerMap;
	public static FileTypeHolder getInstance() {
		return instance;
	}
	private FileTypeHolder() {
		fileTypeHandlerMap = new HashMap<String, FileTypeHandler>();
		// load all built in FileTypeHandlers
		addFileTypeHandler("Copy Number CHP", new String[]{"cnchp", "lohchp"}, AffyCnChpParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("BAM", new String[]{"bam"}, BAMParser.class, BAM.class);
		addFileTypeHandler("Graph", new String[]{"bar"}, BarParser.class, /* Bar.class */ SymLoaderInstNC.class);
		addFileTypeHandler("BED", new String[]{"bed"}, BedParser.class, BED.class);
		addFileTypeHandler("Binary", new String[]{"bgn"}, BgnParser.class, SymLoaderInst.class);
		addFileTypeHandler("Graph", new String[] {"bgr"}, BgrParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Binary", new String[]{"bp1", "bp2"}, Bprobe1Parser.class, SymLoaderInst.class);
		addFileTypeHandler("Binary", new String[] {"bps"}, BpsParser.class, SymLoaderInst.class);
		addFileTypeHandler("Binary", new String[]{"brpt"}, BrptParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Binary", new String[]{"brs"}, BrsParser.class, SymLoaderInst.class);
		addFileTypeHandler("Binary", new String[]{"bsnp"}, BsnpParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Graph", new String[]{"chp"}, null, SymLoaderInstNC.class); // chp files are handles elsewhere, this is just to get them in the FileChooser popup
		addFileTypeHandler("Copy Number", new String[]{"cnt"}, CntParser.class, SymLoaderInst.class);
		addFileTypeHandler("Cytobands", new String[]{"cyt"}, CytobandParser.class, SymLoaderInst.class);
		addFileTypeHandler("DAS", new String[]{Das2FeatureSaxParser.FEATURES_CONTENT_SUBTYPE, "das2feature", "das2xml", "x-das-feature"}, Das2FeatureSaxParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("DAS", new String[]{"das", "dasxml"}, DASFeatureParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Binary", new String[]{"ead"}, ExonArrayDesignParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("FASTA", new String[]{"fa", "fas", "fasta"}, FastaParser.class, Fasta.class);
		addFileTypeHandler("FishClones", new String[]{FishClonesParser.FILE_EXT}, FishClonesParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Genbank", new String[]{"gb", "gen"}, GenbankParser.class, Genbank.class);
		addFileTypeHandler("GFF", new String[] {"gff3"}, GFF3Parser.class, GFF3.class);
		addFileTypeHandler(
			new FileTypeHandler() {
				String[] extensions = new String[]{"gff", "gtf"};
				@Override
				public String getName() { return "GFF"; }
				@Override
				public String[] getExtensions() { return extensions; }
				@Override
				public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
					if (com.affymetrix.genometryImpl.symloader.GFF3.isGFF3(uri)) {
						return new GFF3(uri, featureName, group);
					}
					else {
						return new SymLoaderInstNC(uri, featureName, group);
					}
				}
				@Override
				public Parser getParser() { return new GFFParser(); }
				@Override
				public IndexWriter getIndexWriter(String stream_name) {
					return null;
				}
			}
		);
		addFileTypeHandler("Graph", new String[]{"gr"}, GrParser.class, Gr.class);
		addFileTypeHandler(
			new FileTypeHandler() {
				String[] extensions = new String[]{"link.psl"};
				@Override
				public String getName() { return "PSL"; }
				@Override
				public String[] getExtensions() { return extensions; }
				@Override
				public SymLoader createSymLoader(URI uri, String featureName,
						AnnotatedSeqGroup group) {
					PSL psl = new PSL(uri, featureName, group);
					psl.setIsLinkPsl(true);
					psl.enableSharedQueryTarget(true);
					return psl;
				}
				@Override
				public Parser getParser() { return new LinkPSLParser(); }
				@Override
				public IndexWriter getIndexWriter(String stream_name) {
					int sindex = stream_name.lastIndexOf("/");
					String type_prefix = (sindex < 0) ? null : stream_name.substring(0, sindex + 1);  // include ending "/" in prefix
					PSLParser parser = new PSLParser();
					if (type_prefix != null) {
						parser.setTrackNamePrefix(type_prefix);
					}
					// assume that want to annotate target seqs, and that these are the seqs
					//    represented in seq_group
					parser.setIsLinkPsl(true);
					parser.enableSharedQueryTarget(true);
					parser.setCreateContainerAnnot(true);
					return parser;
				}
			}
		);
		addFileTypeHandler("Binary", new String[]{"bnib"}, NibbleResiduesParser.class, BNIB.class);
		addFileTypeHandler(
				new FileTypeHandler() {
					String[] extensions = new String[]{"psl", "psl3", "pslx"};
					@Override
					public String getName() { return "PSL"; }
					@Override
					public String[] getExtensions() { return extensions; }
					@Override
					public SymLoader createSymLoader(URI uri, String featureName,
							AnnotatedSeqGroup group) {
						PSL psl = new PSL(uri, featureName, group);
						psl.enableSharedQueryTarget(true);
						return psl;
					}
					@Override
					public Parser getParser() { return new PSLParser(); }
					@Override
					public IndexWriter getIndexWriter(String stream_name) {
						int sindex = stream_name.lastIndexOf("/");
						String type_prefix = (sindex < 0) ? null : stream_name.substring(0, sindex + 1);  // include ending "/" in prefix
						PSLParser iWriter = new PSLParser();
						if (type_prefix != null) {
							iWriter.setTrackNamePrefix(type_prefix);
						}
						return iWriter;
					}
				}
			);
		addFileTypeHandler("Scored Interval", new String[]{"sin", "egr", "egr.txt"}, ScoredIntervalParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Scored Map", new String[]{"map"}, ScoredMapParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Regions", new String[]{SegmenterRptParser.CN_REGION_FILE_EXT, SegmenterRptParser.LOH_REGION_FILE_EXT}, SegmenterRptParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Graph", new String[]{"sgr"}, SgrParser.class, Sgr.class);
		addFileTypeHandler(".2bit", new String[]{"2bit"}, TwoBitParser.class, TwoBit.class);
		addFileTypeHandler("Binary", new String[]{"useq"}, USeqRegionParser.class, USeq.class);
		addFileTypeHandler("Genomic Variation", new String[]{"var"}, VarParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Graph", new String[]{"wig", "bedgraph"}, WiggleParser.class, Wiggle.class);
	}

	private void addFileTypeHandler(final String name, final String[] extensions, final Class<? extends Parser> parserClass, final Class<? extends SymLoader> symLoaderClass) {
		addFileTypeHandler(
			new FileTypeHandler() {
				@Override
				public Parser getParser() {
					try {
						return parserClass.getConstructor().newInstance();
					}
					catch (Exception x) {
						Logger.getLogger(FileTypeHolder.class.getName()).log(Level.SEVERE,
							"Failed to create Parser " + parserClass.getName() + " reason = " + (x.getCause() == null ? x.getMessage() : x.getCause().getMessage()));
						return null;
					}
				}

				@Override
				public String getName() {
					return name;
				}

				@Override
				public String[] getExtensions() {
					return extensions;
				}

				@Override
				public SymLoader createSymLoader(URI uri, String featureName,
						AnnotatedSeqGroup group) {
					try {
						Constructor<?> con = symLoaderClass.getConstructor(URI.class, String.class, AnnotatedSeqGroup.class);
						return (SymLoader)con.newInstance(uri, featureName, group);
					}
					catch (Exception x) {
						Logger.getLogger(FileTypeHolder.class.getName()).log(Level.SEVERE,
							"Failed to create SymLoader " + symLoaderClass.getName() + " reason = " + (x.getCause() == null ? x.getMessage() : x.getCause().getMessage()));
						return null;
					}
				}

				@Override
				public IndexWriter getIndexWriter(String stream_name) {
					Parser parser = getParser();
					if (parser instanceof IndexWriter) {
						return (IndexWriter)parser;
					}
					return null;
				}
			}
		);
	}

	/**
	 * add a new FileTypeHandler for a list of extensions
	 * @param fileTypeHandler the FileTypeHandler
	 */
	public void addFileTypeHandler(FileTypeHandler fileTypeHandler) {
		String[] extensions = fileTypeHandler.getExtensions();
		for (String extension : extensions) {
			if (fileTypeHandlerMap.get(extension) != null) {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.SEVERE, "duplicate SymLoaderFactory for extension {0}!!!", new Object[]{extension});
			}
			fileTypeHandlerMap.put(extension, fileTypeHandler);
		}
	}

	/**
	 * remove an existing FileTypeHandler for a given list of extensions
	 * @param fileTypeHandler the FileTypeHandler
	 */
	public void removeFileTypeHandler(FileTypeHandler fileTypeHandler) {
		String[] extensions = fileTypeHandler.getExtensions();
		for (String extension : extensions) {
			if (fileTypeHandlerMap.get(extension) == null) {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.SEVERE, "missing removed SymLoaderFactory for extension {0}!!!", new Object[]{extension});
			}
			fileTypeHandlerMap.remove(extension);
		}
	}

	/**
	 * find the appropriate Parser for the given extension
	 * @param extension the extension to use
	 * @return the appropriate Parser
	 */
	public FileTypeHandler getFileTypeHandler(String extension) {
		if (extension.startsWith("x-das-feature")) {
			return fileTypeHandlerMap.get("das2xml");
		}
		return fileTypeHandlerMap.get(extension);
	}

	/**
	 * find the appropriate FileTypeHandler for the URI, look at last
	 * two . for extension for double extensions like .link.psl
	 * @param uri the uri to use
	 * @return the appropriate FileTypeHandler
	 */
	public FileTypeHandler getFileTypeHandlerForURI(String uri) {
		String lc = GeneralUtils.stripEndings(uri).toLowerCase();
		FileTypeHandler fileTypeHandler = null;
		String extension = lc;
		int position = lc.lastIndexOf('.');
		if (position > -1) {
			extension = lc.substring(position + 1);
			fileTypeHandler = getFileTypeHandler(extension);
			String prefix = lc.substring(0, position - 1);
			position = prefix.lastIndexOf('.');
			if (position > -1) {
				extension = lc.substring(position + 1);
				if (getFileTypeHandler(extension) != null) {
					fileTypeHandler = getFileTypeHandler(extension);
				}
			}
		}
		return fileTypeHandler;
	}

	/**
	 * get a Map linking file type names to the list of extensions
	 * @return the Map of file type names to their list of extensions
	 */
	public Map<String, List<String>> getNameToExtensionMap() {
		Map<String, List<String>> nameToExtensionMap = new TreeMap<String, List<String>>();
		for (FileTypeHandler fileTypeHandler : new HashSet<FileTypeHandler>(fileTypeHandlerMap.values())) {
			String name = fileTypeHandler.getName();
			List<String> extensions = nameToExtensionMap.get(name);
			if (extensions == null) {
				extensions = new ArrayList<String>();
				nameToExtensionMap.put(name, extensions);
			}
			for (String ext : fileTypeHandler.getExtensions()) {
				extensions.add(ext);
			}
		}
		return nameToExtensionMap;
	}
}
