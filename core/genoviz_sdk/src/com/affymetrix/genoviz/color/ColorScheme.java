/*  Copyright (c) 2012 Genentech, Inc.
 *
 *  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.genoviz.color;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sets of colors that work well together.
 * 
 *     Some of these schemes were derived from Cynthia Brewer's color schemes.
 *     Those have been modified to designate one of the colors as a background,
 *     or a background has been added (white or black).
 * 
 *<p>  Brewer <a href="http://www.personal.psu.edu/cab38/ColorSch/Schemes.html"
 *     >categorises</a> schemes into binary, qualitative, sequential, and diverging.
 *     One could consider binary as a special case of either qualitative or sequential.
 *</p>
 * 
 *<p>  In the future we may need to distinguish
 *     between qualitative, diverging, and sequential schemes.
 *     We might be able to come up with our own backgrounds
 *     for the qualitative schemes.
 *     In fact, we have, but they are not entirely satisfactory.
 *     e.g. PAIRED11 is difficult. It is horrid with white and not great with black.
 *     For sequential schemes we just took the first color to be the background.
 *     For diverging schemes we tried to take a middle color as the background.
 *     For sequential schemes we could also take the last color as a background
 *     or add white or black.
 *     This latter might be good on single hue sequential schemes.
 *</p>
 *
 *<p>  For sequential schemes Brewer distinguishes between multi hue and single hue.
 *     See <a href="http://colorbrewer2.org">ColorBrewer.org</a>.
 *</p>
 *
 *<p>  Some schemes are from a paper on
 *     <a href="http://www.biostat.jhsph.edu/bit/compintro/bruce/hcl-colors.pdf"
 *     >HCL-Based Color Palettes in R</a> by Zeilis, Hornik, and Murrell.
 *</p>
 *
 *<p>  <em>Cynthia Brewer's License is below:</em>
 *
 *<p>  Apache-Style Software License for ColorBrewer software and ColorBrewer Color Schemes, Version 1.1
 *<p>  Copyright (c) 2002 Cynthia Brewer, Mark Harrower, and The Pennsylvania State University.
 *     All rights reserved.
 * 
 *<p>  Redistribution and use in source and binary forms, with or without modification, are permitted
 *     provided that the following conditions are met:
 *<ol>
 *<li> Redistributions as source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.</li>
 *<li> The end-user documentation included with the redistribution, if any,
 *     must include the following acknowledgment:
 *<ul><li> This product includes color specifications and designs developed by Cynthia Brewer
 *        (http://colorbrewer.org/).</li>
 *    <li> Alternately, this acknowledgment may appear in the software itself,
 *         if and wherever such third-party acknowledgments normally appear.</li>
 *</ul></li>
 *<li> The name "ColorBrewer" must not be used to endorse or promote products derived from this software
 *     without prior written permission.
 *     For written permission, please contact Cynthia Brewer at cbrewer@psu.edu.</li>
 *<li> Products derived from this software may not be called "ColorBrewer",
 *     nor may "ColorBrewer" appear in their name, without prior written permission of Cynthia Brewer.</li>
 *</ol>
 *
 * @author Eric Blossom
 */
public enum ColorScheme {
	// binary schemes
    IGB    ("black", "cyan"),
    WHITEONBLACK ("black", "white"),
    BLACKONWHITE ("white", "black"),
    MAC    ("#EEEEEE", "black"), // Mac Aqua Background
	
