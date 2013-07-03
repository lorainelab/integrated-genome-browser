
package com.affymetrix.genometry;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometry.util.IndexedSyms;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author hiralv
 */
public class Das2BioSeq extends BioSeq {
	private Map<String, IndexedSyms> type_id2indexedsym = null;
	
	public Das2BioSeq(String seqid, String seqversion, int length) {
		super(seqid, seqversion, length);
	}
	
	@Override
	public synchronized void addAnnotation(SeqSymmetry sym, String ext, boolean index) {
		super.addAnnotation(sym, ext, false);
	}
	
	/**
	 * Add an indexed collection to id2indexedsym.
	 * @param type ID string.
	 * @param value indexedSyms to add to the hash.
	 */
	public final void addIndexedSyms(String type, IndexedSyms value) {
		if(type_id2indexedsym == null){
			type_id2indexedsym = new HashMap<String, IndexedSyms>();
		}
		type_id2indexedsym.put(type,value);
	}

	public final Set<String> getIndexedTypeList() {
		if(type_id2indexedsym == null){
			return Collections.<String>emptySet();
		}
		return type_id2indexedsym.keySet();
	}

	public final IndexedSyms getIndexedSym(String type) {
		if(type_id2indexedsym == null){
			return null;
		}
		return type_id2indexedsym.get(type);
	}
	
	public boolean removeIndexedSym(String type) {
		if(type_id2indexedsym == null || !type_id2indexedsym.containsKey(type)) {
			return false;
		}
		type_id2indexedsym.remove(type);
		return true;
	}
}
