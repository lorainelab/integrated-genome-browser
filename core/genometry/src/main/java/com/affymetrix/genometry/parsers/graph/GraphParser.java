package com.affymetrix.genometry.parsers.graph;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.parsers.Parser;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * interface for all graph file type funtionality. Only applicable to
 * graph file types
 */
public interface GraphParser extends Parser {

    public List<GraphSym> readGraphs(InputStream istr, String stream_name, GenomeVersion seq_group, BioSeq seq)
            throws IOException;

    public void writeGraphFile(GraphSym gsym, GenomeVersion seq_group, String file_name) throws IOException;
}
