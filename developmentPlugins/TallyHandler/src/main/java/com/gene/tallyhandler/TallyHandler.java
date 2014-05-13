package com.gene.tallyhandler;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.parsers.Parser;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoaderTabix;

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
    public SymLoader createSymLoader(URI uri, String featureName,
            AnnotatedSeqGroup group) {
        SymLoader symLoader = null;
        String uriString = uri.toString();
        if (uriString.startsWith(SymLoader.FILE_PREFIX)) {
            uriString = uriString.substring(SymLoader.FILE_PREFIX.length());
        }
        if (SymLoaderTabix.isTabix(uriString)) {
            TallyLineProcessor tlp = new TallyLineProcessor(featureName);
            try {
                symLoader = new SymLoaderTabix(uri, featureName, group, tlp);
            } catch (Exception ex) {
                Logger.getLogger(TallyHandler.class.getName()).log(Level.SEVERE,
                        "Could not initialize tabix line reader for {0}.",
                        new Object[]{featureName});
            }
        } else {
            Logger.getLogger(this.getClass().getName()).log(
                    Level.WARNING, "unable to read index for tally file, reading full file");
            symLoader = new TallyUnindexedSymLoader(uri, featureName, group);
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
