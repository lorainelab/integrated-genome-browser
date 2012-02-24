package com.affymetrix.genometryImpl.parsers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.MisMatchPileupGraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class MismatchFileExporter implements FileExporterI {
	private static final Map<Character, int[]> FULL_BAR_ORDERS = new HashMap<Character, int[]>();
	static {
		FULL_BAR_ORDERS.put('A', new int[]{0,1,2,3,4});
		FULL_BAR_ORDERS.put('T', new int[]{1,0,2,3,4});
		FULL_BAR_ORDERS.put('G', new int[]{2,0,1,3,4});
		FULL_BAR_ORDERS.put('C', new int[]{3,0,1,2,4});
		FULL_BAR_ORDERS.put('N', new int[]{4,0,1,2,3});
	}
	private static final String BASE_ORDER = "ATGCN";

	@Override
	public void exportFile(DataOutputStream dos, SeqSymmetry sym,
			BioSeq aseq) throws IOException {
		String seq = aseq.toString();
		MisMatchPileupGraphSym mismatchSym = (MisMatchPileupGraphSym)sym;
		int[] x = mismatchSym.getGraphXCoords();
		for (int i = 0; i < x.length; i++) {
			char referenceBase = mismatchSym.hasReferenceSequence() ?
					mismatchSym.getReferenceBase(i)
					: aseq.getResidues(x[i],x[i] + 1).toUpperCase().charAt(0);
			int[] barOrder = FULL_BAR_ORDERS.get(referenceBase);
			if (barOrder == null) {
				continue;
			}
			float[] residuesY = mismatchSym.getAllResiduesY(i);
			int total = 0;
			for (int j = 0; j < residuesY.length; j++) {
				total += (int)residuesY[j];
			}
			if (total == 0) {
				continue;
			}
			dos.write(seq.getBytes());
			dos.write('\t');
			dos.write(Integer.toString(x[i]).getBytes());
			dos.write('\t');
			boolean started = false;
			for (int j = 0; j < barOrder.length; j++) {
				int loopIndex = barOrder[j];
				float ytemp = residuesY[loopIndex];
				if (j > 0 && ytemp == 0) {
					continue;
				}
				if (started) {
					dos.write(' ');
				}
				dos.write(BASE_ORDER.charAt(loopIndex));
				dos.write(Integer.toString((int)residuesY[loopIndex]).getBytes());
				started = true;
			}
			dos.write('\n');
		}
	}

	@Override
	public String getFileExtension() {
		return "tally";
	}
}
