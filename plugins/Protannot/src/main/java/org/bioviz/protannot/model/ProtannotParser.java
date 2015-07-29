/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.model;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.MutableSeqSpan;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SupportsCdsSpan;
import com.affymetrix.genometry.comparator.SeqSymStartComparator;
import com.affymetrix.genometry.span.MutableDoubleSeqSpan;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.BasicSeqSymmetry;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.SupportsGeneName;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometry.symmetry.impl.UcscBedDetailSym;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.util.DNAUtils;
import com.google.common.base.Strings;
import com.lorainelab.igb.genoviz.extensions.SeqMapViewI;
import com.lorainelab.igb.services.IgbService;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.bioviz.protannot.NormalizeXmlStrand;
import org.bioviz.protannot.model.Dnaseq.Aaseq.Simsearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Tarun
 */
@Component(provide = ProtannotParser.class)
public class ProtannotParser {

    private static final Logger logger = LoggerFactory.getLogger(ProtannotParser.class);
    private JAXBContext jaxbContext;
    private Unmarshaller jaxbUnmarshaller;
    private Marshaller jaxbMarshaller;
    private static final String end_codon = "Z";
    private Map<String, BioSeq> mrna_hash;
    private Map<String, BioSeq> prot_hash;

    public static final String STARTSTR = "start";
    public static final String ENDSTR = "end";
    public static final String TYPESTR = "type";
    public static final String NAMESTR = "name";
    public static final String EXONSTR = "exon";
    public static final String IDSTR = "id";
    public static final String RESIDUESSTR = "residues";
    public static final String MRNASTR = "mrna";
    public static final String STRANDSTR = "strand";
    public static final String CDSSTR = "cds";
    public static final String METHODSTR = "method";
    public static final String AA_START = "aa_start";
    public static final String AA_END = "aa_end";
    public static final String AA_LENGTH = "aa_length";
    // private Dnaseq dnaseq;
    private IgbService igbService;
    private int padding;
    private final int MIN_PADDING = 150;