    ANN    ("white", "black;grey;silver;blue;magenta"),
    CYRUS  ("black", "pink;lightsteelblue;darkseagreen;plum;sandybrown;palegoldenrod;bisque;lightpink;lightgray"),
    STEVEN ("black", "#FBB4AE;#B3CDE3;#CCEBC5;#DECBE4;#FED9A6;#FFFFCC;#E5D8BD;#FDDAEC;#F2F2F2"), // PASTEL19
    GREGG  ("black", "#8DD3C7;#FFFFB3;#BEBADA;#FB8072;#80B1D3;#FDB462;#B3DE69;#FCCDE5;#D9D9D9;#BC80BD;#CCEBC5;#FFED6F"), // PAIRED12
    DAVID  ("white", "red;green;blue"),
    JUWEI  ("black", "red;lime;cyan"),
    MARTIN ("black", "brown;red;orange;coral"),
    STEVE  ("#EEEEEE", "brown;red;orange;coral"), // Mac Aqua Background
    ED     ("white", "brown;red;orange;coral"),
    HARI   ("lightcyan", "brown;red;orange;coral"),
    ELISE  ("white", "silver;black;red;maroon;olive;lime;green;aqua;teal;blue;fuchsia;purple;orange"),
    // JIM, BARBARA, JOE, TAO, GOYING, JACK, VENU, ADAM
    ACGT      ("white", "green;blue;black;red"), // Traditional colors from chromatogram traces.
    ACGTBLACK ("black", "lightgreen;lightblue;orange;red"), // Consed
    ACGTIGB   ("grey", "lime;lightblue;goldenrod;pink"), // There isn't really a background per se. We use grey for "other".
    IGBORF    ("grey", "maroon;green"), // approx

// Modified Brewer Color Schemes
ACCENT3 ("black", "#7FC97F;#BEAED4;#FDC086"), // qualitative
ACCENT4 ("black", "#7FC97F;#BEAED4;#FDC086;#FFFF99"),
ACCENT5 ("black", "#7FC97F;#BEAED4;#FDC086;#FFFF99;#386CB0"),
ACCENT6 ("black", "#7FC97F;#BEAED4;#FDC086;#FFFF99;#386CB0;#F0027F"),
ACCENT7 ("black", "#7FC97F;#BEAED4;#FDC086;#FFFF99;#386CB0;#F0027F;#BF5B17"),
ACCENT8 ("black", "#7FC97F;#BEAED4;#FDC086;#FFFF99;#386CB0;#F0027F;#BF5B17;#666666"),
//BLUES3 ("#DEEBF7", "#9ECAE1;#3182BD"),
//BLUES4 ("#EFF3FF", "#BDD7E7;#6BAED6;#2171B5"),
//BLUES5 ("#EFF3FF", "#BDD7E7;#6BAED6;#3182BD;#08519C"),
//BLUES6 ("#EFF3FF", "#C6DBEF;#9ECAE1;#6BAED6;#3182BD;#08519C"),
//BLUES7 ("#EFF3FF", "#C6DBEF;#9ECAE1;#6BAED6;#4292C6;#2171B5;#084594"),
//BLUES8 ("#F7FBFF", "#DEEBF7;#C6DBEF;#9ECAE1;#6BAED6;#4292C6;#2171B5;#084594"),
//BLUES9 ("#F7FBFF", "#DEEBF7;#C6DBEF;#9ECAE1;#6BAED6;#4292C6;#2171B5;#08519C;#08306B"),
//WBLUES3 ("white", "#DEEBF7;#9ECAE1;#3182BD"),
//WBLUES4 ("white", "#EFF3FF;#BDD7E7;#6BAED6;#2171B5"),
//WBLUES5 ("white", "#EFF3FF;#BDD7E7;#6BAED6;#3182BD;#08519C"),
//WBLUES6 ("white", "#EFF3FF;#C6DBEF;#9ECAE1;#6BAED6;#3182BD;#08519C"),
//WBLUES7 ("white", "#EFF3FF;#C6DBEF;#9ECAE1;#6BAED6;#4292C6;#2171B5;#084594"),
//WBLUES8 ("white", "#F7FBFF;#DEEBF7;#C6DBEF;#9ECAE1;#6BAED6;#4292C6;#2171B5;#084594"),
//WBLUES9 ("white", "#F7FBFF;#DEEBF7;#C6DBEF;#9ECAE1;#6BAED6;#4292C6;#2171B5;#08519C;#08306B"),
//CBLUES3 ("#FFFFF0", "#DEEBF7;#9ECAE1;#3182BD"),
//CBLUES4 ("cornsilk", "#EFF3FF;#BDD7E7;#6BAED6;#2171B5"),
//CBLUES5 ("cornsilk", "#EFF3FF;#BDD7E7;#6BAED6;#3182BD;#08519C"),
//CBLUES6 ("cornsilk", "#EFF3FF;#C6DBEF;#9ECAE1;#6BAED6;#3182BD;#08519C"),
//CBLUES7 ("cornsilk", "#EFF3FF;#C6DBEF;#9ECAE1;#6BAED6;#4292C6;#2171B5;#084594"),
//CBLUES8 ("cornsilk", "#F7FBFF;#DEEBF7;#C6DBEF;#9ECAE1;#6BAED6;#4292C6;#2171B5;#084594"),
//CBLUES9 ("cornsilk", "#F7FBFF;#DEEBF7;#C6DBEF;#9ECAE1;#6BAED6;#4292C6;#2171B5;#08519C;#08306B"),
//BRBG3 ("#F5F5F5", "#D8B365;#5AB4AC"),
//BRBG4 ("#DFC27D", "#A6611A;#80CDC1;#018571"),
//BRBG5 ("#F5F5F5", "#A6611A;#DFC27D;#80CDC1;#018571"),
//BRBG6 ("#F6E8C3", "#8C510A;#D8B365;#C7EAE5;#5AB4AC;#01665E"),
//BRBG7 ("#F5F5F5", "#8C510A;#D8B365;#F6E8C3;#C7EAE5;#5AB4AC;#01665E"),
//BRBG8 ("#F6E8C3", "#8C510A;#BF812D;#DFC27D;#C7EAE5;#80CDC1;#35978F;#01665E"),
//BRBG9 ("#F5F5F5", "#8C510A;#BF812D;#DFC27D;#F6E8C3;#C7EAE5;#80CDC1;#35978F;#01665E"),
//BRBG10 ("#F6E8C3", "#543005;#8C510A;#BF812D;#DFC27D;#C7EAE5;#80CDC1;#35978F;#01665E;#003C30"),
//BRBG11 ("#F5F5F5", "#543005;#8C510A;#BF812D;#DFC27D;#F6E8C3;#C7EAE5;#80CDC1;#35978F;#01665E;#003C30"),
//BUGN3 ("#E5F5F9", "#99D8C9;#2CA25F"),
//BUGN4 ("#EDF8FB", "#B2E2E2;#66C2A4;#238B45"),
//BUGN5 ("#EDF8FB", "#B2E2E2;#66C2A4;#2CA25F;#006D2C"),
//BUGN6 ("#EDF8FB", "#CCECE6;#99D8C9;#66C2A4;#2CA25F;#006D2C"),
//BUGN7 ("#EDF8FB", "#CCECE6;#99D8C9;#66C2A4;#41AE76;#238B45;#005824"),
//BUGN8 ("#F7FCFD", "#E5F5F9;#CCECE6;#99D8C9;#66C2A4;#41AE76;#238B45;#005824"),
//BUGN9 ("#F7FCFD", "#E5F5F9;#CCECE6;#99D8C9;#66C2A4;#41AE76;#238B45;#006D2C;#00441B"),
//BUPU3 ("#E0ECF4", "#9EBCDA;#8856A7"),
//BUPU4 ("#EDF8FB", "#B3CDE3;#8C96C6;#88419D"),
//BUPU5 ("#EDF8FB", "#B3CDE3;#8C96C6;#8856A7;#810F7C"),
//BUPU6 ("#EDF8FB", "#BFD3E6;#9EBCDA;#8C96C6;#8856A7;#810F7C"),
//BUPU7 ("#EDF8FB", "#BFD3E6;#9EBCDA;#8C96C6;#8C6BB1;#88419D;#6E016B"),
//BUPU8 ("#F7FCFD", "#E0ECF4;#BFD3E6;#9EBCDA;#8C96C6;#8C6BB1;#88419D;#6E016B"),
//BUPU9 ("#F7FCFD", "#E0ECF4;#BFD3E6;#9EBCDA;#8C96C6;#8C6BB1;#88419D;#810F7C;#4D004B"),
DARK23 ("white", "#1B9E77;#D95F02;#7570B3"), // qualitative DARK2
DARK24 ("white", "#1B9E77;#D95F02;#7570B3;#E7298A"),
DARK25 ("white", "#1B9E77;#D95F02;#7570B3;#E7298A;#66A61E"),
DARK26 ("white", "#1B9E77;#D95F02;#7570B3;#E7298A;#66A61E;#E6AB02"),
DARK27 ("white", "#1B9E77;#D95F02;#7570B3;#E7298A;#66A61E;#E6AB02;#A6761D"),
DARK28 ("white", "#1B9E77;#D95F02;#7570B3;#E7298A;#66A61E;#E6AB02;#A6761D;#666666"),
//GNBU3 ("#E0F3DB", "#A8DDB5;#43A2CA"),
//GNBU4 ("#F0F9E8", "#BAE4BC;#7BCCC4;#2B8CBE"),
//GNBU5 ("#F0F9E8", "#BAE4BC;#7BCCC4;#43A2CA;#0868AC"),
//GNBU6 ("#F0F9E8", "#CCEBC5;#A8DDB5;#7BCCC4;#43A2CA;#0868AC"),
//GNBU7 ("#F0F9E8", "#CCEBC5;#A8DDB5;#7BCCC4;#4EB3D3;#2B8CBE;#08589E"),
//GNBU8 ("#F7FCF0", "#E0F3DB;#CCEBC5;#A8DDB5;#7BCCC4;#4EB3D3;#2B8CBE;#08589E"),
//GNBU9 ("#F7FCF0", "#E0F3DB;#CCEBC5;#A8DDB5;#7BCCC4;#4EB3D3;#2B8CBE;#0868AC;#084081"),
//GREENS3 ("#E5F5E0", "#A1D99B;#31A354"),
//GREENS4 ("#EDF8E9", "#BAE4B3;#74C476;#238B45"),
//GREENS5 ("#EDF8E9", "#BAE4B3;#74C476;#31A354;#006D2C"),
//GREENS6 ("#EDF8E9", "#C7E9C0;#A1D99B;#74C476;#31A354;#006D2C"),
//GREENS7 ("#EDF8E9", "#C7E9C0;#A1D99B;#74C476;#41AB5D;#238B45;#005A32"),
//GREENS8 ("#F7FCF5", "#E5F5E0;#C7E9C0;#A1D99B;#74C476;#41AB5D;#238B45;#005A32"),
//GREENS9 ("#F7FCF5", "#E5F5E0;#C7E9C0;#A1D99B;#74C476;#41AB5D;#238B45;#006D2C;#00441B"),
//GREYS3 ("#F0F0F0", "#BDBDBD;#636363"),
//GREYS4 ("#F7F7F7", "#CCCCCC;#969696;#525252"),
//GREYS5 ("#F7F7F7", "#CCCCCC;#969696;#636363;#252525"),
//GREYS6 ("#F7F7F7", "#D9D9D9;#BDBDBD;#969696;#636363;#252525"),
//GREYS7 ("#F7F7F7", "#D9D9D9;#BDBDBD;#969696;#737373;#525252;#252525"),
//GREYS8 ("#FFFFFF", "#F0F0F0;#D9D9D9;#BDBDBD;#969696;#737373;#525252;#252525"),
//GREYS9 ("#FFFFFF", "#F0F0F0;#D9D9D9;#BDBDBD;#969696;#737373;#525252;#252525;#000000"),
//ORANGES3 ("#FEE6CE", "#FDAE6B;#E6550D"),
//ORANGES4 ("#FEEDDE", "#FDBE85;#FD8D3C;#D94701"),
//ORANGES5 ("#FEEDDE", "#FDBE85;#FD8D3C;#E6550D;#A63603"),
//ORANGES6 ("#FEEDDE", "#FDD0A2;#FDAE6B;#FD8D3C;#E6550D;#A63603"),
//ORANGES7 ("#FEEDDE", "#FDD0A2;#FDAE6B;#FD8D3C;#F16913;#D94801;#8C2D04"),
//ORANGES8 ("#FFF5EB", "#FEE6CE;#FDD0A2;#FDAE6B;#FD8D3C;#F16913;#D94801;#8C2D04"),
//ORANGES9 ("#FFF5EB", "#FEE6CE;#FDD0A2;#FDAE6B;#FD8D3C;#F16913;#D94801;#A63603;#7F2704"),
//ORRD3 ("#FEE8C8", "#FDBB84;#E34A33"),
//ORRD4 ("#FEF0D9", "#FDCC8A;#FC8D59;#D7301F"),
//ORRD5 ("#FEF0D9", "#FDCC8A;#FC8D59;#E34A33;#B30000"),
//ORRD6 ("#FEF0D9", "#FDD49E;#FDBB84;#FC8D59;#E34A33;#B30000"),
//ORRD7 ("#FEF0D9", "#FDD49E;#FDBB84;#FC8D59;#EF6548;#D7301F;#990000"),
//ORRD8 ("#FFF7EC", "#FEE8C8;#FDD49E;#FDBB84;#FC8D59;#EF6548;#D7301F;#990000"),
//ORRD9 ("#FFF7EC", "#FEE8C8;#FDD49E;#FDBB84;#FC8D59;#EF6548;#D7301F;#B30000;#7F0000"),
PAIRED3 ("black", "#A6CEE3;#1F78B4;#B2DF8A"), // qualitative
PAIRED4 ("black", "#A6CEE3;#1F78B4;#B2DF8A;#33A02C"),
PAIRED5 ("black", "#A6CEE3;#1F78B4;#B2DF8A;#33A02C;#FB9A99"),
PAIRED6 ("black", "#A6CEE3;#1F78B4;#B2DF8A;#33A02C;#FB9A99;#E31A1C"),
PAIRED7 ("black", "#A6CEE3;#1F78B4;#B2DF8A;#33A02C;#FB9A99;#E31A1C;#FDBF6F"),
PAIRED8 ("black", "#A6CEE3;#1F78B4;#B2DF8A;#33A02C;#FB9A99;#E31A1C;#FDBF6F;#FF7F00"),
PAIRED9 ("black", "#A6CEE3;#1F78B4;#B2DF8A;#33A02C;#FB9A99;#E31A1C;#FDBF6F;#FF7F00;#CAB2D6"),
PAIRED10 ("black", "#1F78B4;#B2DF8A;#33A02C;#FB9A99;#E31A1C;#FDBF6F;#FF7F00;#CAB2D6;#6A3D9A"),
PAIRED11 ("black", "#1F78B4;#B2DF8A;#33A02C;#FB9A99;#E31A1C;#FDBF6F;#FF7F00;#CAB2D6;#6A3D9A;#FFFF99"),
PAIRED12 ("black", "#1F78B4;#B2DF8A;#33A02C;#FB9A99;#E31A1C;#FDBF6F;#FF7F00;#CAB2D6;#6A3D9A;#FFFF99;#B15928"),
PASTEL13 ("black", "#FBB4AE;#B3CDE3;#CCEBC5"), // qualitative PASTEL1
PASTEL14 ("black", "#FBB4AE;#B3CDE3;#CCEBC5;#DECBE4"),
PASTEL15 ("black", "#FBB4AE;#B3CDE3;#CCEBC5;#DECBE4;#FED9A6"),
PASTEL16 ("black", "#FBB4AE;#B3CDE3;#CCEBC5;#DECBE4;#FED9A6;#FFFFCC"),
PASTEL17 ("black", "#FBB4AE;#B3CDE3;#CCEBC5;#DECBE4;#FED9A6;#FFFFCC;#E5D8BD"),
PASTEL18 ("black", "#FBB4AE;#B3CDE3;#CCEBC5;#DECBE4;#FED9A6;#FFFFCC;#E5D8BD;#FDDAEC"),
PASTEL19 ("black", "#FBB4AE;#B3CDE3;#CCEBC5;#DECBE4;#FED9A6;#FFFFCC;#E5D8BD;#FDDAEC;#F2F2F2"),
PASTEL23 ("black", "#B3E2CD;#FDCDAC;#CBD5E8"), // qualitative PASTEL2
PASTEL24 ("black", "#B3E2CD;#FDCDAC;#CBD5E8;#F4CAE4"),
PASTEL25 ("black", "#B3E2CD;#FDCDAC;#CBD5E8;#F4CAE4;#E6F5C9"),
PASTEL26 ("black", "#B3E2CD;#FDCDAC;#CBD5E8;#F4CAE4;#E6F5C9;#FFF2AE"),
PASTEL27 ("black", "#B3E2CD;#FDCDAC;#CBD5E8;#F4CAE4;#E6F5C9;#FFF2AE;#F1E2CC"),
PASTEL28 ("black", "#B3E2CD;#FDCDAC;#CBD5E8;#F4CAE4;#E6F5C9;#FFF2AE;#F1E2CC;#CCCCCC"),
//PIYG3 ("#F7F7F7", "#E9A3C9;#A1D76A"),
//PIYG4 ("#F1B6DA", "#D01C8B;#B8E186;#4DAC26"),
//PIYG5 ("#F7F7F7", "#D01C8B;#F1B6DA;#B8E186;#4DAC26"),
//PIYG6 ("#FDE0EF", "#C51B7D;#E9A3C9;#E6F5D0;#A1D76A;#4D9221"),
//PIYG7 ("#F7F7F7", "#C51B7D;#E9A3C9;#FDE0EF;#E6F5D0;#A1D76A;#4D9221"),
//PIYG8 ("#FDE0EF", "#C51B7D;#DE77AE;#F1B6DA;#E6F5D0;#B8E186;#7FBC41;#4D9221"),
//PIYG9 ("#F7F7F7", "#C51B7D;#DE77AE;#F1B6DA;#FDE0EF;#E6F5D0;#B8E186;#7FBC41;#4D9221"),
//PIYG10 ("#FDE0EF", "#8E0152;#C51B7D;#DE77AE;#F1B6DA;#E6F5D0;#B8E186;#7FBC41;#4D9221;#276419"),
//PIYG11 ("#F7F7F7", "#8E0152;#C51B7D;#DE77AE;#F1B6DA;#FDE0EF;#E6F5D0;#B8E186;#7FBC41;#4D9221;#276419"),
//PRGN3 ("#F7F7F7", "#AF8DC3;#7FBF7B"),
//PRGN4 ("#A6DBA0", "#7B3294;#C2A5CF;#008837"),
//PRGN5 ("#F7F7F7", "#7B3294;#C2A5CF;#A6DBA0;#008837"),
//PRGN6 ("#D9F0D3", "#762A83;#AF8DC3;#E7D4E8;#7FBF7B;#1B7837"),
//PRGN7 ("#F7F7F7", "#762A83;#AF8DC3;#E7D4E8;#D9F0D3;#7FBF7B;#1B7837"),
//PRGN8 ("#D9F0D3", "#762A83;#9970AB;#C2A5CF;#E7D4E8;#A6DBA0;#5AAE61;#1B7837"),
//PRGN9 ("#F7F7F7", "#762A83;#9970AB;#C2A5CF;#E7D4E8;#D9F0D3;#A6DBA0;#5AAE61;#1B7837"),
//PRGN10 ("#D9F0D3", "#40004B;#762A83;#9970AB;#C2A5CF;#E7D4E8;#A6DBA0;#5AAE61;#1B7837;#00441B"),
//PRGN11 ("#F7F7F7", "#40004B;#762A83;#9970AB;#C2A5CF;#E7D4E8;#D9F0D3;#A6DBA0;#5AAE61;#1B7837;#00441B"),
//PUBU3 ("#ECE7F2", "#A6BDDB;#2B8CBE"),
//PUBU4 ("#F1EEF6", "#BDC9E1;#74A9CF;#0570B0"),
//PUBU5 ("#F1EEF6", "#BDC9E1;#74A9CF;#2B8CBE;#045A8D"),
//PUBU6 ("#F1EEF6", "#D0D1E6;#A6BDDB;#74A9CF;#2B8CBE;#045A8D"),
//PUBU7 ("#F1EEF6", "#D0D1E6;#A6BDDB;#74A9CF;#3690C0;#0570B0;#034E7B"),
//PUBU8 ("#FFF7FB", "#ECE7F2;#D0D1E6;#A6BDDB;#74A9CF;#3690C0;#0570B0;#034E7B"),
//PUBU9 ("#FFF7FB", "#ECE7F2;#D0D1E6;#A6BDDB;#74A9CF;#3690C0;#0570B0;#045A8D;#023858"),
//PUBUGN3 ("#ECE2F0", "#A6BDDB;#1C9099"),
//PUBUGN4 ("#F6EFF7", "#BDC9E1;#67A9CF;#02818A"),
//PUBUGN5 ("#F6EFF7", "#BDC9E1;#67A9CF;#1C9099;#016C59"),
//PUBUGN6 ("#F6EFF7", "#D0D1E6;#A6BDDB;#67A9CF;#1C9099;#016C59"),
//PUBUGN7 ("#F6EFF7", "#D0D1E6;#A6BDDB;#67A9CF;#3690C0;#02818A;#016450"),
//PUBUGN8 ("#FFF7FB", "#ECE2F0;#D0D1E6;#A6BDDB;#67A9CF;#3690C0;#02818A;#016450"),
//PUBUGN9 ("#FFF7FB", "#ECE2F0;#D0D1E6;#A6BDDB;#67A9CF;#3690C0;#02818A;#016C59;#014636"),
//PUOR3 ("#F7F7F7", "#F1A340;#998EC3"),
//PUOR4 ("#FDB863", "#E66101;#B2ABD2;#5E3C99"),
//PUOR5 ("#F7F7F7", "#E66101;#FDB863;#B2ABD2;#5E3C99"),
//PUOR6 ("#FEE0B6", "#B35806;#F1A340;#D8DAEB;#998EC3;#542788"),
//PUOR7 ("#F7F7F7", "#B35806;#F1A340;#FEE0B6;#D8DAEB;#998EC3;#542788"),
//PUOR8 ("#FEE0B6", "#B35806;#E08214;#FDB863;#D8DAEB;#B2ABD2;#8073AC;#542788"),
//PUOR9 ("#F7F7F7", "#B35806;#E08214;#FDB863;#FEE0B6;#D8DAEB;#B2ABD2;#8073AC;#542788"),
//PUOR10 ("#FEE0B6", "#7F3B08;#B35806;#E08214;#FDB863;#D8DAEB;#B2ABD2;#8073AC;#542788;#2D004B"),
//PUOR11 ("#F7F7F7", "#7F3B08;#B35806;#E08214;#FDB863;#FEE0B6;#D8DAEB;#B2ABD2;#8073AC;#542788;#2D004B"),
//PURD3 ("#E7E1EF", "#C994C7;#DD1C77"),
//PURD4 ("#F1EEF6", "#D7B5D8;#DF65B0;#CE1256"),
//PURD5 ("#F1EEF6", "#D7B5D8;#DF65B0;#DD1C77;#980043"),
//PURD6 ("#F1EEF6", "#D4B9DA;#C994C7;#DF65B0;#DD1C77;#980043"),
//PURD7 ("#F1EEF6", "#D4B9DA;#C994C7;#DF65B0;#E7298A;#CE1256;#91003F"),
//PURD8 ("#F7F4F9", "#E7E1EF;#D4B9DA;#C994C7;#DF65B0;#E7298A;#CE1256;#91003F"),
//PURD9 ("#F7F4F9", "#E7E1EF;#D4B9DA;#C994C7;#DF65B0;#E7298A;#CE1256;#980043;#67001F"),
//PURPLES3 ("#EFEDF5", "#BCBDDC;#756BB1"),
//PURPLES4 ("#F2F0F7", "#CBC9E2;#9E9AC8;#6A51A3"),
//PURPLES5 ("#F2F0F7", "#CBC9E2;#9E9AC8;#756BB1;#54278F"),
//PURPLES6 ("#F2F0F7", "#DADAEB;#BCBDDC;#9E9AC8;#756BB1;#54278F"),
//PURPLES7 ("#F2F0F7", "#DADAEB;#BCBDDC;#9E9AC8;#807DBA;#6A51A3;#4A1486"),
//PURPLES8 ("#FCFBFD", "#EFEDF5;#DADAEB;#BCBDDC;#9E9AC8;#807DBA;#6A51A3;#4A1486"),
//PURPLES9 ("#FCFBFD", "#EFEDF5;#DADAEB;#BCBDDC;#9E9AC8;#807DBA;#6A51A3;#54278F;#3F007D"),
//RDBU3 ("#F7F7F7", "#EF8A62;#67A9CF"),
//RDBU4 ("#F4A582", "#CA0020;#92C5DE;#0571B0"),
//RDBU5 ("#F7F7F7", "#CA0020;#F4A582;#92C5DE;#0571B0"),
//RDBU6 ("#FDDBC7", "#B2182B;#EF8A62;#D1E5F0;#67A9CF;#2166AC"),
//RDBU7 ("#F7F7F7", "#B2182B;#EF8A62;#FDDBC7;#D1E5F0;#67A9CF;#2166AC"),
//RDBU8 ("#FDDBC7", "#B2182B;#D6604D;#F4A582;#D1E5F0;#92C5DE;#4393C3;#2166AC"),
//RDBU9 ("#F7F7F7", "#B2182B;#D6604D;#F4A582;#FDDBC7;#D1E5F0;#92C5DE;#4393C3;#2166AC"),
//RDBU10 ("#FDDBC7", "#67001F;#B2182B;#D6604D;#F4A582;#D1E5F0;#92C5DE;#4393C3;#2166AC;#053061"),
//RDBU11 ("#F7F7F7", "#67001F;#B2182B;#D6604D;#F4A582;#FDDBC7;#D1E5F0;#92C5DE;#4393C3;#2166AC;#053061"),
//RDGY3 ("#FFFFFF", "#EF8A62;#999999"),
//RDGY4 ("#BABABA", "#CA0020;#F4A582;#404040"),
//RDGY5 ("#FFFFFF", "#CA0020;#F4A582;#BABABA;#404040"),
//RDGY6 ("#E0E0E0", "#B2182B;#EF8A62;#FDDBC7;#999999;#4D4D4D"),
//RDGY7 ("#FFFFFF", "#B2182B;#EF8A62;#FDDBC7;#E0E0E0;#999999;#4D4D4D"),
//RDGY8 ("#E0E0E0", "#B2182B;#D6604D;#F4A582;#FDDBC7;#BABABA;#878787;#4D4D4D"),
//RDGY9 ("#FFFFFF", "#B2182B;#D6604D;#F4A582;#FDDBC7;#E0E0E0;#BABABA;#878787;#4D4D4D"),
//RDGY10 ("#E0E0E0", "#67001F;#B2182B;#D6604D;#F4A582;#FDDBC7;#BABABA;#878787;#4D4D4D;#1A1A1A"),
//RDGY11 ("#FFFFFF", "#67001F;#B2182B;#D6604D;#F4A582;#FDDBC7;#E0E0E0;#BABABA;#878787;#4D4D4D;#1A1A1A"),
//RDPU3 ("#FDE0DD", "#FA9FB5;#C51B8A"),
//RDPU4 ("#FEEBE2", "#FBB4B9;#F768A1;#AE017E"),
//RDPU5 ("#FEEBE2", "#FBB4B9;#F768A1;#C51B8A;#7A0177"),
//RDPU6 ("#FEEBE2", "#FCC5C0;#FA9FB5;#F768A1;#C51B8A;#7A0177"),
//RDPU7 ("#FEEBE2", "#FCC5C0;#FA9FB5;#F768A1;#DD3497;#AE017E;#7A0177"),
//RDPU8 ("#FFF7F3", "#FDE0DD;#FCC5C0;#FA9FB5;#F768A1;#DD3497;#AE017E;#7A0177"),
//RDPU9 ("#FFF7F3", "#FDE0DD;#FCC5C0;#FA9FB5;#F768A1;#DD3497;#AE017E;#7A0177;#49006A"),
//RDYLBU3 ("#FFFFBF", "#FC8D59;#91BFDB"),
//RDYLBU4 ("#FDAE61", "#D7191C;#ABD9E9;#2C7BB6"),
//RDYLBU5 ("#FFFFBF", "#D7191C;#FDAE61;#ABD9E9;#2C7BB6"),
//RDYLBU6 ("#FEE090", "#D73027;#FC8D59;#E0F3F8;#91BFDB;#4575B4"),
//RDYLBU7 ("#FFFFBF", "#D73027;#FC8D59;#FEE090;#E0F3F8;#91BFDB;#4575B4"),
//RDYLBU8 ("#FEE090", "#D73027;#F46D43;#FDAE61;#E0F3F8;#ABD9E9;#74ADD1;#4575B4"),
//RDYLBU9 ("#FFFFBF", "#D73027;#F46D43;#FDAE61;#FEE090;#E0F3F8;#ABD9E9;#74ADD1;#4575B4"),
//RDYLBU10 ("#FEE090", "#A50026;#D73027;#F46D43;#FDAE61;#E0F3F8;#ABD9E9;#74ADD1;#4575B4;#313695"),
//RDYLBU11 ("#FFFFBF", "#A50026;#D73027;#F46D43;#FDAE61;#FEE090;#E0F3F8;#ABD9E9;#74ADD1;#4575B4;#313695"),
//RDYLGN3 ("#FFFFBF", "#FC8D59;#91CF60"),
//RDYLGN4 ("#FDAE61", "#D7191C;#A6D96A;#1A9641"),
//RDYLGN5 ("#FFFFBF", "#D7191C;#FDAE61;#A6D96A;#1A9641"),
//RDYLGN6 ("#FEE08B", "#D73027;#FC8D59;#D9EF8B;#91CF60;#1A9850"),
//RDYLGN7 ("#FFFFBF", "#D73027;#FC8D59;#FEE08B;#D9EF8B;#91CF60;#1A9850"),
//RDYLGN8 ("#FEE08B", "#D73027;#F46D43;#FDAE61;#D9EF8B;#A6D96A;#66BD63;#1A9850"),
//RDYLGN9 ("#FFFFBF", "#D73027;#F46D43;#FDAE61;#FEE08B;#D9EF8B;#A6D96A;#66BD63;#1A9850"),
//RDYLGN10 ("#FEE08B", "#A50026;#D73027;#F46D43;#FDAE61;#D9EF8B;#A6D96A;#66BD63;#1A9850;#006837"),
//RDYLGN11 ("#FFFFBF", "#A50026;#D73027;#F46D43;#FDAE61;#FEE08B;#D9EF8B;#A6D96A;#66BD63;#1A9850;#006837"),
//REDS3 ("#FEE0D2", "#FC9272;#DE2D26"),
//REDS4 ("#FEE5D9", "#FCAE91;#FB6A4A;#CB181D"),
//REDS5 ("#FEE5D9", "#FCAE91;#FB6A4A;#DE2D26;#A50F15"),
//REDS6 ("#FEE5D9", "#FCBBA1;#FC9272;#FB6A4A;#DE2D26;#A50F15"),
//REDS7 ("#FEE5D9", "#FCBBA1;#FC9272;#FB6A4A;#EF3B2C;#CB181D;#99000D"),
//REDS8 ("#FFF5F0", "#FEE0D2;#FCBBA1;#FC9272;#FB6A4A;#EF3B2C;#CB181D;#99000D"),
//REDS9 ("#FFF5F0", "#FEE0D2;#FCBBA1;#FC9272;#FB6A4A;#EF3B2C;#CB181D;#A50F15;#67000D"),
SET13 ("white", "#E41A1C;#377EB8;#4DAF4A"), // qualitative SET1
SET14 ("white", "#E41A1C;#377EB8;#4DAF4A;#984EA3"),
SET15 ("white", "#E41A1C;#377EB8;#4DAF4A;#984EA3;#FF7F00"),
SET16 ("white", "#E41A1C;#377EB8;#4DAF4A;#984EA3;#FF7F00;#FFFF33"),
SET17 ("white", "#E41A1C;#377EB8;#4DAF4A;#984EA3;#FF7F00;#FFFF33;#A65628"),
SET18 ("white", "#E41A1C;#377EB8;#4DAF4A;#984EA3;#FF7F00;#FFFF33;#A65628;#F781BF"),
SET19 ("white", "#E41A1C;#377EB8;#4DAF4A;#984EA3;#FF7F00;#FFFF33;#A65628;#F781BF;#999999"),
SET23 ("black", "#66C2A5;#FC8D62;#8DA0CB"), // qualitative SET2
SET24 ("black", "#66C2A5;#FC8D62;#8DA0CB;#E78AC3"),
SET25 ("black", "#66C2A5;#FC8D62;#8DA0CB;#E78AC3;#A6D854"),
SET26 ("black", "#66C2A5;#FC8D62;#8DA0CB;#E78AC3;#A6D854;#FFD92F"),
SET27 ("black", "#66C2A5;#FC8D62;#8DA0CB;#E78AC3;#A6D854;#FFD92F;#E5C494"),
SET28 ("black", "#66C2A5;#FC8D62;#8DA0CB;#E78AC3;#A6D854;#FFD92F;#E5C494;#B3B3B3"),
SET33 ("black", "#8DD3C7;#FFFFB3;#BEBADA"), // qualitative SET3
SET34 ("black", "#8DD3C7;#FFFFB3;#BEBADA;#FB8072"),
SET35 ("black", "#8DD3C7;#FFFFB3;#BEBADA;#FB8072;#80B1D3"),
SET36 ("black", "#8DD3C7;#FFFFB3;#BEBADA;#FB8072;#80B1D3;#FDB462"),
SET37 ("black", "#8DD3C7;#FFFFB3;#BEBADA;#FB8072;#80B1D3;#FDB462;#B3DE69"),
SET38 ("black", "#8DD3C7;#FFFFB3;#BEBADA;#FB8072;#80B1D3;#FDB462;#B3DE69;#FCCDE5"),
SET39 ("black", "#8DD3C7;#FFFFB3;#BEBADA;#FB8072;#80B1D3;#FDB462;#B3DE69;#FCCDE5;#D9D9D9"),
SET310 ("black", "#8DD3C7;#FFFFB3;#BEBADA;#FB8072;#80B1D3;#FDB462;#B3DE69;#FCCDE5;#D9D9D9;#BC80BD"),
SET311 ("black", "#8DD3C7;#FFFFB3;#BEBADA;#FB8072;#80B1D3;#FDB462;#B3DE69;#FCCDE5;#D9D9D9;#BC80BD;#CCEBC5"),
SET312 ("black", "#8DD3C7;#FFFFB3;#BEBADA;#FB8072;#80B1D3;#FDB462;#B3DE69;#FCCDE5;#D9D9D9;#BC80BD;#CCEBC5;#FFED6F"),
//SPECTRAL3 ("#FFFFBF", "#FC8D59;#99D594"),
//SPECTRAL4 ("#FDAE61", "#D7191C;#ABDDA4;#2B83BA"),
//SPECTRAL5 ("#FFFFBF", "#D7191C;#FDAE61;#ABDDA4;#2B83BA"),
//SPECTRAL6 ("#FEE08B", "#D53E4F;#FC8D59;#E6F598;#99D594;#3288BD"),
//SPECTRAL7 ("#FFFFBF", "#D53E4F;#FC8D59;#FEE08B;#E6F598;#99D594;#3288BD"),
//SPECTRAL8 ("#FEE08B", "#D53E4F;#F46D43;#FDAE61;#E6F598;#ABDDA4;#66C2A5;#3288BD"),
//SPECTRAL9 ("#FFFFBF", "#D53E4F;#F46D43;#FDAE61;#FEE08B;#E6F598;#ABDDA4;#66C2A5;#3288BD"),
//SPECTRAL10 ("#FEE08B", "#9E0142;#D53E4F;#F46D43;#FDAE61;#E6F598;#ABDDA4;#66C2A5;#3288BD;#5E4FA2"),
//SPECTRAL11 ("#FFFFBF", "#9E0142;#D53E4F;#F46D43;#FDAE61;#FEE08B;#E6F598;#ABDDA4;#66C2A5;#3288BD;#5E4FA2"),
//YLGN3 ("#F7FCB9", "#ADDD8E;#31A354"),
//YLGN4 ("#FFFFCC", "#C2E699;#78C679;#238443"),
//YLGN5 ("#FFFFCC", "#C2E699;#78C679;#31A354;#006837"),
//YLGN6 ("#FFFFCC", "#D9F0A3;#ADDD8E;#78C679;#31A354;#006837"),
//YLGN7 ("#FFFFCC", "#D9F0A3;#ADDD8E;#78C679;#41AB5D;#238443;#005A32"),
//YLGN8 ("#FFFFE5", "#F7FCB9;#D9F0A3;#ADDD8E;#78C679;#41AB5D;#238443;#005A32"),
//YLGN9 ("#FFFFE5", "#F7FCB9;#D9F0A3;#ADDD8E;#78C679;#41AB5D;#238443;#006837;#004529"),
//YLGNBU3 ("#EDF8B1", "#7FCDBB;#2C7FB8"),
//YLGNBU4 ("#FFFFCC", "#A1DAB4;#41B6C4;#225EA8"),
//YLGNBU5 ("#FFFFCC", "#A1DAB4;#41B6C4;#2C7FB8;#253494"),
//YLGNBU6 ("#FFFFCC", "#C7E9B4;#7FCDBB;#41B6C4;#2C7FB8;#253494"),
//YLGNBU7 ("#FFFFCC", "#C7E9B4;#7FCDBB;#41B6C4;#1D91C0;#225EA8;#0C2C84"),
//YLGNBU8 ("#FFFFD9", "#EDF8B1;#C7E9B4;#7FCDBB;#41B6C4;#1D91C0;#225EA8;#0C2C84"),
//YLGNBU9 ("#FFFFD9", "#EDF8B1;#C7E9B4;#7FCDBB;#41B6C4;#1D91C0;#225EA8;#253494;#081D58"),
//YLORBR3 ("#FFF7BC", "#FEC44F;#D95F0E"),
//YLORBR4 ("#FFFFD4", "#FED98E;#FE9929;#CC4C02"),
//YLORBR5 ("#FFFFD4", "#FED98E;#FE9929;#D95F0E;#993404"),
//YLORBR6 ("#FFFFD4", "#FEE391;#FEC44F;#FE9929;#D95F0E;#993404"),
//YLORBR7 ("#FFFFD4", "#FEE391;#FEC44F;#FE9929;#EC7014;#CC4C02;#8C2D04"),
//YLORBR8 ("#FFFFE5", "#FFF7BC;#FEE391;#FEC44F;#FE9929;#EC7014;#CC4C02;#8C2D04"),
//YLORBR9 ("#FFFFE5", "#FFF7BC;#FEE391;#FEC44F;#FE9929;#EC7014;#CC4C02;#993404;#662506"),
//YLORRD3 ("#FFEDA0", "#FEB24C;#F03B20"),
//YLORRD4 ("#FFFFB2", "#FECC5C;#FD8D3C;#E31A1C"),
//YLORRD5 ("#FFFFB2", "#FECC5C;#FD8D3C;#F03B20;#BD0026"),
//YLORRD6 ("#FFFFB2", "#FED976;#FEB24C;#FD8D3C;#F03B20;#BD0026"),
//YLORRD7 ("#FFFFB2", "#FED976;#FEB24C;#FD8D3C;#FC4E2A;#E31A1C;#B10026"),
//YLORRD8 ("#FFFFCC", "#FFEDA0;#FED976;#FEB24C;#FD8D3C;#FC4E2A;#E31A1C;#B10026"),
//YLORRD9 ("#FFFFCC", "#FFEDA0;#FED976;#FEB24C;#FD8D3C;#FC4E2A;#E31A1C;#BD0026;#800026"),

// Playing with some alternate backgrounds:
//PURPLESREV9   ("#3F007D", "#FCFBFD;#EFEDF5;#DADAEB;#BCBDDC;#9E9AC8;#807DBA;#6A51A3;#54278F"),
//PURPLESWHITE9 ("white",   "#FCFBFD;#EFEDF5;#DADAEB;#BCBDDC;#9E9AC8;#807DBA;#6A51A3;#54278F;#3F007D"),
//PURPLESBLACK9 ("black",   "#FCFBFD;#EFEDF5;#DADAEB;#BCBDDC;#9E9AC8;#807DBA;#6A51A3;#54278F;#3F007D"),

// From http://www.biostat.jhsph.edu/bit/compintro/bruce/hcl-colors.pdf
// 2012-04-27 - elb
// Qualitative:
HCLDYNAMIC  ("white", "#DCAF94;#9AC47F;#73C3D8;#D3A9E4"),
HCLHARMONIC ("white", "#CCB777;#9AC47F;#65C7BD;#98BBE8"),
HCLCOLD     ("white", "#BBB1EC;#8CBFE3;#65C6C6;#7CC89E"),
HCLWARM     ("white", "#B7BE6C;#D3B380;#E0A8AA;#E0A3D2");
// Diverging:
// Background taken from the middle.
//HCLBLUERUST      ("#E7E7E7", "#0951BA;#9098C9;#C9CDDD;#DCC8CB;#C58C96;#9D1B4B"),
//HCLBLUEROSE      ("#E7E7E7", "#5D82F0;#98A6EC;#C2C6EB;#EABEC4;#E490A3;#D9577E"),
//HCLGREENORANGE   ("#E7E7E7", "#30CC20;#A0DA9C;#D1E4CE;#EEDCCF;#F1C698;#F1C698"),
//HCLTURQUOISEPINK ("#F3F3F3", "#32D5CC;#AEE3DD;#DDEEEC;#F5E7F0;#F7D1E7;#F7AFDE"),
// Sequential:
//HCLGREYS   ("white", "#E7E7E7;#E6E6E6;#E4E5E4;#E0E0E0;#D9D9D9;#D0D0D0;#C3C3C3;#B3B3B3;#A2A2A2;#8C8C8C;#747474;#585858"),
//HCLBLUES   ("white", "#E7E7E7;#E6E6E7;#E4E5E6;#DFDFE5;#D7D9E2;#CCCFDD;#BFC3D9;#AEB2D3;#98A2CC;#7F8AC4;#5B72BD;#0951BA"),
//HCLMAROONS ("white", "#E7EBC4;#EFE88F;#F3E385;#F7DA7C;#F7D073;#F3C26F;#EDB26A;#E49E65;#D6885F;#C56E5B;#B34E56;#9D1B4B"),
//HCLGREENS  ("white", "#E7E7E7;#F9DFDF;#F8DAD0;#F4D3BF;#EBCBAA;#DCC592;#CDBC78;#A2AC41;#85A21F;#5E970D;#5E970D;#1D8B0E"),
//HCLVIOLETS ("white", "#ECB9C1;#E7AEC1;#E3A5C2;#DC9AC4;#D492C7;#CC89C9;#C181CA;#B47ACB;#A574CC;#9170CC;#766CCC;#4B6BCA"),

//    HTML   ("white",  "silver;gray;black;red;maroon;yellow;olive;lime;green;aqua;teal;blue;navy;fuchsia;purple;orange");

