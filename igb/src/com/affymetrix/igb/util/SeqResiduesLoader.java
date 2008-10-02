package com.affymetrix.igb.util;

import java.io.*;
import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.*;
import com.affymetrix.genometryImpl.*;
import com.affymetrix.igb.*;
import com.affymetrix.igb.das2.*;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.QuickLoadView2;
import com.affymetrix.igb.view.QuickLoadServerModel;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;

public class SeqResiduesLoader {

    public static final String PREF_DAS_DNA_SERVER_URL = "DAS DNA Server URL";
    public static final String DEFAULT_DAS_DNA_SERVER = "http://genome.cse.ucsc.edu/cgi-bin/das";

    /**
     *  Load all DNA residues for current AnnotatedBioSeq
     */
    public static boolean loadAllResidues(SmartAnnotBioSeq aseq)  {
	//  if (current_group==null) { 
	//      ErrorHandler.errorPanel("Error", "No sequence group selected.", gviewer); return; }
	//  if (current_seq==null) { 
	//      ErrorHandler.errorPanel("Error", "No sequence selected.", gviewer); return; }
	SeqMapView gviewer = Application.getSingleton().getMapView();
	String seq_name = aseq.getID();
	AnnotatedSeqGroup seq_group = aseq.getSeqGroup();
	System.out.println("processing request to load residues for sequence: " + seq_name);
	if (aseq.isComplete()) {
	    System.out.println("already have residues for " + seq_name);
	    return false;
	}
        
        // Load in the data.  Note that the below assumes bnib format.
	boolean loaded = false;
	if (seq_group instanceof Das2SeqGroup)  {
            loaded = LoadResiduesFromDAS2(seq_group, aseq);
	}
	if (! loaded)  {
            loaded = LoadResiduesFromQuickLoad(seq_group, seq_name, gviewer);
	}
	if (loaded)  { gviewer.setAnnotatedSeq(aseq, true, true, true); }
	return loaded;
    }

