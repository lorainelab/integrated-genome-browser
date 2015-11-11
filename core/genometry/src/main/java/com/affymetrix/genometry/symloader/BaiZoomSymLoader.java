package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.parsers.FileTypehandlerRegistry;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL;
import com.affymetrix.genometry.util.GeneralUtils;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.StubBAMFileIndex;
import net.sf.samtools.seekablestream.SeekableFileStream;
import net.sf.samtools.seekablestream.SeekableHTTPStream;
import net.sf.samtools.seekablestream.SeekableStream;

public class BaiZoomSymLoader extends IndexZoomSymLoader {
    
    public BaiZoomSymLoader(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        super(uri, indexUri, featureName, genomeVersion);
    }

    @Override
    protected SymLoader getDataFileSymLoader() throws Exception {
        return FileTypehandlerRegistry.getFileTypeHolder().getFileTypeHandler("bam").createSymLoader(getBamURI(uri), Optional.ofNullable(indexUri), featureName, genomeVersion);
    }

    private int getRefNo(String igbSeq, SAMSequenceDictionary ssd) {
        List<SAMSequenceRecord> sList = ssd.getSequences();
        for (int i = 0; i < sList.size(); i++) {
            String bamSeq = genomeVersion.getChrSynLookup().getPreferredName(sList.get(i).getSequenceName());
            if (igbSeq.equals(bamSeq)) {
                return i;
            }
        }
        return -1;
    }

    private URI getBamURI(URI baiUri) throws Exception {
        String bamUriString = baiUri.toString().substring(0, baiUri.toString().length() - ".bai".length());
        if (!bamUriString.startsWith(FILE_PROTOCOL) && !bamUriString.startsWith(HTTP_PROTOCOL) && !bamUriString.startsWith(HTTPS_PROTOCOL) && !bamUriString.startsWith(FTP_PROTOCOL)) {
            bamUriString = GeneralUtils.getFileScheme() + bamUriString;
        }
        return new URI(bamUriString);
    }

    protected SeekableStream getSeekableStream(URI uri) throws Exception {
        if (uri.toString().startsWith(HTTP_PROTOCOL) || uri.toString().startsWith(HTTPS_PROTOCOL)) {
            return new SeekableHTTPStream(new URL(uri.toString()));
        } else {
            return new SeekableFileStream(new File(GeneralUtils.fixFileName(uri.toString())));
        }
    }

    @Override
    protected Iterator<Map<Integer, List<List<Long>>>> getBinIter(String seq) {
        try {
            SeekableStream ssData = getSeekableStream(getBamURI(uri));
            SeekableStream ssIndex = getSeekableStream(uri);
            SAMFileReader sfr = new SAMFileReader(ssData, false);
            SAMSequenceDictionary ssd = sfr.getFileHeader().getSequenceDictionary();
            int refno = getRefNo(seq, ssd);
            if (refno == -1) {
                return null;
            } else {
                StubBAMFileIndex sbfi = new StubBAMFileIndex(ssIndex, uri, ssd);
                return sbfi.getBinIter(refno);
            }
        } catch (Exception x) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "cannot read BAI file " + uri, x);
            return null;
        }
    }
}