    /**
     * A map of SVG 1.0 color names to #rrggbb hex strings that Swing can handle.
     * Swing knows some of these names, but not many.
     */
    public static final Map<String, String> SVGColors = new HashMap<String, String>();
    static {
        SVGColors.put("aliceblue", "#F0F8FF");
        SVGColors.put("antiquewhite", "#FAEBD7");
        SVGColors.put("aqua", "#00FFFF"); // known to Swing
        SVGColors.put("aquamarine", "#7FFFD4");
        SVGColors.put("azure", "#F0FFFF");
        SVGColors.put("beige", "#F5F5DC");
        SVGColors.put("bisque", "#FFE4C4");
        SVGColors.put("black", "#000000"); // known to Swing
        SVGColors.put("blanchedalmond", "#FFEBCD");
        SVGColors.put("blue", "#0000FF"); // known to Swing
        SVGColors.put("blueviolet", "#8A2BE2");
        SVGColors.put("brown", "#A52A2A");
        SVGColors.put("burlywood", "#DEB887");
        SVGColors.put("cadetblue", "#5F9EA0");
        SVGColors.put("chartreuse", "#7FFF00");
        SVGColors.put("chocolate", "#D2691E");
        SVGColors.put("coral", "#FF7F50");
        SVGColors.put("cornflowerblue", "#6495ED");
        SVGColors.put("cornsilk", "#FFF8DC");
        SVGColors.put("crimson", "#DC143C");
        SVGColors.put("cyan", "#00FFFF");
        SVGColors.put("darkblue", "#00008B");
        SVGColors.put("darkcyan", "#008B8B");
        SVGColors.put("darkgoldenrod", "#B8860B");
        SVGColors.put("darkgray", "#A9A9A9");
        SVGColors.put("darkgreen", "#006400");
        SVGColors.put("darkgrey", "#A9A9A9");
        SVGColors.put("darkkhaki", "#BDB76B");
        SVGColors.put("darkmagenta", "#8B008B");
        SVGColors.put("darkolivegreen", "#556B2F");
        SVGColors.put("darkorange", "#FF8C00");
        SVGColors.put("darkorchid", "#9932CC");
        SVGColors.put("darkred", "#8B0000");
        SVGColors.put("darksalmon", "#E9967A");
        SVGColors.put("darkseagreen", "#8FBC8F");
        SVGColors.put("darkslateblue", "#483D8B");
        SVGColors.put("darkslategray", "#2F4F4F");
        SVGColors.put("darkslategrey", "#2F4F4F");
        SVGColors.put("darkturquoise", "#00CED1");
        SVGColors.put("darkviolet", "#9400D3");
        SVGColors.put("deeppink", "#FF1493");
        SVGColors.put("deepskyblue", "#00BFFF");
        SVGColors.put("dimgray", "#696969");
        SVGColors.put("dimgrey", "#696969");
        SVGColors.put("dodgerblue", "#1E90FF");
        SVGColors.put("firebrick", "#B22222");
        SVGColors.put("floralwhite", "#FFFAF0");
        SVGColors.put("forestgreen", "#228B22");
        SVGColors.put("fuchsia", "#FF00FF"); // unknown to Swing even though SVG 1.0
        SVGColors.put("gainsboro", "#DCDCDC");
        SVGColors.put("ghostwhite", "#F8F8FF");
        SVGColors.put("gold", "#FFD700");
        SVGColors.put("goldenrod", "#DAA520");
        SVGColors.put("gray", "#808080"); // known to Swing
        SVGColors.put("grey", "gray");
        SVGColors.put("green", "#008000"); // known to Swing
        SVGColors.put("greenyellow", "#ADFF2F");
        SVGColors.put("honeydew", "#F0FFF0");
        SVGColors.put("hotpink", "#FF69B4");
        SVGColors.put("indianred", "#CD5C5C");
        SVGColors.put("indigo", "#4B0082");
        SVGColors.put("ivory", "#FFFFF0");
        SVGColors.put("khaki", "#F0E68C");
        SVGColors.put("lavender", "#E6E6FA");
        SVGColors.put("lavenderblush", "#FFF0F5");
        SVGColors.put("lawngreen", "#7CFC00");
        SVGColors.put("lemonchiffon", "#FFFACD");
        SVGColors.put("lightblue", "#ADD8E6");
        SVGColors.put("lightcoral", "#F08080");
        SVGColors.put("lightcyan", "#E0FFFF");
        SVGColors.put("lightgoldenrodyellow", "#FAFAD2");
        SVGColors.put("lightgray", "#D3D3D3");       // IE6 breaks on this color
        SVGColors.put("lightgreen", "#90EE90");
        SVGColors.put("lightgrey", "#D3D3D3");       // IE6 breaks on this color
        SVGColors.put("lightpink", "#FFB6C1");
        SVGColors.put("lightsalmon", "#FFA07A");
        SVGColors.put("lightseagreen", "#20B2AA");
        SVGColors.put("lightskyblue", "#87CEFA");
        SVGColors.put("lightslategray", "#778899");
        SVGColors.put("lightslategrey", "#778899");
        SVGColors.put("lightsteelblue", "#B0C4DE");
        SVGColors.put("lightyellow", "#FFFFE0");
        SVGColors.put("lime", "#00FF00"); // known to Swing
        SVGColors.put("limegreen", "#32CD32");
        SVGColors.put("linen", "#FAF0E6");
        SVGColors.put("magenta", "#FF00FF");
        SVGColors.put("maroon", "#800000"); // known to Swing
        SVGColors.put("mediumaquamarine", "#66CDAA");
        SVGColors.put("mediumblue", "#0000CD");
        SVGColors.put("mediumorchid", "#BA55D3");
        SVGColors.put("mediumpurple", "#9370DB");
        SVGColors.put("mediumseagreen", "#3CB371");
        SVGColors.put("mediumslateblue", "#7B68EE");
        SVGColors.put("mediumspringgreen", "#00FA9A");
        SVGColors.put("mediumturquoise", "#48D1CC");
        SVGColors.put("mediumvioletred", "#C71585");
        SVGColors.put("midnightblue", "#191970");
        SVGColors.put("mintcream", "#F5FFFA");
        SVGColors.put("mistyrose", "#FFE4E1");
        SVGColors.put("moccasin", "#FFE4B5");
        SVGColors.put("navajowhite", "#FFDEAD");
        SVGColors.put("navy", "#000080"); // known to Swing
        SVGColors.put("oldlace", "#FDF5E6");
        SVGColors.put("olive", "#808000"); // known to Swing
        SVGColors.put("olivedrab", "#6B8E23");
        SVGColors.put("orange", "#FFA500");
        SVGColors.put("orangered", "#FF4500");
        SVGColors.put("orchid", "#DA70D6");
        SVGColors.put("palegoldenrod", "#EEE8AA");
        SVGColors.put("palegreen", "#98FB98");
        SVGColors.put("paleturquoise", "#AFEEEE");
        SVGColors.put("palevioletred", "#DB7093");
        SVGColors.put("papayawhip", "#FFEFD5");
        SVGColors.put("peachpuff", "#FFDAB9");
        SVGColors.put("peru", "#CD853F");
        SVGColors.put("pink", "#FFC0CB");
        SVGColors.put("plum", "#DDA0DD");
        SVGColors.put("powderblue", "#B0E0E6");
        SVGColors.put("purple", "#800080"); // known to Swing
        SVGColors.put("red", "#FF0000"); // known to Swing
        SVGColors.put("rosybrown", "#BC8F8F");
        SVGColors.put("royalblue", "#4169E1");
        SVGColors.put("saddlebrown", "#8B4513");
        SVGColors.put("salmon", "#FA8072");
        SVGColors.put("sandybrown", "#F4A460");
        SVGColors.put("seagreen", "#2E8B57");
        SVGColors.put("seashell", "#FFF5EE");
        SVGColors.put("sienna", "#A0522D");
        SVGColors.put("silver", "#C0C0C0"); // known to Swing
        SVGColors.put("skyblue", "#87CEEB");
        SVGColors.put("slateblue", "#6A5ACD");
        SVGColors.put("slategray", "#708090");
        SVGColors.put("slategrey", "#708090");
        SVGColors.put("snow", "#FFFAFA");
        SVGColors.put("springgreen", "#00FF7F");
        SVGColors.put("steelblue", "#4682B4");
        SVGColors.put("tan", "#D2B48C");
        SVGColors.put("teal", "#008080"); // known to Swing
        SVGColors.put("thistle", "#D8BFD8");
        SVGColors.put("tomato", "#FF6347");
        SVGColors.put("turquoise", "#40E0D0");
        SVGColors.put("violet", "#EE82EE");
        SVGColors.put("wheat", "#F5DEB3");
        SVGColors.put("white", "#FFFFFF"); // known to Swing
        SVGColors.put("whitesmoke", "#F5F5F5");
        SVGColors.put("yellow", "#FFFF00"); // known to Swing
        SVGColors.put("yellowgreen", "#9ACD32");
    }
    
