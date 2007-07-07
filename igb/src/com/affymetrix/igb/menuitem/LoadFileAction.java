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

import com.affymetrix.igb.parsers.Xml2GenometryParser;
import com.affymetrix.igb.parsers.Das2FeatureSaxParser;
import com.affymetrix.genometryImpl.parsers.BrptParser;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.NibbleBioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.xml.sax.InputSource;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.parsers.*;
import com.affymetrix.swing.threads.*;
import com.affymetrix.igb.parsers.*;
import com.affymetrix.igb.util.*;
import com.affymetrix.igb.view.*;

public class LoadFileAction {

  SeqMapView gviewer;
  FileTracker load_dir_tracker;
  static int unknown_group_count = 1;
  public static final String UNKNOWN_GROUP_PREFIX = "Unknown Group";

  static final boolean PARSE_CNT = false; // whether to parse ".cnt" files from CNAT
  static final boolean PARSE_VAR = false; // whether to parse ".var" files (Toronto DB of genomic variations)

  /**
   *  Constructor.
   *  @param ft  a FileTracker used to keep track of directory to load from
   */
  public LoadFileAction(SeqMapView gviewer, FileTracker ft) {
    this.gviewer = gviewer;
    load_dir_tracker = ft;
  }

  public void actionPerformed(ActionEvent e) {
    loadFile();
  }

  static MergeOptionFileChooser chooser = null;

  static MergeOptionFileChooser getFileChooser() {
    if (chooser == null) {
      chooser = new MergeOptionFileChooser();
      chooser.setMultiSelectionEnabled(true);
      chooser.addChoosableFileFilter(new UniFileFilter("axml"));
      chooser.addChoosableFileFilter(new UniFileFilter("bed"));
      chooser.addChoosableFileFilter(new UniFileFilter(
        new String[] {"bps", "bgn", "brs", "bsnp", "brpt", "bnib", "bp1", "bp2", "ead"},
        "Binary Files"));
      chooser.addChoosableFileFilter(new UniFileFilter("cyt", "Cytobands"));
      chooser.addChoosableFileFilter(new UniFileFilter(
        new String[] {"gff", "gtf", "gff3"},
        "GFF Files"));
      chooser.addChoosableFileFilter(new UniFileFilter(
        new String[] {"fa", "fasta"},
        "FASTA Files"));
      chooser.addChoosableFileFilter(new UniFileFilter(
        new String[] {"psl", "psl3"},
        "PSL Files"));
      chooser.addChoosableFileFilter(new UniFileFilter(
        new String[] {"das", "dasxml", "das2xml"},
        "DAS Files"));
      chooser.addChoosableFileFilter(new UniFileFilter(
        new String[] {"gr", "bgr", "sgr", "bar", "chp", "wig"},
        "Graph Files"));
      chooser.addChoosableFileFilter(new UniFileFilter(
        new String[] {"sin", "egr", "egr.txt"},
        "Scored Interval Files"));
      if (PARSE_CNT) {
        chooser.addChoosableFileFilter(new UniFileFilter("cnt", "Copy Number Files"));
      }
      if (PARSE_VAR) {
        chooser.addChoosableFileFilter(new UniFileFilter("var", "Genomic Variation Files"));
      }
      chooser.addChoosableFileFilter(new UniFileFilter("map"));
      HashSet all_known_endings = new HashSet();
      javax.swing.filechooser.FileFilter[] filters = chooser.getChoosableFileFilters();
      for (int i=0; i<filters.length; i++) {
        if (filters[i] instanceof UniFileFilter) {
          UniFileFilter uff = (UniFileFilter) filters[i];
          uff.addCompressionEndings(Streamer.compression_endings);
          all_known_endings.addAll(uff.getExtensions());
        }
      }
      UniFileFilter all_known_types = new UniFileFilter(
        (String[]) all_known_endings.toArray(new String[all_known_endings.size()]),
        "Known Types");
      all_known_types.setExtensionListInDescription(false);
      all_known_types.addCompressionEndings(Streamer.compression_endings);
      chooser.addChoosableFileFilter(all_known_types);
      chooser.setFileFilter(all_known_types);
    }
    return chooser;
  }

