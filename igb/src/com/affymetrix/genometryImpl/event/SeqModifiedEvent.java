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

package com.affymetrix.genometryImpl.event;

import java.util.EventObject;

import com.affymetrix.genometryImpl.SmartAnnotBioSeq;

/**
 *  Event representing a change to a SmartAnnotBioSeq;
 *    usually the addition of annotations.
 *
 *  If change is addition/removal/modification of annotations, 
 *    then SeqModifiedEvent holds pointer not to the annotations that 
 *    were added / modified, but rather to a list of the "top-level" (first or 
 *    second level) annotations that fully encompass the change
 */
public class SeqModifiedEvent extends EventObject {
  SmartAnnotBioSeq seq;

  public SeqModifiedEvent(SmartAnnotBioSeq modified_seq)  {
    this(modified_seq, modified_seq);
  }

  public SeqModifiedEvent(Object src, SmartAnnotBioSeq modified_seq)  {
    super(src);
    seq = modified_seq;
  }

  public SmartAnnotBioSeq getModifiedSeq() { return seq; }
}
