
package com.affymetrix.igb.viewmode;

import com.affymetrix.igb.shared.MapViewModeHolder;
import java.util.ArrayList;
import java.util.List;


import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;

import com.affymetrix.igb.shared.MapViewGlyphFactoryA;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;


/**
 *
 * @author hiralv
 */
public class OperatorGlyphFactory extends MapViewGlyphFactoryA {
	private final Operator operator;
	private final MapViewGlyphFactoryI factory;
	
	public OperatorGlyphFactory(Operator operator, MapViewGlyphFactoryI factory){
		this.operator = operator;
		if(factory.isCategorySupported(operator.getOutputCategory())){
			this.factory = factory;
		}else{
			this.factory = MapViewModeHolder.getInstance().getDefaultFactoryFor(operator.getOutputCategory());
		}
	}
	
	@Override
	public String getName() {
		return operator.getName();
	}

	public String getActualFactoryName(){
		return factory.getName();
	}
	
	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style, Direction tier_direction, SeqMapViewExtendedI smv) {
		final String meth = BioSeq.determineMethod(sym);

		if (meth == null) {
			return factory.getViewModeGlyph(sym, style, tier_direction, smv);
		} else {
			if (!style.getSeparate()) {
				List<SeqSymmetry> list = new ArrayList<SeqSymmetry>(1);
				list.add(sym);
				return getViewModeGlyph(list, meth, style, Direction.BOTH, smv);
			} else {
				List<List<SeqSymmetry>> lists = getChilds(smv, sym);
				if(Direction.FORWARD == tier_direction){
					return getViewModeGlyph(lists.get(0), meth, style, tier_direction, smv);
				}else{
					return getViewModeGlyph(lists.get(1), meth, style, tier_direction, smv);
				}
					
			}
		}
	}

	private ViewModeGlyph getViewModeGlyph(List<SeqSymmetry> list, String meth, ITrackStyleExtended style, Direction direction, SeqMapViewExtendedI smv) {
	
		SymWithProps result_sym = (SymWithProps) operator.operate(smv.getAnnotatedSeq(), list);
		SymWithProps output = result_sym;
		
		if (result_sym != null) {
			if(result_sym instanceof GraphSym){
				GraphSym graphSym = (GraphSym)result_sym;
				if(operator.getOperandCountMin(operator.getOutputCategory()) == 0){
					boolean isGraph = style.isGraphTier();
					graphSym.setID(meth);
					graphSym.setGraphState(graphSym.getGraphState());
					style.setGraphTier(isGraph); // may get modified above, so reset it back
				}
			}else if(!(result_sym instanceof RootSeqSymmetry)){
				TypeContainerAnnot container = new TypeContainerAnnot(meth);
				container.addChild(result_sym);
				output = container;
			}
			
			output.setProperty("method", meth);	
			output.setProperty("id", meth);
		}

		return factory.getViewModeGlyph(output, style, direction, smv);
	}

	@Override
	public boolean isCategorySupported(FileTypeCategory category) {
		if (operator.getOperandCountMin(category) > 1 || operator.getOperandCountMax(category) < 1) {
			return false;
		}
		for (FileTypeCategory checkCategory : FileTypeCategory.values()) {
			if (checkCategory != category && operator.getOperandCountMin(checkCategory) > 0) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean supportsTwoTrack() {
		return  operator.getOutputCategory() == FileTypeCategory.Annotation ||
				operator.getOutputCategory() == FileTypeCategory.Alignment;
	}

	private static List<List<SeqSymmetry>> getChilds(SeqMapViewExtendedI smv, SeqSymmetry parentSym){
		int initial_size = parentSym.getChildCount()/2;
		List<List<SeqSymmetry>> lists = new ArrayList<List<SeqSymmetry>>(2);
		List<SeqSymmetry> forward = new ArrayList<SeqSymmetry>(initial_size);
		List<SeqSymmetry> reverse = new ArrayList<SeqSymmetry>(initial_size);
		SeqSymmetry sym;
		SeqSpan span;
		for(int i=0; i<parentSym.getChildCount(); i++){
			sym = parentSym.getChild(i);
			span = smv.getViewSeqSpan(sym);
			
			if(span == null || span.getLength() == 0)
				continue;
			
			if(span.isForward()){
				forward.add(sym);
			}else{
				reverse.add(sym);
			}
		}
		lists.add(forward);
		lists.add(reverse);
		
		return lists;
	}
}
