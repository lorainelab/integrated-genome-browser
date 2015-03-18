package com.affymetrix.igb.parsers;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.quickload.QuickLoadSymLoader;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.LocalUrlCacher;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is an extension of the QuickLoadSymLoader class, specifically for chp
 * files.
 * If this is not a chp file (extension ends with "chp") an Exception is thrown
 */
public class QuickLoadSymLoaderChp extends QuickLoadSymLoader {

    public QuickLoadSymLoaderChp(URI uri, String featureName, AnnotatedSeqGroup group) {
        super(uri, featureName, group);
        if (!extension.endsWith("chp")) {
            throw new IllegalStateException("wrong QuickLoad for chp file");
        }
    }

    protected Map<String, List<? extends SeqSymmetry>> loadSymmetriesThread(final GenericFeature feature, final SeqSpan overlapSpan)
            throws OutOfMemoryError, Exception {
        // special-case chp files, due to their LazyChpSym DAS/2 loading
        return addMethodsToFeature(feature, QuickLoadSymLoaderChp.this.getGenome());
    }

    protected void addAllSymmetries(final GenericFeature feature, List<? extends SeqSymmetry> results)
            throws OutOfMemoryError {
        // special-case chp files, due to their LazyChpSym DAS/2 loading
        addMethodsToFeature(feature, results);
    }

    //Only used for "chp"
    private static Map<String, List<? extends SeqSymmetry>> addMethodsToFeature(
            GenericFeature feature, List<? extends SeqSymmetry> results) {
        if (results == null) {
            return Collections.<String, List<? extends SeqSymmetry>>emptyMap();
        }

        Map<String, List<SeqSymmetry>> syms = SymLoader.splitResultsByTracks(results);
        Map<String, List<? extends SeqSymmetry>> added = new HashMap<>();
        syms.entrySet().stream().filter(entry -> entry.getKey() != null).forEach(entry -> {
            feature.setMethod(entry.getKey());
            added.put(entry.getKey(), entry.getValue());
        });
        return added;
    }

    /**
     * Only used for non-symloader files.
     */
    @Override
    public List<? extends SeqSymmetry> getGenome() {
        try {
            // special-case CHP files. ChpParser only has
            //    a parse() method that takes the file name
            // (ChpParser uses Affymetrix Fusion SDK for actual file parsing)
            File f = LocalUrlCacher.convertURIToFile(this.uri);
            return ChpParser.parse(f.getAbsolutePath(), true);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            logException(ex);
            throw new RuntimeException(ex);
        }
    }
}
