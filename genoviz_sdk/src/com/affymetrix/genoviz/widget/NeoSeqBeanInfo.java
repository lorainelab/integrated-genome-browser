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

package com.affymetrix.genoviz.widget;

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.EventSetDescriptor;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class NeoSeqBeanInfo extends SimpleBeanInfo {

  /**
   * @return a customizer.
   */
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(NeoSeq.class, NeoSeqCustomizer.class);
  }

  /**
   * sets property descriptors for all the NeoSeq properties.
   * Tried doing this to see if I could get the NeoSeq
   * to stop jamming the bean box.
   * It failed to do so.
   */
  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor result[] = new PropertyDescriptor[5];
    try {
      int i = -1;
      result[++i] = new PropertyDescriptor("spacing", NeoSeq.class);
      result[++i] = new PropertyDescriptor("background", NeoSeq.class);
      result[++i] = new PropertyDescriptor("foreground", NeoSeq.class);
      result[++i] = new PropertyDescriptor("font", NeoSeq.class);
      result[++i] = new PropertyDescriptor("selectionEvent", NeoSeq.class);
      result[i].setPropertyEditorClass(SlctnEvntEditor.class);
      result[i++] = new PropertyDescriptor("editable", NeoSeq.class);
    } catch (IntrospectionException e) {
      System.err.println("getProperties() doesn't work.");
      result = null;
    }
    return result;
  }

}
