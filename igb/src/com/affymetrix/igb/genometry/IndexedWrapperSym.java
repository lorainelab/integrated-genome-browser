package com.affymetrix.igb.genometry;

import java.util.*;

import com.affymetrix.genometry.*;

/**
 *  IndexedWrapperSym wraps an existing SymWithProps and adds the notion of a ScoredContainerSym 
 *     parent and an index into that parent 
 *    (usually the index such that parent.getChild(index) == parent)
 */
public class IndexedWrapperSym implements IndexedSym, SymWithProps  {
  SeqSymmetry wrapped_sym;
  ScoredContainerSym parent_sym;
  int index_in_parent = -1;

  public IndexedWrapperSym(SeqSymmetry sym) {
    wrapped_sym = sym;
  }

  public SeqSymmetry getWrappedSym() { return wrapped_sym; }

  /*  
   *  IndexedSym implementation
   */

  /**
   *  note that setParent() of IndexedWrapperSym does _not_ set a parent sym for the 
   *    wrapped_sym, therefore it is possible for the IndexedWrapperSym to have a different
   *    parent than the wrapped symmetry (if the wrapped symmetry has a parent at all)
   */
  public void setParent(ScoredContainerSym parent) {
    parent_sym = parent;
  }
  public ScoredContainerSym getParent() {
    return parent_sym;
  }
  public void setIndex(int index) {
    index_in_parent = index;
  }
  public int getIndex() {
    return index_in_parent;
  }

  /* 
   * SeqSymmetry implementation -- pass-through methods to wrapped symmetry 
   */
 
  public String getID() {
    return wrapped_sym.getID();
  }
  public int getSpanCount() {
    return wrapped_sym.getSpanCount();
  }
  public SeqSpan getSpan(BioSeq seq) {
    return wrapped_sym.getSpan(seq);
  }
  public SeqSpan getSpan(int index) {
    return wrapped_sym.getSpan(index);
  }
  public boolean getSpan(BioSeq seq, MutableSeqSpan span) {
    return wrapped_sym.getSpan(seq, span);
  }
  public boolean getSpan(int index, MutableSeqSpan span) {
    return wrapped_sym.getSpan(index, span);
  }
  public BioSeq getSpanSeq(int index) {
    return wrapped_sym.getSpanSeq(index);
  }
  public int getChildCount() {
    return wrapped_sym.getChildCount();
  }
  public SeqSymmetry getChild(int index) {
    return wrapped_sym.getChild(index);
  }

  /*
   * Propertied implementation -- pass-through methods to wrapped symmetry
   */

  /** currently not including index as a property */
  public Map getProperties() { 
    if (wrapped_sym instanceof SymWithProps) {
      return ((SymWithProps)wrapped_sym).getProperties(); 
    }
    else { return null; }
  }

  /** currently not including index as a property */
  public Map cloneProperties() { 
    if (wrapped_sym instanceof SymWithProps) {
      return ((SymWithProps)wrapped_sym).getProperties(); 
    }
    else { return null; }
  }

  /** currently not including index as a property */
  public Object getProperty(String key) { 
    if (wrapped_sym instanceof SymWithProps) {
      return ((SymWithProps)wrapped_sym).getProperty(key); 
    }
    else { return null; }
  }
  
  public boolean setProperty(String key, Object val) { 
    if (wrapped_sym instanceof SymWithProps) {
      return ((SymWithProps)wrapped_sym).setProperty(key, val); 
    }
    else { return false; }
  }
}
