/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometryImpl;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.CopyOnWriteArraySet;
import com.affymetrix.genometryImpl.event.SymMapChangeEvent;
import com.affymetrix.genometryImpl.event.SymMapChangeListener;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.StateProvider;
import com.affymetrix.genometryImpl.util.SynonymLookup;
/**
 *
 * @version $Id$
 */
public class AnnotatedSeqGroup {

	final private String id;
	private String organism;
	private String description;
	private String source; //as in Das2 server name
	final private List<GenericVersion> gVersions = new ArrayList<GenericVersion>();
	private boolean use_synonyms;
	final private Map<String, BioSeq> id2seq;
	private ArrayList<BioSeq> seqlist; //lazy copy of id2seq.values()
	private boolean id2seq_dirty_bit; // used to keep the lazy copy
	final private TreeMap<String,Set<SeqSymmetry>> id2sym_hash;	// list of names -> sym
	final private TreeMap<String,Set<String>> symid2id_hash;	// main sym id -> list of other names
	final private static Set<SymMapChangeListener> sym_map_change_listeners = new CopyOnWriteArraySet<SymMapChangeListener>();
	private HashMap<String, Integer> type_id2annot_id = new HashMap<String, Integer>();
	
	
	/**
	 * Private copy of the synonym lookup table.
	 * @see com.affymetrix.genometryImpl.util.SynonymLookup#getDefaultLookup()
	 */
	private final static SynonymLookup lookup = SynonymLookup.getDefaultLookup();

	public AnnotatedSeqGroup(String gid) {
		id = gid;
		use_synonyms = true;
		id2seq = Collections.<String, BioSeq>synchronizedMap(new LinkedHashMap<String, BioSeq>());
		id2seq_dirty_bit = false;
		seqlist = new ArrayList<BioSeq>();
		id2sym_hash = new TreeMap<String,Set<SeqSymmetry>>();
		symid2id_hash = new TreeMap<String,Set<String>>();
	}

	final public String getID() {
		return id;
	}

	final public void setSource(String source) {
		this.source = source;
	}

	final public void setDescription(String str) {
		description = str;
	}

	final public String getDescription() {
		return description;
	}

	/** may want to move set/getOrganism() to an AnnotatedGenome subclass */
	final public void setOrganism(String org) {
		organism = org;
	}

	final public String getOrganism() {
		return organism;
	}

	final public void addVersion(GenericVersion gVersion) {
		if (!this.gVersions.contains(gVersion)) {
			this.gVersions.add(gVersion);
		}
	}

	final public List<GenericVersion> getVersions() {
		return this.gVersions;
	}
	

	final public void addType(String type, Integer annot_id) {
		type_id2annot_id.put(type, annot_id);
	}
	
	public final Set<String> getTypeList() {
		return type_id2annot_id.keySet();
	}

	public final Integer getAnnotationId(String type) {
		return type_id2annot_id.get(type);
	}
	
	public final boolean isAuthorized(AnnotSecurity annotSecurity, String type) {
		 boolean isAuthorized =  annotSecurity.isAuthorized(this.getID(), type, getAnnotationId(type));
	     Logger.getLogger(AnnotatedSeqGroup.class.getName()).fine((isAuthorized ? "Showing  " : "Blocking ") + " Annotation " + type + " ID=" + getAnnotationId(type));
		 return isAuthorized;
	}
	
	public final Map<String, Object> getProperties(AnnotSecurity annotSecurity, String type) {
		Map<String, Object> props = annotSecurity.getProperties(this.getID(), type, getAnnotationId(type));
		return props;
	}
	
	public final boolean isBarGraphData(String data_root, AnnotSecurity annotSecurity, String type) {
		 return annotSecurity.isBarGraphData(data_root, this.getID(), type, getAnnotationId(type));
	}
	
	public final boolean isUseqGraphData(String data_root, AnnotSecurity annotSecurity, String type) {
		 return annotSecurity.isUseqGraphData(data_root, this.getID(), type, getAnnotationId(type));
	}
	

	
	/** By default, simply returns the global StateProvider, but subclasses
	 *  can implement a different one for each seq group.
	 */
	final public static StateProvider getStateProvider() {
		return DefaultStateProvider.getGlobalStateProvider();
	}

	/** Call this method if you alter the Map of IDs to SeqSymmetries.
	 *  @param source  The source responsible for the change, used in constructing
	 *    the {@link SymMapChangeEvent}.
	 */
	final public void symHashChanged(Object source) {
		for (SymMapChangeListener l : getSymMapChangeListeners()) {
			l.symMapModified(new SymMapChangeEvent(source, this));
		}
	}

	final private static Set<SymMapChangeListener> getSymMapChangeListeners() {
		return sym_map_change_listeners;
	}

	final public static void addSymMapChangeListener(SymMapChangeListener l) {
		sym_map_change_listeners.add(l);
	}

	final public static void removeSymMapChangeListener(SymMapChangeListener l) {
		sym_map_change_listeners.remove(l);
	}

