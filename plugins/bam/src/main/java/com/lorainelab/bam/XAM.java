package com.lorainelab.bam;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symloader.SamRecordFlag;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.BAMSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.BAM_FLAG;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.CIGAR;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.MATE_START;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.NA;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.NAME;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.NH;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.SHOW_MASK;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

/**
 *
 * @author hiralv
 */
public abstract class XAM extends SymLoader {

    protected final List<LoadStrategy> strategyList = new ArrayList<>();

    protected static final boolean DEBUG = false;
    protected boolean skipUnmapped = true;
    protected SAMFileReader reader;
    protected SAMFileHeader header;
    protected final Map<BioSeq, String> seqs = new HashMap<>();

    public static final String RESIDUESPROP = "residues";
    public static final String BASEQUALITYPROP = "baseQuality";
    public static final String INSRESIDUESPROP = "insResidues";

    public XAM(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion seq_group) {
        super(uri, indexUri, featureName, seq_group);

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
                BioSeq seq = genomeVersion.addSeq(seqID, seqLength, uri.toString());
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
        return new ArrayList<>(seqs.keySet());
    }

    @Override
    public List<SeqSymmetry> getGenome() throws Exception {
        init();
        List<SeqSymmetry> results = new ArrayList<>();
        for (BioSeq seq : genomeVersion.getSeqList()) {
            results.addAll(getChromosome(seq));
        }
        return results;
    }

    @Override
    public List<SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
        init();
        SimpleSeqSpan seqSpan = new SimpleSeqSpan(seq.getMin(), seq.getMax(), seq);
        return parse(seqSpan);
    }

    @Override
    public List<SeqSymmetry> getRegion(SeqSpan span) throws Exception {
        init();
        return parse(span);
    }

    /**
     * @return a list of symmetries for the given chromosome range.
     */
    public abstract List<SeqSymmetry> parse(SeqSpan span) throws Exception;

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

        List<SeqSpan> insertChildren = new ArrayList<>();
        List<SeqSpan> children = getChildren(seq, sr, insertChildren);

        int blockMins[] = new int[children.size()];
        int blockMaxs[] = new int[children.size()];
        for (int i = 0; i < children.size(); i++) {
            blockMins[i] = children.get(i).getMin() + span.getMin();
            blockMaxs[i] = blockMins[i] + children.get(i).getLength();
        }

        int iblockMins[] = new int[insertChildren.size()];
        int iblockMaxs[] = new int[insertChildren.size()];
        for (int i = 0; i < insertChildren.size(); i++) {
            iblockMins[i] = insertChildren.get(i).getMin() + span.getMin();
            iblockMaxs[i] = iblockMins[i] + insertChildren.get(i).getLength();
        }

        if (children.isEmpty()) {
            blockMins = new int[1];
            blockMins[0] = span.getStart();
            blockMaxs = new int[1];
            blockMaxs[0] = span.getEnd();
        }

        BAMSym sym = new BAMSym(meth, seq, start, end, sr.getReadName(),
                sr.getMappingQuality(), span.isForward(), blockMins, blockMaxs, iblockMins,
                iblockMaxs, sr.getCigar(), includeResidues ? sr.getReadString() : null, sr.getBaseQualityString());
        sym.setProperty(NAME, sr.getReadName());
        if (sr.getAttribute(NH) != null) {
            sym.setProperty(NH, sr.getAttribute(NH));
        }
        sym.setFlags(sr.getFlags());
        sym.setDuplicateReadFlag(sr.getDuplicateReadFlag());
        sym.setReadPairedFlag(sr.getReadPairedFlag());
        SamRecordFlag srf = new SamRecordFlag(sym.getFlags());
        for (Map.Entry<String, String> entry : srf.getFlagProperties().entrySet()) {

            if (sr.getMateAlignmentStart() > 0) {
                sym.setProperty(entry.getKey(), entry.getValue());
            } else {
                //unfortunately, we need to filter out some properties in this scenario
                if (!SamRecordFlag.MATE_PROPS.contains(entry.getKey())) {
                    sym.setProperty(entry.getKey(), entry.getValue());
                }
            }

        }

        if (sym.getReadPairedFlag()) {
            int mateStart = sr.getMateAlignmentStart() - 1;
            if (mateStart == -1) {
                mateStart = 0;
                sym.setProperty(MATE_START, NA);
            } else {
                sym.setProperty(MATE_START, GeneralUtils.applyCommaFormatting(mateStart));
            }
            sym.setMateStart(mateStart);
            sym.setMateNegativeStrandFlag(sr.getMateNegativeStrandFlag());
            sym.setProperty(BAM_FLAG, sym.getFlags());

        }

        return sym;
    }

    protected static void addAllSAMRecordProperties(SymWithProps sym, SAMRecord sr) {
        for (SAMTagAndValue tv : sr.getAttributes()) {
            sym.setProperty(tv.tag, tv.value);
        }
        sym.setProperty(CIGAR, sr.getCigar());
        sym.setProperty(SHOW_MASK, true);

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

    protected static List<SeqSpan> getChildren(BioSeq seq, SAMRecord sr, List<SeqSpan> insertChilds) {
        Cigar cigar = sr.getCigar();
        boolean isNegative = sr.getReadNegativeStrandFlag();
        List<SeqSpan> results = new ArrayList<>();
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
