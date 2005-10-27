/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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

import java.io.*;
import java.net.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.zip.GZIPInputStream;
import org.xml.sax.InputSource;

import com.affymetrix.genoviz.widget.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.swing.threads.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.parsers.*;
import com.affymetrix.igb.util.*;
import com.affymetrix.igb.view.*;
import com.affymetrix.igb.IGB;

public class LoadFileAction {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  static String user_dir = System.getProperty("user.dir");
  SeqMapView gviewer;
  FileTracker load_dir_tracker;
  static int unknown_group_count = 1;
  public static final String UNKNOWN_GROUP_PREFIX = "Unknown Group";


  /**
   *  Constructor.
   *  @param ft  a FileTracker used to keep track of directory to load from
   */
  public LoadFileAction(SeqMapView gviewer, FileTracker ft) {
    this.gviewer = gviewer;
    load_dir_tracker = ft;
  }

  public void actionPerformed(ActionEvent e) {
    AnnotatedBioSeq aseq = loadFile();
    //if (aseq != null) {
      // This is redundant: was done in loadFile();
      //gviewer.setAnnotatedSeq(aseq, true, true);
    //}
  }

  static MergeOptionFileChooser chooser = null;

  static MergeOptionFileChooser getFileChooser() {
    if (chooser == null) {
      chooser = new MergeOptionFileChooser();
      chooser.setMultiSelectionEnabled(true);
      chooser.addChoosableFileFilter(new UniFileFilter("axml"));
      chooser.addChoosableFileFilter(new UniFileFilter("bed"));
      chooser.addChoosableFileFilter(new UniFileFilter(
        new String[] {"bps", "bgn", "brs", "bsnp", "brpt", "bnib", "bp1"},
        "Binary Files"));
      chooser.addChoosableFileFilter(new UniFileFilter(
        new String[] {"gff", "gtf"},
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
        new String[] {"gr", "bgr", "sgr", "bar"},
        "Graph Files"));
      chooser.addChoosableFileFilter(new UniFileFilter(
        new String[] {"sin", "egr", "egr.txt"},
        "Scored Interval Files"));
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

  public MutableAnnotatedBioSeq loadFile()  {
    MutableAnnotatedBioSeq aseq = null;

    MergeOptionFileChooser chooser = getFileChooser();
    chooser.setCurrentDirectory(load_dir_tracker.getFile());
    chooser.rescanCurrentDirectory();
    if (gmodel.getSelectedSeqGroup() == null) {
      chooser.merge_button.setSelected(false);
      chooser.merge_button.setEnabled(false);
    }
    else {
      // default to "merge" if already have a selected seq group to merge with,
      //    because non-merging is an uncommon choice
      chooser.merge_button.setSelected(true);
      chooser.merge_button.setEnabled(true);
    }

    int option = chooser.showOpenDialog(gviewer.getFrame());

    if (option == JFileChooser.APPROVE_OPTION) {
      load_dir_tracker.setFile(chooser.getCurrentDirectory());
      File[] fils = chooser.getSelectedFiles();
      AnnotatedSeqGroup previous_seq_group = gmodel.getSelectedSeqGroup();

      if (chooser.merge_button.isSelected()) {
        aseq = gmodel.getSelectedSeq();
      }
      else {
        gmodel.setSelectedSeq(null);
        gmodel.setSelectedSeqGroup(null);
        aseq = null;
      }

      for (int i=0; i<fils.length; i++) {
        File cfil = fils[i];
        String file_name = cfil.toString();
        if (file_name.indexOf("http:") > -1) {  // direct input of http...
          // This method of inputing a URL is not the best way to go, but it sometimes works...
          // On Linux, and maybe in general, if the user types "http://www.google.com",
          // it will come out as "/home/user/http:/www.google.com", so we have to
          // trim off the beginning stuff AND add back the double slash "//" after http.
          String url_name = "http://" + file_name.substring(file_name.indexOf("http:")+6);
          System.out.println("detected url input: " + url_name);
          loadFromUrl(gviewer, url_name, aseq);
        }
        else {
          aseq = load(gviewer, cfil, aseq);
        }
      }

      if (gmodel.getSelectedSeqGroup() == null) {
        // This primarily can happen if the merge button is not selected
        // and the loading of the file fails or fails to create a seq group.
        gmodel.setSelectedSeqGroup(previous_seq_group);
      }

      gviewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
    }
    return aseq;
  }

  public MutableAnnotatedBioSeq load(File annotfile) {
    return load(gviewer, annotfile, null);
  }

  public static MutableAnnotatedBioSeq load(SeqMapView gviewer, File annotfile, MutableAnnotatedBioSeq input_seq) {
    MutableAnnotatedBioSeq aseq = null;
    InputStream fistr = null;
    try {
      int file_length = (int)annotfile.length();
      //fistr = new FileInputStream(annotfile);
      StringBuffer sb = new StringBuffer();
      fistr = Streamer.getInputStream(annotfile,  sb);
      String stripped_name = sb.toString();

      if (GraphSymUtils.isAGraphFilename(stripped_name)) {
        AnnotatedSeqGroup seq_group = SingletonGenometryModel.getGenometryModel().getSelectedSeqGroup();
        if (seq_group == null) {
          ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", "Must select a a genome before loading a graph.  " +
            "Graph data must be merged with already loaded genomic data.", null);
        } else {
          Map seqs = seq_group.getSeqs();
//          GraphSymUtils.readGraphs(fistr, annotfile.getAbsolutePath(), seqs, input_seq);
          URL url = annotfile.toURI().toURL();
          OpenGraphAction.loadGraphFile(url, seqs, input_seq);
        }
      }
      else {
        aseq = load(gviewer, fistr, stripped_name, input_seq, file_length);
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

  public static MutableAnnotatedBioSeq loadFromUrl(SeqMapView gviewer, String url_name, MutableAnnotatedBioSeq input_seq) {
    MutableAnnotatedBioSeq result = null;
    InputStream istr = null;
    try {
      URL loadurl = new URL(url_name);
      istr = new BufferedInputStream(loadurl.openStream());
      result = load(gviewer, istr, url_name, input_seq);
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", "Error loading file", ex);
    } finally {
      if (istr != null) try {istr.close();} catch (Exception e) {}
    }
    return result;
  }

  public static MutableAnnotatedBioSeq load(SeqMapView gviewer, InputStream instr, String stream_name,
                                     MutableAnnotatedBioSeq input_seq) {
    return load(gviewer, instr, stream_name, input_seq, -1);
  }

  /** Loads from an InputStream.
   *  Detects the type of file based on the filename ending of the
   *  stream_name parameter, for example ".dasxml".
   *  The stream will be passed through uncompression routines in the Streamer
   *  class if necessary.
   */
  public static MutableAnnotatedBioSeq load(SeqMapView gviewer, InputStream instr, String stream_name,
                                     MutableAnnotatedBioSeq input_seq, int stream_length) {
    MutableAnnotatedBioSeq aseq = null;
    InputStream str = null;
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

      Map seqhash = null;
      AnnotatedSeqGroup selected_group = gmodel.getSelectedSeqGroup();
      if (selected_group != null)  {
        seqhash = selected_group.getSeqs();
      }

      if (lcname.endsWith(".axml")) {
        Xml2GenometryParser parser = new Xml2GenometryParser();
        aseq = parser.parse(str, input_seq);
        parser = null;
      }
      else if (lcname.endsWith(".das") || lcname.endsWith(".dasxml")) {
        Das1FeatureSaxParser parser = new Das1FeatureSaxParser();
        aseq = parser.parse(str, input_seq);
        parser = null;
      }
      else if (lcname.endsWith(".das2xml")) {
	System.out.println("in LoadFileAction.load(), parsing with Das2FeatureSaxParser");
	Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
	java.util.List results = parser.parse(new InputSource(str), selected_group, true);
	System.out.println("result count: " + results.size());
	for (int i=0; i<results.size(); i++) {
	  SeqSymmetry sym = (SeqSymmetry)results.get(i);
	  //	  SeqUtils.printSymmetry(sym);
	}
	aseq = input_seq;
      }
      else if (lcname.endsWith(".map"))  {
        ScoredMapParser parser = new ScoredMapParser();
        parser.parse(str, stream_name, input_seq);
        aseq = input_seq;
        parser = null;
      }
      else if (lcname.endsWith(".sin") ||
               lcname.endsWith(".egr") ||
               lcname.endsWith(".txt")
               ) {
        if (seqhash == null) {
          // It would be possible to allow "sin" files to be read when seqhash is null
          // by using the same technique used below for GFF files
          ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", "Scored interval files can only be loaded if a genome is already loaded.\n"+
          "Please load a genome before opening this file '" + stream_name + "'.", null);
        } else {
          ScoredIntervalParser parser = new ScoredIntervalParser();
          Map id2sym_hash = IGB.getSymHash();
          parser.parse(str, stream_name, seqhash, id2sym_hash);
          IGB.symHashChanged(parser);
          aseq = input_seq;
          parser = null;
        }
      }
      else if (lcname.endsWith(".psl") || lcname.endsWith( ".psl3")) {
        PSLParser parser = new PSLParser();
        parser.enableSharedQueryTarget(true);
        if (seqhash == null) {
          aseq = parser.parse(str, input_seq, stream_name);
        }
        else {
          int psl_option = -1;
          Object[] options;

          // If the name ends with ".link.psl" then assume it is a mapping
          // of probe sets to consensus seqs to genome, and thus select
          // psl_option = 1 "target".
          // Otherwise, the user has to tell us wether to annotate the
          // "query" or "target" or "other"
          if (lcname.endsWith(".link.psl")) {
            psl_option = 1; // "target"
            if (seqhash != null) {
              // Make a copy of the seqhash, because we do NOT want all the temporary
              // sequences found in the link.psl file to be added to the real seqmap.
              Map seqhash_copy = new HashMap();
              seqhash_copy.putAll(seqhash);
              seqhash = seqhash_copy;
            }
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
            parser.parse(str, stream_name, seqhash, null, true, false);
          }
          else if (annotate_target)  {
            parser.parse(str, stream_name, null, seqhash, false, true);
          }
          else if (annotate_other)  {
            parser.parse(str, stream_name, null, null, seqhash, false, false, true);
          }
          aseq = input_seq;
        }
        parser = null;
      }
      else if (lcname.endsWith(".bps")) {
        // bps parsing requires a Map of sequences (seqid ==> BioSeq) rather than a single BioSeq
        //        if (seqhash == null) {
        //          seqhash = new HashMap();
        //          seqhash.put(input_seq.getID(), input_seq);
        //        }
        if (seqhash == null) {
          ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", 
            ".bps files can only be loaded if a seq group is already selected", null);
        }
        else {
          String annot_type = stream_name.substring(0, stream_name.indexOf(".bps"));
          DataInputStream dis = new DataInputStream(str);
          BpsParser psl_reader = new BpsParser();
          psl_reader.parse(dis, annot_type, seqhash);
          psl_reader = null;
        }
        aseq = input_seq;
      }
      else if (lcname.endsWith(".bed")) {
        System.out.println("loading via BedParser");
        String annot_type = stream_name.substring(0, stream_name.indexOf(".bed"));
        BedParser parser = new BedParser();
	//        if (seqhash == null) {
        if (selected_group == null) {
          aseq = parser.parse(str, input_seq);
        }
        else {
	  //          parser.parse(str, seqhash, true, annot_type);
	  // really need to switch create_container (last argument) to true soon!
          parser.parse(str, selected_group, true, annot_type, false);
          aseq = input_seq;
        }
        parser = null;
      }
      else if (lcname.endsWith(".bgn")) {
        if (seqhash == null) {
          ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", 
            ".bgn files can only be loaded if a seq group is already selected", null);
        }
        else {
          BgnParser gene_reader = new BgnParser();
          String annot_type = stream_name.substring(0, stream_name.indexOf(".bgn"));
          gene_reader.parse(str, annot_type, seqhash, -1);
        }
        aseq = input_seq;
      }
      else if (lcname.endsWith(".brs")) {
        if (seqhash == null) {
          ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", 
            ".brs files can only be loaded if a seq group is already selected", null);
        }
        else {
          BrsParser refseq_reader = new BrsParser();
          String annot_type = stream_name.substring(0, stream_name.indexOf(".brs"));
          Map id2sym_hash = IGB.getSymHash();
          java.util.List alist = refseq_reader.parse(str, annot_type, seqhash, id2sym_hash, -1);
          IGB.symHashChanged(refseq_reader);
          System.out.println("total refseq annotations loaded: " + alist.size());
        }
        aseq = input_seq;
      }
      else if (lcname.endsWith(".bsnp")) {
        if (seqhash == null) {
          ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", 
            ".bsnp files can only be loaded if a seq group is already selected", null);
        }
        else {
          BsnpParser parser = new BsnpParser();
          String annot_type = stream_name.substring(0, stream_name.indexOf(".bsnp"));
          java.util.List alist = parser.parse(str, annot_type, seqhash, true);
	  System.out.println("total snps loaded: " + alist.size());
        }
        aseq = input_seq;
      }
      else if (lcname.endsWith(".brpt")) {
        if (seqhash == null) {
          ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", 
            ".brpt files can only be loaded if a seq group is already selected", null);
        }
        else {
          BrptParser parser = new BrptParser();
          String annot_type = stream_name.substring(0, stream_name.indexOf(".brpt"));
          java.util.List alist = parser.parse(str, annot_type, seqhash, true);
          System.out.println("total repeats loaded: " + alist.size());
        }
        aseq = input_seq;
      }
      else if (lcname.endsWith(".bp1")) {
        Bprobe1Parser parser = new Bprobe1Parser();
        String annot_type = stream_name.substring(0, stream_name.indexOf(".bp1"));
        parser.parse(str, gmodel.getSelectedSeqGroup(), true, annot_type);
        aseq = input_seq;
      }
      else if (lcname.endsWith(".gff") || lcname.endsWith(".gtf")) {
        // assume it's GFF1, GFF2, or GTF format
        GFFParser parser = new GFFParser();

        parser.addStandardFilters();
        if (seqhash == null) {
          String new_name = UNKNOWN_GROUP_PREFIX + " " + unknown_group_count;
          AnnotatedSeqGroup new_group= gmodel.addSeqGroup(new_name);
          unknown_group_count++;
          if (input_seq != null) {
            new_group.addSeq(input_seq);
          }
          parser.parse(str, new_group.getSeqs(), IGB.getSymHash(), false);
          gmodel.setSelectedSeqGroup(new_group); // needs to come after parsing for QuickLoad panel to find all the sequences
          aseq = input_seq;
          gmodel.setSelectedSeq(input_seq);
        }
        else {
          System.out.println("in GFFParser, annotating all seqs in SeqMapView seqhash");
          parser.parse(str, seqhash, IGB.getSymHash(), false);
          aseq = input_seq;
        }
        IGB.symHashChanged(parser);
        parser = null;
      }
      else if (lcname.endsWith(".fa") || lcname.endsWith(".fasta")) {
        FastaParser parser = new FastaParser();
        if (input_seq != null) {
          aseq = parser.parse(str, input_seq, input_seq.getLength());
        }
        else  {
          // to help eliminate memory spike (by dynamic reallocation of memory in StringBuffer -- don't ask...)
          // give upper limit to sequence length, based on file size -- this will be an overestimate (due to
          //   white space, name header, etc.), but probably no more than 10% greater than actual size, which
          //   is a lot better than aforementioned memory spike, which can temporarily double the amount of
          //   memory needed
          //          aseq = parser.parse(str, input_seq);
          //          int file_length = (int)seqfile.length();
          aseq = parser.parse(str, input_seq, stream_length);  // if stream_length <= 0, will be ignored...
        }
        parser = null;
      }
      else if (lcname.endsWith(".bnib")) {
        if (input_seq == null || input_seq instanceof NibbleBioSeq) {
          aseq = NibbleResiduesParser.parse(str, (NibbleBioSeq)input_seq);
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
      if (seqhash == null) {
        if (aseq != null) {
          String new_name = UNKNOWN_GROUP_PREFIX + " " + unknown_group_count;
          AnnotatedSeqGroup new_group= gmodel.addSeqGroup(new_name);
          unknown_group_count++;
          new_group.addSeq(aseq);
          gmodel.setSelectedSeqGroup(new_group);
          gmodel.setSelectedSeq(aseq);
        }
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", "Error loading file", ex);
    } finally {
      if (str != null) try {str.close();} catch (Exception e) {}
    }
    return aseq;
  }



  /** A JFileChooser that has a checkbox for whether you want to merge annotations.
   *  Note that an alternative way of adding a checkbox to a JFileChooser
   *  is to use JFileChooser.setAccessory().  The only advantage to this
   *  subclass is more control of where the JCheckBox is placed inside the
   *  dialog.
   */
  public static class MergeOptionFileChooser extends JFileChooser {
    public JCheckBox merge_button = new JCheckBox("Merge with currently-loaded data", true);
    protected JDialog createDialog(Component parent) throws HeadlessException {
      JDialog dialog = super.createDialog(parent);
      Box box = new Box(BoxLayout.X_AXIS);
      box.add(Box.createHorizontalStrut(5));
      box.add(merge_button);
      dialog.getContentPane().add(box, BorderLayout.SOUTH);
      merge_button.setMnemonic('M');
      return dialog;
    }
  }
}





