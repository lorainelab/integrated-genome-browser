/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.menuitem;

import com.affymetrix.genometryImpl.parsers.graph.ScoredMapParser;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.igb.Application;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import org.xml.sax.InputSource;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.BAMParser;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.parsers.BgnParser;
import com.affymetrix.genometryImpl.parsers.Bprobe1Parser;
import com.affymetrix.genometryImpl.parsers.BpsParser;
import com.affymetrix.genometryImpl.parsers.BrptParser;
import com.affymetrix.genometryImpl.parsers.BrsParser;
import com.affymetrix.genometryImpl.parsers.BsnpParser;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.parsers.Das2FeatureSaxParser;
import com.affymetrix.genometryImpl.parsers.ExonArrayDesignParser;
import com.affymetrix.genometryImpl.parsers.FastaParser;
import com.affymetrix.genometryImpl.parsers.FishClonesParser;
import com.affymetrix.genometryImpl.parsers.GFFParser;
import com.affymetrix.genometryImpl.parsers.GFF3Parser;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.parsers.SegmenterRptParser;
import com.affymetrix.genometryImpl.parsers.VarParser;
import com.affymetrix.genometryImpl.parsers.das.DASFeatureParser;
import com.affymetrix.genometryImpl.parsers.das.DASSymmetry;
import com.affymetrix.genometryImpl.parsers.gchp.AffyCnChpParser;
import com.affymetrix.genometryImpl.parsers.gchp.ChromLoadPolicy;
import com.affymetrix.genometryImpl.parsers.graph.CntParser;
import com.affymetrix.genometryImpl.parsers.useq.ArchiveInfo;
import com.affymetrix.genometryImpl.parsers.useq.USeqRegionParser;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genoviz.swing.threads.InvokeUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.parsers.ChpParser;
import com.affymetrix.igb.quickload.QuickLoadFeatureLoading;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.util.concurrent.Executor;
import org.xml.sax.SAXException;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @version $Id$
 */
public final class LoadFileAction {

	private final JFrame gviewerFrame;
	private final FileTracker load_dir_tracker;
	private static int unknown_group_count = 1;
	private static final String UNKNOWN_GROUP_PREFIX = "Unknown Group";
	private static final String MERGE_MESSAGE = 
			"Must select a genome before loading a graph.  "
			+ "Graph data must be merged with already loaded genomic data.";

	/**
	 *  Constructor.
	 *  @param ft  a FileTracker used to keep track of directory to load from
	 */
	public LoadFileAction(JFrame gviewerFrame, FileTracker ft) {
		this.gviewerFrame = gviewerFrame;
		load_dir_tracker = ft;
	}

	public void actionPerformed(ActionEvent e) {
		loadFile(GenometryModel.getGenometryModel(), load_dir_tracker, gviewerFrame);
	}
	private static MergeOptionFileChooser chooser = null;

