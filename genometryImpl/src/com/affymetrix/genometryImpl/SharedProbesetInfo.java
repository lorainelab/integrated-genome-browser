package com.affymetrix.genometryImpl;

import java.util.*;
import com.affymetrix.genometry.*;

public class SharedProbesetInfo {
  BioSeq seq;
  int probe_length;
  String id_prefix;
  Map props;
  
  public SharedProbesetInfo(BioSeq seq, int probe_length, String id_prefix, Map props) {
    this.seq = seq;
    this.probe_length = probe_length;
    this.id_prefix = id_prefix;
    this.props = props;
  }

  public BioSeq getBioSeq() { return seq; }
  public int getProbeLength() { return probe_length; }
  public String getIDPrefix() { return id_prefix; }
  public Map getProps() { return props; }
  
}
