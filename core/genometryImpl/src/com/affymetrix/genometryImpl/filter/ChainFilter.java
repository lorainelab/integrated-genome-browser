package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class ChainFilter implements SymmetryFilterI {

	List<SymmetryFilterI> filters;
	
	@Override
	public String getDisplay() {
		return "Chain Filter";
	}
	
	@Override
	public String getName() {
		return "chain_filter";
	}
	
	public void setFilter(List<SymmetryFilterI> filters){
		this.filters = filters;
	}
	
	public List<SymmetryFilterI> getFilters(){
		return filters;
	}

	@Override
	public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym) {
		boolean allow = true;
		for(SymmetryFilterI filter : filters){
			allow &= filter.filterSymmetry(seq, sym);
			if(!allow){
				break;
			}
		}
		return allow;
	}

	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public SymmetryFilterI newInstance() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
