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
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import htsjdk.samtools.seekablestream.SeekableStream;

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

    /*getBinIter() is not used and cannot find equivalent Class files needed to implement the method.
    * Hence decided to return null from the method to avoid compile errors.*/
    @Override
    protected Iterator<Map<Integer, List<List<Long>>>> getBinIter(String seq) {
       return null;
    }
}
