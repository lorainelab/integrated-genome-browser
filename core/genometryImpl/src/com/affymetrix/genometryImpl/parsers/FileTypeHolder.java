package com.affymetrix.genometryImpl.parsers;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.affymetrix.genometryImpl.symloader.Bar;
import com.affymetrix.genometryImpl.symloader.Fasta;
import com.affymetrix.genometryImpl.symloader.FastaIdx;
import com.affymetrix.genometryImpl.symloader.GFF;
import com.affymetrix.genometryImpl.symloader.GFF3;
import com.affymetrix.genometryImpl.symloader.Genbank;
import com.affymetrix.genometryImpl.symloader.Gr;
import com.affymetrix.genometryImpl.symloader.PSL;
import com.affymetrix.genometryImpl.symloader.SAM;
import com.affymetrix.genometryImpl.symloader.Sgr;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoaderInst;
import com.affymetrix.genometryImpl.symloader.SymLoaderInstNC;
import com.affymetrix.genometryImpl.symloader.SymLoaderTabix;
import com.affymetrix.genometryImpl.symloader.TwoBit;
import com.affymetrix.genometryImpl.symloader.USeq;
import com.affymetrix.genometryImpl.symloader.VCF;
import com.affymetrix.genometryImpl.symloader.Wiggle;
import com.affymetrix.genometryImpl.util.GeneralUtils;

/**
 * all the FileTypeHandler implementations are saved here, included dynamically
 * added FileTypeHandlers
 */
