/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License").
 * A copy of the license must be included with any distribution of
 * this source code.
 * Distributions from Affymetrix, Inc., place this in the
 * IGB_LICENSE.html file.
 *
 * The license is also available at
 * http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry.parsers;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.List;

public final class PSLParser extends AbstractPSLParser implements AnnotationWriter, IndexWriter, Parser {

    @Override
    public List<? extends SeqSymmetry> parse(InputStream is,
            GenomeVersion genomeVersion, String nameType, String uri, boolean annotate_seq)
            throws Exception {
        // reference to LoadFileAction.ParsePSL
        enableSharedQueryTarget(true);
        InputStream is2 = is;
        if (!annotate_seq) {
            is2 = new DataInputStream(is);
        }
        return parse(is2, annotate_seq ? uri : nameType, null, genomeVersion, null, false, annotate_seq, false);
    }
}