  /** Load a file into the global singleton genometry model. */
  public MutableAnnotatedBioSeq loadFile()  {
    return loadFile(SingletonGenometryModel.getGenometryModel());
  }

  public MutableAnnotatedBioSeq loadFile(SingletonGenometryModel gmodel)  {

    MergeOptionFileChooser chooser = getFileChooser();
    chooser.setCurrentDirectory(load_dir_tracker.getFile());
    chooser.rescanCurrentDirectory();
    if (gmodel.getSelectedSeqGroup() == null) {
      chooser.no_merge_button.setEnabled(true);
      chooser.no_merge_button.setSelected(true);
      chooser.merge_button.setEnabled(false);
    }
    else {
      // default to "merge" if already have a selected seq group to merge with,
      //    because non-merging is an uncommon choice
      chooser.merge_button.setSelected(true);
      chooser.merge_button.setEnabled(true);
    }
    chooser.genome_name_TF.setEnabled(chooser.no_merge_button.isSelected());
    chooser.genome_name_TF.setText(UNKNOWN_GROUP_PREFIX + " " +(unknown_group_count++));

    int option = chooser.showOpenDialog(gviewer.getFrame());

    if (option == JFileChooser.APPROVE_OPTION) {
      load_dir_tracker.setFile(chooser.getCurrentDirectory());
      File[] fils = chooser.getSelectedFiles();

      AnnotatedSeqGroup previous_seq_group = gmodel.getSelectedSeqGroup();
      MutableAnnotatedBioSeq previous_seq = gmodel.getSelectedSeq();

      if (! chooser.merge_button.isSelected()) {
        // Not merging, so create a new Seq Group
        String new_name = chooser.genome_name_TF.getText();
        AnnotatedSeqGroup new_group = gmodel.addSeqGroup(new_name);
        gmodel.setSelectedSeqGroup(new_group);
      }

      MutableAnnotatedBioSeq new_seq = null;

      for (int i=0; i<fils.length; i++) {
        File cfil = fils[i];
        String file_name = cfil.toString();
        if (file_name.indexOf("http:") > -1) {  // direct input of http...
          // This method of inputing a URL is not the best way to go, but it sometimes works...
          // On Linux, and maybe in general, if the user types "http://www.google.com",
          // it will come out as "/home/user/http:/www.google.com", so we have to
          // trim off the beginning stuff AND add back the double slash "//" after http.
//          String url_name = "http://" + file_name.substring(file_name.indexOf("http:")+6);
//          System.out.println("detected url input: " + url_name);
//          loadFromUrl(gviewer, url_name, aseq);
          //TODO: maybe support this again?
          System.out.println("Loading from a URL is not currently supported.");
        }
        else {
          new_seq = load(gviewer, cfil, gmodel, gmodel.getSelectedSeq());
        }
      }

      AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
      if (group == null) {
        // This primarily can happen if the merge button is not selected
        // and the loading of the file fails or fails to create a seq group.
        gmodel.setSelectedSeqGroup(previous_seq_group);
        gmodel.setSelectedSeq(previous_seq);
      }
      else {
        // The purpose of calling setSelectedSeqGroup, even if identity of
        // the seq group has not changed, is to make sure that
        // the DataLoadView and the AnnotBrowserView update their displays.
        // (Because the contents of the seq group may have changed.)
        gmodel.setSelectedSeqGroup(group);

        if (group != previous_seq_group) {
          if (new_seq != null && group.getSeqList().contains(new_seq)) {
            gmodel.setSelectedSeq(new_seq);
          } else if (group.getSeqCount() > 0) {
            gmodel.setSelectedSeq(group.getSeq(0));
          }
        }
        else {
          // the seq_group has not changed, but the seq might have
          if (new_seq != null && group.getSeqList().contains(new_seq)) {
            gmodel.setSelectedSeq(new_seq);
          } else if (previous_seq != null) {
            // Setting the selected Seq, even if it hasn't changed identity, is to
            // make the SeqMapView update itself.  (It's contents may have changed.)
            gmodel.setSelectedSeq(previous_seq);
          }
        }
      }
    }

    return gmodel.getSelectedSeq();
  }

//  public MutableAnnotatedBioSeq load(File annotfile) {
//    return load(gviewer, annotfile, gmodel.getSelectedSeq());
//  }

