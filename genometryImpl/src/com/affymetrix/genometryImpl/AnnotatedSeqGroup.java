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
import java.util.regex.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.event.*;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.StateProvider;
import com.affymetrix.genometryImpl.util.SynonymLookup;

public class AnnotatedSeqGroup {

  String id;
  String organism;
  String version; // not currently used?
  //Date version_date;  // not currently used
  String description;

  boolean use_synonyms = true;

  Map<String,MutableAnnotatedBioSeq> id2seq = new LinkedHashMap<String,MutableAnnotatedBioSeq>();//TODO: require SmartAnnotBioSeq
  ArrayList<MutableAnnotatedBioSeq> seqlist = new ArrayList<MutableAnnotatedBioSeq>();

  static Vector<SymMapChangeListener> sym_map_change_listeners = new Vector<SymMapChangeListener>(1);
  
  SortedMap<String,Object> id2sym_hash = new ListmakingHashMap();

  public AnnotatedSeqGroup(String gid) {
    id = gid;
  }

  public String getID() { return id; }

  /**
   *  Returns a List of MutableAnnotatedBioSeq objects.
   *  Will not return null.  The list is in the same order as in
   *  {@link #getSeq(int)}.
   */
  public List<MutableAnnotatedBioSeq> getSeqList() {
    if (seqlist.size() != id2seq.size()) {
      // lazily keep the seqlist up-to-date
      seqlist = new ArrayList<MutableAnnotatedBioSeq>(id2seq.values());
    }
    return seqlist;
  }

  /**
   *  Returns the set of type id String's of all the types on all
   *  the SmartAnnotBioSeq's returned by getSeqList().
   */
  public Set<String> getTypeIds() {
    Set<String> types = new TreeSet<String>();
    List<MutableAnnotatedBioSeq> seq_list = getSeqList();
    for (MutableAnnotatedBioSeq seq : seq_list) {
      if (seq instanceof SmartAnnotBioSeq) {
        types.addAll(((SmartAnnotBioSeq) seq).getTypeIds());
      }
    }
    return types;
  }
  
  public Set<String> getGraphTypeIds() {
    Set<String> types = new TreeSet<String>();
    for (MutableAnnotatedBioSeq seq : getSeqList()) {
      if (seq instanceof SmartAnnotBioSeq) {
        types.addAll(((SmartAnnotBioSeq) seq).getGraphTypeIds());
      }
    }
    return types;
  }

