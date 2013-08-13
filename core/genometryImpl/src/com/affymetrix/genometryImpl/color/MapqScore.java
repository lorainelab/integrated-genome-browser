package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class MapqScore extends Score {
	
	@Override
	public Color getColor(SeqSymmetry sym){
		if(sym instanceof BAMSym) {
			int score = ((BAMSym) sym).getMapq();
			if(score != BAMSym.NO_MAPQ) {
				return getScoreColor(score);
			}
		}
		return null;
	}
}
