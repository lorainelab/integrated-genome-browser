package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.SeqUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author hiralv
 */
public class FindMateOperator extends AbstractAnnotationTransformer {

	public FindMateOperator() {
		super(FileTypeCategory.Alignment);
	}
	
	@Override
	public String getName(){
		return "findmate";
	}
	
	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList){
		if (symList.size() != 1 || !(symList.get(0) instanceof TypeContainerAnnot)) {
			return null;
		}
		
		return findMate((TypeContainerAnnot)symList.get(0), aseq);
	}
	
	@Override
	public FileTypeCategory getOutputCategory() {
		return FileTypeCategory.PairedRead;
	}

	/**
	 * Finds the mate
	 * @param t
	 * @param aseq
	 * @return 
	 */
	private SeqSymmetry findMate(TypeContainerAnnot t, BioSeq aseq) {
		Map<String, List<SeqSymmetry>> map = new HashMap<String, List<SeqSymmetry>>(1000);
		TypeContainerAnnot result = new TypeContainerAnnot(t.getType()+"bampair", "bampair", false);
		SeqSymmetry child;
		for (int i = 0; i < t.getChildCount(); i++) {
			child = t.getChild(i);
			List<SeqSymmetry> pairs = map.get(child.getID());
			if(pairs == null){
				pairs = new ArrayList<SeqSymmetry>(2);
				map.put(child.getID(), pairs);
			}
			pairs.add(child);
		}
		
		List<SeqSymmetry> pairs;
		SeqSymmetry child1, child2;
		SimpleSymWithProps container;
		for(Entry<String, List<SeqSymmetry>> entry : map.entrySet()){
			pairs = entry.getValue();
			container = new SimpleSymWithProps();
			if(pairs.size() >= 2){
				//TODO: Handle multiple parters here
				//TODO: Check reverse case here
				child1 = pairs.get(0);
				child2 = pairs.get(1);
				SeqSymmetry union = SeqUtils.union(child1, child2, aseq);
				container.addChild(child1);
				container.addChild(child2);
				container.addSpan(union.getSpan(aseq));
			} else if (pairs.size() == 1) {
				child1 = pairs.get(0);
				container.addChild(child1);
				container.addSpan(child1.getSpan(aseq));
			}
			
			result.addChild(container);
		}
		// copy spans
		for (int i = 0; i < t.getSpanCount(); i++) {
			result.addSpan(t.getSpan(i));
		}
	
		map.clear();
		return result;
	}
	
	@Override
	public Operator newInstance(){
		try {
			return getClass().getConstructor().newInstance();
		} catch (Exception ex) {
		}
		return null;
	}
}
