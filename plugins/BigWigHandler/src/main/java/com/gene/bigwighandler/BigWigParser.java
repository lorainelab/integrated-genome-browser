package com.gene.bigwighandler;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.parsers.graph.GraphParser;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class BigWigParser implements GraphParser {

    @Override
    public List<? extends SeqSymmetry> parse(InputStream is,
            GenomeVersion group, String nameType, String uri,
            boolean annotate_seq) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<GraphSym> readGraphs(InputStream istr, String stream_name,
            GenomeVersion seq_group, BioSeq seq) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeGraphFile(GraphSym gsym, GenomeVersion seq_group,
            String file_name) throws IOException {
        // TODO Auto-generated method stub

    }

}
