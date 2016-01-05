package org.lorainelab.igb.bam;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symloader.LineProcessor;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.LocalUrlCacher;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.SAMException;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMTextReader;
import net.sf.samtools.util.BufferedLineReader;
import net.sf.samtools.util.CloseableIterator;
import org.broad.tribble.readers.LineReader;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hiralv
 */
public class SAM extends XAM implements LineProcessor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SAM.class);

    public SAM(URI uri, String featureName, GenomeVersion seq_group) {
        super(uri, Optional.empty(), featureName, seq_group);
    }

    @Override
    public void init() throws Exception {
        try {
            reader = new SAMFileReader(LocalUrlCacher.convertURIToBufferedStream(uri));
            reader.setValidationStringency(ValidationStringency.SILENT);

            if (this.isInitialized) {
                return;
            }

            if (initTheSeqs()) {
                super.init();
            }

        } catch (SAMFormatException ex) {
            ErrorHandler.errorPanel("SAM exception", "A SAMFormatException has been thrown by the Picard tools.\n"
                    + "Please validate your BAM files (see http://picard.sourceforge.net/command-line-overview.shtml#ValidateSamFile). "
                    + "See console for the details of the exception.\n", Level.SEVERE);
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    protected boolean initTheSeqs() {
        boolean ret = super.initTheSeqs();

        if (ret) {
            if (header.getSortOrder() != SAMFileHeader.SortOrder.coordinate) {
                Logger.getLogger(SAM.class.getName()).log(Level.SEVERE, "Sam file must be sorted by coordinate.");
                return false;
            }
        }
        return ret;
    }

    @Override
    public List<SeqSymmetry> parse(SeqSpan span) throws Exception {
        init();
        if (reader != null) {
            CloseableIterator<SAMRecord> iter = reader.iterator();
            if (iter != null && iter.hasNext()) {
                return parse(iter, span, true);
            }
        }

        return Collections.<SeqSymmetry>emptyList();
    }

    public List<SeqSymmetry> parse(CloseableIterator<SAMRecord> iter, SeqSpan span, boolean check) throws Exception {
        BioSeq seq = span.getBioSeq();
        int min = span.getMin();
        int max = span.getMax();
        List<SeqSymmetry> symList = new ArrayList<>(1000);
        List<Throwable> errList = new ArrayList<>();
        int maximum;
        String seqId = seqs.get(seq);
        SAMRecord sr = null;
        try {
            while (iter.hasNext() && (!Thread.currentThread().isInterrupted())) {
                try {
                    sr = iter.next();
                    maximum = sr.getAlignmentEnd();

                    if (check) {
                        if (!seqId.equals(sr.getReferenceName())) {
                            continue;
                        }

                        if (!(checkRange(sr.getAlignmentStart(), maximum, min, max))) {
                            if (maximum > max) {
                                break;
                            }
                            continue;
                        }
                    }

                    if (skipUnmapped && sr.getReadUnmappedFlag()) {
                        continue;
                    }
                    symList.add(convertSAMRecordToSymWithProps(sr, seq, uri.toString()));
                } catch (SAMException e) {
                    errList.add(e);
                }
            }
            return symList;
        } finally {
            if (iter != null) {
                iter.close();
            }
            if (!errList.isEmpty()) {
                ErrorHandler.errorPanel("SAM exception", "Ignoring " + errList.size() + " records", errList, Level.WARNING);
            }
        }
    }

    private static boolean checkRange(int start, int end, int min, int max) {

        //getChromosome && getRegion
        if (end < min || start > max) {
            return false;
        }

        return true;
    }

    @Override
    public List<? extends SeqSymmetry> processLines(BioSeq seq, LineReader lineReader) throws Exception {
        // LineTrackerI ignored, since the SAMTextReader hides the lines
        SAMTextReader str = new SAMTextReader(new AsciiTabixLineReader(lineReader), header, ValidationStringency.SILENT);
        SimpleSeqSpan seqSpan = new SimpleSeqSpan(seq.getMin(), seq.getMax(), seq);
        return parse(str.queryUnmapped(), seqSpan, false);
    }

    public void init(URI uri) {
        reader = new SAMFileReader(LocalUrlCacher.convertURIToBufferedStream(uri));
        reader.setValidationStringency(ValidationStringency.SILENT);
        header = reader.getFileHeader();
    }

    private class AsciiTabixLineReader extends BufferedLineReader {

        private final LineReader readerImpl;
        private int lineNumber;

        AsciiTabixLineReader(LineReader readerImpl) {
            super(null);
            this.readerImpl = readerImpl;
            lineNumber = 0;
        }

        @Override
        public String readLine() {
            try {
                return readerImpl.readLine();
            } catch (IOException ex) {
                Logger.getLogger(SAM.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                lineNumber++;
            }
            return null;
        }

        @Override
        public int getLineNumber() {
            return lineNumber;
        }

        @Override
        public void close() {
            readerImpl.close();
        }

        @Override
        public int peek() {
            return -1;
        }

    }

    @Override
    public SeqSpan getSpan(String line) {
        return null; // not used yet
    }

    public boolean processInfoLine(String line, List<String> infoLines) {
        return false; // not used yet
    }
}