public class FileTypeHolder {
	private static final FileTypeHolder instance = new FileTypeHolder();
	private final Map<String, FileTypeHandler> fileTypeHandlerMap;
	private final Map<String, FileTypeHandler> dummyHandlerMap;
	public static FileTypeHolder getInstance() {
		return instance;
	}
	private FileTypeHolder() {
		fileTypeHandlerMap = new HashMap<String, FileTypeHandler>();
		dummyHandlerMap = new HashMap<String, FileTypeHandler>();
		// load all built in FileTypeHandlers
		addFileTypeHandler("Copy Number CHP", new String[]{"cnchp", "lohchp"}, FileTypeCategory.Annotation, AffyCnChpParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("BAM", new String[]{"bam"}, FileTypeCategory.Alignment, null, BAM.class);
		addFileTypeHandler(
			new FileTypeHandler() {
				String[] extensions = new String[]{"sam"};
				@Override
				public String getName() { return "SAM"; }
				@Override
				public String[] getExtensions() { return extensions; }
				@Override
				public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
					return SymLoaderTabix.getSymLoader(new SAM(uri, featureName, group));
				}
				@Override
				public Parser getParser() { return null; }
				@Override
				public IndexWriter getIndexWriter(String stream_name) {
					return null;
				}
				@Override
				public FileTypeCategory getFileTypeCategory() {
					return FileTypeCategory.Alignment;
				}
			}
		);
		addFileTypeHandler("Graph", new String[]{"bar"}, FileTypeCategory.Graph, BarParser.class, Bar.class /* SymLoaderInstNC.class */);
		addFileTypeHandler(
			new FileTypeHandler() {
				String[] extensions = new String[]{"bed"};
				@Override
				public String getName() { return "BED"; }
				@Override
				public String[] getExtensions() { return extensions; }
				@Override
				public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
					return SymLoaderTabix.getSymLoader(new BED(uri, featureName, group));
				}
				@Override
				public Parser getParser() { return new BedParser(); }
				@Override
				public IndexWriter getIndexWriter(String stream_name) {
					return (IndexWriter) getParser();
				}
				@Override
				public FileTypeCategory getFileTypeCategory() {
					return FileTypeCategory.Annotation;
				}
			}
		);
		addFileTypeHandler(
				new FileTypeHandler() {
				String[] extensions = new String[]{"gff3"};
				@Override
				public String getName() { return "GFF"; }
				@Override
				public String[] getExtensions() { return extensions; }
				@Override
				public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
					return SymLoaderTabix.getSymLoader(new GFF3(uri, featureName, group));
				}
				@Override
				public GFF3Parser getParser() { return new GFF3Parser(); }
				@Override
				public IndexWriter getIndexWriter(String stream_name) {
					return null;
				}
				@Override
				public FileTypeCategory getFileTypeCategory() {
					return FileTypeCategory.Annotation;
				}
			});
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
						return SymLoaderTabix.getSymLoader(new GFF3(uri, featureName, group));
					}
					else {
						return SymLoaderTabix.getSymLoader(new GFF(uri, featureName, group));
					}
				}
				@Override
				public Parser getParser() { return new GFFParser(); }
				@Override
				public IndexWriter getIndexWriter(String stream_name) {
					return null;
				}
				@Override
				public FileTypeCategory getFileTypeCategory() {
					return FileTypeCategory.Annotation;
				}
			}
		);
		addFileTypeHandler("Binary", new String[]{"bgn"}, FileTypeCategory.Annotation, BgnParser.class, SymLoaderInst.class);
		addFileTypeHandler("Graph", new String[] {"bgr"}, FileTypeCategory.Graph, BgrParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Binary", new String[]{"bp1", "bp2"}, FileTypeCategory.Annotation, Bprobe1Parser.class, SymLoaderInst.class);
		addFileTypeHandler("Binary", new String[] {"bps"}, FileTypeCategory.Annotation, BpsParser.class, SymLoaderInst.class);
		addFileTypeHandler("Binary", new String[]{"brpt"}, FileTypeCategory.Annotation, BrptParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Binary", new String[]{"brs"}, FileTypeCategory.Annotation, BrsParser.class, SymLoaderInst.class);
		addFileTypeHandler("Binary", new String[]{"bsnp"}, FileTypeCategory.Annotation, BsnpParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Graph", new String[]{"chp"}, FileTypeCategory.ScoredContainer, null, SymLoaderInstNC.class); // chp files are handles elsewhere, this is just to get them in the FileChooser popup
		addFileTypeHandler("Copy Number", new String[]{"cnt"}, FileTypeCategory.Graph, CntParser.class, SymLoaderInst.class);
		addFileTypeHandler("Cytobands", new String[]{"cyt"}, /*FileTypeCategory.Cytoband*/ null, CytobandParser.class, SymLoaderInst.class);
		addFileTypeHandler("DAS", new String[]{Das2FeatureSaxParser.FEATURES_CONTENT_SUBTYPE, "das2feature", "das2xml", "x-das-feature"}, FileTypeCategory.Annotation, Das2FeatureSaxParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("DAS", new String[]{"das", "dasxml"}, FileTypeCategory.Annotation, DASFeatureParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Binary", new String[]{"ead"}, FileTypeCategory.Annotation, ExonArrayDesignParser.class, SymLoaderInstNC.class);
		addFileTypeHandler(
				new FileTypeHandler() {
					String[] extensions = new String[]{"fa", "fas", "fasta"};
					@Override
					public String getName() { return "FASTA"; }
					@Override
					public String[] getExtensions() { return extensions; }
					@Override
					public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
						SymLoader symLoader = new FastaIdx(uri, featureName, group);
						if (!((FastaIdx)symLoader).isValid()) {
//							Logger.getLogger(this.getClass().getName()).log(
//									Level.WARNING, "unable to read index or dict for fasta file, reading full file");
							symLoader = new Fasta(uri, featureName, group);
						}
						return symLoader;
					}
					@Override
					public Parser getParser() { return new FastaParser(); }
					@Override
					public IndexWriter getIndexWriter(String stream_name) {
						return null;
					}
					@Override
					public FileTypeCategory getFileTypeCategory() {
						return FileTypeCategory.Sequence;
					}
				}
			);
		addFileTypeHandler("FishClones", new String[]{FishClonesParser.FILE_EXT}, FileTypeCategory.Annotation, FishClonesParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Genbank", new String[]{"gb", "gen"}, FileTypeCategory.Annotation, null, Genbank.class);
		addFileTypeHandler("Graph", new String[]{"gr"}, FileTypeCategory.Graph, GrParser.class, Gr.class);
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
					return SymLoaderTabix.getSymLoader(psl);
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
				@Override
				public FileTypeCategory getFileTypeCategory() {
					return FileTypeCategory.ProbeSet;
				}
			}
		);
		addFileTypeHandler("Binary", new String[]{"bnib"}, FileTypeCategory.Sequence, NibbleResiduesParser.class, BNIB.class);
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
						return SymLoaderTabix.getSymLoader(psl);
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
					@Override
					public FileTypeCategory getFileTypeCategory() {
						return FileTypeCategory.Annotation;
					}
				}
			);
		addFileTypeHandler("Scored Interval", new String[]{"sin", "egr", "egr.txt"}, FileTypeCategory.ScoredContainer, ScoredIntervalParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Scored Map", new String[]{"map"}, FileTypeCategory.ScoredContainer, ScoredMapParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Regions", new String[]{SegmenterRptParser.CN_REGION_FILE_EXT, SegmenterRptParser.LOH_REGION_FILE_EXT}, FileTypeCategory.Annotation, SegmenterRptParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Graph", new String[]{"sgr"}, FileTypeCategory.Graph, SgrParser.class, Sgr.class);
		addFileTypeHandler(".2bit", new String[]{"2bit"}, FileTypeCategory.Sequence, TwoBitParser.class, TwoBit.class);
		addFileTypeHandler("Binary", new String[]{"useq"}, FileTypeCategory.Annotation, USeqRegionParser.class, USeq.class);
		addFileTypeHandler("Genomic Variation", new String[]{"var"}, FileTypeCategory.Annotation, VarParser.class, SymLoaderInstNC.class);
		addFileTypeHandler("Graph", new String[]{"wig"}, FileTypeCategory.Graph, WiggleParser.class, Wiggle.class);
		addFileTypeHandler(
			new FileTypeHandler() {
				String[] extensions = new String[]{"bdg","bedgraph"};
				@Override
				public String getName() { return "Graph"; }
				@Override
				public String[] getExtensions() { return extensions; }
				@Override
				public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
					return SymLoaderTabix.getSymLoader(new Wiggle(uri, featureName, group));
				}
				@Override
				public Parser getParser() { return new WiggleParser(); }
				@Override
				public IndexWriter getIndexWriter(String stream_name) {
					return (IndexWriter) getParser();
				}
				@Override
				public FileTypeCategory getFileTypeCategory() {
					return FileTypeCategory.Graph;
				}
			}
		);
		addFileTypeHandler(
			new FileTypeHandler() {
				String[] extensions = new String[]{"vcf"};
				@Override
				public String getName() { return "VCF"; }
				@Override
				public String[] getExtensions() { return extensions; }
				@Override
				public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
					return SymLoaderTabix.getSymLoader(new VCF(uri, featureName, group));
				}
				@Override
				public Parser getParser() { return null; }
				@Override
				public IndexWriter getIndexWriter(String stream_name) {
					return null;
				}
				@Override
				public FileTypeCategory getFileTypeCategory() {
					return FileTypeCategory.Alignment;
				}
			}
		);
	}

	private void addDummyHandler(final String name, final String[] extensions, final FileTypeCategory category) {
		addFileTypeHandler(
			new FileTypeHandler() {
				@Override
				public Parser getParser() {
					return null;
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
					return null;
				}

				@Override
				public IndexWriter getIndexWriter(String stream_name) {
					return null;
				}

				@Override
				public FileTypeCategory getFileTypeCategory() {
					return category;
				}
			}
		);
	}
	
	private void addFileTypeHandler(final String name, final String[] extensions, final FileTypeCategory category, final Class<? extends Parser> parserClass, final Class<? extends SymLoader> symLoaderClass) {
		addFileTypeHandler(
			new FileTypeHandler() {
				@Override
				public Parser getParser() {
					try {
						
						if(parserClass == null){
							return null;
						}
						
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

				@Override
				public FileTypeCategory getFileTypeCategory() {
					return category;
				}
			}
		);
	}

	public void addDummyHandler(FileTypeHandler fileTypeHandler) {
		addHandler(fileTypeHandler, dummyHandlerMap);
	}
	
	/**
	 * add a new FileTypeHandler for a list of extensions
	 * @param fileTypeHandler the FileTypeHandler
	 */
	public void addFileTypeHandler(FileTypeHandler fileTypeHandler) {
		addHandler(fileTypeHandler, fileTypeHandlerMap);
	}

	private static void addHandler(FileTypeHandler fileTypeHandler, Map<String, FileTypeHandler> map) {
		String[] extensions = fileTypeHandler.getExtensions();
		for (String extension : extensions) {
			if (map.get(extension) != null) {
				Logger.getLogger(FileTypeHolder.class.getName()).log(Level.SEVERE, "duplicate SymLoaderFactory for extension {0}!!!", new Object[]{extension});
			}
			map.put(extension, fileTypeHandler);
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
				Logger.getLogger(FileTypeHolder.class.getName()).log(Level.SEVERE, "missing removed SymLoaderFactory for extension {0}!!!", new Object[]{extension});
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
		if (extension == null) {
			return null;
		}
		if (extension.startsWith("x-das-feature")) {
			return fileTypeHandlerMap.get("das2xml");
		}
		FileTypeHandler handler = fileTypeHandlerMap.get(extension);
		// If handler is not found then look up in dummy handlers
		if(handler == null){
			handler = dummyHandlerMap.get(extension);
		}
		return handler;
	}

	public String getExtensionForURI(String uri) {
		String extension = null;
		String lc = GeneralUtils.stripEndings(uri).toLowerCase();
		extension = lc;
		int position = lc.lastIndexOf('.');
		if (position > -1) {
			extension = lc.substring(position + 1);
			String prefix = lc.substring(0, Math.max(0,position - 1));
			position = prefix.lastIndexOf('.');
			if (position > -1) {
				String tryExtension = lc.substring(position + 1);
				if (getFileTypeHandler(tryExtension) != null) {
					extension = tryExtension;
				}
			}
		}
		return extension;
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
		if (position == -1) {
			fileTypeHandler = getFileTypeHandler(lc);
		}
		else {
			extension = lc.substring(position + 1);
			fileTypeHandler = getFileTypeHandler(extension);
			String prefix = lc.substring(0, Math.max(0,position - 1));
			position = prefix.lastIndexOf('.');
			if (position > -1) {
				extension = lc.substring(position + 1);
				if (getFileTypeHandler(extension) != null) {
					fileTypeHandler = getFileTypeHandler(extension);
				}
			}
		}
		if(fileTypeHandler == null){
			Logger.getAnonymousLogger(FileTypeHolder.class.getName()).log(Level.SEVERE, "No file handler found for type {0} of uri {1}",new Object[]{extension,uri});
		}
		return fileTypeHandler;
	}

	/**
	 * get a Map linking file type names to the list of extensions
	 * @return the Map of file type names to their list of extensions
	 */
	public Map<String, List<String>> getNameToExtensionMap(FileTypeCategory category) {
		Map<String, List<String>> nameToExtensionMap = new TreeMap<String, List<String>>();
		for (FileTypeHandler fileTypeHandler : new HashSet<FileTypeHandler>(fileTypeHandlerMap.values())) {
			if(category != null && fileTypeHandler.getFileTypeCategory() != category){
				continue;
			}
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
	
	private static final List<String> TABIX_FILE_TYPES = new ArrayList<String>(Arrays.asList(new String[]{"sam", "bed", "bedgraph", "bdg", "gff", "gff3", "gtf", "psl", "psl3", "pslx", "vcf"}));
	public List<String> getTabixFileTypes() {
		return TABIX_FILE_TYPES;
	}
}
