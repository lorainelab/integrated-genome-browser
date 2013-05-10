package com.affymetrix.igb.colorproviders;

import com.affymetrix.genometryImpl.color.ColorProvider;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class ReadGroup extends ColorProvider {
	
	@Override
	public Color getColor(SeqSymmetry obj){
		if(obj instanceof BAMSym){
			BAMSym sym = (BAMSym)obj;
		}
		
		return null;
	}
}
