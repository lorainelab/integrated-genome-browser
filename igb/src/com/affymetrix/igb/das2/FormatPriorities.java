package com.affymetrix.igb.das2;

import java.util.*;

/**
 *  FormatPriorities is intended to help the IGB DAS/2 client figure out
 *  what format to retrieve annotations in, given the formats supported by the server for a particular
 *  versioned source (returned via the /types query).
 *
 *  May want to add some smarts in here on a per-type basis, so for example if a server can server
 *   up either formatA or formatB for both type1 and type2, the client may stil prefer formatB for type1
 *   and formatA for type2.  For first cut not worrying about this, just giving the client a fixed
 *   format priorities that apply to all types
 *
 */
public final class FormatPriorities {

  /**
   *  Different format types, prioritized
   */
  static String[] ordered_formats = {
    "link.psl",
    "ead",
    "bp2", 
    "brs",
    "bgn",
    "bps",
    "cyt",
    "bed",
    "psl",
    "gff",
    "bar",
  };

  //  static String default_format = "das2feature";

    static String getFormat(Das2Type type) {
        if (type.getID().endsWith(".bar")) {  // temporary way to recognize graph "types"...
            return "bar";
        }
        if (type.getID().endsWith(".bed")) {
            return "bed";
        }
        else {
            Map type_formats = type.getFormats();
            if (type_formats != null) {
                int fcount = ordered_formats.length;
                for (int i = 0; i < ordered_formats.length; i++) {
                    String format = ordered_formats[i];
                    if (type_formats.get(format) != null) {
                        return format;
                    }
                }
            }
        }
        // return default_format;
        return null;
    }

  /**
   *  input is list of Das2Types, output is preferred format name
   *    that can be served by server for all the types
   */
  //  static String getFormat(List types) {
  //
  //  }
}