  public static MutableAnnotatedBioSeq load(SeqMapView gviewer, File annotfile, SingletonGenometryModel gmodel, MutableAnnotatedBioSeq input_seq) {
    MutableAnnotatedBioSeq aseq = null;
    InputStream fistr = null;
    try {
      // need to handle CHP files as a special case, because ChpParser currently only has
      //    a parse() method that takes the file name as an argument, no method to parse from
      //    an inputstream (ChpParser uses Affymetrix Fusion SDK for actual file parsing)
      //
      // Also cannot handle compressed chp files, so don't bother with the Streamer class.
      if (annotfile.getName().toLowerCase().endsWith(".chp")) {
        //System.out.println("%%%%% received load request for CHP file: " + annotfile.getPath());
        java.util.List results = ChpParser.parse(annotfile.getPath());
	// aseq = getLastSeq(results);
      }
      else {
	//int file_length = (int)annotfile.length();
	//fistr = new FileInputStream(annotfile);
	StringBuffer sb = new StringBuffer();
	fistr = Streamer.getInputStream(annotfile,  sb);
	String stripped_name = sb.toString();
	//int pindex = stripped_name.lastIndexOf(".");
	//String suffix = null;
	//if (pindex >= 0)  { suffix = stripped_name.substring(pindex+1); }
	if (GraphSymUtils.isAGraphFilename(stripped_name)) {
	  AnnotatedSeqGroup seq_group = SingletonGenometryModel.getGenometryModel().getSelectedSeqGroup();
	  if (seq_group == null) {
	    ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", "Must select a a genome before loading a graph.  " +
				    "Graph data must be merged with already loaded genomic data.", null);
	  } else {
	    URL url = annotfile.toURI().toURL();
	    OpenGraphAction.loadGraphFile(url, seq_group, input_seq);
	  }
	}
	else {
	  aseq = load(gviewer, fistr, stripped_name, gmodel, input_seq);
	}
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", "Error loading file", ex);
    }
    finally {
      if (fistr != null) try {fistr.close();} catch (Exception e) {}
    }
    return aseq;
  }

  // This seems to be unused.
  public static MutableAnnotatedBioSeq loadFromUrl(SeqMapView gviewer, String url_name, 
      SingletonGenometryModel gmodel, MutableAnnotatedBioSeq input_seq)
  throws IOException {
    IOException ioe = null;
    MutableAnnotatedBioSeq result = null;
    InputStream istr = null;
    try {
      URL loadurl = new URL(url_name);
      istr = new BufferedInputStream(loadurl.openStream());
      result = load(gviewer, istr, url_name, gmodel, input_seq);
    }
    catch (IOException ex) {
      ioe = ex;
    } finally {
      if (istr != null) try {istr.close();} catch (Exception e) {}
    }

    if (ioe != null) {
      throw ioe;
    }

    return result;
  }

