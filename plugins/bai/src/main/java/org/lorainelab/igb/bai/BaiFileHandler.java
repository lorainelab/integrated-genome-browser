package org.lorainelab.igb.bai;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.IndexWriter;
import com.affymetrix.genometry.parsers.Parser;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symloader.SymLoaderTabix;
import com.affymetrix.genometry.symloader.Wiggle;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Sai Charan Reddy Vallapureddy
 * @email  vallapucharan@gmail.com
 */

@Component(immediate = true)
public class BaiFileHandler implements FileTypeHandler {

    String[] extensions = new String[]{"bai"};
    
    @Override
    public String getName() {
        return "bai";
    }

    @Override
    public String[] getExtensions() {
        return extensions;
    }

    @Override
    public SymLoader createSymLoader(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        URI bedgraphURI=null;
        BaiToBedgraphConverter bai = new BaiToBedgraphConverter(uri);
        bedgraphURI = bai.returnTempBedgraphFile().toURI();
        return SymLoaderTabix.getSymLoader(new Wiggle(bedgraphURI, indexUri, featureName, genomeVersion));
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
        return FileTypeCategory.Annotation;
    }
    
}