package com.affymetrix.genometryImpl.parsers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface FileExporterI {
	public void exportFile(DataOutputStream dos, SeqSymmetry sym, BioSeq aseq) throws IOException;
	public void exportFile(DataOutputStream dos, List<SeqSymmetry> syms, BioSeq aseq) throws IOException;
	public String getFileExtension();
}
