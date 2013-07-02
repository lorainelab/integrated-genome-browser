package com.affymetrix.genometry;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author hiralv
 */
public class Das2AnnotatedSeqGroup extends AnnotatedSeqGroup {
	private Map<String, SymLoader> type_id2symloader = null;
	private final TreeMap<String,Set<String>> symid2id_hash;	// main sym id -> list of other names
	private final TreeMap<String,Set<SeqSymmetry>> id2sym_hash;	// list of names -> sym
	
	public Das2AnnotatedSeqGroup(String id){
		super(id);
		symid2id_hash = new TreeMap<String,Set<String>>();
		id2sym_hash = new TreeMap<String,Set<SeqSymmetry>>();
	}
	
	@Override
	public Set<String> getSymmetryIDs(String symID) {
		return this.symid2id_hash.get(symID);
	}
		
	@Override
	public void addToIndex(String id, SeqSymmetry sym) {
		if (id == null || sym == null) {
			throw new NullPointerException();
		}
		this.putSeqInList(id.toLowerCase(), sym);
	}
	
	@Override
	protected void putSeqInList(String id, SeqSymmetry sym) {
		Set<SeqSymmetry> seq_list = id2sym_hash.get(id);
		if (seq_list == null) {
			seq_list = new LinkedHashSet<SeqSymmetry>();
			id2sym_hash.put(id,seq_list);
		}
		seq_list.add(sym);
		
		String lcSymID = sym.getID().toLowerCase();
		if (id.equals(lcSymID)) {
			return;
		}
		Set<String> id_list = symid2id_hash.get(lcSymID);
		if (id_list == null) {
			id_list = new HashSet<String>();
			symid2id_hash.put(lcSymID, id_list);
		}
		id_list.add(id);
	}
	
	@Override
	protected void removeSymmetry(String lcSymID, SeqSymmetry sym){
		symid2id_hash.remove(lcSymID);
	}
	
	public final void addSymLoader(String type, SymLoader value){
		if(type_id2symloader == null){
			type_id2symloader = new HashMap<String, SymLoader>();
		}
		type_id2symloader.put(type, value);
	}

	public final Set<String> getSymloaderList(){
		if(type_id2symloader == null){
			return Collections.<String>emptySet();
		}
		return type_id2symloader.keySet();
	}

	public final SymLoader getSymLoader(String type){
		if(type_id2symloader == null) {
			return null;
		}		
		return type_id2symloader.get(type);
	}

	public boolean  removeSymLoader(String type) {
		if(type_id2symloader == null || !type_id2symloader.containsKey(type)) {
			return false;
		}
		type_id2symloader.remove(type);
		return true;
	}
	
	@Override
	protected BioSeq createBioSeq(String seqid, String version, int length){
		return new Das2BioSeq(seqid, version, length);
	}

	@Override
	public Set<SeqSymmetry> findSyms(Pattern regex) {
		final Set<SeqSymmetry> symset = new HashSet<SeqSymmetry>();
		final Matcher matcher = regex.matcher("");
		Thread current_thread = Thread.currentThread();
		for (Map.Entry<String, Set<SeqSymmetry>> ent : id2sym_hash.entrySet()) {
			if (current_thread.isInterrupted()) {
				break;
			}

			String seid = ent.getKey();
			Set<SeqSymmetry> val = ent.getValue();
			if (seid != null && val != null) {
				matcher.reset(seid);
				if (matcher.matches()) {
					symset.addAll(val);
				}
			}
		}
		return symset;
	}
	
	public static AnnotatedSeqGroup tempGenome(AnnotatedSeqGroup oldGenome) {
		AnnotatedSeqGroup tempGenome = new Das2AnnotatedSeqGroup(oldGenome.getID());
		tempGenome.setOrganism(oldGenome.getOrganism());
		for (BioSeq seq : oldGenome.getSeqList()) {
			tempGenome.addSeq(seq.getID(), seq.getLength());
		}
		return tempGenome;
	}
}
