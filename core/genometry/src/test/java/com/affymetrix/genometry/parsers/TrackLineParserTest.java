/*
 * TrackLineParserTest.java
 * JUnit based test
 *
 * Created on October 13, 2006, 5:08 PM
 */
package com.affymetrix.genometry.parsers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TrackLineParserTest {

    public TrackLineParserTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSetTrackProperties() {
        String str = "track foo=bar this=\"that\" color=123,100,10 useScore=1 ignore= ignore2 nothing=\"\" url=\"http://www.foo.bar?x=y&this=$$\"";
        TrackLineParser tlp = new TrackLineParser();
        tlp.parseTrackLine(str);

        assertEquals("bar", tlp.getTrackLineContent().get("foo"));
        assertEquals("that", tlp.getTrackLineContent().get("this"));
        assertEquals("1", tlp.getTrackLineContent().get("usescore"));
        assertEquals(null, tlp.getTrackLineContent().get("useScore"));
        assertEquals("", tlp.getTrackLineContent().get("nothing"));
        assertEquals(null, tlp.getTrackLineContent().get("ignore"));
        assertEquals(null, tlp.getTrackLineContent().get("ignore2"));
        assertEquals("http://www.foo.bar?x=y&this=$$", tlp.getTrackLineContent().get("url"));

    }

}
