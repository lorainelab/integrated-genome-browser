package com.gene.bigwighandler;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.IndexWriter;
import com.affymetrix.genometry.parsers.Parser;
import com.affymetrix.genometry.symloader.SymLoader;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Component(name = BigWigHandler.COMPONENT_NAME, immediate = true)
public class BigWigHandler implements FileTypeHandler {

    public static final String COMPONENT_NAME = "BigWigHandler";
    static final String[] EXTENSIONS = new String[]{"bw", "bigWig", "bigwig"};

    public BigWigHandler() {
        super();
    }

    @Override
    public String getName() {
        return "Graph";
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
        return new BigWigSymLoader(uri, featureName, group);
    }

    @Override
    public Parser getParser() {
        return new BigWigParser();
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
