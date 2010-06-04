package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.igb.tiers.TierGlyph.Direction;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class DependentData {

	static public enum DependentType {

		SUMMARY,
		COVERAGE
	}

	final private String id;
	final private String parentUrl;
	final private DependentType type;
	private Direction direction;
	private SymWithProps sym;

	public DependentData(String id, DependentType type, String parentUrl, SymWithProps sym) {
		this.id = id;
		this.parentUrl = parentUrl;
		this.type = type;
		this.sym = sym;
	}

	public DependentData(String id, DependentType type, String parentUrl, Direction direction, SymWithProps sym) {
		this(id, type, parentUrl, sym);
		this.direction = direction;
	}

	public SymWithProps createTier() {
		BioSeq aseq = GenometryModel.getGenometryModel().getSelectedSeq();
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
		syms.add(aseq.getAnnotation(parentUrl));
		if (type == DependentType.SUMMARY) {
			sym = SeqMapView.createSummaryGraph(id, syms, direction);
			return sym;
		}

		sym = SeqMapView.createCoverageTier(id, syms);
		return sym;
	}

	public String getParentUrl(){
		return parentUrl;
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