    private String background;
    private String[] foregrounds;

    ColorScheme(String theBackground, String theForegrounds) {
        this.background = theBackground;
        this.foregrounds = theForegrounds.split(";");
    }

    public String getBackground() {
        if (SVGColors.containsKey(this.background)) {
            this.background = SVGColors.get(this.background);
        }
        return this.background;
    }
    public int numberOfForegrounds() {
        return this.foregrounds.length;
    }
    public String getForeground(int theOne) {
        if (SVGColors.containsKey(foregrounds[theOne])) {
            foregrounds[theOne] = SVGColors.get(foregrounds[theOne]);
        }
        return this.foregrounds[theOne];
    }
    public List<String> getForegroundColors() {
        for (int i = 0; i < this.foregrounds.length; i++) {
            if (SVGColors.containsKey(foregrounds[i])) {
                foregrounds[i] = SVGColors.get(foregrounds[i]);
            }
        }
        return Arrays.asList(this.foregrounds);
    }

	/**
	 * Override to provide a graphic interpretation, swatches of colors.
	 * This is not really in the original spirit of the toString method
	 * as a debugging tool.
	 * We use it to make it easier to include in a JComboBox.
	 * @return 
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("<html>");
        sb.append("<span style=\"background-color: ").append(this.getBackground()).append("\"> &nbsp; ");
        for (String c: this.getForegroundColors()) {
			// http://www.unicode.org/charts/PDF/U2580.pdf
			// http://en.wikipedia.org/wiki/Box-drawing_character
			// Look for "Block Elements" in the above page.
			sb.append("<span style=\"color: ").append(c).append("\"> &#9608; </span>");
        }
		sb.append(" &nbsp;</span>");
 		//sb.append(" ").append(this.name()); // uncomment this to include names.
        return sb.toString();
	}
}
