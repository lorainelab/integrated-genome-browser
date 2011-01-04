package com.affymetrix.genometryImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author hiralv
 */
public class MisMatchGraphSym extends GraphSym {

	Map<SymWithResidues.ResiduesChars, GraphSym> reference = new HashMap<SymWithResidues.ResiduesChars, GraphSym>();

	public MisMatchGraphSym(int[] x, int[] w, float[] y, String id, BioSeq seq){
		super(x,w,y,id,seq);
	}

	public void addReference(SymWithResidues.ResiduesChars ch, GraphSym gsym){
		reference.put(ch, gsym);
	}

	public Map<SymWithResidues.ResiduesChars, GraphSym> getReference(){
		return reference;
	}
	
	@Override
	public Map<String, Object> getLocationProperties(int x){
		char ch = this.getGraphSeq().getResidues(x, x+1).charAt(0);
		float y, ytotal = 0;
		String yStr;

		Map<String, Object> locprops = new HashMap<String, Object>();
		Map<SymWithResidues.ResiduesChars, Float> refVals = new  HashMap<SymWithResidues.ResiduesChars, Float>();

		for(Entry<SymWithResidues.ResiduesChars, GraphSym> entry : reference.entrySet()){
			y = !entry.getKey().equal(ch) ? entry.getValue().getYCoordFromX(x) : getYCoordFromX(x);
			refVals.put(entry.getKey(), y);
			ytotal += y;
		}

		for(Entry<SymWithResidues.ResiduesChars, Float> entry : refVals.entrySet()){
			yStr = String.format("%.0f (%.0f %s)", entry.getValue(), entry.getValue() * (100/ytotal), "%");
			locprops.put(entry.getKey().toString(), yStr);
		}
		
		return locprops;
	}
}
