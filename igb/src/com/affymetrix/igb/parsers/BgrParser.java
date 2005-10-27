package com.affymetrix.igb.parsers;

import java.io.*;
import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.*;

public class BgrParser {

  /**
   * Writes bgr format.
   *<pre>
   *.bgr format:
   *  Old Header:
   *    UTF-8 encoded seq name
   *    UTF-8 encoded seq version
   *    4-byte int for total number of data points
   *  New Header:
   *    UTF-8 encoded:
   *       seq_name
   *       release_name (seq version)
   *       analysis_group_name
   *       map_analysis_group_name
   *       method_name
   *       parameter_set_name
   *       value_type_name
   *       control_group_name
   *    4-byte int for total number of data points
   *  Then for each data point:
   *    4-byte int for base position
   *    4-byte float for value
   * </pre>
   */
  public static boolean writeBgrFormat(GraphSym graf, OutputStream ostr)
    throws IOException  {
    System.out.println("writing graph: " + graf);
    BufferedOutputStream bos = new BufferedOutputStream(ostr);
    DataOutputStream dos = new DataOutputStream(bos);
    int[] xcoords = graf.getGraphXCoords();
    float[] ycoords = graf.getGraphYCoords();
    int total_points = xcoords.length;
    Map headers = graf.getProperties();

    if (headers == null) {  // then write eight null entries in a row
      for (int i=0; i<8; i++) { dos.writeUTF("null"); }
    }
    else {
      if (headers.get("seq_name") == null) { dos.writeUTF("null"); }
      else { dos.writeUTF((String)headers.get("seq_name")); }
      if (headers.get("release_name") == null)  {dos.writeUTF("null"); }
      else  { dos.writeUTF((String)headers.get("release_name")); }
      if (headers.get("analysis_group_name") == null)  { dos.writeUTF("null");}
      else  { dos.writeUTF((String)headers.get("analysis_group_name")); }
      if (headers.get("map_analysis_group_name") == null)  { dos.writeUTF("null"); }
      else  { dos.writeUTF((String)headers.get("map_analysis_group_name")); }
      if (headers.get("method_name") == null)  { dos.writeUTF("null");}
      else  { dos.writeUTF((String)headers.get("method_name")); }
      if (headers.get("parameter_set_name") == null)  { dos.writeUTF("null");}
      else  dos.writeUTF((String)headers.get("parameter_set_name"));
      if (headers.get("value_type_name") == null)  { dos.writeUTF("null"); }
      else  { dos.writeUTF((String)headers.get("value_type_name")); }
      if (headers.get("control_group_name") == null) { dos.writeUTF("null"); }
      else { dos.writeUTF((String)headers.get("control_group_name")); }
    }
    dos.writeInt(total_points);
    for (int i=0; i<total_points; i++) {
      dos.writeInt((int)xcoords[i]);
      dos.writeFloat((float)ycoords[i]);
    }
    //      dos.flush();
    dos.close();
    return true;
  }


  public static GraphSym parse(InputStream istr, BioSeq aseq) throws IOException {
    Map seqhash = new HashMap();
    seqhash.put(aseq.getID(), aseq);
    return parse(istr, seqhash);
  }

  public static GraphSym parse(InputStream istr, Map seqhash)
    throws IOException  {
    com.affymetrix.genoviz.util.Timer tim = new com.affymetrix.genoviz.util.Timer();
    tim.start();
    int count = 0;
    BufferedInputStream bis = new BufferedInputStream(istr);
    DataInputStream dis = new DataInputStream(bis);
    HashMap props = new HashMap();
    String seq_name = dis.readUTF();
    String release_name = dis.readUTF();
    String analysis_group_name = dis.readUTF();
    System.out.println(seq_name + ", " + release_name + ", " + analysis_group_name);
    String map_analysis_group_name = dis.readUTF();
    String method_name = dis.readUTF();
    String parameter_set_name = dis.readUTF();
    String value_type_name = dis.readUTF();
    String control_group_name = dis.readUTF();
    props.put("seq_name", seq_name);
    props.put("release_name", release_name);
    props.put("analysis_group_name", analysis_group_name);
    props.put("map_analysis_group_name", map_analysis_group_name);
    props.put("method_name", method_name);
    props.put("parameter_set_name", parameter_set_name);
    props.put("value_type_name", value_type_name);
    props.put("control_group_name", control_group_name);

    int total_points = dis.readInt();
    System.out.println("loading graph from binary file, name = " + seq_name +
                       ", release = " + release_name +
                       ", total_points = " + total_points);
    int[] xcoords = new int[total_points];
    float[] ycoords = new float[total_points];
    for (int i=0; i<total_points; i++) {
      xcoords[i] = dis.readInt();
      ycoords[i] = dis.readFloat();
      count++;
    }
    // can't just hash, because _could_ be a synonym instead of an exact match
    //    BioSeq seq = (BioSeq)seqhash.get(seq_name);
    MutableAnnotatedBioSeq seq = null;
    SynonymLookup lookup = SynonymLookup.getDefaultLookup();
    Iterator iter = seqhash.values().iterator();
    while (iter.hasNext()) {
      MutableAnnotatedBioSeq testseq = (MutableAnnotatedBioSeq)iter.next();
      if (lookup.isSynonym(testseq.getID(), seq_name)) {
        seq = testseq;
        break;
      }
    }
    if (seq == null) {
      System.out.println("seq not found, creating new seq");
      seq = new NibbleBioSeq(seq_name, release_name, 500000000);
    }
    //    if (seq == null) { seq = new SimpleBioSeq(seq_name, 500000000); }
    /*
     BioSeq seq = null;
     if (aseq == null) {
      seq = new SimpleBioSeq(seq_name, 500000000);
    }
    else { seq = aseq; }
    */
    String graph_name =
      analysis_group_name + ", " +
      value_type_name + ", " +
      parameter_set_name;

    // need to replace seq_name with name of graph (some combo of group name and conditions...)
    GraphSym graf = new GraphSym(xcoords, ycoords, graph_name, seq);
    graf.setProperties(props);
    double load_time = tim.read()/1000f;
    System.out.println("loaded graf, total points = " + count);
    System.out.println("time to load graf from binary: " + load_time);
    return graf;
  }

}
