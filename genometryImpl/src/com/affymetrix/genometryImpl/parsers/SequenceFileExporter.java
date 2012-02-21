package com.affymetrix.genometryImpl.parsers;

import java.io.DataOutputStream;
import java.io.IOException;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class SequenceFileExporter implements FileExporterI {

	@Override
	public void exportFile(DataOutputStream dos, SeqSymmetry sym,
			BioSeq aseq) throws IOException {
		// TODO Auto-generated method stub
	
	}

	@Override
	public String getFileExtension() {
		return "fasta";
	}
}
