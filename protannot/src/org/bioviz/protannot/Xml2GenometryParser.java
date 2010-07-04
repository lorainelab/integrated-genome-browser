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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Reads xml file to convert it into a Genometry.
 */

final class Xml2GenometryParser {

    private static final boolean DEBUG = false;
    private Map<String,BioSeq> mrna_hash;
    private Map<String,BioSeq> prot_hash;
    // instance variables needed during the parse
    private List<int[]> transCheckExons;	// used to sanity-check exon translation
	private static final String end_codon = "Z";

	/**
	 * Create a new BioSeq and add annotations to it.
	 * @param doc
	 * @return
	 * @throws Exception
	 */
    BioSeq parse(Document doc) throws Exception{
		mrna_hash = new HashMap<String,BioSeq>();
		prot_hash = new HashMap<String,BioSeq>();

        try {
            BioSeq ret_genomic = processDocument(doc);

			return ret_genomic;

        } catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
        }
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
       Element top_element = seqdoc.getDocumentElement();
		String name = top_element.getTagName();
		if (!name.equalsIgnoreCase("dnaseq")) {
			return null;
		}
		if (DEBUG) {
			System.err.println("processing dna seq");
		}
		String version = "";
		try {
			version = top_element.getAttribute("version");
		} catch (Exception e) {
			// ignore exception
		}
		String seq = "genome";
		try {
			seq = top_element.getAttribute("seq");
		} catch (Exception e) {
			// ignore exception
		}

		BioSeq chrom = buildChromosome(top_element, seq, version);

		processDNASeq(chrom, top_element);

