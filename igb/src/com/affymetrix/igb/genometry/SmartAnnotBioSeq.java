/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.genometry;

import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.event.SeqModifiedEvent;
import com.affymetrix.igb.event.SeqModifiedListener;

/**
 *   Extends NibbleBioSeq to add "retrieve top-level feature by 'method'/'type'".
 *
 *   Also imposes structure in the top two levels of annotation hierarchy.
 *   First level for a given type is a container symmetry with that type;
 *   second level is still containers, broken down by location, and dependent on how
 *      the annotatations were loaded.
 *
 *   Also adds reference to AnnotatedSeqGroup (getSeqGroup()), and
 *     isSynonymous() method.
 *
 */
public class SmartAnnotBioSeq extends NibbleBioSeq  {
  Map type2sym = null;   // lazy instantiation of type2sym hash...
  List modified_listeners = null;
  AnnotatedSeqGroup seq_group;
  boolean modify_events_enabled = true;
  boolean modification_cached = false;

  public SmartAnnotBioSeq() { }

  public SmartAnnotBioSeq(String seqid, String seqversion, int length) {
    super(seqid, seqversion, length);
  }

  public AnnotatedSeqGroup getSeqGroup() {
    return seq_group;
  }

  public void setSeqGroup(AnnotatedSeqGroup group) {
    seq_group = group;
  }

  public boolean isSynonymous(String synonym) {
    if (getID().equals(synonym)) { return true; }
    else {
      SynonymLookup lookup = SynonymLookup.getDefaultLookup();
      return (lookup.isSynonym(getID(), synonym));
    }
  }

  // not sure if should expose type2sym like this, maybe return just a list of type strings instead?
  public Map getTypes() { return type2sym; }

  /**
   *  Returns a top-level symmetry or null.
   *  Used to return a TypeContainerAnnot, but now returns a SymWithProps which is
   *     either a TypeContainerAnnot or a GraphSym, so GraphSyms can be retrieved with graph id given as type
   */
  public SymWithProps getAnnotation(String type) {
  //  public TypeContainerAnnot getAnnotation(String type) {
    if (type2sym == null) { return null; }
    //    return (TypeContainerAnnot)type2sym.get(type);
    return (SymWithProps)type2sym.get(type);
  }

  public void addModifiedListener(SeqModifiedListener listener) {
    if (modified_listeners == null) {
      modified_listeners = new ArrayList();
    }
    modified_listeners.add(listener);
  }

  public void removeModifiedListener(SeqModifiedListener listener) {
    if (modified_listeners != null) {
      modified_listeners.remove(listener);
      if (modified_listeners.isEmpty()) {
	// null out listeners list if no more listeners
	modified_listeners = null;
      }
    }
  }

  /**
   *  Toggling notification of listeners to seq modification events.
   *  If notification is turned off, then seq modifications will be accumulated
   *  and passed as a single modification event when notification is turned back on.
   */
  public void setModifyEventsEnabled(boolean b) {
    modify_events_enabled = b;
    if (modify_events_enabled && modification_cached) {
      notifyModified();
    }
  }

  protected void notifyModified()  {
    if (modified_listeners != null) {
      if (modify_events_enabled) {
	SeqModifiedEvent evt = new SeqModifiedEvent(this);
	notifyModified(evt);
      }
      else {
	modification_cached = true;
      }
    }
  }

  protected void notifyModified(SeqModifiedEvent evt) {
    if (! modify_events_enabled) {
      throw new RuntimeException("ERROR: SmartAnnotBioSeq.notifyModified() called, but " +
				 "modify_events_enabled flag == false");
    }
    if (modified_listeners != null) {
      System.out.println("SeqModifiedEvent occurred on " + this.getID() + " notifying listeners");
      Iterator iter = modified_listeners.iterator();
      while (iter.hasNext()) {
	SeqModifiedListener listener = (SeqModifiedListener)iter.next();
	listener.seqModified(evt);
      }
    }
    modification_cached = false;
  }

