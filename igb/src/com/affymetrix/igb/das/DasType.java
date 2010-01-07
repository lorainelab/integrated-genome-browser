package com.affymetrix.igb.das;

public final class DasType {
  private String type_id;

  DasType(DasSource source, String id, String method, String category) {
    type_id = id;
  }

  public String getID() { return type_id; }
}
