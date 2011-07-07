package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.MisMatchGraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;

import com.affymetrix.igb.tiers.TierGlyph.Direction;
import com.affymetrix.igb.view.load.GeneralLoadView;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class DependentData {

	static public enum DependentType {

		SUMMARY,
		COVERAGE,
		MISMATCH
	}

	final private String id;
	final private String parent_method;
	final private DependentType type;
	private Direction direction;
	private SymWithProps sym;

	public DependentData(String id, DependentType type, String parentUrl) {
		this.id = id;
		this.parent_method = parentUrl;
		this.type = type;
	}

	public DependentData(String id, DependentType type, String parentUrl, Direction direction) {
		this(id, type, parentUrl);
		this.direction = direction;
	}

	public SymWithProps createTier(BioSeq aseq) {

		SymWithProps temp = sym;

		if (type == DependentType.MISMATCH){
			sym = createMisMatchGraph(aseq);
		}else if (type == DependentType.SUMMARY) { //Check if type is summary.
			sym = createSummaryGraph(aseq);
		} else {	//If type is not summary then it should be coverage.
			sym = createCoverageTier(aseq);
		}

		if(temp != null){
			aseq.removeAnnotation(temp);
		}

		aseq.addAnnotation(sym);
		return sym;
	}

	private GraphSym createMisMatchGraph(BioSeq aseq){
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
		SeqSymmetry tsym = aseq.getAnnotation(parent_method);
		syms.add(tsym);
		
		int[] startEnd = getStartEnd(tsym, aseq);
		SeqSpan loadSpan = new SimpleSeqSpan(startEnd[0], startEnd[1], aseq);

		//Load Residues
		if(!aseq.isAvailable(loadSpan)){
			if(!GeneralLoadView.getLoadView().loadResidues(loadSpan, true)){
				return (GraphSym) sym;
			}
		}
		
		MisMatchGraphSym mgsym = SeqSymSummarizer.getMismatchGraph(syms, aseq, false, id, startEnd[0], startEnd[1]);

		return mgsym;
	}

	public static int[] getStartEnd(SeqSymmetry tsym, BioSeq aseq){
		int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;

		for(int i=0; i<tsym.getChildCount(); i++){
			SeqSymmetry childSym = tsym.getChild(i);
			SeqSpan span = childSym.getSpan(aseq);
			if(span.getMax() > end){
				end = span.getMax();
			}

			if(span.getMin() < start){
				start = span.getMin();
			}
		}

		return new int[]{start, end};
	}
	
	private GraphSym createSummaryGraph(BioSeq aseq){
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
		syms.add(aseq.getAnnotation(parent_method));
		GraphSym gsym = null;
		if (direction == Direction.FORWARD || direction == Direction.REVERSE) {
			Boolean isForward = direction == Direction.FORWARD ? true : false;
			gsym = SeqSymSummarizer.getSymmetrySummary(syms, aseq, false, id, isForward);
		} else {
			gsym = SeqSymSummarizer.getSymmetrySummary(syms, aseq, false, id);
		}

		gsym.setID(id);
		
		return gsym;
	}

	private SymWithProps createCoverageTier(BioSeq aseq) {
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
		syms.add(aseq.getAnnotation(parent_method));
		SeqSymmetry union_sym = SeqSymSummarizer.getUnion(syms, aseq);
		SymWithProps wrapperSym;
		if (union_sym instanceof SymWithProps) {
			wrapperSym = (SymWithProps) union_sym;
		} else {
			wrapperSym = new SimpleSymWithProps();
			((SimpleSymWithProps) wrapperSym).addChild(union_sym);
			for (int i = 0; i < union_sym.getSpanCount(); i++) {
				((SimpleSymWithProps) wrapperSym).addSpan(union_sym.getSpan(i));
			}
		}
		wrapperSym.setProperty("method", id);
		wrapperSym.setProperty("id", id);
		
		return wrapperSym;
	}

	public String getParentMethod(){
		return parent_method;
	}

	public String getID(){
		return id;
	}

	public DependentType getType(){
		return type;
	}

	public Direction getDirection(){
		return direction;
	}

	public SymWithProps getSym(){
		return sym;
	}
}
