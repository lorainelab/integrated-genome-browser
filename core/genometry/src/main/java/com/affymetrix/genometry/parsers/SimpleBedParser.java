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

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SimpleBedParser implements AnnotationWriter {

    public String getMimeType() {
        return "text/plain";
    }

    public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
            String type, OutputStream outstream) throws IOException {
        boolean success;
        List<SeqSpan> spanlist = new ArrayList<>(syms.size());  // initialize to number of top-level syms, won't be lower...
        for (SeqSymmetry sym : syms) {
            SeqUtils.collectLeafSpans(sym, seq, spanlist);
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }

        try {
            Writer bw = new BufferedWriter(new OutputStreamWriter(outstream));
            for (SeqSpan span : spanlist) {
                bw.write(span.getBioSeq().getID());
                bw.write('\t');
                bw.write(Integer.toString(span.getMin()));
                bw.write('\t');
                bw.write(Integer.toString(span.getMax()));
                bw.write('\n');

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
            bw.flush();
            success = true;
        } catch (Exception ex) {
            success = false;
            IOException ioe = new IOException(ex.getMessage());
            ioe.initCause(ex);
            throw ioe;
        }
        return success;
    }
}
