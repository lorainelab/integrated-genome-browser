package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.SeqUtils;
import java.util.List;

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
	 * This is a dummy method to emulate find mate.
	 * @param t
	 * @param aseq
	 * @return 
	 */
	private SeqSymmetry findMate(TypeContainerAnnot t, BioSeq aseq) {
		TypeContainerAnnot result = new TypeContainerAnnot(t.getType()+"bampair","bampair");
		for (int i = 0; i < t.getChildCount() - 1; i+=2) {
			SeqSymmetry child1 = t.getChild(i);
			SeqSymmetry child2 = t.getChild(i+1);
			SeqSymmetry union = SeqUtils.union(child1, child2, aseq);
			SimpleSymWithProps container = new SimpleSymWithProps();
			container.addChild(child1);
			container.addChild(child2);
			container.addSpan(union.getSpan(aseq));
			
			result.addChild(container);
		}
		// copy spans
		for (int i = 0; i < t.getSpanCount(); i++) {
			result.addSpan(t.getSpan(i));
		}
		return result;
	}
}