		return chrom;
    }

    private static BioSeq buildChromosome(Element top_element, String seq, String version)
            throws DOMException {
        BioSeq chrom = null;
        NodeList children = top_element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String cname = child.getNodeName();
            if (cname != null && cname.equalsIgnoreCase("residues")) {
                Text resnode = (Text) child.getFirstChild();
                String residues = resnode.getData();
                chrom = new BioSeq(seq, version, residues.length());
                chrom.setResidues(residues);
            }
        }
        return chrom;
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
                    }
                }
            }
        }

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (name != null && name.equalsIgnoreCase("aaseq")) {
                processProtein(prot_hash, (Element) child);
            }
        }
    }

    /**
     Process protein in BioSeq for each child node of element provided.
     * @param   elem        Node for which protein is to be processed
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private static void processProtein(Map<String,BioSeq> prot_hash, Element elem) {
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
    private static void processSimSearch(BioSeq query_seq, Element elem) {
        NodeList children = elem.getChildNodes();
        String method = elem.getAttribute("method");
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (name != null && name.equalsIgnoreCase("simhit")) {
                processSimHit(query_seq, (Element) child, method);
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
    private static void processSimHit(BioSeq query_seq, Element elem, String method) {
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
                        hitSpan = new SimpleMutableSeqSpan(spanSpan.getMin(), spanSpan.getMax(), query_seq); // Doubtful
                    } else {
                        SeqUtils.encompass(hitSpan, spanSpan, (MutableSeqSpan) hitSpan);
                    }
                    hitSym.setProperty(ProtAnnotMain.TYPESTR, "hitspan");
                    num_spans++;
                }
            }
        }
        String prop =  (Integer.valueOf(num_spans)).toString();
        hitSym.setProperty("num_spans", prop);
        hitSym.setProperty(ProtAnnotMain.TYPESTR, "hit");
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
    private static void addDescriptors(Element elem, SimpleSymWithProps sym) {

        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element chelem = (Element) child;
                if (name.equalsIgnoreCase("descriptor")) {
                    String desc_name = chelem.getAttribute(ProtAnnotMain.TYPESTR);
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
    private static SeqSymmetry processSimSpan(BioSeq query_seq, Element elem) {
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
        String prop = (Integer.valueOf(start + 1)).toString();
        spanSym.setProperty("aa_start", prop);
        prop = (Integer.valueOf(end + 1)).toString();
        spanSym.setProperty("aa_end", prop);
        prop = (Integer.valueOf(end - start + 1)).toString();
        spanSym.setProperty("aa_length", prop);
        SeqSpan qspan = new SimpleSeqSpan(start+query_seq.getMin(), end+query_seq.getMin(), query_seq); // Doubtful
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

        if (DEBUG) {
			int start = Integer.parseInt(elem.getAttribute(ProtAnnotMain.STARTSTR));
			int end = Integer.parseInt(elem.getAttribute(ProtAnnotMain.ENDSTR));
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
        if (DEBUG) {
			int start = Integer.parseInt(elem.getAttribute(ProtAnnotMain.STARTSTR));
			int end = Integer.parseInt(elem.getAttribute(ProtAnnotMain.ENDSTR));
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
        int start = Integer.parseInt(elem.getAttribute(ProtAnnotMain.STARTSTR));
        int end = Integer.parseInt(elem.getAttribute(ProtAnnotMain.ENDSTR));

        if (DEBUG) {
            System.err.println("mrna:  start = " + start + "  end = " + end);
        }
        NodeList children = elem.getChildNodes();
        SeqSpan span = new SimpleSeqSpan(start, end, genomic);

        TypeContainerAnnot m2gSym = new TypeContainerAnnot(elem.getAttribute("method"));
        m2gSym.addSpan(span);
        addDescriptors(elem, m2gSym);
        m2gSym.setProperty(ProtAnnotMain.TYPESTR, "mRNA");
        boolean forward = (span.isForward());


		transCheckExons = new ArrayList<int[]>();
        List<SeqSymmetry> exon_list = new ArrayList<SeqSymmetry>();
        List<Node> exon_insert_list = new ArrayList<Node>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodename = child.getNodeName();
            if (nodename != null) {
                if (nodename.equalsIgnoreCase("exon")) {
                    SymWithProps exSym = processExon(genomic, (Element) child);
                    exSym.setProperty(ProtAnnotMain.TYPESTR, "exon");
                    exon_list.add(exSym);
                } else if (nodename.equalsIgnoreCase("exon_insert")) {
                    exon_insert_list.add(child);
                }
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

		BioSeq mrna = addSpans(m2gSym, genomic, exon_insert_list, start);
		
		String protein_id = determineProteinID(children);

		String amino_acid = getAminoAcid(m2gSym);
		processCDS(children, genomic, m2gSym, mrna, protein_id, amino_acid);

        m2gSym.setID("");
        genomic.addAnnotation(m2gSym);
        mrna.addAnnotation(m2gSym);
    }

	private String getAminoAcid(TypeContainerAnnot m2gSym){
		String residue = (String) m2gSym.getProperty("protein sequence");

		if(residue == null)
			return "";
		else
			residue += end_codon;

		return residue;
	}

	private static String determineProteinID(NodeList children) throws DOMException {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			String nodename = child.getNodeName();
			if (nodename != null && nodename.equalsIgnoreCase("descriptor")) {
				Element el = (Element) child;
				String type = el.getAttribute(ProtAnnotMain.TYPESTR);
				if (type != null && type.equalsIgnoreCase("protein_product_id")) {
					Text tnode = (Text) el.getFirstChild();
					return tnode.getData();
				}
			}
		}
		return null;
	}


	private BioSeq addSpans(TypeContainerAnnot m2gSym, BioSeq genomic, List exon_insert_list, int start)
			throws NumberFormatException {
		int exoncount = m2gSym.getChildCount();
        int mrnalength = 0;
        for (int i = 0; i < exoncount; i++) {
            SeqSymmetry esym = m2gSym.getChild(i);
            SeqSpan gspan = esym.getSpan(genomic);
            mrnalength += gspan.getLength();
        }
        for (int i = 0; i < exon_insert_list.size(); i++) {
            Element iel = (Element) exon_insert_list.get(i);
            int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
            mrnalength += ilength;
        }

		int end = 0;
		String mrna_id = "mrna";
		BioSeq mrna = new BioSeq(mrna_id, null, mrnalength);
		mrna.setBounds(start, start+mrnalength);
		mrna_hash.put(mrna_id, mrna);
		SeqSpan mrna_span = new SimpleSeqSpan(mrna.getMin(), mrna.getMax(), mrna); //Corrected
		m2gSym.addSpan(mrna_span);
		for (int i = 0; i < exoncount; i++) {
			SimpleSymWithProps esym = (SimpleSymWithProps) m2gSym.getChild(i);
			SeqSpan gspan = esym.getSpan(genomic);
			end = start + gspan.getLength();
			List<Element> hit_inserts = new ArrayList<Element>();
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
			if (!hit_inserts.isEmpty()) {
				processExonInsert((MutableSeqSymmetry) esym, hit_inserts, genomic, mrna);
			}
			start = end;
		}
		return mrna;
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
    private static void processExonInsert(MutableSeqSymmetry exonSym, List<Element> hit_inserts,
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
        int start = Integer.parseInt(elem.getAttribute(ProtAnnotMain.STARTSTR));
        int end = Integer.parseInt(elem.getAttribute(ProtAnnotMain.ENDSTR));

        transCheckExons.add(new int[]{start,end});

        SeqSpan span = new SimpleSeqSpan(start, end, genomic);
        SimpleSymWithProps exonsym = new SimpleSymWithProps();
        addDescriptors(elem, exonsym);
        exonsym.setProperty(ProtAnnotMain.STARTSTR, elem.getAttribute(ProtAnnotMain.STARTSTR));
        exonsym.setProperty(ProtAnnotMain.ENDSTR, elem.getAttribute(ProtAnnotMain.ENDSTR));
		exonsym.setProperty("length", String.valueOf(end - start));
        exonsym.addSpan(span);
        return exonsym;
    }

	private void processCDS(NodeList children, BioSeq genomic, TypeContainerAnnot m2gSym, BioSeq mrna, String protein_id, String amino_acid) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			String nodename = child.getNodeName();
			if (nodename != null && nodename.equalsIgnoreCase("cds")) {
					processCDS(genomic, (Element) child, m2gSym, mrna, protein_id, amino_acid);
			}
		}
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
            BioSeq mrna, String protein_id, String amino_acid) {

        String attr = elem.getAttribute("transstart");
        if (attr == null || attr.length() == 0) {
            attr = elem.getAttribute("start");
        }
        int start = Integer.parseInt(attr);

		// transstop indicates last base of actual translation
        attr = elem.getAttribute("transstop");
        if (attr == null || attr.length() == 0) {
            attr = elem.getAttribute(ProtAnnotMain.ENDSTR);
        }
        int end = Integer.parseInt(attr);

        checkTranslationLength(start,end);


        // could just do this as a single seq span (start, end, seq), but then would end up recreating
        //   the cds segments, which will get ignored afterwards...
        SeqSpan gstart_point = new SimpleSeqSpan(start, start, genomic);
        SeqSpan gend_point = new SimpleSeqSpan(end, end, genomic);
        SimpleSymWithProps result = new SimpleSymWithProps();
        result.addSpan(gstart_point);
        SeqSymmetry[] m2gPath = new SeqSymmetry[]{m2gSym};
        SeqUtils.transformSymmetry((MutableSeqSymmetry) result, m2gPath);
        SeqSpan mstart_point = result.getSpan(mrna);

		if(mstart_point == null) {
			throw new NullPointerException("Conflict with start and end in processCDS.");
		}

        result = new SimpleSymWithProps();

        result.addSpan(gend_point);
        SeqUtils.transformSymmetry((MutableSeqSymmetry) result, m2gPath);
        SeqSpan mend_point = result.getSpan(mrna);

		if(mend_point == null) {
			throw new NullPointerException("Conflict with start and end in processCDS.");
		}

        TypeContainerAnnot m2pSym = new TypeContainerAnnot(elem.getAttribute("method"));

        SeqSpan mspan = new SimpleSeqSpan(mstart_point.getStart(), mend_point.getEnd(), mrna);
        BioSeq protein = new BioSeq(protein_id, null, mspan.getLength() / 3);
		protein.setResidues(amino_acid);
		protein.setBounds(mspan.getMin(), mspan.getMin() + mspan.getLength()/3); // Corrected

        prot_hash.put(protein_id, protein);
        SeqSpan pspan = new SimpleSeqSpan(protein.getMin(), protein.getMax(), protein); //Corrected
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

	private void checkTranslationLength(int start ,int end){

        int length = 0;
        for(int[] exon : transCheckExons){
            int exon_start = exon[0];
            int exon_end = exon[1];

			//int old_length = length;
            if(exon_start >= start && exon_end <= end){
				// exon completely in translated region
                length += exon_end - exon_start;
            } else if(exon_start <= start && exon_end >= start){
				// translation start is past beginning of exon
                length += exon_end - start;
            } else if(exon_start <= end && exon_end >= end){
				// translation end is before ending of exon
                length += end - exon_start;
            }
			//System.out.println(",added length: " + (length - old_length));
        }

        if(length % 3 != 0)
            System.out.println("WARNING:  Translation length is " + length + " and remainder modulo 3 is " + length % 3);
    }
}
