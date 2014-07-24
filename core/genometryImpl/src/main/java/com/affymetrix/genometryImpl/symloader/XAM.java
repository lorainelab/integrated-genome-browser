package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;

import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

/**
 *
 * @author hiralv
 */
public abstract class XAM extends SymLoader {

    protected final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();

    protected static final boolean DEBUG = false;
    protected boolean skipUnmapped = true;
    protected SAMFileReader reader;
    protected SAMFileHeader header;
    protected final Map<BioSeq, String> seqs = new HashMap<BioSeq, String>();

    public static final String CIGARPROP = "cigar";
    public static final String RESIDUESPROP = "residues";
    public static final String BASEQUALITYPROP = "baseQuality";
    public static final String SHOWMASK = "showMask";
    public static final String INSRESIDUESPROP = "insResidues";

    public XAM(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
        super(uri, featureName, seq_group);

        strategyList.add(LoadStrategy.NO_LOAD);
        strategyList.add(LoadStrategy.VISIBLE);
    }

    @Override
    public List<LoadStrategy> getLoadChoices() {
        return strategyList;
    }

    protected boolean initTheSeqs() {
        header = reader.getFileHeader();
        if (header == null || header.getSequenceDictionary() == null
                || header.getSequenceDictionary().getSequences() == null || header.getSequenceDictionary().getSequences().isEmpty()) {
            Logger.getLogger(BAM.class.getName()).log(Level.WARNING, "Couldn't find sequences in file");
            return false;
        }
        Thread thread = Thread.currentThread();
        for (SAMSequenceRecord ssr : header.getSequenceDictionary().getSequences()) {
            try {
                if (thread.isInterrupted()) {
                    break;
                }
                String seqID = ssr.getSequenceName();
                int seqLength = ssr.getSequenceLength();
                BioSeq seq = group.addSeq(seqID, seqLength, uri.toString());
                if (seq.getVersion() != null) {
                    seq.setVersion(group.getID());
                }
                seqs.put(seq, seqID);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return !thread.isInterrupted();
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        init();
        return new ArrayList<BioSeq>(seqs.keySet());
    }

    @Override
    public List<SeqSymmetry> getGenome() throws Exception {
        init();
        List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
        for (BioSeq seq : group.getSeqList()) {
            results.addAll(getChromosome(seq));
        }
        return results;
    }

    @Override
    public List<SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
        init();
        return parse(seq, seq.getMin(), seq.getMax(), true, false);
    }

    @Override
    public List<SeqSymmetry> getRegion(SeqSpan span) throws Exception {
        init();
        return parse(span.getBioSeq(), span.getMin(), span.getMax(), true, false);
    }

    /**
     * @return a list of symmetries for the given chromosome range.
     */
    public abstract List<SeqSymmetry> parse(BioSeq seq, int min, int max, boolean containerSym, boolean contained) throws Exception;

    /**
     * Convert SAMRecord to SymWithProps.
     *
     * @param sr - SAMRecord
     * @param seq - chromosome
     * @param meth - method name
     * @return SimpleSymWithProps
     */
    protected static SymWithProps convertSAMRecordToSymWithProps(SAMRecord sr, BioSeq seq, String meth) {
        SymWithProps sym = convertSAMRecordToSymWithProps(sr, seq, meth, true);
        addAllSAMRecordProperties(sym, sr);
        return sym;
    }

    protected static SymWithProps convertSAMRecordToSymWithProps(SAMRecord sr, BioSeq seq, String meth, boolean includeResidues) {
        SimpleSeqSpan span;
        int start = sr.getAlignmentStart() - 1; // convert to interbase
        int end = sr.getAlignmentEnd();
        if (!sr.getReadNegativeStrandFlag()) {
            span = new SimpleSeqSpan(start, end, seq);
        } else {
            span = new SimpleSeqSpan(end, start, seq);
        }

        List<SeqSpan> insertChilds = new ArrayList<SeqSpan>();
        List<SeqSpan> childs = getChildren(seq, sr.getCigar(), sr.getReadNegativeStrandFlag(), insertChilds);

        int blockMins[] = new int[childs.size()];
        int blockMaxs[] = new int[childs.size()];
        for (int i = 0; i < childs.size(); i++) {
            blockMins[i] = childs.get(i).getMin() + span.getMin();
            blockMaxs[i] = blockMins[i] + childs.get(i).getLength();
        }

        int iblockMins[] = new int[insertChilds.size()];
        int iblockMaxs[] = new int[insertChilds.size()];
        for (int i = 0; i < insertChilds.size(); i++) {
            iblockMins[i] = insertChilds.get(i).getMin() + span.getMin();
            iblockMaxs[i] = iblockMins[i] + insertChilds.get(i).getLength();
        }

        if (childs.isEmpty()) {
            blockMins = new int[1];
            blockMins[0] = span.getStart();
            blockMaxs = new int[1];
            blockMaxs[0] = span.getEnd();
        }

        BAMSym sym = new BAMSym(meth, seq, start, end, sr.getReadName(),
                sr.getMappingQuality(), span.isForward(), blockMins, blockMaxs, iblockMins,
                iblockMaxs, sr.getCigar(), includeResidues ? sr.getReadString() : null, sr.getBaseQualityString());
        sym.setProperty("id", sr.getReadName());
        sym.setProperty("method", meth);
        if (sr.getAttribute("NH") != null) {
            sym.setProperty("NH", sr.getAttribute("NH"));
        }
        sym.setFlags(sr.getFlags());
        sym.setDuplicateReadFlag(sr.getDuplicateReadFlag());
        sym.setReadPairedFlag(sr.getReadPairedFlag());
        if (sym.getReadPairedFlag()) {
            sym.setFirstOfPairFlag(sr.getFirstOfPairFlag());
            sym.setSecondOfPairFlag(sr.getSecondOfPairFlag());
        }
        return sym;
    }

    private static void addAllSAMRecordProperties(SymWithProps sym, SAMRecord sr) {
        for (SAMTagAndValue tv : sr.getAttributes()) {
            sym.setProperty(tv.tag, tv.value);
        }
        sym.setProperty(CIGARPROP, sr.getCigar());
        sym.setProperty(SHOWMASK, true);

//		Not using "SEQ" anywhere. So commenting out for now.
//		if (sr.getCigar() == null || sym.getProperty("MD") == null) {
//			//sym.setProperty("residues", sr.getReadString());
//		} else {
//			// If both the MD and Cigar properties are set, don't need to specify residues.
//			byte[] SEQ = SequenceUtil.makeReferenceFromAlignment(sr, false);
//			sym.setProperty("SEQ", SEQ);
//		}
        getFileHeaderProperties(sr.getHeader(), sym);
    }

    private static List<SeqSpan> getChildren(BioSeq seq, Cigar cigar, boolean isNegative, List<SeqSpan> insertChilds) {
        List<SeqSpan> results = new ArrayList<SeqSpan>();
        if (cigar == null || cigar.numCigarElements() == 0) {
            return results;
        }
        int currentChildStart = 0;
        int currentChildEnd = 0;
        int celLength = 0;
        SeqSpan previousSpan = null;

        for (CigarElement cel : cigar.getCigarElements()) {
            try {
                celLength = cel.getLength();
                if (cel.getOperator() == CigarOperator.DELETION) {
//					currentChildStart = currentChildEnd;
//					currentChildEnd = currentChildStart  + celLength;
                    currentChildEnd += celLength;
                } else if (cel.getOperator() == CigarOperator.INSERTION) {
                    // TODO -- allow possibility that INSERTION is terminator, not M
//					currentChildStart = currentChildEnd;
//					currentChildEnd = currentChildStart;
                    if (!isNegative) {
                        insertChilds.add(new SimpleSeqSpan(currentChildEnd, currentChildEnd + celLength, seq));
                    } else {
                        insertChilds.add(new SimpleSeqSpan(currentChildEnd + celLength, currentChildEnd, seq));
                    }
                } else if (cel.getOperator() == CigarOperator.M) {
                    // print matches
                    currentChildEnd += celLength;
                    if (!isNegative) {
                        if (previousSpan != null && previousSpan.getStart() == currentChildStart) {
                            results.remove(previousSpan);
                        }
                        previousSpan = new SimpleSeqSpan(currentChildStart, currentChildEnd, seq);
                    } else {
                        if (previousSpan != null && previousSpan.getEnd() == currentChildStart) {
                            results.remove(previousSpan);
                        }
                        previousSpan = new SimpleSeqSpan(currentChildEnd, currentChildStart, seq);
                    }
                    results.add(previousSpan);
                } else if (cel.getOperator() == CigarOperator.N) {
                    currentChildStart = currentChildEnd + celLength;
                    currentChildEnd = currentChildStart;
                } else if (cel.getOperator() == CigarOperator.PADDING) {
                    // TODO -- allow possibility that PADDING is terminator, not M
                    // print matches
                    currentChildEnd += celLength;
                } else if (cel.getOperator() == CigarOperator.SOFT_CLIP) {
                    // skip over soft clip
                } else if (cel.getOperator() == CigarOperator.HARD_CLIP) {
                    // hard clip can be ignored
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return results;
    }

    private static void getFileHeaderProperties(SAMFileHeader hr, SymWithProps sym) {
        if (hr == null) {
            return;
        }
        //Sequence Dictionary
        SAMSequenceDictionary ssd = hr.getSequenceDictionary();
        for (SAMSequenceRecord ssr : ssd.getSequences()) {
            if (ssr.getAssembly() != null) {
                sym.setProperty("genomeAssembly", ssr.getAssembly());
            }
            if (ssr.getSpecies() != null) {
                sym.setProperty("species  ", ssr.getSpecies());
            }
        }
        //Read Group
        for (SAMReadGroupRecord srgr : hr.getReadGroups()) {
            for (Entry<String, String> en : srgr.getAttributes()) {
                sym.setProperty(en.getKey(), en.getValue());
            }
        }

        //Program
        for (SAMProgramRecord spr : hr.getProgramRecords()) {
            for (Entry<String, String> en : spr.getAttributes()) {
                sym.setProperty(en.getKey(), en.getValue());
            }
        }
    }

    /**
     * Rewrite the residue string, based upon cigar information.
     */
    @SuppressWarnings("unused")
    private static String interpretCigar(Cigar cigar, String residues, int spanLength, StringBuffer insResidues) {
        if (cigar == null || cigar.numCigarElements() == 0) {
            return residues;
        }
        char[] sb = new char[spanLength];
        int currentPos = 0;
        int currentEnd = 0;
        for (CigarElement cel : cigar.getCigarElements()) {
            try {
                int celLength = cel.getLength();
                if (cel.getOperator() == CigarOperator.DELETION) {
                    char[] tempArr = new char[celLength];
                    Arrays.fill(tempArr, '_');		// print deletion as '_'
                    System.arraycopy(tempArr, 0, sb, currentEnd, tempArr.length);
                } else if (cel.getOperator() == CigarOperator.INSERTION) {
                    insResidues.append(residues.substring(currentPos, currentPos + celLength));
                    currentPos += celLength;	// print insertion
                    continue;
                } else if (cel.getOperator() == CigarOperator.M) {
                    char[] tempArr = residues.substring(currentPos, currentPos + celLength).toCharArray();
                    System.arraycopy(tempArr, 0, sb, currentEnd, tempArr.length);
                    currentPos += celLength;	// print matches
                } else if (cel.getOperator() == CigarOperator.N) {
                    char[] tempArr = new char[celLength];
                    Arrays.fill(tempArr, '-');
                    System.arraycopy(tempArr, 0, sb, currentEnd, tempArr.length);
                } else if (cel.getOperator() == CigarOperator.PADDING) {
                    char[] tempArr = new char[celLength];
                    Arrays.fill(tempArr, '*');		// print padding as '*'
                    System.arraycopy(tempArr, 0, sb, currentEnd, tempArr.length);
                    currentPos += celLength;
                } else if (cel.getOperator() == CigarOperator.SOFT_CLIP) {
                    currentPos += celLength;	// skip over soft clip
                    continue;
                } else if (cel.getOperator() == CigarOperator.HARD_CLIP) {
                    continue;				// hard clip can be ignored
                }
                currentEnd += celLength;
            } catch (Exception ex) {
                ex.printStackTrace();
                if (spanLength - currentPos > 0) {
                    char[] tempArr = new char[spanLength - currentPos];
                    Arrays.fill(tempArr, '.');
                    System.arraycopy(tempArr, 0, sb, 0, tempArr.length);
                }
            }
        }

        return String.valueOf(sb);
    }

}
