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
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class NeoTracerBeanInfo extends SimpleBeanInfo {

  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(NeoTracer.class, NeoTracerCustomizer.class);
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor result[] = new PropertyDescriptor[10];
    try {
      int i = -1;
      result[++i] = new PropertyDescriptor("direction", NeoTracer.class);
      result[i].setPropertyEditorClass(SeqDirectionEditor.class);
      result[++i] = new PropertyDescriptor("background", NeoTracer.class);
      result[++i] = new PropertyDescriptor("basesBackground", NeoTracer.class);
      result[++i] = new PropertyDescriptor("tracesBackground", NeoTracer.class);
      result[++i] = new PropertyDescriptor("trimColor", NeoTracer.class);
      // This is currently ignored by NeoTracer. It should not be.
      //result[++i] = new PropertyDescriptor("selectionColor", NeoTracer.class);
      result[++i] = new PropertyDescriptor("selectionEvent", NeoTracer.class);
      result[i].setPropertyEditorClass(SlctnEvntEditor.class);
      result[++i] = new PropertyDescriptor("font", NeoTracer.class);
      result[++i] = new PropertyDescriptor("opaque", NeoTracer.class);
      result[++i] = new PropertyDescriptor("doubleBuffered", NeoTracer.class);
      result[++i] = new PropertyDescriptor("PixelFuzziness", NeoTracer.class);
      // Not really available in NeoTracer
      //result[++i] = new PropertyDescriptor("subSelectionAllowed", NeoTracer.class);
      //result[++i] = new PropertyDescriptor("selectionAppearance", NeoTracer.class);
      //result[++i].setPropertyEditorClass(SlctnApprnceEditor.class);
      // Not really a property of NeoTracer. Rather a prop of the data model.
      //result[++i] = new PropertyDescriptor("basesTrimmedLeft", NeoTracer.class);
      //result[++i] = new PropertyDescriptor("basesTrimmedRight", NeoTracer.class);
      //result[++i] = new PropertyDescriptor("foreground", NeoTracer.class); // dont really need this.
    } catch (IntrospectionException e) {
      System.err.println("getProperties() doesn't work.");
      result = null;
    }
    return result;
  }

}
