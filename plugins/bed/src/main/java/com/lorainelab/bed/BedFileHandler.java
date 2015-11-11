/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.bed;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.parsers.BedParser;
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
public class BedFileHandler implements FileTypeHandler {

    String[] extensions = new String[]{"bed"};

    @Override
    public String getName() {
        return "BED";
    }

    @Override
    public String[] getExtensions() {
        return extensions;
    }

    @Override
    public SymLoader createSymLoader(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        BedSymloader bed = new BedSymloader(uri, indexUri, featureName, genomeVersion);
        return SymLoaderTabix.getSymLoader(bed);
    }

    @Override
    public Parser getParser() {
        return new BedParser();
    }

    @Override
    public IndexWriter getIndexWriter(String stream_name) {
        return (IndexWriter) getParser();
    }

    @Override
    public FileTypeCategory getFileTypeCategory() {
        return FileTypeCategory.Annotation;
    }

}