	/**
	 *  Returns a List of BioSeq objects.
	 *  Will not return null.  The list is in the same order as in
	 *  {@link #getSeq(int)}.
	 */
	public List<BioSeq> getSeqList() {
		if (id2seq_dirty_bit) {
			// lazily keep the seqlist up-to-date
			seqlist = new ArrayList<BioSeq>(id2seq.values());
			id2seq_dirty_bit = false;
		}
		return Collections.<BioSeq>unmodifiableList(seqlist);
	}

	/**
	 *  Returns the sequence at the given position in the sequence list.
	 */
	public BioSeq getSeq(int index) {
		final List<BioSeq> seq_list = getSeqList();
		if (index < seq_list.size()) {
			return seq_list.get(index);
		}
		return null;
	}

	/** Returns the number of sequences in the group. */
	public int getSeqCount() {
		return id2seq.size();
	}

	/**
	 *  Sets whether or not to use the SynonymLookup class to search for synonymous
	 *  BioSeqs when using the getSeq(String) method.
	 *  If you set this to false and then add new sequences, you should probably
	 *  NOT later set it back to true unless you are sure you did not add
	 *  some synonymous sequences.
	 */
	public void setUseSynonyms(boolean b) {
		use_synonyms = b;
	}

	/** Gets a sequence based on its name, possibly taking synonyms into account.
	 *  See {@link #setUseSynonyms(boolean)}.
	 *
	 * @param synonym the string identifier of the requested BioSeq
	 * @return a BioSeq for the given synonym or null
	 */
	public BioSeq getSeq(String synonym) {
		BioSeq aseq = id2seq.get(synonym);
		if (use_synonyms && aseq == null) {
			// Try and find a synonym.
			for (String syn : lookup.getSynonyms(synonym,false)) {
				aseq = id2seq.get(syn);
				if (aseq != null) {
					return aseq;
				}
			}
		}
		return aseq;
	}

	/**
	 *  For the given symmetry, tries to find in the group a sequence
	 *    that is pointed to by that symmetry.
	 *  @return the first sequence it finds (by iterating through sym's spans),
	 *    or null if none is found.
	 * PRECONDITION: sym != null.
	 */
	public BioSeq getSeq(SeqSymmetry sym) {
		final int spancount = sym.getSpanCount();
		for (int i = 0; i < spancount; i++) {
			SeqSpan span = sym.getSpan(i);
			BioSeq seq1 = span.getBioSeq();
			String seqid = seq1.getID();
			BioSeq seq2 = id2seq.get(seqid);
			if ((seq2 != null) && (seq1 == seq2)) {
				return seq2;
			}
		}
		return null;
	}

	final public boolean isSynonymous(String synonym) {
		return id.equals(synonym) || lookup.isSynonym(id, synonym);
	}

	/**
	 *  Returns the BioSeq with the given id (or synonym), creating it if necessary,
	 *  and increasing its length to the given sym if necessary.
	 */
	public BioSeq addSeq(String seqid, int length) {
		if (seqid == null) {
			throw new NullPointerException();
		}

		BioSeq aseq = getSeq(seqid);
		if (aseq != null) {
			if (aseq.getLength() < length) {
				aseq.setLength(length);
			}
		} else {
			aseq = new BioSeq(seqid, this.getID(), length);
			this.addSeq(aseq);
		}
		return aseq;
	}

	/**
	 * Adds the BioSeq to the group.
	 */
	final public void addSeq(BioSeq seq) {
		final BioSeq oldseq = id2seq.get(seq.getID());
		if (oldseq == null) {
			id2seq_dirty_bit = true;
			id2seq.put(seq.getID(), seq);
			seq.setSeqGroup(this);
		} else {
			throw new RuntimeException("ERROR! tried to add seq: " + seq.getID() + " to AnnotatedSeqGroup: " +
					this.getID() + ", but seq with same id is already in group");
		}
	}

	/**
	 * @return list of SeqSymmetries matching the pattern.
	 */
	final public List<SeqSymmetry> findSyms(Pattern regex) {
		final HashSet<SeqSymmetry> symset = new HashSet<SeqSymmetry>();
		final Matcher matcher = regex.matcher("");

		for (Map.Entry<String, Set<SeqSymmetry>> ent : id2sym_hash.entrySet()) {
			String seid = ent.getKey();
			Set<SeqSymmetry> val = ent.getValue();
			if (seid != null && val != null) {
				matcher.reset(seid);
				if (matcher.matches()) {
					symset.addAll(val);
				}
			}
		}
		return new ArrayList<SeqSymmetry>(symset);
	}

	/** Finds all symmetries with the given case-insensitive ID.
	 *  @return a non-null List, possibly an empty one.
	 */
	final public Set<SeqSymmetry> findSyms(String id) {
		if (id == null) {
			return Collections.<SeqSymmetry>emptySet();
		}
		Set<SeqSymmetry> sym_list = id2sym_hash.get(id.toLowerCase());
		if (sym_list == null) {
			return Collections.<SeqSymmetry>emptySet();
		}
		return Collections.<SeqSymmetry>unmodifiableSet(sym_list);
	}