	private static MergeOptionFileChooser getFileChooser() {
		if (chooser != null) {
			return chooser;
		}

		chooser = new MergeOptionFileChooser();
		chooser.setMultiSelectionEnabled(true);
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"bam"}, "BAM Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"bed"}, "BED Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"bps", "bgn", "brs", "bsnp", "brpt", "bnib", "bp1", "bp2", "ead","useq"},
						"Binary Files"));
		chooser.addChoosableFileFilter(new UniFileFilter("cyt", "Cytobands"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"gff", "gtf", "gff3"},
						"GFF Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"fa", "fasta", "fas"},
						"FASTA Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"psl", "psl3"},
						"PSL Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"das", "dasxml", "das2xml"},
						"DAS Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"gr", "bgr", "sgr", "bar", "chp", "wig"},
						"Graph Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"sin", "egr", "egr.txt"},
						"Scored Interval Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						"cnt", "Copy Number Files")); // ".cnt" files from CNAT
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"cnchp", "lohchp"}, "Copy Number CHP Files"));

		chooser.addChoosableFileFilter(new UniFileFilter(
						"var", "Genomic Variation Files")); // ".var" files (Toronto DB of genomic variations)
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{SegmenterRptParser.CN_REGION_FILE_EXT, SegmenterRptParser.LOH_REGION_FILE_EXT},
						"Regions Files")); // Genotype Console Segmenter
		chooser.addChoosableFileFilter(new UniFileFilter(
						FishClonesParser.FILE_EXT, "FishClones")); // ".fsh" files (fishClones.txt from UCSC)
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"map"}, "Scored Map Files"));

		HashSet<String> all_known_endings = new HashSet<String>();
		for (javax.swing.filechooser.FileFilter filter : chooser.getChoosableFileFilters()) {
			if (filter instanceof UniFileFilter) {
				UniFileFilter uff = (UniFileFilter) filter;
				uff.addCompressionEndings(GeneralUtils.compression_endings);
				all_known_endings.addAll(uff.getExtensions());
			}
		}
		UniFileFilter all_known_types = new UniFileFilter(
						all_known_endings.toArray(new String[all_known_endings.size()]),
						"Known Types");
		all_known_types.setExtensionListInDescription(false);
		all_known_types.addCompressionEndings(GeneralUtils.compression_endings);
		chooser.addChoosableFileFilter(all_known_types);
		chooser.setFileFilter(all_known_types);
		return chooser;
	}

	/** Load a file into the global singleton genometry model. */
	private static void loadFile(final GenometryModel gmodel, final FileTracker load_dir_tracker, final JFrame gviewerFrame) {

		MergeOptionFileChooser fileChooser = getFileChooser();
		File currDir = load_dir_tracker.getFile();
		if (currDir == null) {
			currDir = new File(System.getProperty("user.home"));
		}
		fileChooser.setCurrentDirectory(currDir);
		fileChooser.rescanCurrentDirectory();
		if (gmodel.getSelectedSeqGroup() == null) {
			fileChooser.no_merge_button.setEnabled(true);
			fileChooser.no_merge_button.setSelected(true);
			fileChooser.merge_button.setEnabled(false);
		} else {
			// default to "merge" if already have a selected seq group to merge with,
			//    because non-merging is an uncommon choice
			fileChooser.merge_button.setSelected(true);
			fileChooser.merge_button.setEnabled(true);
		}
		fileChooser.genome_name_TF.setEnabled(fileChooser.no_merge_button.isSelected());
		fileChooser.genome_name_TF.setText(UNKNOWN_GROUP_PREFIX + " " + unknown_group_count);

		int option = fileChooser.showOpenDialog(gviewerFrame);

		if (option != JFileChooser.APPROVE_OPTION) {
			return;
		}

		load_dir_tracker.setFile(fileChooser.getCurrentDirectory());

		final File[] fils = fileChooser.getSelectedFiles();
		final AnnotatedSeqGroup previous_seq_group = gmodel.getSelectedSeqGroup();
		final BioSeq previous_seq = gmodel.getSelectedSeq();
		final boolean mergeSelected = fileChooser.merge_button.isSelected();
		if (!mergeSelected) {
			// Not merging, so create a new Seq Group
			unknown_group_count++;
		}
		
		final AnnotatedSeqGroup loadGroup = mergeSelected ? gmodel.getSelectedSeqGroup() : gmodel.addSeqGroup(fileChooser.genome_name_TF.getText());

		GenericVersion version = GeneralLoadUtils.getLocalFilesVersion(loadGroup);
		version.addFeature(new GenericFeature(
				fils[0].getName(), null, version, new QuickLoadFeatureLoading(version, fils[0].getAbsolutePath()), fils));

		ServerList.fireServerInitEvent(ServerList.getLocalFilesServer(), ServerStatus.Initialized);
	}

	

	private static BioSeq loadFilesIntoSeq( File[] fils, JFrame gviewerFrame, GenometryModel gmodel, AnnotatedSeqGroup seq_group, BioSeq seq) {
		BioSeq new_seq = null;
		for (File cfil : fils) {
			String file_name = cfil.toString();
			int httpIndex = file_name.toLowerCase().indexOf("http:");
			if (httpIndex > -1) {
				try {
					// Strip off initial characters up to and including http:
					// Sometimes this is necessary, as URLs can start with invalid "http:/"
					String streamName = GeneralUtils.convertStreamNameToValidURLName(file_name);
					InputStream istr = LocalUrlCacher.getInputStream(streamName);
					if (istr == null) {
						ErrorHandler.errorPanel("ERROR", "Error loading URL:\n" + streamName);
					}
					// Convert stream to a file.  Only use the name after the last "/", otherwise filename will be URL-encoded
					// and will not look good to the user.
					File f = GeneralUtils.convertStreamToFile(istr, streamName.substring(streamName.lastIndexOf("/")));
					
					new_seq = load(gviewerFrame, streamName, f, gmodel, seq_group, seq);
				} catch (IOException ex) {
					ex.printStackTrace();
					ErrorHandler.errorPanel(gviewerFrame, "ERROR", "Error loading URL", ex);
				}
			} else {
				try {
					String fileName = cfil.getName().toLowerCase();
					new_seq = load(gviewerFrame, fileName, cfil, gmodel, seq_group, seq);
				} catch (Exception ex) {
					ex.printStackTrace();
					ErrorHandler.errorPanel(gviewerFrame, "ERROR", "Error loading file", ex);
				}
			}
		}
		return new_seq;
	}

	private static BioSeq load(
			JFrame gviewerFrame, String annotFileLC, File annotfile, GenometryModel gmodel, AnnotatedSeqGroup seq_group, BioSeq input_seq)
			throws IOException {
		if (annotFileLC.endsWith(".chp")) {
			// special-case CHP files. ChpParser only has
			//    a parse() method that takes the file name
			// (ChpParser uses Affymetrix Fusion SDK for actual file parsing)
			// Also cannot handle compressed chp files
			ChpParser.parse(annotfile.getPath());
			return null;
		}
		if (annotFileLC.endsWith(".bam")) {
			// special-case BAM files, because Picard can only parse from files.
			if (seq_group == null) {
				ErrorHandler.errorPanel(gviewerFrame, "ERROR", MERGE_MESSAGE, null);
			} else {
				BAMParser parser = new BAMParser(annotfile, seq_group);
				parser.parse();
			}
			return null;
		}

		InputStream fistr = null;
		try {
			StringBuffer sb = new StringBuffer();
			fistr = GeneralUtils.getInputStream(annotfile, sb);
			String stripped_name = sb.toString();

			//is it a useq graph archive?
			boolean useqGraphArchive = false;
			ArchiveInfo ai = ArchiveInfo.fetchArchiveInfo(annotfile, false);
			if (ai != null && ai.getDataType().equals(ArchiveInfo.DATA_TYPE_VALUE_GRAPH)) {
				useqGraphArchive = true;
			}

			//is it a graph file?
			if (GraphSymUtils.isAGraphFilename(stripped_name) || useqGraphArchive) {
				if (seq_group == null) {
					ErrorHandler.errorPanel(gviewerFrame, "ERROR", MERGE_MESSAGE, null);
				} else {
					URL url = annotfile.toURI().toURL();
					OpenGraphAction.loadGraphFile(url, seq_group, input_seq);
				}
				return null;
			}
			//load as non graph data
			return load(gviewerFrame, fistr, stripped_name, gmodel, seq_group, input_seq);
			
		} // Don't catch exception, just throw it
		finally {
			GeneralUtils.safeClose(fistr);
		}
	}

	private static void setGroupAndSeq(GenometryModel gmodel, AnnotatedSeqGroup previous_seq_group, BioSeq previous_seq, BioSeq new_seq) {
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		if (group == null) {
			// This primarily can happen if the merge button is not selected
			// and the loading of the file fails or fails to create a seq group.
			gmodel.setSelectedSeqGroup(previous_seq_group);
			gmodel.setSelectedSeq(previous_seq);
		} else {
			// The purpose of calling setSelectedSeqGroup, even if identity of
			// the seq group has not changed, is to make sure that
			// the DataLoadView and the AnnotBrowserView update their displays.
			// (Because the contents of the seq group may have changed.)
			gmodel.setSelectedSeqGroup(group);
			if (new_seq != null && group.getSeqList().contains(new_seq)) {
				gmodel.setSelectedSeq(new_seq);
			} else {
				if (group != previous_seq_group) {
					if (group.getSeqCount() > 0) {
						gmodel.setSelectedSeq(group.getSeq(0));
					}
				} else {
					// the seq_group has not changed, but the seq might have
					if (previous_seq != null) {
						// Setting the selected Seq, even if it hasn't changed identity, is to
						// make the SeqMapView update itself.  (Its contents may have changed.)
						gmodel.setSelectedSeq(previous_seq);
					}
				}
			}
		}
	}

	/** Loads from an InputStream.
	 *  Detects the type of file based on the filename ending of the
	 *  stream_name parameter, for example ".dasxml".
	 *  The stream will be passed through uncompression routines
	 *  if necessary.
	 */
	public static BioSeq load(JFrame gviewerFrame, InputStream instr, String stream_name, GenometryModel gmodel, AnnotatedSeqGroup selected_group, BioSeq input_seq) throws IOException {
		if (selected_group == null) {
			// this should never happen
			throw new IOException("Must select a genome before loading a file");
		}

		Application.getSingleton().logInfo("loading file: " + stream_name);

		Exception the_exception = null;
		BioSeq aseq = null;
		InputStream str = null;

		try {
			StringBuffer stripped_name = new StringBuffer();
			str = GeneralUtils.unzipStream(instr, stream_name, stripped_name);
			stream_name = stripped_name.toString();

			if (str instanceof BufferedInputStream) {
				str = (BufferedInputStream) str;
			} else {
				str = new BufferedInputStream(str);
			}
			aseq = DoParse(str, selected_group, input_seq, stream_name, gviewerFrame, gmodel);
		} catch (Exception ex) {
			the_exception = ex;
			//ErrorHandler.errorPanel(gviewerFrame, "ERROR", "Error loading file", ex);
		} finally {
			GeneralUtils.safeClose(str);
		}

		// The purpose of calling setSelectedSeqGroup, even if identity of
		// the seq group has not changed, is to make sure that
		// the DataLoadView and the AnnotBrowserView update their displays.
		// (Because the contents of the seq group may have changed.)
		//
		// Note that this must be done regardless of whether this load() method was
		// called from inside this class or in loading a bookmark, etc.

		gmodel.setSelectedSeqGroup(gmodel.getSelectedSeqGroup());

		if (the_exception != null) {
			if (the_exception instanceof IOException) {
				throw (IOException) the_exception;
			} else {
				IOException new_exception = new IOException();
				new_exception.initCause(the_exception);
				throw new_exception;
			}
		}
		return aseq;
	}

	private static BioSeq DoParse(
					InputStream str, AnnotatedSeqGroup selected_group, BioSeq input_seq,
					String stream_name, JFrame gviewerFrame, GenometryModel gmodel)
					throws IOException, InterruptedException, HeadlessException, SAXException {
		String lcname = stream_name.toLowerCase();

		int dotIndex = stream_name.lastIndexOf('.');
		String annot_type = dotIndex <= 0 ? stream_name : stream_name.substring(0, dotIndex);

		if (lcname.endsWith(".cyt")) {
			CytobandParser parser = new CytobandParser();
			parser.parse(str, selected_group, true);
			return input_seq;
		}
		if (lcname.endsWith(".cnt")) {
			CntParser parser = new CntParser();
			parser.parse(str, selected_group);
			return input_seq;
		}
		if (lcname.endsWith(".var")) {
			VarParser parser = new VarParser();
			parser.parse(str, selected_group);
			return input_seq;
		}
		if ((lcname.endsWith("." + SegmenterRptParser.CN_REGION_FILE_EXT) || lcname.endsWith("." + SegmenterRptParser.LOH_REGION_FILE_EXT))) {
			SegmenterRptParser parser = new SegmenterRptParser();
			parser.parse(str, stream_name, selected_group);
			return input_seq;
		}
		if (lcname.endsWith("." + FishClonesParser.FILE_EXT)) {
			FishClonesParser parser = new FishClonesParser(true);
			parser.parse(str, annot_type, selected_group);
			return input_seq;
		}
		if (lcname.endsWith(".cnchp") || lcname.endsWith(".lohchp")) {
			AffyCnChpParser parser = new AffyCnChpParser();
			parser.parse(null, ChromLoadPolicy.getLoadAllPolicy(), str, stream_name, selected_group);
			return input_seq;
		}
		if (lcname.endsWith(".das") || lcname.endsWith(".dasxml")) {
			DASFeatureParser parser = new DASFeatureParser();
			Collection<DASSymmetry> results;
			try {
				results = parser.parse(str, selected_group);
				return LoadFileAction.<DASSymmetry>getFirstSeq(results);
			} catch (XMLStreamException ex) {
				Logger.getLogger(LoadFileAction.class.getName()).log(Level.SEVERE, null, ex);
			}
			return null;
		}
		if (lcname.endsWith(".das2xml")) {
			Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
			List<SeqSymmetry> results = parser.parse(new InputSource(str), stream_name, selected_group, true);
			return LoadFileAction.<SeqSymmetry>getFirstSeq(results);
		}
		if (lcname.endsWith(".map")) {
			ScoredMapParser parser = new ScoredMapParser();
			parser.parse(str, stream_name, input_seq, selected_group);
			return input_seq;
		}
		if (lcname.endsWith(".sin") || lcname.endsWith(".egr") || lcname.endsWith(".txt")) {
			ScoredIntervalParser parser = new ScoredIntervalParser();
			parser.parse(str, stream_name, selected_group);
			return input_seq;
		}
		if (lcname.endsWith(".psl") || lcname.endsWith(".psl3")) {
			ParsePSL(lcname, gviewerFrame, str, stream_name, selected_group);
			return input_seq;
		}
		if (lcname.endsWith(".bps")) {
			DataInputStream dis = new DataInputStream(str);
			BpsParser.parse(dis, annot_type, null, selected_group, false, true);
			return input_seq;
		}
		if (lcname.endsWith(".bed")) {
			BedParser parser = new BedParser();
			// really need to switch create_container (last argument) to true soon!
			parser.parse(str, gmodel, selected_group, true, annot_type, false);
			return input_seq;
		}
		if (lcname.endsWith(".useq")) {
			USeqRegionParser parser = new USeqRegionParser();
			parser.parse(str, selected_group, stream_name, true, null);
			return input_seq;
		}
		if (lcname.endsWith(".bgn")) {
			BgnParser parser = new BgnParser();
			parser.parse(str, annot_type, selected_group, true);
			return input_seq;
		}
		if (lcname.endsWith(".brs")) {
			BrsParser.parse(str, annot_type, selected_group);
			return input_seq;
		}
		if (lcname.endsWith(".bsnp")) {
			List<SeqSymmetry> alist = BsnpParser.parse(str, annot_type, selected_group, true);
			Application.getSingleton().logDebug("total snps loaded: " + alist.size());
			return input_seq;
		}
		if (lcname.endsWith(".brpt")) {
			BrptParser parser = new BrptParser();
			List<SeqSymmetry> alist = parser.parse(str, annot_type, selected_group, true);
			Application.getSingleton().logDebug("total repeats loaded: " + alist.size());
			return input_seq;
		}
		if (lcname.endsWith(".bp1") || lcname.endsWith(".bp2")) {
			Bprobe1Parser parser = new Bprobe1Parser();
			parser.parse(str, selected_group, true, annot_type, true);
			return input_seq;
		}
		if (lcname.endsWith(".ead")) {
			ExonArrayDesignParser parser = new ExonArrayDesignParser();
			parser.parse(str, selected_group, true, annot_type);
			return input_seq;
		}
		if (lcname.endsWith(".gff") || lcname.endsWith(".gtf")) {
			// assume it's GFF1, GFF2, GTF, or GFF3 format
			GFFParser parser = new GFFParser();
			parser.setUseStandardFilters(true);
			parser.parse(str, annot_type, selected_group, false);
			return null;
		}
		if (lcname.endsWith(".gff3")) {
			/* Force parcing as GFF3 */
			GFF3Parser parser = new GFF3Parser();
			parser.parse(str, annot_type, selected_group);
			return input_seq;
		}
		if (lcname.endsWith(".fa") || lcname.endsWith(".fas") || lcname.endsWith(".fasta")) {
			List<BioSeq> seqs = FastaParser.parseAll(str, selected_group);
			if (input_seq != null && seqs.contains(input_seq)) {
				return input_seq;
			}
			if (!seqs.isEmpty()) {
				return seqs.get(0);
			}
			return null;
		}
		if (lcname.endsWith(".bnib")) {
			BioSeq aseq = NibbleResiduesParser.parse(str, selected_group);
			if (aseq != gmodel.getSelectedSeq()) {
				//TODO: maybe set the current seq to this seq
				Application.getSingleton().logWarning("This is not the currently-selected sequence.");
			}
			return aseq;
		}

		ErrorHandler.errorPanel(gviewerFrame, "FORMAT NOT RECOGNIZED", "Format not recognized for file: " + stream_name, null);
		return null;
	}

	// Parse PSL files.  These files specifically have .psl or .psl3 extensions.
	private static void ParsePSL(
					String lcname, JFrame gviewerFrame, InputStream str, String stream_name, AnnotatedSeqGroup selected_group)
					throws IOException, HeadlessException, InterruptedException {
		PSLParser parser = new PSLParser();
		parser.enableSharedQueryTarget(true);
		int psl_option = -1;

		// If the name ends with ".link.psl" then assume it is a mapping
		// of probe sets to consensus seqs to genome, and thus select
		// psl_option = 1 "target".

		if (lcname.endsWith(".link.psl")) {
			parser.setIsLinkPsl(true);
			psl_option = 1; // "target"
		} else if (lcname.endsWith(".psl")) {
			psl_option = 1;
		} else {
			// .psl3 file
			// the user has to tell us whether to annotate the "query" or "target" or "other"
			Object[] options = new Object[]{"Query", "Target", "Other"};

			if (SwingUtilities.isEventDispatchThread()) {
				psl_option = JOptionPane.showOptionDialog(gviewerFrame, "Annotate which sequence?", "PSL annotation options", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, "Target");
			} else {
				psl_option = InvokeUtils.invokeOptionDialog(gviewerFrame, "Annotate which sequence?", "PSL annotation options", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, "Target");
			}
		}
		boolean annotate_query = psl_option == 0;
		boolean annotate_target = psl_option == 1;
		boolean annotate_other = psl_option == 2;

		if (annotate_query) {
			parser.parse(str, stream_name, selected_group, null, null, annotate_query, annotate_target, annotate_other);
		} else if (annotate_target) {
			parser.parse(str, stream_name, null, selected_group, null, annotate_query, annotate_target, annotate_other);
		} else if (annotate_other) {
			parser.parse(str, stream_name, null, null, selected_group, annotate_query, annotate_target, annotate_other);
		}
	}

	/** Returns the first BioSeq on the first SeqSymmetry in the given list, or null. */
	private static <S extends SeqSymmetry> BioSeq getFirstSeq(Collection<S> syms) {
		BioSeq first_seq = null;
		if (syms != null && !syms.isEmpty()) {
			SeqSymmetry fsym = syms.iterator().next();
			SeqSpan fspan = fsym.getSpan(0);
			first_seq = fspan.getBioSeq();
		}
		return first_seq;
	}

	/** A JFileChooser that has a checkbox for whether you want to merge annotations.
	 *  Note that an alternative way of adding a checkbox to a JFileChooser
	 *  is to use JFileChooser.setAccessory().  The only advantage to this
	 *  subclass is more control of where the JCheckBox is placed inside the
	 *  dialog.
	 */
	private static class MergeOptionFileChooser extends JFileChooser {

		ButtonGroup bgroup = new ButtonGroup();
		public JRadioButton merge_button = new JRadioButton(BUNDLE.getString("mergeWithCurrentlyLoadedData"), true);
		public JRadioButton no_merge_button = new JRadioButton(BUNDLE.getString("createNewGenome"), false);
		public JTextField genome_name_TF = new JTextField(BUNDLE.getString("unknownGenome"));
		Box box = null;

		public MergeOptionFileChooser() {
			super();
			bgroup.add(no_merge_button);
			bgroup.add(merge_button);
			merge_button.setSelected(true);

			genome_name_TF.setEnabled(no_merge_button.isSelected());

			no_merge_button.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					genome_name_TF.setEnabled(no_merge_button.isSelected());
				}
			});

			box = new Box(BoxLayout.X_AXIS);
			box.setBorder(BorderFactory.createEmptyBorder(5, 5, 8, 5));
			box.add(Box.createHorizontalStrut(5));
			box.add(merge_button);
			box.add(no_merge_button);
			box.add(Box.createRigidArea(new Dimension(5, 0)));
			box.add(genome_name_TF);

			merge_button.setMnemonic('M');
			no_merge_button.setMnemonic('C');
		}

		@Override
		protected JDialog createDialog(Component parent) throws HeadlessException {
			JDialog dialog = super.createDialog(parent);

			dialog.getContentPane().add(box, BorderLayout.SOUTH);
			return dialog;
		}
	}
}