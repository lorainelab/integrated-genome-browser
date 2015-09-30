package com.gene.tallyhandler;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.IndexWriter;
import com.affymetrix.genometry.parsers.Parser;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symloader.SymLoaderTabix;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TallyHandler implements FileTypeHandler {

    private static final String[] EXTENSIONS = new String[]{"tally"};

    @Override
    public String getName() {
        return "Tally";
    }

    @Override
    public String[] getExtensions() {
        return EXTENSIONS;
    }

    public static List<String> getFormatPrefList() {
        return Arrays.asList(EXTENSIONS);
    }

    @Override
    public SymLoader createSymLoader(URI uri, Optional<URI> indexUri, String featureName,
            GenomeVersion genomeVersion) {
        SymLoader symLoader = null;
        String uriString = uri.toString();
        if (uriString.startsWith(FILE_PROTOCOL)) {
            uriString = uriString.substring(FILE_PROTOCOL.length());
        }
        if (SymLoaderTabix.isTabix(uriString)) {
            TallyLineProcessor tlp = new TallyLineProcessor(featureName);
            try {
                symLoader = new SymLoaderTabix(uri, indexUri, featureName, genomeVersion, tlp);
            } catch (Exception ex) {
                Logger.getLogger(TallyHandler.class.getName()).log(Level.SEVERE,
                        "Could not initialize tabix line reader for {0}.",
                        new Object[]{featureName});
            }
        } else {
            Logger.getLogger(this.getClass().getName()).log(
                    Level.WARNING, "unable to read index for tally file, reading full file");
            symLoader = new TallyUnindexedSymLoader(uri, featureName, genomeVersion);
            try {
                ((TallyUnindexedSymLoader) symLoader).init();
            } catch (Exception x) {
                Logger.getLogger(this.getClass().getName()).log(
                        Level.SEVERE, "Error loading " + uri, x);
            }
        }
        return symLoader;
    }

    @Override
    public Parser getParser() {
        return null;
    }

    @Override
    public IndexWriter getIndexWriter(String stream_name) {
        return null;
    }

    @Override
    public FileTypeCategory getFileTypeCategory() {
        return FileTypeCategory.Graph;
    }
}
