package com.lorainelab.bam;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.IndexWriter;
import com.affymetrix.genometry.parsers.Parser;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symloader.SymLoaderTabix;
import java.net.URI;
import java.util.Optional;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true)
public class SamFileTypeHolder implements FileTypeHandler {

    String[] extensions = new String[]{"sam"};

    @Override
    public String getName() {
        return "SAM";
    }

    @Override
    public String[] getExtensions() {
        return extensions;
    }

    @Override
    public SymLoader createSymLoader(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        SAM sam = new SAM(uri, featureName, genomeVersion);
        return SymLoaderTabix.getSymLoader(sam);
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
        return FileTypeCategory.Alignment;
    }
}
