/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.util;

import java.util.regex.*;

/**
 * Converts URLs to filenames that are supported on most file systems.
 * Trying to use something similar to the XML character reference format, but use it 
 *    for encoding characters that aren't supported in filenames on some systems,
 *    for info on XML character references see XML spec at http://www.w3.org/TR/2000/REC-xml-20001006
 *
 *  So when encoding a URL as a filename, a character CHAR in the URL that isn't supported 
 *    as a filename character gets encoded to "character reference" format, which is
 *    '&#' + (int)CHAR + ';'
 *  And when decoding filename to URL, any string in filename of form '&#' + (int)CHAR + ';' gets
 *    converted back to character CHAR
 *
 * UrlToFileName is intended mainly for caching URL content, so that a URL's content can be written to
 *    a file whose name is the encoded form of the URL
 */
public class UrlToFileName {

  /**
   *  Matches chars in URLs that need to encoded/escaped when converted to filename.
   *  Those are: 
   *  <pre>
   *  \ / : * ? " &gt; &lt; | +
   *  </pre>
   */
    public static final Pattern char_encode_pattern = Pattern.compile(
							 "[" +
                                                         "\\\\" +   //  match \
                                                         "/" +      //  match /
							 "\\:" +    //  match :
							 "\\*" +    //  match *
							 "\\?" +    //  match ?
							 "\\\"" +   //  match "
							 "\\<" +    //  match <
							 "\\>" +    //  match >
                                                         "\\|" +    //  match |
                                                         "\\+" +    //  match +
                                                         "]" );

  static final Pattern char_decode_pattern = Pattern.compile("&#(\\d+);"); 


  /**
   *  Convert URL to filename.
   */
  public static String encode(String url) {
    Matcher char_encode_matcher = char_encode_pattern.matcher(url);
    StringBuffer buf = new StringBuffer();
    while (char_encode_matcher.find()) {
      String grp = char_encode_matcher.group();
      char ch = grp.charAt(0);
      char_encode_matcher.appendReplacement(buf, "&#" + (int)ch + ";");
    }
    char_encode_matcher.appendTail(buf);
    return buf.toString();
  }


  /**
   *  Convert filename to URL.
   */
  public static String decode(String filename) {
    Matcher char_decode_matcher = char_decode_pattern.matcher(filename);
    StringBuffer buf = new StringBuffer();
    while (char_decode_matcher.find()) {
      String int_str = char_decode_matcher.group(1);
      int char_int = Integer.parseInt(int_str);
      char ch = (char)char_int;
      String char_str = Character.toString(ch);
      // can't add just '\' back, because then it will signal escaping the next char, so
      //     need to add '\\' instead
      if (char_str.equals("\\")) { char_str = "\\\\"; }
      char_decode_matcher.appendReplacement(buf, char_str);
    }
    char_decode_matcher.appendTail(buf);
    return buf.toString();
  }


  static String[] default_test_urls = { "http://test.url.com/testing/url/to/filename/encoding",
					"this\\should/test:all*the?chars\"that<need>encoding|I+hope" };
  /**
   *  a main() for testing purposes
   */
  public static void main(String[] args) {
    String test_urls[] = null;
    if (args.length > 0) {  test_urls = args; }
    else { test_urls = default_test_urls; }
    for (int i=0; i<test_urls.length; i++) {
      String test_url = test_urls[i];
      System.out.println("test " + i);
      System.out.println("original url:         " + test_url);
      System.out.println("encode(url):          " + encode(test_url));;
      System.out.println("decode(encode(url)):  " + decode(encode(test_url)));
    }
  }




}
