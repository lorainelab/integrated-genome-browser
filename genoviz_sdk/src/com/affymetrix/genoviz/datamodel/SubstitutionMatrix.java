/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.datamodel;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * represents an amino acid substitution scoring matrix.
 */
public class SubstitutionMatrix {

  Hashtable outer = new Hashtable();

  public SubstitutionMatrix() {
  }

  /**
   * establishes a score for the substition of amino acid a with amino acid b.
   *
   * @param a original amino acid
   * @param b substitued amino acid
   * @param score
   */
  public void put(char a, char b, double score) {
    Character obja = new Character(a);
    Character objb = new Character(b);
    Hashtable inner = (Hashtable) outer.get(obja);

    if (null == inner) {
      inner = new Hashtable();
      outer.put(obja, inner);
    }
    Float objscore = new Float(score);
    inner.put(objb, objscore);
  }

  /**
   * retreives the score for a substituted by b.
   *
   * @param a original amino acid
   * @param b substitued amino acid
   * @return score
   */
  public double get(char a, char b) {
    double score = 0;
    Character obja = new Character(a);
    Hashtable inner = (Hashtable) outer.get(obja);
    if (null != inner) {
      Float objscore = (Float) (inner.get(new Character(b)));
      if (null != objscore) {
        score = objscore.doubleValue();
      }
    }
    return score;
  }

  /**
   * @return a string representation of the matrix.
   */
  public String toString() {
    StringBuffer s = new StringBuffer();
    s.append("Substitution Matrix\n");
    Enumeration eo = outer.keys();
    while (eo.hasMoreElements()) {
      Character obja = (Character) eo.nextElement();
      Hashtable inner = (Hashtable) outer.get(obja);
      Enumeration ei = inner.keys();
      s.append("" + obja + "\t");
      while (ei.hasMoreElements()) {
        Character objb = (Character) ei.nextElement();
        double f = get(obja.charValue(), objb.charValue());
        s.append(" " + f);
      }
      s.append("\n");
    }
    return new String(s);
  }

}