  /**
   *  Creates an empty top-level container sym.
   *  @return an instance of {@link TypeContainerAnnot}
   */
  public MutableSeqSymmetry addAnnotation(String type) {
    if (type2sym == null) { type2sym = new HashMap(); }
    MutableSeqSymmetry container = new TypeContainerAnnot(type);
    ((SymWithProps)container).setProperty("method", type);
    SeqSpan span = new SimpleSeqSpan(0, this.getLength(), this);
    container.addSpan(span);
    type2sym.put(type, container);
    super.addAnnotation(container);
    notifyModified();
    return container;
  }

  /**
   *  Adds an annotation as a child of the top-level container sym
   *     for the given type.  Creates new top-level container
   *     if doesn't yet exist.
   */
  public void addAnnotation(SeqSymmetry sym, String type) {
    if (type2sym == null) { type2sym = new HashMap(); }
    MutableSeqSymmetry container = (MutableSeqSymmetry)type2sym.get(type);
    if (container == null) {
      container = addAnnotation(type);
    }
    container.addChild(sym);
    notifyModified();
  }


  /**
   *  Overriding addAnnotation(sym) to try and extract a "method"/"type" property
   *    from the sym.
   *  <pre>
   *    If can be found, then instead of adding annotation directly
   *    to seq, use addAnnotation(sym, type).  Which ends up adding the annotation
   *    as a child of a container annotation (generally means two levels of container,
   *    since parsers call addAnnotation with a container already if indicated).
   *    So for example for DAS transcript-exon annotation will get a four-level
   *    hierarchy:
   *       1. Top-level container annot per seq
   *       2. 2nd-level container annot per DAS call (actually probably special DasFeatureRequestSym
   *       3. Transcript syms
   *       4. Exon syms
   *
   *  GraphSym's are added directly, not in containers.
   *  </pre>
   */
  public void addAnnotation(SeqSymmetry sym) {
    // add graphs directly as annotations
    // GAH 3-20-2006  now adding graphs to type2sym hash also, with id of graph used as type
    if (sym instanceof GraphSym) {
      if (type2sym == null) { type2sym = new HashMap(); }
      String id = sym.getID();
      if (id == null) {
	System.out.println("WARNING: GraphSym ID is null!!!");
	throw new RuntimeException("GraphSym ID is null, this should never happen!");
      }
      type2sym.put(id, sym);
      super.addAnnotation(sym);
      notifyModified();
      return;
    }
    String type = null;
    if (sym instanceof TypedSym)  {
      type = ((TypedSym)sym).getType();
    }
    // add other SymWithProps with a "method" property as children
    //   of a top-level container
    if (type == null && sym instanceof SymWithProps)  {
      SymWithProps swp = (SymWithProps)sym;
      // TODO: use the existing determineMethod() method
      type = (String)swp.getProperty("method");
      if (type == null) { type = (String)swp.getProperty("type"); }
      if (type == null) { type = (String)swp.getProperty("meth"); }
      //      else { super.addAnnotation(sym); }
    }
    if (type != null)  {
      // add as child to the top-level container
      addAnnotation(sym, type); // side-effect calls notifyModified()
      return;
    }
    //    else { super.addAnnotation(sym); }  // this includes GraphSyms
    else  {
      throw new RuntimeException(
          "SmartAnnotBioSeq.addAnnotation(sym) will only accept " +
          " SeqSymmetries that are also SymWithProps and " +
          " have a _method_ property");
    }
  }

  public void removeAnnotation(SeqSymmetry annot) {
    // special handling for GraphSyms
    if (annot instanceof GraphSym) {
      super.removeAnnotation(annot);
      notifyModified();
      return;
    }
    else if (annot instanceof SymWithProps) {
      if (type2sym != null) {
        // TODO: addAnnotation and removeAnnotation use different method to get "method" !??
	String type = (String)(((SymWithProps)annot).getProperty("method"));
	if (type != null) {
	  if (type2sym.get(type) == annot) {
	    //	    type2sym.remove(annot);
	    type2sym.remove(type);
	    super.removeAnnotation(annot);
	    notifyModified();
	    return;
	  }
	}
      }
    }
    throw new RuntimeException("SmartAnnotBioSeq.removeAnnotation(sym) not yet allowed " +
			       "except when sym is top-level annotation " +
			       "(container or graph)" );
  }

  public void removeAnnotation(int index) {
    SeqSymmetry annot = getAnnotation(index);
    removeAnnotation(annot);  // this will handle super call, removal from type2sym, notification, etc.
  }


}
