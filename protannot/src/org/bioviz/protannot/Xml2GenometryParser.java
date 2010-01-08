package org.bioviz.protannot;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.comparator.SeqSymStartComparator;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Reads xml file to convert it into a Genometry.
 */

final class Xml2GenometryParser {

    private DocumentBuilderFactory dbFactory;
    private DocumentBuilder dBuilder;
    private boolean DEBUG = false;
    private HashMap<String,BioSeq> mrna_hash;
    private HashMap<String,BioSeq> prot_hash;
    // instance variables needed during the parse
    private String current_simsearch_method = null;

    /**
     *Initialized dbFactory and dBuilder
     */

    Xml2GenometryParser() {
        try {
            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a new BioSeq and add annotations to it.
     * @param   istr    Inputstream of selected file from the file browser.
     * @return          Returns BioSeq of parsed file.
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    BioSeq parse(InputStream istr) {
        mrna_hash = new HashMap<String,BioSeq>();
        prot_hash = new HashMap<String,BioSeq>();

        try {
            InputSource insrc = new InputSource(istr);
            Document seqdoc = dBuilder.parse(insrc);
            return processDocument(seqdoc);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
    <dnaseq>
    <genesearch>
    <gene>
    <primarytranscript>
    <mrna>
    <exon />
    <exon />
    <cds>
    <cdsseg />
    <cdsseg />
    </cds>
    </mrna>
    </primarytranscript>
    </gene>
    </genesearch>
    </dnaseq>
     */
    /**
     * Takes in Document object to parse it and convert into BioSeq.
     * @param   seqdoc  Document object name
     * @return          Returns BioSeq of given document object.
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private BioSeq processDocument(Document seqdoc) {
        BioSeq genomic = null;
        Element top_element = seqdoc.getDocumentElement();
        int glength = 0;
        String name = top_element.getTagName();
        if (name.equalsIgnoreCase("dnaseq")) {
            if (DEBUG) {
                System.err.println("processing dna seq");
            }

            /** if have residues, then ignore the dnaseq's length attribute */
            NodeList children = top_element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                String cname = child.getNodeName();
                if (cname != null && cname.equalsIgnoreCase("residues")) {
                    Text resnode = (Text) child.getFirstChild();
                    String residues = resnode.getData();

                    genomic = new BioSeq("genome", null, residues.length());
                    genomic.setResidues(residues);
                }
            }
            if (genomic == null) {
                try {
                    glength = Integer.parseInt(top_element.getAttribute("length"));
                } catch (Exception ex) {
                    System.err.println("problem with dnaseq length attribute, arbitrarily assigning 200000");
                    glength = 200000;
                }
                genomic = new BioSeq("genome", null, glength);
            }

            processDNASeq(genomic, top_element);
        }
        return genomic;
    }

    /**
     * Process dna in BioSeq for each child node of element provided.
     * @param   genomic
     * @param   elem        Node in genomic for which dna is to be processed
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private void processDNASeq(BioSeq genomic, Element elem) {
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (name != null) {
                if (name.equalsIgnoreCase("genesearch")) {
                    processGeneSearch(genomic, (Element) child);
                } else {
                    if (name.equalsIgnoreCase("mRNA")) {
                        processMRNA(genomic, (Element) child);
                        // residues are now dealt with directly in processDocument() method
                        //	else if (name.equalsIgnoreCase("residues")) {
                        //	  processResidues(genomic, (Element)child);
                        //	}
                    }
                }
            }
        }

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (name != null && name.equalsIgnoreCase("aaseq")) {
                processProtein(genomic, (Element) child);
            }
        }
    }

    /**
     Process protien in BioSeq for each child node of element provided.
     * @param   genomic
     * @param   elem        Node in genomic for which protien is to be processed
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private void processProtein(BioSeq genomic, Element elem) {
        String pid = elem.getAttribute("id");
        BioSeq protein = prot_hash.get(pid);
        if (protein == null) {
            System.err.println("Error: no bioseq matching id: " + pid
                    + ". Skipping it.");
            return;
        }
        if (DEBUG) {
            System.err.println("aaseq: id = " + pid + ",  " + protein);
        }

        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (name != null && name.equalsIgnoreCase("simsearch")) {
                processSimSearch(protein, (Element) child);
            }
        }
    }

    /**
     *
     * @param   query_seq
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private void processSimSearch(BioSeq query_seq, Element elem) {
        NodeList children = elem.getChildNodes();
        String method = elem.getAttribute("method");
        this.current_simsearch_method = method;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (name != null && name.equalsIgnoreCase("simhit")) {
                processSimHit(query_seq, (Element) child);
            }
        }
    }

    /**
     *
     * @param   query_seq
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.MutableSeqSpan
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.SymWithProps
     * @see     com.affymetrix.genometryImpl.TypeContainerAnnot
     * @see     com.affymetrix.genometryImpl.util.SeqUtils
     */
    private void processSimHit(BioSeq query_seq, Element elem) {
        String method = this.current_simsearch_method;
        // method can never be null -- if it is, the XML is wrong
        TypeContainerAnnot hitSym = new TypeContainerAnnot(method);
        addDescriptors(elem, hitSym);

        String hit_name = elem.getAttribute("name");
        String hit_descr = elem.getAttribute("desc");

        if (hit_name != null && hit_name.length() > 0) {
            hitSym.setProperty("name", hit_name);
        }
        if (hit_descr != null && hit_descr.length() > 0) {
            hitSym.setProperty("descr", hit_descr);
        }

        SeqSpan hitSpan = null;
        NodeList children = elem.getChildNodes();
        int num_spans = 0;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element chelem = (Element) child;
                if (name.equalsIgnoreCase("simspan")) {
                    SeqSymmetry spanSym = processSimSpan(query_seq, chelem);
                    ((SymWithProps) spanSym).setProperty("method", method);
                    hitSym.addChild(spanSym);
                    SeqSpan spanSpan = spanSym.getSpan(query_seq);
                    if (hitSpan == null) {
                        hitSpan = new SimpleMutableSeqSpan(spanSpan.getStart(), spanSpan.getEnd(), query_seq);
                    } else {
                        SeqUtils.encompass(hitSpan, spanSpan, (MutableSeqSpan) hitSpan);
                    }
                    hitSym.setProperty("type", "hitspan");
                    num_spans++;
                }
            }
        }
        String prop = (new Integer(num_spans)).toString();
        hitSym.setProperty("num_spans", new String(prop));
        hitSym.setProperty("type", "hit");
        hitSym.addSpan(hitSpan);
        hitSym.setID("");
        query_seq.addAnnotation(hitSym);
    }

    /**
     * Adds description from elem to sym.
     * @param   elem    Source from which description is to added.
     * @param   sym     Target to which description is added.
     * @see     com.affymetrix.genometryImpl.SimpleSymWithProps
     */
    private void addDescriptors(Element elem, SimpleSymWithProps sym) {

        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element chelem = (Element) child;
                if (name.equalsIgnoreCase("descriptor")) {
                    String desc_name = chelem.getAttribute("type");
                    Text tnode = (Text) chelem.getFirstChild();
                    if (tnode != null) {
                        String desc_text = tnode.getData();
                        sym.setProperty(desc_name, desc_text);
                    }
                }
            }
        }
        Object test = sym.getProperty("domain_pos");
        if (test != null) {
            sym.setProperty("name", test);
//      sym.removeProperty("domain_pos");       // not supported in new library
        }
    }

    /**
     *
     * @param   query_seq
     * @param   elem
     * @return  SeqSymmetry
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.SimpleSymWithProps
     */
    private SeqSymmetry processSimSpan(BioSeq query_seq, Element elem) {
        int start = Integer.parseInt(elem.getAttribute("query_start")) - 1;
        int end;
        //  need to standardize on which tag to use!
        // also, should be able to use Element.hasAttribute() to test here, but
        //  compiler is saying this method doesn't exist -- there may be some disagreement
        //  between DOM API I'm referring to and DOM implementation I'm actually using
        //  (the docs say Element.hasAttribute() is part of "DOM level 2" interface)
        try {
            end = Integer.parseInt(elem.getAttribute("query_end")) - 1;
        } catch (Exception ex) {
            end = Integer.parseInt(elem.getAttribute("query_stop")) - 1;
        }
        if (start < end) {
            end++;
        } else {
            start++;
        }

        SimpleSymWithProps spanSym = new SimpleSymWithProps();
        addDescriptors(elem, spanSym);
        String prop = (new Integer(start + 1)).toString();
        spanSym.setProperty("aa_start", prop);
        prop = (new Integer(end + 1)).toString();
        spanSym.setProperty("aa_end", prop);
        prop = (new Integer(end - start + 1)).toString();
        spanSym.setProperty("aa_length", prop);
        SeqSpan qspan = new SimpleSeqSpan(start, end, query_seq);
        spanSym.addSpan(qspan);
        return spanSym;
    }

    /**
     *
     * @param   genomic
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private void processGeneSearch(BioSeq genomic, Element elem) {
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodename = child.getNodeName();
            if (null != nodename && nodename.equalsIgnoreCase("gene")) {
                processGene(genomic, (Element) child);
            }
        }
    }

    /**
     *
     * @param   genomic
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private void processGene(BioSeq genomic, Element elem) {
        int start = Integer.parseInt(elem.getAttribute("start")) - 1;
        int end = Integer.parseInt(elem.getAttribute("end")) - 1;
        if (start < end) {
            end++;
        } else {
            start++;
        }
        if (DEBUG) {
            System.err.println("gene:  start = " + start + "  end = " + end);
        }

        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodename = child.getNodeName();
            if (null != nodename && nodename.equalsIgnoreCase("primarytranscript")) {
                processTranscript(genomic, (Element) child);
            }
        }
    }

    /**
     *
     * @param   genomic
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private void processTranscript(BioSeq genomic, Element elem) {
        int start = Integer.parseInt(elem.getAttribute("start")) - 1;
        int end = Integer.parseInt(elem.getAttribute("end")) - 1;
        if (start < end) {
            end++;
        } else {
            start++;
        }
        if (DEBUG) {
            System.err.println("transcript:  start = " + start + "  end = " + end);
        }
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodename = child.getNodeName();
            if (null != nodename && nodename.equalsIgnoreCase("mrna")) {
                processMRNA(genomic, (Element) child);
            }
        }
    }

    /**
     *
     * @param   genomic
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SimpleSymWithProps
     * @see     com.affymetrix.genometryImpl.MutableSeqSymmetry
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.SymWithProps
     * @see     com.affymetrix.genometryImpl.TypeContainerAnnot
     * @see     com.affymetrix.genometryImpl.util.SeqUtils
     */
    private void processMRNA(BioSeq genomic, Element elem) {
        int start = Integer.parseInt(elem.getAttribute("start")) - 1;
        int end = Integer.parseInt(elem.getAttribute("end")) - 1;
        if (start < end) {
            end++;
        } else {
            start++;
        }
        if (DEBUG) {
            System.err.println("mrna:  start = " + start + "  end = " + end);
        }
        NodeList children = elem.getChildNodes();
        SeqSpan span = new SimpleSeqSpan(start, end, genomic);

        TypeContainerAnnot m2gSym = new TypeContainerAnnot(elem.getAttribute("method"));
        m2gSym.addSpan(span);
        addDescriptors(elem, m2gSym);
        m2gSym.setProperty("type", "mRNA");
        boolean forward = (span.isForward());


        List<SeqSymmetry> exon_list = new ArrayList<SeqSymmetry>();
        List exon_insert_list = new ArrayList();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodename = child.getNodeName();
            if (nodename != null) {
                if (nodename.equalsIgnoreCase("exon")) {
                    SymWithProps exSym = processExon(genomic, (Element) child);
                    exSym.setProperty("type", "exon");
                    exon_list.add(exSym);
                } else if (nodename.equalsIgnoreCase("exon_insert")) {
                    exon_insert_list.add(child);
                }
                //	else if (nodename.equalsIgnoreCase("cds")) {
                //	  processCDS(seq, (Element)child, m2gSym);
                //	}
            }
        }

        // NEED TO SORT EXON_INSERTS!!!
        //    5' TO 3' ALONG TRANSCRIPT.  OTHERWISE TRYING TO INSERT A 5' ONE
        //    AFTER A 3' ONE HAS ALREADY BEEN INSERTED WILL MESS UP COORDINATES OF 3' ONE
        // NOT YET IMPLEMENTED -- ASSUMING FOR NOW THAT EXON_INSERTS ARE ALREADY ORDERED IN THE XML

        // sorting exons, so that later position calculations are accurate

        Collections.sort(exon_list, new SeqSymStartComparator( genomic, forward));
        for (SeqSymmetry esym : exon_list) {
            m2gSym.addChild(esym);
        }

        int exoncount = m2gSym.getChildCount();
        int mrnalength = 0;
        for (int i = 0; i < exoncount; i++) {
            SeqSymmetry esym = m2gSym.getChild(i);
            SeqSpan gspan = esym.getSpan(genomic);
            mrnalength += gspan.getLength();
        }
        for (int i = 0; i < exon_insert_list.size(); i++) {
            Element iel = (Element) exon_insert_list.get(i);
            //	int istart = Integer.parseInt(iel.getAttribute("insert_at"));
            int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
            mrnalength += ilength;
        }

        start = 0;
        end = 0;
        String mrna_id = "mrna";
        BioSeq mrna = new BioSeq(mrna_id, null, mrnalength);
        mrna_hash.put(mrna_id, mrna);
        SeqSpan mrna_span = new SimpleSeqSpan(0, mrnalength, mrna);
        m2gSym.addSpan(mrna_span);

        for (int i = 0; i < exoncount; i++) {
            SimpleSymWithProps esym = (SimpleSymWithProps) m2gSym.getChild(i);
            SeqSpan gspan = esym.getSpan(genomic);
            end = start + gspan.getLength();
            Vector<Element> hit_inserts = new Vector<Element>();

            /** check each exon_insert, figure out which (if any) exons it overlaps */
            for (int insert_index = 0; insert_index < exon_insert_list.size(); insert_index++) {
                Element iel = (Element) exon_insert_list.get(insert_index);
                int istart = Integer.parseInt(iel.getAttribute("insert_at"));
                int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
                if (SeqUtils.contains(gspan, (SeqSpan) iel)) {
                    // need to add children to this exon symmetry to indicate an insertion
                    //   (or possibly deletion?) of bases in the transcript relative to the genomic
                    //	    processExonInsert(esym, istart, ilength);
                    System.err.println("insert: insertion_start = " + istart + ", length = " + ilength);
                    // remove this exon_insert from list to consider in future passes
                    //    need to also decrement the insert_index to make sure removal doesn't cause
                    //    next exon_insert to not be considered...
                    exon_insert_list.remove(insert_index);
                    hit_inserts.add(iel);
                    insert_index--;
                    end += ilength;
                }
            }

            SeqSpan tspan = new SimpleSeqSpan(start, end, mrna);
            esym.addSpan(tspan);

            if (hit_inserts.size() > 0) {
                processExonInsert((MutableSeqSymmetry) esym, hit_inserts, genomic, mrna);
            }

            start = end;
        }

        String protein_id = null;

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodename = child.getNodeName();
            if (nodename != null && nodename.equalsIgnoreCase("descriptor")) {
                Element el = (Element) child;
                String type = el.getAttribute("type");
                if (type != null && type.equalsIgnoreCase("protein_product_id")) {
                    Text tnode = (Text) el.getFirstChild();
                    protein_id = tnode.getData();
                    //          System.err.println("Retrieved:" + protein_id + ".");
                    break;
                }
            }
        }

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodename = child.getNodeName();
            if (nodename != null) {
                if (nodename.equalsIgnoreCase("cds")) {
                    processCDS(genomic, (Element) child, m2gSym, mrna, protein_id);
                }
            }
        }

        m2gSym.setID("");
        genomic.addAnnotation(m2gSym);
        mrna.addAnnotation(m2gSym);
    }

    /**
     *
     * @param   exonSym
     * @param   hit_inserts
     * @param   genomic
     * @param   mrna
     * @see     com.affymetrix.genometryImpl.MutableSeqSymmetry
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SeqSpan
     */
    private void processExonInsert(MutableSeqSymmetry exonSym, Vector<Element> hit_inserts,
            BioSeq genomic, BioSeq mrna) {
        // assumes that hit_inserts are in order 5' to 3' along transcript
        // assumes that each exon_insert in hit_inserts actually is contained in the exon
        // assumes that the genomic and transcript spans of the exon are already
        //       part of the exonSym and that the transcript span already correctly takes into account
        //       the additional bases introduced by the exon inserts

        //   map from genomic coords over to transcript coords to figure out where to "split" the
        //       exonSym into children

        SeqSpan egSpan = exonSym.getSpan(genomic);
        SeqSpan etSpan = exonSym.getSpan(mrna);

        int genStart = egSpan.getStart();
        int transStart = etSpan.getStart();

        for (int insert_index = 0; insert_index < hit_inserts.size(); insert_index++) {
            Element iel = hit_inserts.get(insert_index);
            int istart = Integer.parseInt(iel.getAttribute("insert_at"));
            int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
            int genLength = Math.abs(istart - genStart);
            int transEnd = transStart + genLength;

            // split out exon seg between last insert (or start of exon) and current insert
            //   [unless start of exon and the insert is actually at exact beginning of exon]
            if (istart != genStart) {
                MutableSeqSymmetry segSym = new SimpleMutableSeqSymmetry();
                SeqSpan gSpan = new SimpleSeqSpan(genStart, istart, genomic);  // start of insert is end of exon seg
                SeqSpan tSpan = new SimpleSeqSpan(transStart, transEnd, mrna);
                segSym.addSpan(gSpan);
                segSym.addSpan(tSpan);
                exonSym.addChild(segSym);
            }
            // now add exon seg for the current insert
            transStart = transEnd;
            transEnd += ilength;
            SeqSpan insert_tspan = new SimpleSeqSpan(transStart, transEnd, mrna);
            SeqSpan insert_gspan = new SimpleSeqSpan(istart, istart, genomic);
            MutableSeqSymmetry isegSym = new SimpleMutableSeqSymmetry();
            isegSym.addSpan(insert_tspan);
            // experimenting with adding a zero-length placeholder for exon insert relative to genomic
            isegSym.addSpan(insert_gspan);
            exonSym.addChild(isegSym);

            // set current genomic start point for next loop to location of current insert
            genStart = istart;
            transStart = transEnd;

        }

        // if last insert is not _exactly_ at end of exon, then need to add last exon seg
        //   after finished looping through inserts
        if (genStart != egSpan.getEnd()) {
            SeqSpan gSpan = new SimpleSeqSpan(genStart, egSpan.getEnd(), genomic);
            SeqSpan tSpan = new SimpleSeqSpan(transStart, etSpan.getEnd(), mrna);
            MutableSeqSymmetry endSym = new SimpleMutableSeqSymmetry();
            endSym.addSpan(gSpan);
            endSym.addSpan(tSpan);
            exonSym.addChild(endSym);
        }
    }

    /**
     *
     * @param   genomic
     * @param   elem
     * @return  SymWithProps
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.SimpleSymWithProps
     * @see     com.affymetrix.genometryImpl.SymWithProps
     */
    private SymWithProps processExon(BioSeq genomic, Element elem) {
        // should not be any nodes underneath exon tags (at least in current pseudo-DTD
        //  GAH 10-6-2001
        int start = Integer.parseInt(elem.getAttribute("start")) - 1;
        int end = Integer.parseInt(elem.getAttribute("end")) - 1;
        if (start < end) {
            end++;
        } else {
            start++;
        }

        //    System.out.println("exon:  start = " + start + "  end = " + end);
        SeqSpan span = new SimpleSeqSpan(start, end, genomic);
        SimpleSymWithProps exonsym = new SimpleSymWithProps();
        addDescriptors(elem, exonsym);
        exonsym.setProperty("start", elem.getAttribute("start"));
        exonsym.setProperty("end", elem.getAttribute("end"));
        exonsym.setProperty("length", String.valueOf(end - start + 1));
        exonsym.addSpan(span);
        return exonsym;
        //    m2gSym.addChild(exonsym);
    }

    /**
     *
     * @param   genomic
     * @param   elem
     * @param   m2gSym
     * @param   mrna
     * @param   protein_id
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SimpleSymWithProps
     * @see     com.affymetrix.genometryImpl.MutableSeqSymmetry
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.TypeContainerAnnot
     * @see     com.affymetrix.genometryImpl.util.SeqUtils
     */
    private void processCDS(BioSeq genomic, Element elem, SimpleSymWithProps m2gSym,
            BioSeq mrna, String protein_id) {

        String attr = elem.getAttribute("transstart");
        if (attr == null || attr.length() == 0) {
            attr = elem.getAttribute("start");
        }
        int start = Integer.parseInt(attr) - 1;
        /**
        because transstop currently indicates first base of stop codon (rather than
        last base of actual translation), forgoing the usual +1 addition...
         */
        //    int end = Integer.parseInt(elem.getAttribute("transstop")) + 1;
        attr = elem.getAttribute("transstop");
        if (attr == null || attr.length() == 0) {
            attr = elem.getAttribute("end");
        }
        int end = Integer.parseInt(attr) - 1;



        //    if (start < end) { end++; } else { start++; }
        if (end < start) {
            start++;
            end++;
        }

        // could just do this as a single seq span (start, end, seq), but then would end up recreating
        //   the cds segments, which will get ignored afterwards...
        SeqSpan gstart_point = new SimpleSeqSpan(start, start, genomic);
        SeqSpan gend_point = new SimpleSeqSpan(end, end, genomic);
        SimpleSymWithProps result = new SimpleSymWithProps();
        result.addSpan(gstart_point);
        SeqSymmetry[] m2gPath = new SeqSymmetry[]{m2gSym};
        SeqUtils.transformSymmetry((MutableSeqSymmetry) result, m2gPath);
        SeqSpan mstart_point = result.getSpan(mrna);

        result = new SimpleSymWithProps();

        result.addSpan(gend_point);
        SeqUtils.transformSymmetry((MutableSeqSymmetry) result, m2gPath);
        SeqSpan mend_point = result.getSpan(mrna);
        TypeContainerAnnot m2pSym = new TypeContainerAnnot(elem.getAttribute("method"));

        SeqSpan mspan = new SimpleSeqSpan(mstart_point.getStart(), mend_point.getEnd(), mrna);
        BioSeq protein = new BioSeq(protein_id, null, mspan.getLength() / 3);

        prot_hash.put(protein_id, protein);
        SeqSpan pspan = new SimpleSeqSpan(0, protein.getLength(), protein);
        if (DEBUG) {
            System.err.println("protein: length = " + pspan.getLength());
        }
        m2pSym.addSpan(mspan);
        m2pSym.addSpan(pspan);

        m2pSym.setID("");
        protein.addAnnotation(m2pSym);
        mrna.addAnnotation(m2pSym);

        // Use genometry manipulations to map cds start/end on genome to cds start/end on transcript
        //    (so that cds becomes mrna2protein symmetry on mrna (and on protein...)

    }
}
