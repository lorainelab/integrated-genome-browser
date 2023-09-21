
package org.lorainelab.igb.genotyping;

import org.osgi.service.component.annotations.Component;
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
 * Handles files from  genotyping assays.
 * 
 */
@Component(immediate = true)
public class GenotypeFileHandler implements FileTypeHandler {

    String[] extensions = new String[]{"23andme", "23andMe"};

    @Override
    public String getName() {
        return "GenotypeFileHandler";
    }

    @Override
    public String[] getExtensions() {
        return extensions;
    }

    @Override
    public SymLoader createSymLoader(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        // Because the 23 and Me format is tab-delimited, we can use tabix
        // If we need to support other file formats, do that by creating and returning different SymLoaders implementations here
        TwentyThreeAndMeSymLoader genotypeSymLoader = new TwentyThreeAndMeSymLoader(uri, indexUri, featureName, genomeVersion);
        return SymLoaderTabix.getSymLoader(genotypeSymLoader);
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
      // return FileTypeCategory.Annotation;
      return FileTypeCategory.PersonalGenomics;
    }

}

