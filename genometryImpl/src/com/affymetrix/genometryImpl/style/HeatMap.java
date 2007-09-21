/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.style;

import java.awt.Color;
import java.util.*;

public class HeatMap {

  /** Name of the Black-and-White Standard HeatMap. */
  public static final String HEATMAP_0 = "Black/White";

  /** Name of the Violet Standard HeatMap. */
  public static final String HEATMAP_1 = "Violet";

  /** Name of the Blue/Yellow Standard HeatMap. */
  public static final String HEATMAP_2 = "Blue/Yellow";

  /** Name of the Red/Green Standard HeatMap. */
  public static final String HEATMAP_3 = "Red/Black/Green";

  /** Name of the second Blue/Yellow Standard HeatMap. */
  public static final String HEATMAP_4 = "Blue/Yellow 2";

  /** Name of the Standard Rainbow HeatMap. */
  public static final String HEATMAP_RAINBOW = "Rainbow";

  /** Name of the Standard Rainbow HeatMap. */
  public static final String HEATMAP_RED_GRAY_BLUE = "Red/Gray/Blue";

  /** Name of the Transparent Black-and-White Standard HeatMap. */
  public static final String HEATMAP_T_0 = "Transparent B/W";

  /** Name of the Transparent Violet Standard HeatMap. */
  public static final String HEATMAP_T_1 = "Transparent Blue";

  /** Name of the Transparent Blue/Yellow Standard HeatMap. */
  public static final String HEATMAP_T_2 = "Transparent Red";

  /** Name of the Transparent Red/Green Standard HeatMap. */
  public static final String HEATMAP_T_3 = "Transparent Green";

  public static final String PREF_HEATMAP_NAME = "Default Heatmap";
  public static final String def_heatmap_name = HEATMAP_2;

  public static String[] HEATMAP_NAMES = {
    HEATMAP_0, HEATMAP_1, HEATMAP_2, HEATMAP_3, HEATMAP_4,
    HEATMAP_T_0, HEATMAP_T_2, HEATMAP_T_3, HEATMAP_T_1,
    HEATMAP_RAINBOW, HEATMAP_RED_GRAY_BLUE
  };

  static Map<String,HeatMap> name2heatmap = new HashMap<String,HeatMap>();

  String name;
  Color[] colors;

  public HeatMap(String name, Color[] colors) {
    this.name = name;
    this.colors = colors;
  }

  public String getName() {
    return name;
  }

  public Color[] getColors() {
    return colors;
  }

  /** Gets the color at the given index value.
   * @param heatmap_index an integer in the range 0 to 255.  If the specified
   *  index is outside this range, the color corresponding to index 0 or 255
   *  will be returned.
   */
  public Color getColor(int heatmap_index) {
    if (heatmap_index < 0) {
      return colors[0];
    } else if (heatmap_index > 255) {
      return colors[255];
    } else {
      return colors[heatmap_index];
    }
  }

  /**
   *  Returns one of the standard pre-defined heat maps using the names in
   *  HEATMAP_NAMES, or null if one with the given name does not exist.
   */
  public static HeatMap getStandardHeatMap(String name) {
    HeatMap hm = name2heatmap.get(name);
    if (hm == null) {
      int r,g,b;
      int bins = 256;
      Color[] cc = new Color[bins];

      if (HEATMAP_0.equals(name)) {
        r=0; g=0; b=0;
        for (int i=0; i<bins; i++) {
          cc[i] = new Color(r++, g++, b++);
        }
      } else if (HEATMAP_1.equals(name)) {
        r=0; g=0; b=0;
        for (int i=0; i<bins; i++) {
          cc[i] = new Color(r++, g, b++);
        }
      } else if (HEATMAP_2.equals(name)) {
        r=0; g=0; b=255;
        for (int i=0; i<bins; i++) {
          cc[i] = new Color(r++, g++, b--);
        }
      } else if (HEATMAP_3.equals(name)) {
        // red-black-green colormap
        for (int i=0; i<bins; i++) {
          r = Math.max(255 - 2*i, 0);
          g = Math.min(Math.max(2 * (i-128), 0), 255);
          b = 0;
          cc[i] = new Color(r, g, b);
        }
      } else if (HEATMAP_4.equals(name)) {
        r=0; g=0; b=128;
        for (int i=0; i<bins; i++) {
          cc[i] = new Color(r++, g++, b);
          if (i % 2 == 0)  { b--; }
        }
      } else if (HEATMAP_RAINBOW.equals(name)) {
        for (int i=0; i<bins; i++) {
          // hues from red to blue
          cc[i] = new Color(Color.HSBtoRGB(0.66f*(1.0f*i)/bins, 0.8f, 1.0f));
        }
      } else if (HEATMAP_RED_GRAY_BLUE.equals(name)) {
        int gray_level = 128+64;
        for (int i=0; i<bins; i++) {
          // start with hues from red to green to blue
          // then convert the green part into a background gray level
          Color c = new Color(Color.HSBtoRGB(0.66f*(1.0f*i)/bins, 0.8f, 1.0f));
          int gg = (gray_level * c.getGreen()) / 256;
          cc[i] = new Color(
              Math.max(c.getRed(), gg),
              gg, 
              Math.max(c.getBlue(), gg));
        }
      }

      // Now the transparent ones
      else if (HEATMAP_T_0.equals(name)) {
        r=0; g=0; b=0;
        for (int i=0; i<bins; i++) {
          cc[i] = new Color(r++, g++, b++, 128);
        }
      } else if (HEATMAP_T_1.equals(name)) {
        r=0; g=0; b=0;
        for (int i=0; i<bins; i++) {
          cc[i] = new Color(r, g, b++, 128);
        }
      } else if (HEATMAP_T_2.equals(name)) {
        r=0; g=0; b=0;
        for (int i=0; i<bins; i++) {
          cc[i] = new Color(r++, g, b, 128);
        }
      } else if (HEATMAP_T_3.equals(name)) {
        r=0; g=0; b=0;
        for (int i=0; i<bins; i++) {
          cc[i] = new Color(r, g++, b, 128);
        }
      } else {
        cc = null;
      }

      if (cc != null) {
        hm = new HeatMap(name, cc);
        name2heatmap.put(name, hm);
      }
    }

    return hm;
  }

  /** Make a HeatMap that interpolates linearly between the two given colors. */
  public static HeatMap makeLinearHeatmap(String name, Color low, Color high) {
    Color[] colors = new Color[256];
    HeatMap heat_map = new HeatMap(name, colors);

    for (int i=0; i<256; i++) {
      float x = (i*1.0f)/255.0f;
      colors[i] = interpolateColor(low, high, x);
    }

    return heat_map;
  }


  /**
   *  Creates a new color inbetween c1 and c2.
   *  @param x  The fraction of the new color (0.00 to 1.00) that
   *  should be based on color c2, the rest is based on c1.
   */
  public static Color interpolateColor(Color c1, Color c2, float x) {
    if (x <= 0.0f) {
      return c1;
    } else if (x >= 1.0f) {
      return c2;
    } else {
      int r = (int) ((1.0f - x) * c1.getRed() + x * c2.getRed());
      int g = (int) ((1.0f - x) * c1.getGreen() + x * c2.getGreen());
      int b = (int) ((1.0f - x) * c1.getBlue() + x * c2.getBlue());

      return new Color(r, g, b);
    }
  }
}