  /** Loads from an InputStream.
   *  Detects the type of file based on the filename ending of the
   *  stream_name parameter, for example ".dasxml".
   *  The stream will be passed through uncompression routines in the Streamer
   *  class if necessary.
   */
  public static MutableAnnotatedBioSeq load(SeqMapView gviewer, InputStream instr,
        String stream_name, SingletonGenometryModel gmodel, MutableAnnotatedBioSeq input_seq)
  throws IOException {
    System.out.println("loading file: " + stream_name);

    Exception the_exception = null;
    MutableAnnotatedBioSeq aseq = null;
    InputStream str = null;

    AnnotatedSeqGroup selected_group = gmodel.getSelectedSeqGroup();
    if (selected_group == null) {
      // this should never happen
      throw new IOException("Must select a genome before loading a file");
    }

    try {
      StringBuffer stripped_name = new StringBuffer();
      str = Streamer.unzipStream(instr, stream_name, stripped_name);
      stream_name = stripped_name.toString();
      String lcname = stream_name.toLowerCase();


      if (str instanceof BufferedInputStream)  {
        str = (BufferedInputStream) str;
      }
      else {
        str = new BufferedInputStream(str);
      }

      if (lcname.endsWith(".cyt")) {
        CytobandParser parser = new CytobandParser();
        parser.parse(str, selected_group);
        aseq = input_seq;
        parser = null;
      }
      else if (lcname.endsWith(".axml")) {
        Xml2GenometryParser parser = new Xml2GenometryParser();
        aseq = parser.parse(str, input_seq);
        parser = null;
      }
      else if (PARSE_CNT && lcname.endsWith(".cnt")) {
        CntParser parser = new CntParser();
        parser.parse(str, selected_group);
        aseq = input_seq;
        parser = null;
      }
      else if (PARSE_VAR && lcname.endsWith(".var")) {
        VarParser parser = new VarParser();
        parser.parse(str, selected_group);
        aseq = input_seq;
        parser = null;
      }
      else if (lcname.endsWith(".das") || lcname.endsWith(".dasxml")) {
        Das1FeatureSaxParser parser = new Das1FeatureSaxParser();
        java.util.List results = parser.parse(str, selected_group);
        aseq = getFirstSeq(results);
        parser = null;
      }
      else if (lcname.endsWith(".das2xml")) {
	Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
	java.util.List results = parser.parse(new InputSource(str), stream_name, selected_group, true);
        aseq = getFirstSeq(results);
        parser = null;
      }
      else if (lcname.endsWith(".map"))  {
        ScoredMapParser parser = new ScoredMapParser();
        parser.parse(str, stream_name, input_seq, selected_group);
        aseq = input_seq;
        parser = null;
      }
      else if (lcname.endsWith(".sin") || lcname.endsWith(".egr") || lcname.endsWith(".txt")) {
        ScoredIntervalParser parser = new ScoredIntervalParser();
        parser.parse(str, stream_name, selected_group);
        aseq = input_seq;
        parser = null;
      }
      else if (lcname.endsWith(".psl") || lcname.endsWith( ".psl3")) {
        PSLParser parser = new PSLParser();
        parser.enableSharedQueryTarget(true);
        {
          int psl_option = -1;
          Object[] options;

          // If the name ends with ".link.psl" then assume it is a mapping
          // of probe sets to consensus seqs to genome, and thus select
          // psl_option = 1 "target".
          // Otherwise, the user has to tell us wether to annotate the
          // "query" or "target" or "other"
          if (lcname.endsWith(".link.psl")) {
            parser.setIsLinkPsl(true);
            psl_option = 1; // "target"
          } else {
            if (lcname.endsWith(".psl3")) {
              options = new Object[] { "Query", "Target", "Other"};
            }
            else {
              options = new Object[] { "Query", "Target"};
            }
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
              psl_option = JOptionPane.showOptionDialog(gviewer.getFrame(), "Annotate which sequence?",
                                                      "PSL annotation options", JOptionPane.DEFAULT_OPTION,
                                                      JOptionPane.QUESTION_MESSAGE, null,
                                                      options, "Target");
            } else {
              psl_option = InvokeUtils.invokeOptionDialog(gviewer.getFrame(), "Annotate which sequence?",
                                                      "PSL annotation options", JOptionPane.DEFAULT_OPTION,
                                                      JOptionPane.QUESTION_MESSAGE, null,
                                                      options, "Target");
            }
          }
          boolean annotate_query = (psl_option == 0);
          boolean annotate_target = (psl_option == 1);
          boolean annotate_other = (psl_option == 2);
          System.out.println("annotate: query = " + annotate_query +
                             ", target = " + annotate_target +
                             ", other = " + annotate_other);
          if (annotate_query) {
            parser.parse(str, stream_name, selected_group, null, null, true, false, false);
          }
          else if (annotate_target)  {
            parser.parse(str, stream_name, null, selected_group, null, false, true, false);
          }
          else if (annotate_other)  {
            parser.parse(str, stream_name, null, null, selected_group, false, false, true);
          }
          aseq = input_seq;
        }
        parser = null;
      }
      else if (lcname.endsWith(".bps")) {
        String annot_type = stream_name.substring(0, stream_name.indexOf(".bps"));
        DataInputStream dis = new DataInputStream(str);
        BpsParser bps_parser = new BpsParser();
        bps_parser.parse(dis, annot_type, null, selected_group, false, true);
        bps_parser = null;
        aseq = input_seq;
      }
      else if (lcname.endsWith(".bed")) {
        String annot_type = stream_name.substring(0, stream_name.indexOf(".bed"));
        BedParser parser = new BedParser();
        // really need to switch create_container (last argument) to true soon!
        parser.parse(str, selected_group, true, annot_type, false);
        aseq = input_seq;
        parser = null;
      }
      else if (lcname.endsWith(".bgn")) {
        String annot_type = stream_name.substring(0, stream_name.indexOf(".bgn"));
        BgnParser parser = new BgnParser();
        parser.parse(str, annot_type, selected_group, -1, true);
        aseq = input_seq;
        parser = null;
      }
      else if (lcname.endsWith(".brs")) {
        BrsParser parser = new BrsParser();
        String annot_type = stream_name.substring(0, stream_name.indexOf(".brs"));
        java.util.List alist = parser.parse(str, annot_type, selected_group);
        aseq = input_seq;
        parser = null;
      }
      else if (lcname.endsWith(".bsnp")) {
        BsnpParser parser = new BsnpParser();
        String annot_type = stream_name.substring(0, stream_name.indexOf(".bsnp"));
        java.util.List alist = parser.parse(str, annot_type, selected_group, true);
        System.out.println("total snps loaded: " + alist.size());
        aseq = input_seq;
        parser = null;
      }
      else if (lcname.endsWith(".brpt")) {
        BrptParser parser = new BrptParser();
        String annot_type = stream_name.substring(0, stream_name.indexOf(".brpt"));
        java.util.List alist = parser.parse(str, annot_type, selected_group, true);
        System.out.println("total repeats loaded: " + alist.size());
        aseq = input_seq;
        parser = null;
      }
      else if (lcname.endsWith(".bp1") || lcname.endsWith(".bp2")) {
        Bprobe1Parser parser = new Bprobe1Parser();
        String annot_type = stream_name.substring(0, stream_name.indexOf(".bp"));
        parser.parse(str, selected_group, true, annot_type, true);
        aseq = input_seq;
        parser = null;
      }
      else if (lcname.endsWith(".ead")) {
	ExonArrayDesignParser parser = new ExonArrayDesignParser();
	String default_type = stream_name.substring(0, stream_name.indexOf(".ead"));
	parser.parse(str, selected_group, true, default_type);
	aseq = input_seq;
	parser = null;
      }
      else if (lcname.endsWith(".gff") || lcname.endsWith(".gtf") || lcname.endsWith(".gff3")) {
        // assume it's GFF1, GFF2, GTF, or GFF3 format
        GFFParser parser = new GFFParser();
        parser.setUseStandardFilters(true);
        int index = stream_name.indexOf(".gff");
        if (index < 0) {
          index = stream_name.indexOf(".gtf");
        }
        String annot_type = stream_name;
        if (index > 0) {
          annot_type = stream_name.substring(0, index);
        }
        parser.parse(str, annot_type, selected_group, false);
        parser = null;
      }
      else if (lcname.endsWith(".fa") || lcname.endsWith(".fasta")) {
        FastaParser parser = new FastaParser();
        java.util.List seqs = parser.parseAll(str, selected_group);

        if (input_seq != null && seqs.contains(input_seq)) {
          aseq = input_seq;
        } else if (! seqs.isEmpty()) {
          aseq = (MutableAnnotatedBioSeq) seqs.get(0);
        } else {
          aseq = null;
        }
        parser = null;
      }
      else if (lcname.endsWith(".bnib")) {
        //TODO: check that these conditions make sense
        if (input_seq == null || input_seq instanceof NibbleBioSeq) {
          aseq = NibbleResiduesParser.parse(str, selected_group);
          if (aseq != gmodel.getSelectedSeq()) {
            //TODO: maybe set the current seq to this seq
            System.out.println("WARNING: this is not the currently-selected sequence.");
          }
        }
        else {
          ErrorHandler.errorPanel(gviewer.getFrame(), "ABORTED LOADING BNIB FILE",
            "The currently loaded sequence is not the correct type to merge with a bnib file", null);
        }
      }
      else {
	ErrorHandler.errorPanel(gviewer.getFrame(), "FORMAT NOT RECOGNIZED",
          "Format not recognized for file: " + stream_name, null);
      }

      System.gc();
    }
    catch (Exception ex) {
      the_exception = ex;
      //ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", "Error loading file", ex);
    } finally {
      if (str != null) try {str.close();} catch (Exception e) {}
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

//  private static AnnotatedSeqGroup getNewGroup() {
//    unknown_group_count++;
//    String new_name = UNKNOWN_GROUP_PREFIX + " " + unknown_group_count;
//    AnnotatedSeqGroup new_group= gmodel.addSeqGroup(new_name);
//    return new_group;
//  }

  /** Returns the first BioSeq on the first SeqSymmetry in the given list, or null. */
  private static MutableAnnotatedBioSeq getFirstSeq(java.util.List syms) {
    MutableAnnotatedBioSeq first_seq = null;
    if (syms != null && ! syms.isEmpty()) {
      SeqSymmetry fsym = (SeqSymmetry) syms.get(0);
      SeqSpan fspan = fsym.getSpan(0);
      first_seq = (MutableAnnotatedBioSeq) fspan.getBioSeq();
    }
    return first_seq;
  }

  /** Returns the first BioSeq on the last SeqSymmetry in the given list, or null. */
  private static MutableAnnotatedBioSeq getLastSeq(java.util.List syms) {
    MutableAnnotatedBioSeq last_seq = null;
    if (syms != null && ! syms.isEmpty()) {
      SeqSymmetry fsym = (SeqSymmetry) syms.get(syms.size() - 1);
      SeqSpan fspan = fsym.getSpan(0);
      last_seq = (MutableAnnotatedBioSeq) fspan.getBioSeq();
    }
    return last_seq;
  }

  /** A JFileChooser that has a checkbox for whether you want to merge annotations.
   *  Note that an alternative way of adding a checkbox to a JFileChooser
   *  is to use JFileChooser.setAccessory().  The only advantage to this
   *  subclass is more control of where the JCheckBox is placed inside the
   *  dialog.
   */
  public static class MergeOptionFileChooser extends JFileChooser {
    ButtonGroup bgroup = new ButtonGroup();
    public JRadioButton merge_button = new JRadioButton("Merge with currently-loaded data", true);
    public JRadioButton no_merge_button = new JRadioButton("Create new genome: ", false);
    public JTextField genome_name_TF = new JTextField("Unknown Genome");

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
      box.setBorder(BorderFactory.createEmptyBorder(5,5,8,5));
      box.add(Box.createHorizontalStrut(5));
      box.add(merge_button);
      box.add(no_merge_button);
      box.add(Box.createRigidArea(new Dimension(5,0)));
      box.add(genome_name_TF);

      merge_button.setMnemonic('M');
      no_merge_button.setMnemonic('C');
    }


    protected JDialog createDialog(Component parent) throws HeadlessException {
      JDialog dialog = super.createDialog(parent);

      dialog.getContentPane().add(box, BorderLayout.SOUTH);
      return dialog;
    }
  }
}