	/** Finds all symmetries with the given case-insensitive ID and add them to
	 *  the given list.
	 *  @param id  a case-insensitive id.
	 *  @param results  the list to which entries will be appended. It is responsibility of
	 *   calling code to clear out results list before calling this, if desired.
	 *  @param try_appended_id whether to also search for ids of the form
	 *   id + ".1", id + ".2", etc.
	 *  @return true if any symmetries were added to the list.
	 */
	//TODO: does this routine do what is expected?  What if id does not exist, but id + ".1" does?
	// What if id + ".1" does not exist, but id + ".2" does?
	// Does this list need to be in order?
	//TODO: This method is used only by ChpParser.  Move there.
	final public boolean findSyms(String id, List<SeqSymmetry> results, boolean try_appended_id) {
		if (id == null) {
			return false;
		}

		final String lid = id.toLowerCase();
		final Set<SeqSymmetry> seqsym_list = id2sym_hash.get(lid);
		if (seqsym_list != null) {
			results.addAll(seqsym_list);
			return true;
		}

		if (!try_appended_id) {
			return false;
		}

		return lookForAppendedIDs(lid, results);
	}

	// try id appended with ".n" where n is 0, 1, etc. till there is no match
	private boolean lookForAppendedIDs(String lid, List<SeqSymmetry> results) {
		boolean success = false;
		int postfix = 0;
		Set<SeqSymmetry> seq_sym_list;
		while ((seq_sym_list = id2sym_hash.get(lid + "." + postfix)) != null) {
			results.addAll(seq_sym_list);
			success = true;
			postfix++;
		}
		return success;
	}

	/**
	 *  Associates a symmetry with a case-insensitive ID.  You can later retrieve the
	 *  list of all matching symmetries for a given ID by calling findSyms(String).
	 *  Neither argument should be null.
	 */
	final public void addToIndex(String id, SeqSymmetry sym) {
		if (id == null || sym == null) {
			throw new NullPointerException();
		}
		this.putSeqInList(id.toLowerCase(), sym);
	}

	final public Set<String> getSymmetryIDs(String symID) {
		return this.symid2id_hash.get(symID);
	}


	/**
	 *  Returns input id if no GraphSyms on any seq in the given seq group
	 *  are already using that id.
	 *  Otherwise uses id to build a new unique id.
	 *  The id returned is unique for GraphSyms on all seqs in the given group.
	 */
	final public static String getUniqueGraphID(String id, AnnotatedSeqGroup seq_group) {
		String result = id;
		for (BioSeq seq : seq_group.getSeqList()) {
			result = getUniqueGraphID(result, seq);
		}
		return result;
	}

	/**
	 *  Returns input id if no GraphSyms on seq with given id.
	 *  Otherwise uses id to build a new id that is not used by a GraphSym (or top-level container sym )
	 *     currently on the seq.
	 *  The id returned is only unique for GraphSyms on that seq, may be used for graphs on other seqs.
	 */
	final public static String getUniqueGraphID(String id, BioSeq seq) {
		if (id == null) {
			return null;
		}
		if (seq == null) {
			return id;
		}

		int prevcount = 0;
		String newid = id;
		while (seq.getAnnotation(newid) != null) {
			prevcount++;
			newid = id + "." + prevcount;
		}
		return newid;
	}


	/**
	 * Function to add a SeqSymmetry to the id2sym_hash (and symid2id_hash).
	 * @param id ID string (lower-cased).
	 * @param sym SeqSymmetry to add to the hash.
	 */
	final private void putSeqInList(String id, SeqSymmetry sym) {
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

	/**
	 * Remove symmetry from seq group, if it exists.
	 * @param sym
	 */
	final public void removeSymmetry(SeqSymmetry sym) {
		if (sym == null || sym.getID() == null) {
			return;
		}
		String lcSymID = sym.getID().toLowerCase();
		Set<SeqSymmetry> symList = id2sym_hash.get(lcSymID);
		if (symList != null && symList.contains(sym)) {
			symList.remove(sym);
			if (symList.isEmpty()) {
				id2sym_hash.remove(lcSymID);
			}
		}
		symid2id_hash.remove(lcSymID);
	}

	/**
	 * Create a temporary shallow-copy genome, to avoid any side-effects.
	 * @param oldGenome
	 * @return
	 */
	public static AnnotatedSeqGroup tempGenome(AnnotatedSeqGroup oldGenome) {
		AnnotatedSeqGroup tempGenome = new AnnotatedSeqGroup(oldGenome.getID());
		tempGenome.setOrganism(oldGenome.getOrganism());
		if (oldGenome == null) {
			return tempGenome;
		}
		for (BioSeq seq : oldGenome.getSeqList()) {
			tempGenome.addSeq(seq.getID(), seq.getLength());
		}
		return tempGenome;
	}
}
