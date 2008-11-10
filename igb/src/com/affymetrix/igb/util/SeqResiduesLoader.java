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
import com.affymetrix.genometryImpl.parsers.FastaParser;

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
            // -1 indicates no min/max
            // Try to load in bnib format
            
            /*
            String uri = generateDas2URI(seq_group, aseq, -1, -1, true);
            loaded = LoadResiduesFromDAS2(seq_group, uri);
            if (!loaded) {
                // Try to load in fasta format
                uri = generateDas2URI(seq_group, aseq, -1, -1, false);
                loaded = LoadResiduesFromDAS2(seq_group, uri);
            }
             * */
            
            String uri = generateDas2URI(seq_group, aseq, -1, -1, false);
            loaded = LoadResiduesFromDAS2(seq_group, uri);
            if (!loaded) {
                // Try to load in fasta format
                uri = generateDas2URI(seq_group, aseq, -1, -1, true);
                loaded = LoadResiduesFromDAS2(seq_group, uri);
            }
            
	}
	if (! loaded)  {
            loaded = LoadResiduesFromQuickLoad(seq_group, seq_name, gviewer);
	}
	if (loaded)  { gviewer.setAnnotatedSeq(aseq, true, true, true); }
	return loaded;
    }

    // try loading via DAS/2 server that genome was originally modeled from  
    private static boolean LoadResiduesFromDAS2(AnnotatedSeqGroup seq_group, String uri) {
        boolean loaded;
        
        InputStream istr = null;
        Map headers = new HashMap();
        try {
            istr = LocalUrlCacher.getInputStream(uri, QuickLoadServerModel.getCacheResidues(), headers);
            // System.out.println(headers);
            String content_type = (String) headers.get("content-type");
            System.out.println("    response content-type: " + content_type);
            if (istr == null || content_type == null) {
                System.out.println("  Didn't get a proper response from DAS/2; aborting DAS/2 residues loading.");
                return false;
            }
            if (content_type.equals(NibbleResiduesParser.getMimeType())) {
                // check for bnib format
                // NibbleResiduesParser handles creating a BufferedInputStream from the input stream
                System.out.println("   response is in bnib format, parsing...");
                NibbleResiduesParser.parse(istr, seq_group);
                return true;
            }

            if (content_type.equals(FastaParser.getMimeType())) {
                // check for fasta format
                System.out.println("   response is in fasta format, parsing...");
                FastaParser.parseSingle(istr, seq_group);
                return true;
            }

            System.out.println("   response is not in accepted format, aborting DAS/2 residues loading");
            return false;
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
    
    // try loading via DAS/2 server  
    private static String GetFASTADas2Residues(AnnotatedSeqGroup seq_group, SmartAnnotBioSeq aseq, int min, int max) {
        String uri = generateDas2URI(seq_group, aseq, min, max, false);
        InputStream istr = null;
        Map headers = new HashMap();
        try {
            istr = LocalUrlCacher.getInputStream(uri, QuickLoadServerModel.getCacheResidues(), headers);
            // System.out.println(headers);
            String content_type = (String) headers.get("content-type");
            System.out.println("    response content-type: " + content_type);
            if (istr == null || content_type == null) {
                System.out.println("  Didn't get a proper response from DAS/2; aborting DAS/2 residues loading.");
                return null;
            }
           
            if (content_type.equals(FastaParser.getMimeType())) {
                // check for fasta format
                System.out.println("   response is in fasta format, parsing...");
                return FastaParser.parseResidues(istr);
            }

            System.out.println("   response is not in accepted format, aborting DAS/2 residues loading");
            return null;
        } catch (Exception ex) {
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

        return null;
    }


     
     // Generate URI (e.g., "http://www.bioviz.org/das2/genome/A_thaliana_TAIR8/chr1?range=0:1000")     
     private static String generateDas2URI(AnnotatedSeqGroup seq_group, SmartAnnotBioSeq aseq, int min, int max, boolean bnibFormat) {
        Das2SeqGroup das2_group = (Das2SeqGroup) seq_group;
        Das2VersionedSource das2_vsource = das2_group.getOriginalVersionedSource();
        Das2Region segment = das2_vsource.getSegment(aseq);
        String segment_uri = segment.getID();
        System.out.println("trying to load residues via DAS/2");
        String uri; 
        if (max > -1 || !bnibFormat) {
            uri = segment_uri + "?format=fasta";
        }
        else {
            uri = segment_uri + "?format=bnib";
        }
        if (max > -1) {
            // ranged
            uri = uri + "&range=" + min + ":" + max;
        }

        System.out.println("   request URI: " + uri);
        return uri;
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
   
     */
    public static boolean loadPartialResidues(SeqSpan span, AnnotatedSeqGroup seq_group)  {
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
	    return loadAllResidues((SmartAnnotBioSeq)aseq);
            //return true;
	}
	
        if (!(aseq instanceof GeneralBioSeq)) {
            System.err.println("quickloaded seq is _not_ a GeneralBioSeq: " + aseq);
            return false;
        }

        /*  *  Access residues via DAS reference server
         *
         *  DAS reference server can be specified by setting PREF_DAS_DNA_SERVER_URL preference value.
         *  Currently defaults to UCSC DAS reference server (this will cause problems if genome is not
         *     available at UCSC)
         */
        String das_dna_server = UnibrowPrefsUtil.getLocation(PREF_DAS_DNA_SERVER_URL, DEFAULT_DAS_DNA_SERVER);
        String residues = GetDAS1Residues(das_dna_server, current_genome_name, seqid, min, max, length);
        
        if (residues == null) {
            if (!(seq_group instanceof Das2SeqGroup))
                return false;
            
           residues = GetFASTADas2Residues(seq_group, (SmartAnnotBioSeq)aseq, min, max);
           if (residues == null)
               return false;
        }
        
        AddResiduesToComposition(aseq, residues, span);
        
        SeqMapView gviewer = Application.getSingleton().getMapView();
        gviewer.setAnnotatedSeq(aseq, true, true, true);

	return true;
    }

    // Get the residues from the DAS server
    private static String GetDAS1Residues(String das_dna_server, String current_genome_name, String seqid, int min, int max, int length) {
        String residues = null;
        
        if (seqid == null) {
            System.out.println("Couldn't determine das sequence residues -- seqid was null");
            return null;
        }    
        try {
            String das_dna_source = DasUtils.findDasSource(das_dna_server, current_genome_name);
            if (das_dna_source == null) {
                System.out.println("Couldn't find das source genome '" + current_genome_name + "'\n on DAS server:\n" + das_dna_server);
                return null;    // if das_dna_source is null, there's no way to determine the residues
            }
            String das_seqid = DasUtils.findDasSeqID(das_dna_server, das_dna_source, seqid);
            if (das_seqid == null) {
                System.out.println("Couldn't access sequence residues on DAS server\n" + " seqid: '" + seqid + "'\n" + " genome: '" + current_genome_name + "'\n" + " DAS server: " + das_dna_server);
                return null;    // if seqid is null, there's no way to determine the residues
            }
            residues = DasUtils.getDasResidues(das_dna_server, das_dna_source, das_seqid, min, max);
            System.out.println("DAS DNA request length: " + length);
            System.out.println("DAS DNA response length: " + residues.length());
        } catch (Exception ex) {
            System.out.println("Couldn't access sequence residues on DAS server\n" + " seqid: '" + seqid + "'\n" + " genome: '" + current_genome_name + "'\n" + " DAS server: " + das_dna_server);
        }

        return residues;
    }

   
    // Adds the residues to the composite sequence.  This allows merging of subsequences.
    private static void AddResiduesToComposition(AnnotatedBioSeq aseq, String residues, SeqSpan span) {
        BioSeq subseq = new SimpleBioSeq(aseq.getID() + ":" + span.getMin() + "-" + span.getMax(), residues);

        SeqSpan span1 = new SimpleSeqSpan(0, span.getLength(), subseq);
        SeqSpan span2 = span;
        MutableSeqSymmetry subsym = new SimpleMutableSeqSymmetry();
        subsym.addSpan(span1);
        subsym.addSpan(span2);

        GeneralBioSeq compseq = (GeneralBioSeq) aseq;
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
            int compmin = Math.min(compspan.getMin(), span.getMin());
            int compmax = Math.max(compspan.getMax(), span.getMax());
            SeqSpan new_compspan = new SimpleSeqSpan(compmin, compmax, aseq);
            compsym.removeSpan(compspan);
            compsym.addSpan(new_compspan);
            //        System.out.println("adding to composition: " );
            //        SeqUtils.printSymmetry(compsym);
        }
    }


}