    public ProtannotParser() {
        try {
            jaxbContext = JAXBContext.newInstance(Dnaseq.class);
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            jaxbMarshaller = jaxbContext.createMarshaller();
        } catch (JAXBException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public Dnaseq parse(InputStream inputStream) throws JAXBException {

        mrna_hash = new HashMap<>();
        prot_hash = new HashMap<>();

        Dnaseq dnaseq = (Dnaseq) jaxbUnmarshaller.unmarshal(inputStream);
        return dnaseq;
    }

    public BioSeq parse(Dnaseq dnaseq) {

        mrna_hash = new HashMap<>();
        prot_hash = new HashMap<>();
        NormalizeXmlStrand.normalizeDnaseq(dnaseq);
        BioSeq chromosome = buildChromosome(dnaseq);
        GenomeVersion gv = new GenomeVersion(dnaseq.getVersion());
        chromosome.setGenomeVersion(gv);
        processDNASeq(chromosome, dnaseq);
        return chromosome;
    }

    public BioSeq parse(SeqMapViewI seqMapView, Dnaseq dnaseq) {
        mrna_hash = new HashMap<>();
        prot_hash = new HashMap<>();
        List<SeqSymmetry> selectedSyms = seqMapView.getSelectedSyms();
        BioSeq bioseq = seqMapView.getViewSeq();
        String seqId = bioseq.getId();
        if (!seqId.startsWith("chr")) {
            seqId = "chr" + seqId;
        }
        MutableSeqSymmetry mutableSeqSymmetry = new SimpleMutableSeqSymmetry();
        int spanStart, spanEnd;
        for (SeqSymmetry sym : selectedSyms) {
            Dnaseq.MRNA mrna = new Dnaseq.MRNA();

            //populating exons to mrna
            for (int i = 0; i < sym.getChildCount(); i++) {
                SeqSymmetry exonSym = sym.getChild(i);
                Dnaseq.MRNA.Exon exon = new Dnaseq.MRNA.Exon();
                exon.setStart(BigInteger.valueOf(exonSym.getSpan(bioseq).getStart()));
                exon.setEnd(BigInteger.valueOf(exonSym.getSpan(bioseq).getEnd()));
                mrna.getExon().add(exon);
            }
            if (sym instanceof SupportsCdsSpan) {
                SeqSpan cdsSpan = ((SupportsCdsSpan) sym).getCdsSpan();
                Dnaseq.MRNA.Cds cds = new Dnaseq.MRNA.Cds();
                if (cds == null) {
                    continue;
                }
                cds.setStart(BigInteger.valueOf(cdsSpan.getStart()));
                cds.setEnd(BigInteger.valueOf(cdsSpan.getEnd()));
                mrna.setCds(cds);
            }
            if (checkForward(sym)) {
                mrna.setStart(BigInteger.valueOf(sym.getSpan(bioseq).getStart()));
                mrna.setEnd(BigInteger.valueOf(sym.getSpan(bioseq).getEnd()));
            } else {
                mrna.setStart(BigInteger.valueOf(sym.getSpan(bioseq).getEnd()));
                mrna.setEnd(BigInteger.valueOf(sym.getSpan(bioseq).getStart()));
            }
            checkTranslationLength(mrna);

            mrna.setLocation(seqId + ":" + mrna.getStart().intValue() + "-" + mrna.getEnd().intValue());
            dnaseq.getMRNAAndAaseq().add(mrna);

            addDescriptorsToMrna(sym, mrna);

        }
        if (checkForward(selectedSyms)) {
            spanStart = selectedSyms.stream().mapToInt(sym -> sym.getSpan(bioseq).getStart()).min().orElse(0);
            spanEnd = selectedSyms.stream().mapToInt(sym -> sym.getSpan(bioseq).getEnd()).max().orElse(0);
        } else {
            spanStart = selectedSyms.stream().mapToInt(sym -> sym.getSpan(bioseq).getStart()).max().orElse(0);
            spanEnd = selectedSyms.stream().mapToInt(sym -> sym.getSpan(bioseq).getEnd()).min().orElse(0);
        }

        SimpleSeqSpan residueSpan;
        computPadding(spanStart - spanEnd);
        if (checkForward(selectedSyms)) {
            if (padding > spanStart) {
                spanStart = padding;
            }
            residueSpan = new SimpleSeqSpan(spanStart - padding, spanEnd + padding, bioseq);
        } else {
            if (padding > spanEnd) {
                spanEnd = padding;
            }
            residueSpan = new SimpleSeqSpan(spanStart + padding, spanEnd - padding, bioseq);
        }
        mutableSeqSymmetry.addSpan(residueSpan);
        dnaseq.setSeq(seqId);
        dnaseq.setVersion(bioseq.getGenomeVersion().getUniqueID());
        igbService.loadResidues(mutableSeqSymmetry.getSpan(bioseq), true);

        String residuesStr = SeqUtils.getResidues(mutableSeqSymmetry, bioseq);
        Dnaseq.Residues residue = new Dnaseq.Residues();
        residue.setValue(residuesStr.toLowerCase());

        if (checkForward(selectedSyms)) {
            residue.setStart(BigInteger.valueOf(residueSpan.getStart()));
            residue.setEnd(BigInteger.valueOf(residueSpan.getEnd()));
        } else {
            residue.setStart(BigInteger.valueOf(residueSpan.getEnd()));
            residue.setEnd(BigInteger.valueOf(residueSpan.getStart()));
        }
        dnaseq.setResidues(residue);
        dnaseq.setLocation(seqId + ":" + spanStart + "-" + spanEnd);

        addProteinSequenceToMrnas(dnaseq, bioseq);
        dnaseq.setVersion(bioseq.getId());

//        if (true) {
//            try {
//                jaxbMarshaller.marshal(dnaseq, new File("sample_dnaseq.xml"));
//            } catch (JAXBException ex) {
//                logger.error(ex.getMessage(), ex);
//            }
//        }
        NormalizeXmlStrand.normalizeDnaseq(dnaseq);
        BioSeq chromosome = buildChromosome(dnaseq);
        chromosome.setGenomeVersion(bioseq.getGenomeVersion());
        processDNASeq(chromosome, dnaseq);
        return chromosome;

    }

    private void computPadding(int residueLength) {
        if (residueLength < 0) {
            residueLength *= -1;
        }
        int paddingFactor = residueLength / 5000;
        if (paddingFactor <= 0) {
            paddingFactor = 1;
        }
        padding = paddingFactor * MIN_PADDING;
    }

    private void addDescriptorsToMrna(SeqSymmetry sym, Dnaseq.MRNA mrna) {
        mrna.addDescriptor("protein_product_id", sym.getID());

        if (sym instanceof SupportsGeneName) {
            mrna.addDescriptor("title", ((SupportsGeneName) sym).getGeneName());
        }

        if (sym instanceof BasicSeqSymmetry) {
            mrna.addDescriptor("mRNA accession", ((BasicSeqSymmetry) sym).getID());
            mrna.addDescriptor("URL", "http://www.google.com/search?q=" + ((BasicSeqSymmetry) sym).getID());

            if (checkForward(sym)) {
                mrna.setStrand("+");
            } else {
                mrna.setStrand("-");
            }
        }

        if (sym instanceof UcscBedDetailSym) {
            mrna.addDescriptor("description", ((UcscBedDetailSym) sym).getDescription());
        }

        mrna.addDescriptor("genome name", igbService.getSeqMapView().getViewSeq().getGenomeVersion().getName());
    }

    private boolean checkForward(SeqSymmetry sym) {
        if (sym instanceof BasicSeqSymmetry) {
            return ((BasicSeqSymmetry) sym).isForward();
        }
        return false;
    }

    private boolean checkForward(List<SeqSymmetry> selectedSyms) {
        if (selectedSyms.isEmpty()) {
            return false;
        }
        SeqSymmetry sym = selectedSyms.get(0);
        return checkForward(sym);
    }

    private boolean checkForward(Dnaseq.MRNA mrna) {
        return "+".equals(mrna.getStrand());
    }

    private void transformCdsForNegativeStrand(Dnaseq.MRNA.Cds cds) {
        int start = cds.getStart().intValue();
        int end = cds.getEnd().intValue();
        cds.setStart(BigInteger.valueOf(end));
        cds.setEnd(BigInteger.valueOf(start));
    }

    private void transformExonForNegativeStrand(Dnaseq.MRNA.Exon exon) {
        int start = exon.getStart().intValue();
        int end = exon.getEnd().intValue();
        exon.setStart(BigInteger.valueOf(end));
        exon.setEnd(BigInteger.valueOf(start));
    }

    public void addProteinSequenceToMrnas(Dnaseq dnaseq, BioSeq bioseq) {
        for (int i = 0; i < dnaseq.getMRNAAndAaseq().size(); i++) {
            Object seq = dnaseq.getMRNAAndAaseq().get(i);
            if (seq instanceof Dnaseq.MRNA) {
                Dnaseq.MRNA mrna = (Dnaseq.MRNA) seq;
                MutableSeqSymmetry mutableSeqSymmetry;
                if (!checkForward(mrna)) {
                    transformCdsForNegativeStrand(mrna.getCds());
                }
                int cdsStart = mrna.getCds().getStart().intValue();
                int cdsEnd = mrna.getCds().getEnd().intValue();

                StringBuilder allExonsResidue = new StringBuilder();
                for (int j = 0; j < mrna.getExon().size(); j++) {
                    Dnaseq.MRNA.Exon exon = mrna.getExon().get(j);
                    mutableSeqSymmetry = new SimpleMutableSeqSymmetry();
                    if (!checkForward(mrna)) {
                        transformExonForNegativeStrand(exon);
                    }
                    int exonStart = exon.getStart().intValue();
                    int exonEnd = exon.getEnd().intValue();
                    if (exonEnd < cdsStart || exonStart > cdsEnd) {
                        continue;
                    }
                    int translationSpanStart = 0;
                    int translationSpanEnd = 0;
                    translationSpanStart = getTranslationStartPoint(exonStart, cdsStart, exonEnd);
                    translationSpanEnd = getTranslationEndPoint(exonStart, cdsEnd, exonEnd);

                    mutableSeqSymmetry.addSpan(new SimpleSeqSpan(translationSpanStart, translationSpanEnd, bioseq));
                    final String exonResidue = SeqUtils.getResidues(mutableSeqSymmetry, bioseq);
                    allExonsResidue.append(exonResidue);
                    exon.addDescriptor("genomic sequence", exonResidue);
                }

                if (!checkForward(mrna)) {
                    allExonsResidue = new StringBuilder(DNAUtils.getReverseComplement(allExonsResidue));
                }
                String mrnaProtein = DNAUtils.translate(allExonsResidue.toString(), cdsStart % 3, DNAUtils.ONE_LETTER_CODE);
                mrna.addDescriptor("protein sequence", mrnaProtein);
                mrna.addDescriptor("mRNA coding sequence", allExonsResidue.toString());
            }
        }

    }

    private int getTranslationStartPoint(int exonStart, int cdsStart, int exonEnd) {
        int spanStart;
        if (exonStart < cdsStart && exonEnd > cdsStart) {
            spanStart = cdsStart;
        } else {
            spanStart = exonStart;
        }
        return spanStart;
    }

    private int getTranslationEndPoint(int exonStart, int cdsEnd, int exonEnd) {
        int spanEnd;
        if (exonStart < cdsEnd && exonEnd > cdsEnd) {
            spanEnd = cdsEnd;
        } else {
            spanEnd = exonEnd;
        }
        return spanEnd;
    }

    private BioSeq buildChromosome(Dnaseq dnaseq) {
        String seq = dnaseq.getSeq();

        BioSeq chromosome = null;
        if (dnaseq.getResidues() != null) {
            String residue = dnaseq.getResidues().getValue();
            chromosome = new BioSeq(seq, residue.length());
            chromosome.setResidues(residue);
        }
        return chromosome;
    }

    private void processDNASeq(BioSeq chromosome, Dnaseq dnaseq) {
        List<Object> mrnaAndAaseq = dnaseq.getMRNAAndAaseq();
        for (Object obj : mrnaAndAaseq) {
            if (obj != null && obj instanceof Dnaseq.MRNA) {
                processMRNA(chromosome, (Dnaseq.MRNA) obj);
            } else if (obj instanceof Dnaseq.Aaseq) {
                processProtein(prot_hash, (Dnaseq.Aaseq) obj);
            }
        }
    }

    /**
     * Process protein in BioSeq for each child node of element provided.
     *
     * @param elem Node for which protein is to be processed
     * @see com.affymetrix.genometryImpl.BioSeq
     */
    private static void processProtein(Map<String, BioSeq> prot_hash, Dnaseq.Aaseq aaseq) {
        String pid = aaseq.getId();
        BioSeq protein = prot_hash.get(pid);
        if (protein == null) {
            logger.error("Error: no bioseq matching id: " + pid
                    + ". Skipping it.");
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("aaseq: id = " + pid + ",  " + protein);
        }

        aaseq.getSimsearch().stream().forEach((simSearch) -> {
            processSimSearch(protein, simSearch);
        });
    }

    private static void processSimSearch(BioSeq query_seq, Simsearch simSearch) {
        String method = simSearch.getMethod();
        simSearch.getSimhit().stream().forEach((simhit) -> {
            processSimHit(query_seq, simhit, method);
        });
    }

    private static void processSimHit(BioSeq query_seq, Simsearch.Simhit simhit, String method) {
        // method can never be null -- if it is, the XML is wrong
        TypeContainerAnnot hitSym = new TypeContainerAnnot(method);
        addDescriptors(simhit.getDescriptor(), hitSym);

        SeqSpan hitSpan = null;
        int num_spans = 0, aa_start = Integer.MAX_VALUE, aa_end = Integer.MIN_VALUE;
        for (Simsearch.Simhit.Simspan simspan : simhit.getSimspan()) {
            SeqSymmetry spanSym = processSimSpan(query_seq, simspan);
            ((SymWithProps) spanSym).setProperty(METHODSTR, method);
            hitSym.addChild(spanSym);
            SeqSpan spanSpan = spanSym.getSpan(query_seq);
            if (hitSpan == null) {
                hitSpan = new SimpleMutableSeqSpan(spanSpan.getMin(), spanSpan.getMax(), query_seq);
            } else {
                SeqUtils.encompass(hitSpan, spanSpan, (MutableSeqSpan) hitSpan);
            }
            //hitSym.setProperty(TYPESTR, "hitspan");
            int start = Integer.valueOf(((SymWithProps) spanSym).getProperty(AA_START).toString());
            int end = Integer.valueOf(((SymWithProps) spanSym).getProperty(AA_END).toString());
            aa_start = Math.min(aa_start, start);
            aa_end = Math.max(aa_end, end);
            num_spans++;
        }
        String prop = (Integer.valueOf(num_spans)).toString();
        hitSym.setProperty("num_spans", prop);
        hitSym.setProperty(TYPESTR, "simHit");
        hitSym.setProperty(AA_START, String.valueOf(aa_start));
        hitSym.setProperty(AA_END, String.valueOf(aa_end));
        hitSym.setProperty(AA_LENGTH, String.valueOf(aa_end - aa_start));
        hitSym.addSpan(hitSpan);
        hitSym.setID("");
        query_seq.addAnnotation(hitSym);
    }

    private static SeqSymmetry processSimSpan(BioSeq query_seq, Simsearch.Simhit.Simspan simspan) {
        int start = simspan.getQueryStart().intValue();
        int end = simspan.getQueryEnd().intValue();

        SimpleSymWithProps spanSym = new SimpleSymWithProps();
        addDescriptors(simspan.getDescriptor(), spanSym);
        String prop = (Integer.valueOf(start)).toString();
        spanSym.setProperty(AA_START, prop);
        prop = (Integer.valueOf(end)).toString();
        spanSym.setProperty(AA_END, prop);
        prop = (Integer.valueOf(end - start)).toString();
        spanSym.setProperty(AA_LENGTH, prop);
        //Multiplying start and end by 3. Because three letters forms one amino acid.
        SeqSpan qspan = new SimpleSeqSpan((start * 3) + query_seq.getMin(), (end * 3) + query_seq.getMin(), query_seq);
        spanSym.addSpan(qspan);
        return spanSym;
    }

    private List<int[]> transCheckExons;

    private void processMRNA(BioSeq chromosome, Dnaseq.MRNA mrna) {
        int start = mrna.getStart().intValue();
        int end = mrna.getEnd().intValue();

        logger.debug("mrna:  start = " + start + "  end = " + end);
        SeqSpan span = new SimpleSeqSpan(start, end, chromosome);

        TypeContainerAnnot m2gSym = new TypeContainerAnnot("");
        m2gSym.addSpan(span);
        addDescriptors(mrna.getDescriptor(), m2gSym);
        m2gSym.setProperty(TYPESTR, "mRNA");
        boolean forward = (span.isForward());

        transCheckExons = new ArrayList<>();
        List<SeqSymmetry> exon_list = new ArrayList<>();
        List<Node> exon_insert_list = new ArrayList<>();
        List<Dnaseq.MRNA.Exon> exons = mrna.getExon();
        for (Dnaseq.MRNA.Exon exon : exons) {
            SymWithProps exSym = processExon(chromosome, exon);
            exSym.setProperty(TYPESTR, EXONSTR);
            exon_list.add(exSym);
        }

        Collections.sort(exon_list, new SeqSymStartComparator(chromosome, forward));
        for (SeqSymmetry esym : exon_list) {
            m2gSym.addChild(esym);
        }

        BioSeq mrnaChromosome = addSpans(m2gSym, chromosome, exon_insert_list, start);

        String proteinId = determineProteinID(mrna.getDescriptor());

        String amino_acid = getAminoAcid(m2gSym);

        processCDS(chromosome, mrna.getCds(), m2gSym, mrnaChromosome, proteinId, amino_acid);

        m2gSym.setID("");
        chromosome.addAnnotation(m2gSym);
        mrnaChromosome.addAnnotation(m2gSym);
    }

    /**
     *
     * @param chromosome
     * @param elem
     * @param m2gSym
     * @param mrnaChromosome
     * @param proteinId
     * @see com.affymetrix.genometryImpl.BioSeq
     * @see com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps
     * @see com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry
     * @see com.affymetrix.genometryImpl.SeqSpan
     * @see com.affymetrix.genometryImpl.symmetry.SeqSymmetry
     * @see com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot
     * @see com.affymetrix.genometryImpl.util.SeqUtils
     */
    private void processCDS(BioSeq chromosome, Dnaseq.MRNA.Cds cds, SimpleSymWithProps m2gSym,
            BioSeq mrnaChromosome, String proteinId, String aminoAcid) {

        int cdsStart;
        if (cds.getTransstart() != null) {
            cdsStart = cds.getTransstart().intValue();
        } else {
            cdsStart = cds.getStart().intValue();
        }

        // transstop indicates last base of actual translation
        int cdsEnd;
        if (cds.getTransstop() != null) {
            cdsEnd = cds.getTransstop().intValue();
        } else {
            cdsEnd = cds.getEnd().intValue();
        }

        // could just do this as a single seq span (start, end, seq), but then would end up recreating
        //   the cds segments, which will get ignored afterwards...
        SeqSpan gstart_point = new SimpleSeqSpan(cdsStart, cdsStart, chromosome);
        SeqSpan gend_point = new SimpleSeqSpan(cdsEnd, cdsEnd, chromosome);
        SimpleSymWithProps result = new SimpleSymWithProps();
        result.addSpan(gstart_point);
        SeqSymmetry[] m2gPath = new SeqSymmetry[]{m2gSym};
        SeqUtils.transformSymmetry((MutableSeqSymmetry) result, m2gPath);
        SeqSpan mstart_point = result.getSpan(mrnaChromosome);

        if (mstart_point == null) {
            mstart_point = new MutableDoubleSeqSpan(cdsStart, cdsStart, mrnaChromosome);
            //throw new NullPointerException("Conflict with start and end in processCDS.");
        }

        result = new SimpleSymWithProps();

        result.addSpan(gend_point);
        SeqUtils.transformSymmetry((MutableSeqSymmetry) result, m2gPath);
        SeqSpan mend_point = result.getSpan(mrnaChromosome);

        if (mend_point == null) {
            int total = mstart_point.getStart();
            for (int i = 0; i < m2gSym.getChildCount(); i++) {
                SeqSymmetry child = m2gSym.getChild(i);
                int exonStart = child.getSpan(chromosome).getStart();
                int exonEnd = child.getSpan(chromosome).getEnd();
                if (exonStart > cdsEnd || exonEnd < cdsStart) {
                    continue;
                }
                int translationStart = getTranslationStartPoint(exonStart, cdsStart, exonEnd);
                int translationEnd = getTranslationEndPoint(exonStart, cdsEnd, exonEnd);
                int length = translationEnd - translationStart;
                total += length;
            }
            mend_point = new MutableDoubleSeqSpan(total, total, mrnaChromosome);
            //throw new NullPointerException("Conflict with start and end in processCDS.");
        }
        // because CDS has no method attribute in any example files.
        TypeContainerAnnot m2pSym = new TypeContainerAnnot("");

        SeqSpan mspan = new SimpleSeqSpan(mstart_point.getStart(), mend_point.getEnd(), mrnaChromosome);
        BioSeq protein = new BioSeq(proteinId, mspan.getLength());
        protein.setResidues(processAminoAcid(aminoAcid));
        protein.setBounds(mspan.getMin(), mspan.getMin() + mspan.getLength());

        prot_hash.put(proteinId, protein);
        SeqSpan pspan = new SimpleSeqSpan(protein.getMin(), protein.getMax(), protein);
        if (logger.isDebugEnabled()) {
            logger.debug("protein: length = " + pspan.getLength());
        }
        m2pSym.addSpan(mspan);
        m2pSym.addSpan(pspan);

        m2pSym.setID("");
        protein.addAnnotation(m2pSym);
        mrnaChromosome.addAnnotation(m2pSym);

        // Use genometry manipulations to map cds start/end on genome to cds start/end on transcript
        //    (so that cds becomes mrna2protein symmetry on mrna (and on protein...)
    }

    private static String processAminoAcid(String residue) {
        if (residue.isEmpty()) {
            return residue;
        }

        char[] amino_acid = new char[residue.length() * 3];
        for (int i = 0; i < amino_acid.length; i++) {
            if (i % 3 == 0) {
                amino_acid[i] = residue.charAt(i / 3);
            } else {
                amino_acid[i] = ' ';
            }
        }
        return String.valueOf(amino_acid);
    }

    private void checkTranslationLength(Dnaseq.MRNA mrna) {
        int length = 0;
        int cdsStart = mrna.getCds().getStart().intValue();
        int cdsEnd = mrna.getCds().getEnd().intValue();
        for (Dnaseq.MRNA.Exon exon : mrna.getExon()) {
            if (exon.getEnd().intValue() < cdsStart || exon.getStart().intValue() > cdsEnd) {
                continue;
            }
            int translationStart = getTranslationStartPoint(exon.getStart().intValue(), cdsStart, exon.getEnd().intValue());
            int translationEnd = getTranslationEndPoint(exon.getStart().intValue(), cdsEnd, exon.getEnd().intValue());
            length += Math.abs(translationEnd - translationStart);
        }

        if (length % 3 != 0) {
            logger.warn("WARNING:  Translation length is " + length + " and remainder modulo 3 is " + length % 3);
        }

    }

    private static String getAminoAcid(TypeContainerAnnot m2gSym) {
        String residue = (String) m2gSym.getProperty("protein sequence");

        if (residue == null) {
            return "";
        } else {
            //residue += end_codon;
        }

        return residue;
    }

    private static String determineProteinID(List<Dnaseq.Descriptor> descriptors) throws DOMException {
        for (Dnaseq.Descriptor descriptor : descriptors) {
            String type = descriptor.getType();
            if (type != null && type.equalsIgnoreCase("protein_product_id")) {
                return descriptor.getValue();
            }
        }
        return null;
    }

    private SymWithProps processExon(BioSeq genomic, Dnaseq.MRNA.Exon exon) {
        // should not be any nodes underneath exon tags (at least in current pseudo-DTD
        //  GAH 10-6-2001
        int start = exon.getStart().intValue();
        int end = exon.getEnd().intValue();

        transCheckExons.add(new int[]{start, end});

        SeqSpan span = new SimpleSeqSpan(start, end, genomic);
        SimpleSymWithProps exonsym = new SimpleSymWithProps();
        addDescriptors(exon.getDescriptor(), exonsym);
        exonsym.setProperty(STARTSTR, start + "");
        exonsym.setProperty(ENDSTR, end + "");
        exonsym.setProperty("length", String.valueOf(end - start));
        exonsym.addSpan(span);
        return exonsym;
    }

    private BioSeq addSpans(TypeContainerAnnot m2gSym, BioSeq chromosome, List exon_insert_list, int start)
            throws NumberFormatException {
        int exoncount = m2gSym.getChildCount();
        int mrnalength = determinemRNALength(exoncount, m2gSym, chromosome, exon_insert_list);
        int end = 0;
        String mrna_id = MRNASTR;
        BioSeq mrna = new BioSeq(mrna_id, mrnalength);
        mrna.setBounds(start, start + mrnalength);
        mrna_hash.put(mrna_id, mrna);
        SeqSpan mrna_span = new SimpleSeqSpan(mrna.getMin(), mrna.getMax(), mrna);
        m2gSym.addSpan(mrna_span);
        for (int i = 0; i < exoncount; i++) {
            SimpleSymWithProps esym = (SimpleSymWithProps) m2gSym.getChild(i);
            SeqSpan gspan = esym.getSpan(chromosome);
            end = start + gspan.getLength();
            List<Element> hit_inserts = new ArrayList<>();
            end = determineOverlappingExons(exon_insert_list, gspan, hit_inserts, end);
            SeqSpan tspan = new SimpleSeqSpan(start, end, mrna);
            esym.addSpan(tspan);
            if (!hit_inserts.isEmpty()) {
                processExonInsert((MutableSeqSymmetry) esym, hit_inserts, chromosome, mrna);
            }
            start = end;
        }
        return mrna;
    }

    private static int determineOverlappingExons(List exon_insert_list, SeqSpan gspan, List<Element> hit_inserts, int end) throws NumberFormatException {
        for (int insert_index = 0; insert_index < exon_insert_list.size(); insert_index++) {
            Element iel = (Element) exon_insert_list.get(insert_index);
            int istart = Integer.parseInt(iel.getAttribute("insert_at"));
            int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
            if (SeqUtils.contains(gspan, (SeqSpan) iel)) {
                // need to add children to this exon symmetry to indicate an insertion
                //   (or possibly deletion?) of bases in the transcript relative to the genomic
                //	    processExonInsert(esym, istart, ilength);
                logger.error("insert: insertion_start = " + istart + ", length = " + ilength);
                // remove this exon_insert from list to consider in future passes
                //    need to also decrement the insert_index to make sure removal doesn't cause
                //    next exon_insert to not be considered...
                exon_insert_list.remove(insert_index);
                hit_inserts.add(iel);
                insert_index--;
                end += ilength;
            }
        }
        return end;
    }

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

        for (Element iel : hit_inserts) {
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

    private static int determinemRNALength(int exoncount, TypeContainerAnnot m2gSym, BioSeq chromosome, List exon_insert_list) throws NumberFormatException {
        int mrnalength = 0;
        for (int i = 0; i < exoncount; i++) {
            SeqSymmetry esym = m2gSym.getChild(i);
            SeqSpan gspan = esym.getSpan(chromosome);
            mrnalength += gspan.getLength();
        }
        for (Object exon_insert_list1 : exon_insert_list) {
            Element iel = (Element) exon_insert_list1;
            int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
            mrnalength += ilength;
        }
        return mrnalength;
    }

    private static void addDescriptors(List<Dnaseq.Descriptor> descriptors, SimpleSymWithProps sym) {

        for (Dnaseq.Descriptor descriptor : descriptors) {
            String desc_name = descriptor.getType();
            String desc_text = descriptor.getValue();
            if (!Strings.isNullOrEmpty(desc_text)) {
                sym.setProperty(desc_name, desc_text);
            }
        }
        Object test = sym.getProperty("domain_pos");
        if (test != null) {
            sym.setProperty(NAMESTR, test);
        }
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

}
