/*
 * TrackLineParserTest.java
 * JUnit based test
 *
 * Created on October 13, 2006, 5:08 PM
 */

package com.affymetrix.genometryImpl.parsers;

import junit.framework.*;

public class TrackLineParserTest extends TestCase {

  public TrackLineParserTest(String testName) {
    super(testName);
  }

  protected void setUp() throws Exception {
  }

  protected void tearDown() throws Exception {
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TrackLineParserTest.class);

    return suite;
  }

  public void testSetTrackProperties() {
    String str = "track foo=bar this=\"that\" color=123,100,10 useScore=1 ignore= ignore2 nothing=\"\" url=\"http://www.foo.bar?x=y&this=$$\"";
    TrackLineParser tlp = new TrackLineParser();
    tlp.parseTrackLine(str);

    assertEquals("bar", tlp.getCurrentTrackHash().get("foo"));
    assertEquals("that", tlp.getCurrentTrackHash().get("this"));
    assertEquals("1", tlp.getCurrentTrackHash().get("usescore"));
    assertEquals(null, tlp.getCurrentTrackHash().get("useScore"));
    assertEquals("", tlp.getCurrentTrackHash().get("nothing"));
    assertEquals(null, tlp.getCurrentTrackHash().get("ignore"));
    assertEquals(null, tlp.getCurrentTrackHash().get("ignore2"));
    assertEquals("http://www.foo.bar?x=y&this=$$", tlp.getCurrentTrackHash().get("url"));

  }

}
