/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genoviz.tiers;

import java.awt.Color;
import java.util.Map;

/**
 *
 * @author Ed Erwin
 */
class DefaultIAnnotStyle implements IAnnotStyle {

  public DefaultIAnnotStyle() {
  }

  public Color getColor() {
    return Color.GRAY;
  }

  public void setColor(Color c) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public boolean getShow() {
    return true;
  }

  public void setShow(boolean b) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public String getUniqueName() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public String getHumanName() {
    return "no name";
  }

  public void setHumanName(String s) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  Color bg = Color.BLACK;
  
  public Color getBackground() {
    return bg;
  }

  public void setBackground(Color c) {
    bg = c;
  }

  public boolean getCollapsed() {
    return false;
  }

  public void setCollapsed(boolean b) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public int getMaxDepth() {
    return 10;
  }

  public void setMaxDepth(int m) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void setHeight(double h) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public double getHeight() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void setY(double y) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public double getY() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public boolean getExpandable() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void setExpandable(boolean b) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public boolean isGraphTier() {
    return false;
  }

  public void setGraphTier(boolean b) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public Map<String, Object> getTransientPropertyMap() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void copyPropertiesFrom(IAnnotStyle s) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

}