    // try loading via DAS/2 server that genome was originally modelled from  
    private static boolean LoadResiduesFromDAS2(AnnotatedSeqGroup seq_group, SmartAnnotBioSeq aseq) {
        boolean loaded;
        Das2SeqGroup das2_group = (Das2SeqGroup) seq_group;
        Das2VersionedSource das2_vsource = das2_group.getOriginalVersionedSource();
        Das2Region segment = das2_vsource.getSegment(aseq);
        String segment_uri = segment.getID();
        System.out.println("trying to load residues via DAS/2");
        String bnib_uri = segment_uri + "?format=bnib";
        System.out.println("   request URI: " + bnib_uri);
        InputStream istr = null;
        Map headers = new HashMap();
        try {
            istr = LocalUrlCacher.getInputStream(bnib_uri, QuickLoadServerModel.getCacheResidues(), headers);
            // System.out.println(headers);
            String content_type = (String) headers.get("content-type");
            System.out.println("    response content-type: " + content_type);
            if ((istr != null) && (content_type != null) && (content_type.equals(NibbleResiduesParser.getMimeType()))) {
                // check for bnib format
                // NibbleResiduesParser handles creating a BufferedInputStream from the input stream
                System.out.println("   response is in bnib format, parsing...");
                NibbleResiduesParser.parse(istr, seq_group);
                loaded = true;
            } else {
                System.out.println("   response is not in bnib format, aborting DAS/2 residues loading");
                loaded = false;
            }
        } catch (Exception ex) {
            loaded = false;
            ex.printStackTrace();
        } finally {
            try {
                if (istr != null) {
                    istr.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return loaded;
    }

    
    private static boolean LoadResiduesFromQuickLoad(AnnotatedSeqGroup seq_group, String seq_name, SeqMapView gviewer) {
        boolean loaded;
        InputStream istr = null;
        String root_url = QuickLoadView2.getQuickLoadUrl();
        String genome_name = seq_group.getID();
        try {
            System.out.println("trying to load residues via default QuickLoad location");
            String url_path = root_url + genome_name + "/" + seq_name + ".bnib";
            System.out.println("  location of bnib file: " + url_path);
            istr = LocalUrlCacher.getInputStream(url_path, QuickLoadServerModel.getCacheResidues());
            // NibbleResiduesParser handles creating a BufferedInputStream from the input stream
            NibbleResiduesParser.parse(istr, seq_group);
            loaded = true;
        } catch (Exception ex) {
            loaded = false;
            ErrorHandler.errorPanel("Error", 
                    "cannot access sequence:\n" + "seq = '" + seq_name + "'\n" + "version = '" + genome_name + "'\n" + "server = " + root_url, gviewer, ex);
        } finally {
            try {
                istr.close();
            } catch (Exception e) {
            }
        }
        return loaded;
    }


    /**
     *  Load sequence residues for a span along a sequence.
     *  Access residues via DAS reference server
     *
     *  DAS reference server can be specified by setting PREF_DAS_DNA_SERVER_URL preference value.
     *  Currently defaults to UCSC DAS reference server (this will cause problems if genome is not
     *     available at UCSC)
     */
    public static boolean loadPartialResidues(SeqSpan span, AnnotatedSeqGroup seq_group)  {
	SeqMapView gviewer = Application.getSingleton().getMapView();
	String das_dna_server = UnibrowPrefsUtil.getLocation(PREF_DAS_DNA_SERVER_URL, DEFAULT_DAS_DNA_SERVER);
	AnnotatedBioSeq aseq = (AnnotatedBioSeq)span.getBioSeq();
	String seqid = aseq.getID();
	System.out.println("trying to load residues for span: " + SeqUtils.spanToString(span));
	String current_genome_name = seq_group.getID();
	System.out.println("current genome name: " + current_genome_name);

	//    System.out.println("seq_id: " + seqid);
	int min = span.getMin();
	int max = span.getMax();
	int length = span.getLength();

	if ((min <= 0) && (max >= aseq.getLength())) {
	    System.out.println("loading all residues");
	    // loadAllResidues(aseq.getID());
            return true;
	}
	
        if (!(aseq instanceof NibbleBioSeq)) {
            System.err.println("quickloaded seq is _not_ a NibbleBioSeq: " + aseq);
            return false;
        }

        String residues = GetResidues(das_dna_server, current_genome_name, seqid, min, max, length);
        if (residues == null) {
            return false;
        }
        
        BioSeq subseq = new SimpleBioSeq(aseq.getID() + ":" + min + "-" + max, residues);

        SeqSpan span1 = new SimpleSeqSpan(0, length, subseq);
        SeqSpan span2 = span;
        MutableSeqSymmetry subsym = new SimpleMutableSeqSymmetry();
        subsym.addSpan(span1);
        subsym.addSpan(span2);

        NibbleBioSeq compseq = (NibbleBioSeq) aseq;
        MutableSeqSymmetry compsym = (MutableSeqSymmetry) compseq.getComposition();
        if (compsym == null) {
            //System.err.println("composite symmetry is null!");
            compsym = new SimpleMutableSeqSymmetry();
            compsym.addChild(subsym);
            compsym.addSpan(new SimpleSeqSpan(span2.getMin(), span2.getMax(), aseq));
            compseq.setComposition(compsym);
        } else {
            compsym.addChild(subsym);
            SeqSpan compspan = compsym.getSpan(aseq);
            int compmin = Math.min(compspan.getMin(), min);
            int compmax = Math.max(compspan.getMax(), max);
            SeqSpan new_compspan = new SimpleSeqSpan(compmin, compmax, aseq);
            compsym.removeSpan(compspan);
            compsym.addSpan(new_compspan);
        //        System.out.println("adding to composition: " );
        //        SeqUtils.printSymmetry(compsym);
        }
        gviewer.setAnnotatedSeq(aseq, true, true, true);

	return true;
    }

    // Get the residues from the DAS server
    private static String GetResidues(String das_dna_server, String current_genome_name, String seqid, int min, int max, int length) {
        String residues = null;
        try {
            String das_dna_source = DasUtils.findDasSource(das_dna_server, current_genome_name);
            if (das_dna_source == null) {
                System.out.println("Couldn't find das source genome '" + current_genome_name + "'\n on DAS server:\n" + das_dna_server);
            }
            String das_seqid = DasUtils.findDasSeqID(das_dna_server, das_dna_source, seqid);
            if (das_seqid == null) {
                System.out.println("Couldn't access sequence residues on DAS server\n" + " seqid: '" + seqid + "'\n" + " genome: '" + current_genome_name + "'\n" + " DAS server: " + das_dna_server);
            }
            residues = DasUtils.getDasResidues(das_dna_server, das_dna_source, das_seqid, min, max);
            System.out.println("DAS DNA request length: " + length);
            System.out.println("DAS DNA response length: " + residues.length());
        } catch (Exception ex) {
            System.out.println("Couldn't access sequence residues on DAS server\n" + " seqid: '" + seqid + "'\n" + " genome: '" + current_genome_name + "'\n" + " DAS server: " + das_dna_server);
        }

        return residues;
    }

}