  /**
   *  Returns true if any seq in the group contains an annotation of the given type.
   *  Equivalent to getTypeIds().contains(type), but usually faster.
   */
  public boolean hasType(String type) {
    if (type == null) {
      return false;
    }
    List seq_list = getSeqList();
    Iterator iter = seq_list.iterator();
    while (iter.hasNext()) {
      MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq) iter.next();
      if (seq instanceof SmartAnnotBioSeq) {
        if (((SmartAnnotBioSeq) seq).getTypeIds().contains(type.toLowerCase())) {
          return true;
        }
      }
    }
    return false;
  }
  
  /**
   *  Remove all annotations of a given type from all seqs in this group.
   */
  public void removeType(String type) {
    List seq_list = getSeqList();
    Iterator iter = seq_list.iterator();
    while (iter.hasNext()) {
      MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq) iter.next();
      if (seq instanceof SmartAnnotBioSeq) {
        ((SmartAnnotBioSeq) seq).removeType(type.toLowerCase());
      }
    }
  }

  public String getUniqueTypeID(String id) {
    if (id == null) { return null; }
    String newid = id;
    int prevcount = 0;
    while (hasType(newid)) {
      prevcount++;
      newid = id + "." + prevcount;
    }
    return newid;
  }

  /**
   *  Returns the sequence at the given position in the sequence list.
   */
  public MutableAnnotatedBioSeq getSeq(int index) {
    List the_list = getSeqList();
    if (index < the_list.size()) {
      return (MutableAnnotatedBioSeq) the_list.get(index);
    }
    else { return null; }
  }

  /** Returns the number of sequences in the group. */
  public int getSeqCount() {
    return id2seq.size();
  }

  /**
   *  Sets whether or not to use the SynonymLookup class to search for synonymous
   *  BioSeq's when using the getSeq(String) method.
   *  If you set this to false and then add new sequences, you should probably
   *  NOT later set it back to true unless you are sure you did not add
   *  some synonymous sequences.
   */
  public void setUseSynonyms(boolean b) {
    use_synonyms = b;
  }

  /** Gets a sequence based on its name, possibly taking synonyms into account.
   *  See {@link #setUseSynonyms(boolean)}.
   */
  public MutableAnnotatedBioSeq getSeq(String synonym) {
    MutableAnnotatedBioSeq aseq = id2seq.get(synonym);
    if (use_synonyms && aseq == null) {
      // try and find a synonym
      SynonymLookup lookup = SynonymLookup.getDefaultLookup();

      for (MutableAnnotatedBioSeq synseq : id2seq.values()) {
        if (synseq instanceof SmartAnnotBioSeq)  {
          if (((SmartAnnotBioSeq)synseq).isSynonymous(synonym)) {
            aseq = synseq;
            break;
          }
        } else {
          if (lookup.isSynonym(synseq.getID(), synonym)) {
            aseq = synseq;
            break;
          }
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
   */
  public MutableAnnotatedBioSeq getSeq(SeqSymmetry sym) {
    MutableAnnotatedBioSeq result = null;
    if (sym != null) {
      int spancount = sym.getSpanCount();
      for (int i=0; i<spancount; i++) {
        SeqSpan span = sym.getSpan(i);
        BioSeq seq1 = span.getBioSeq();
        String seqid = seq1.getID();
        MutableAnnotatedBioSeq seq2 = id2seq.get(seqid);
        if ((seq2 != null) && (seq1 == seq2)) {
          result = seq2;
          break;
        }
      }
    }
    return result;
  }

  public boolean isSynonymous(String synonym) {
    if (id.equals(synonym)) { return true; }
    else {
      SynonymLookup lookup = SynonymLookup.getDefaultLookup();
      return (lookup.isSynonym(id, synonym));
    }
  }

  /**
   *  Returns the BioSeq with the given id (or synonym), creating it if necessary,
   *  and increasing its length to the given value if necessary.
   */
  public SmartAnnotBioSeq addSeq(String seqid, int length) {
    if (seqid == null) {
      throw new NullPointerException();
    }
    SmartAnnotBioSeq aseq;
    aseq = (SmartAnnotBioSeq) getSeq(seqid);
    if (aseq != null) {
      if (aseq.getLength() < length) {
        aseq.setLength(length);
      }
    }
    else {
      aseq = new SmartAnnotBioSeq(seqid, this.getID(), length);
      this.addSeq(aseq);
    }
    return aseq;
  }

  public void addSeq(MutableAnnotatedBioSeq seq) {
    // It would be nice to require that all children seqs be SmartAnnotBioSeqs,
    // but there is still some code in the internal Affy code that might be
    // adding Combosite BioSeq's instead.
    MutableAnnotatedBioSeq oldseq = id2seq.get(seq.getID());
    if (oldseq == null) {
      id2seq.put(seq.getID(), seq);
      //seqlist.add(seq); // don't add to seqlist, to keep it properly sorted, rebuild it only when needed
      if (seq instanceof SmartAnnotBioSeq) {
        ((SmartAnnotBioSeq)seq).setSeqGroup(this);
      }
    }
    else {
      throw new RuntimeException("ERROR! tried to add seq: " + seq.getID() + " to AnnotatedSeqGroup: " +
        this.getID() + ", but seq with same id is already in group");
    }
  }

  public void setDescription(String str) { description = str; }
  public String getDescription() { return description; }

  /** may want to move set/getOrganism() to an AnnotatedGenome subclass */
  public void setOrganism(String org) {  organism = org; }
  public String getOrganism() { return organism; }

  /** Not currently used, may want to move getVersion() to an AnnotatedGenome subclass */
  public String getVersion() { return version; }

//  /** Not currently used, may want to move getVersionDate() to an AnnotatedGenome subclass */
//  public Date getVersionDate() { return version_date; }

  /** NOT YET IMPLEMENTED */  //TODO: Isn't this implemented?
  @SuppressWarnings("unchecked")
  public List<SeqSymmetry> findSyms(Pattern regex) {
    HashSet<SeqSymmetry> symset = new HashSet<SeqSymmetry>();
    Matcher matcher = regex.matcher("");

    for (Map.Entry<String,Object> ent : id2sym_hash.entrySet()) {
      String seid = ent.getKey();
      Object val = ent.getValue();
      if (seid != null && val != null) {
        matcher.reset(seid);
        if (matcher.matches()) {
          if (val instanceof List) {
            symset.addAll((List<SeqSymmetry>) val);
          } else {
            symset.add((SeqSymmetry) val);
          }
        }
      }
    }
    //    System.out.println("!!!! AnnotatedSeqGroup.findSyms(Pattern) called, syms found: " + symset.size());
    return new ArrayList<SeqSymmetry>(symset);
  }



  /** Finds all symmetries with the given case-insensitive ID.
   *  @return a non-null List, possibly an empty one.
   */
  @SuppressWarnings("unchecked")
  public List<SeqSymmetry> findSyms(String id) {
    if (id==null) {
      return Collections.<SeqSymmetry>emptyList();
    }
    List<SeqSymmetry> sym_list = null;
    Object o = id2sym_hash.get(id.toLowerCase());
    if (o == null) {
      sym_list = Collections.<SeqSymmetry>emptyList();
    } else if (o instanceof List) {
      sym_list = (List<SeqSymmetry>) o;
    } else {
      sym_list = new ArrayList<SeqSymmetry>(1);
      sym_list.add((SeqSymmetry) o);
    }
    return sym_list;
  }

  /** Finds all symmetries with the given case-insensitive ID and add them to
   *  the given list.  Also looks for id + ".1", id + ".2", etc.
   */
  public boolean findSyms(String id, List<SeqSymmetry> results) {
    return findSyms(id, results, true);
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
  @SuppressWarnings("unchecked")
  public boolean findSyms(String id, List<SeqSymmetry> results, boolean try_appended_id) {
    boolean success = false;
    if (id != null) {
      String lid = id.toLowerCase();
      Object obj = id2sym_hash.get(lid);
      if (obj instanceof SeqSymmetry) {
        results.add((SeqSymmetry) obj);
        success = true;
      }
      else if (obj instanceof List) {
        results.addAll((List<SeqSymmetry>) obj);
        success = true;
      }
      // try id appended with ".n" where n is 0, 1, etc. till there is no match
      else if (obj == null && try_appended_id) {
        int postfix = 0;
        Object appendobj;
        while ( (appendobj = id2sym_hash.get(lid + "." + postfix)) != null) {
          if (appendobj instanceof SeqSymmetry) {
            results.add((SeqSymmetry) appendobj);
            success = true;
          }
          else if (appendobj instanceof List) {
            List<SeqSymmetry> syms = (List<SeqSymmetry>) appendobj;
            results.addAll(syms);
            success = true;
          }
          postfix++;
        }
      }
    }
    return success;
  }

  /**
   *  Assosicates a symmetry with a case-insensitive ID.  You can later retrieve the
   *  list of all matching symmetries for a given ID by calling findSyms(String).
   *  Niether argument should be null.
   */
  public void addToIndex(String id, SeqSymmetry sym) {
    if (id==null || sym==null) {
      throw new NullPointerException();
    }
    id2sym_hash.put(id.toLowerCase(), sym);
  }

  /** Returns a set of the String IDs that have been added to the ID index using
   *  addToIndex(String, SeqSymmetry).  The IDs will be returned in lower-case.
   *  Each of the keys can be used as a parameter for the findSyms(String) method.
   */
  public Set<String> getSymmetryIDs() {
    return id2sym_hash.keySet();
  }

  /** Returns a set of the String IDs alphabetically between start and end
   *  that have been added to the ID index using
   *  addToIndex(String, SeqSymmetry).  The IDs will be returned in lower-case.
   *  Each of the keys can be used as a parameter for the findSyms(String) method.
   *  @param start  A String indicating the lowest index value; null or empty start
   *   string will get all index strings up to the given end value.
   *  @param end    A String indicating the highest index value; null or empty end
   *   string will get all index strings above the given start value.
   */
  public Set<String> getSymmetryIDs(String start, String end) {
    if (start == null) { start = ""; }
    if (end == null) { end = ""; }

    start = start.toLowerCase();
    end = end.toLowerCase();

    if (start.equals("") && end.equals("")) {
      return getSymmetryIDs();
    } else if (start.equals("")) {
      return id2sym_hash.headMap(end).keySet();
    } else if (end.equals("")) {
      return id2sym_hash.tailMap(start).keySet();
    } else {
      return id2sym_hash.subMap(start, end).keySet();
    }
  }

  /** Call this method if you alter the Map of IDs to SeqSymmetries.
   *  @param source  The source responsible for the change, used in constructing
   *    the {@link SymMapChangeEvent}.
   */
  public void symHashChanged(Object source) {
    List list = getSymMapChangeListeners();
    for (int i=0; i<list.size(); i++) {
      SymMapChangeListener l = (SymMapChangeListener) list.get(i);
      l.symMapModified(new SymMapChangeEvent(source, this));
    }
  }

  public static List getSymMapChangeListeners() {
    return sym_map_change_listeners;
  }

  public static void addSymMapChangeListener(SymMapChangeListener l) {
    sym_map_change_listeners.add(l);
  }

  public static void removeSymMapChangeListener(SymMapChangeListener l) {
    sym_map_change_listeners.remove(l);
  }

  /**
   *  Returns input id if no GraphSyms on any seq in the given seq group
   *  are already using that id.
   *  Otherwise uses id to build a new unique id.
   *  The id returned is unique for GraphSyms on all seqs in the given group.
   */
  public static String getUniqueGraphID(String id, AnnotatedSeqGroup seq_group) {
    String result = id;
    Iterator iter = seq_group.getSeqList().iterator();
    while (iter.hasNext()) {
      AnnotatedBioSeq seq = (AnnotatedBioSeq) iter.next();
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
  public static String getUniqueGraphID(String id, BioSeq seq) {
    if (id == null) { return null; }
    String newid = id;
    if (seq instanceof SmartAnnotBioSeq) {
      SmartAnnotBioSeq sab = (SmartAnnotBioSeq)seq;
      int prevcount = 0;
      while (sab.getAnnotation(newid) != null) {
        prevcount++;
        newid = id + "." + prevcount;
      }
    }
    else if (seq instanceof AnnotatedBioSeq)  {
      AnnotatedBioSeq aseq = (AnnotatedBioSeq)seq;
      // check every annotation on seq, but assume graphs are directly attached to seq, so
      //   don't have to do recursive descent into children?
      // potentially really bad performance, but this is just a fallback -- most
      //      seqs that GraphSyms are being attached to will be SmartAnnotBioSeqs and dealt with
      //      in the other branch of the conditional
      int prevcount = 0;
      int acount = aseq.getAnnotationCount();
      boolean hit = true;
      while (hit) {
        hit = false;
        for (int i=0; i<acount; i++) {
          SeqSymmetry sym = aseq.getAnnotation(i);
          if ((sym instanceof GraphSym) && (newid.equals(sym.getID()))) {
              prevcount++;
              newid = id + "." + prevcount;
              hit = true;
              break;
          }
        }
      }
    }
    else {
      // if not an AnnotatedBioSeq, just return original ID for now.
      newid = id;
    }

    return newid;
  }
  
  /** By default, simply returns the global StateProvider, but subclasses
   *  can implement a different one for each seq group.
   */
  public StateProvider getStateProvider() {
    return DefaultStateProvider.getGlobalStateProvider();
  }

  /**
   *  A subclass of TreeMap that changes the behavior of put(key, value)
   *  If the given key is already associated with a value, 
   *  rather than replacing the previous value with the new value:
   *     if previous value is a List, add new value to end of list
   *     otherwise create new list, add previous value and new value to list, 
   *        and make the new list the value associated with the key
   *
   */
  @SuppressWarnings("unchecked")
  public static class ListmakingHashMap extends TreeMap<String,Object> {

    static final long serialVersionUID = 1L;

    @Override
    public Object put(String key, Object value) {
      Object x = this.get(key);
      if (x == value) {
        return x; // do not store the same value twice
      }

      if (value == null) {
        super.put(key, null);
      } else if (x == null) {
        super.put(key, value);
      } else if (x instanceof List) {
        List<Object> list_x = (List<Object>) x;
        if (! list_x.contains(value)) {
          list_x.add(value);
        }
      } else {
        ArrayList<Object> al = new ArrayList<Object>(2);
        al.add(x);
        al.add(value);
        super.put(key, al);
      }
      return x;
    }

    // Does exactly the same thing as the superclass:
    // reports true if the value is contained directly as a value,
    // but not if the value is included inside one of the Lists
    @Override
    public boolean containsValue(Object value) {
      return super.containsValue(value);
    }
  }

}
