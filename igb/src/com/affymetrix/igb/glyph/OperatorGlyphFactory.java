
package com.affymetrix.igb.glyph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;

import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;


/**
 *
 * @author hiralv
 */
public class OperatorGlyphFactory implements MapViewGlyphFactoryI {
	private final Operator operator;
	private final MapViewGlyphFactoryI factory;
	
	public OperatorGlyphFactory(Operator operator, MapViewGlyphFactoryI factory){
		this.operator = operator;
		this.factory = factory;
	}
	
	public void init(Map<String, Object> options) { }
	
	public String getName() {
		return operator.getName();
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym, ITrackStyleExtended style, Direction tier_direction) {
		final String meth = BioSeq.determineMethod(sym);

		if (meth == null) {
			return factory.getViewModeGlyph(sym, style, tier_direction);
		} else {

			if (!style.getSeparate()) {
				List<SeqSymmetry> list = new ArrayList<SeqSymmetry>(1);
				list.add(sym);
				return createGlyph(list, meth, style, Direction.BOTH);
			} else {
				List<List<SeqSymmetry>> lists = getChilds(factory.getSeqMapView(), sym);
				if(Direction.FORWARD == tier_direction){
					return createGlyph(lists.get(0), meth, style, tier_direction);
				}else{
					return createGlyph(lists.get(1), meth, style, tier_direction);
				}
					
			}
		}
	}

	public void createGlyph(final SeqSymmetry sym, final SeqMapViewExtendedI smv) {
		//not implemented
	}

	private ViewModeGlyph createGlyph(List<SeqSymmetry> list, String meth, ITrackStyleExtended style, Direction direction) {
	
		SymWithProps result_sym = (SymWithProps) operator.operate(factory.getSeqMapView().getAnnotatedSeq(), list);

		if (result_sym != null) {
			result_sym.setProperty("method", meth);
			if (result_sym.getProperty("id") == null) {
				result_sym.setProperty("id", meth);
			}
		}

		return factory.getViewModeGlyph(result_sym, style, direction);
	}

	public boolean isFileSupported(String fileFormat) {
		if(fileFormat == null) {
			return false;
		}
		FileTypeHandler fth = FileTypeHolder.getInstance().getFileTypeHandler(fileFormat);
		FileTypeCategory category = fth.getFileTypeCategory();
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
	
	@Override
	public final SeqMapViewExtendedI getSeqMapView(){
		return factory.getSeqMapView();
	}
}
