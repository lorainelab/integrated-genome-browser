package com.affymetrix.genometryImpl.parsers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class AnnotationFileExporter implements FileExporterI {

	@Override
	public void exportFile(DataOutputStream dos, SeqSymmetry sym,
			BioSeq aseq) throws IOException {
		int childcount = sym.getChildCount();
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(childcount);
		for (int i = 0; i < childcount; i++) {
			SeqSymmetry child = sym.getChild(i);
			if (child instanceof SeqSymmetry) {
				syms.add(child);
			}
		}

		exportFile(dos, syms, aseq);
	}

	@Override
	public void exportFile(DataOutputStream dos, List<SeqSymmetry> syms, BioSeq aseq) throws IOException {
		BedParser.writeBedFormat(dos, syms, aseq);
	}
	
	@Override
	public String getFileExtension() {
		return "bed";
	}
}
