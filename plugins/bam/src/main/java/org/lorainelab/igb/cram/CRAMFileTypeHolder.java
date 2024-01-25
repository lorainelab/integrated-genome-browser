package org.lorainelab.igb.cram;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.IndexWriter;
import com.affymetrix.genometry.parsers.Parser;
import com.affymetrix.genometry.symloader.SymLoader;
import org.osgi.service.component.annotations.Component;

import java.net.URI;
import java.util.Optional;

@Component(immediate = true)
public class CRAMFileTypeHolder implements FileTypeHandler {
    String[] extensions = new String[]{"cram"};
    @Override
    public String getName() {
        return "cram";
    }

    @Override
    public String[] getExtensions() {
        return extensions;
    }

    @Override
    public SymLoader createSymLoader(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        CRAM cram = new CRAM(uri, indexUri, featureName, genomeVersion);
        return cram;
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
