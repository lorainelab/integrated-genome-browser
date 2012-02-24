package com.affymetrix.genometryImpl.parsers;

import java.io.DataOutputStream;
import java.io.IOException;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithResidues;

public class SequenceFileExporter implements FileExporterI {
	private static final int COLUMNS = 50;
	@Override
	public void exportFile(DataOutputStream dos, SeqSymmetry sym,
			BioSeq aseq) throws IOException {
		SimpleSymWithResidues residuesSym = (SimpleSymWithResidues)sym.getChild(0);
		String residues = residuesSym.getResidues();
		dos.write('>');
		dos.write(aseq.toString().getBytes());
		dos.write('\n');
		int pointer = 0;
		while (pointer < residues.length()) {
			int end = Math.min(pointer + COLUMNS, residues.length());
			dos.write(residues.substring(pointer, end).getBytes());
			dos.write('\n');
			pointer += COLUMNS;
		}
	}

	@Override
	public String getFileExtension() {
		return "fasta";
	}
}
