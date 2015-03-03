/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.bookmarks;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author lorainelab
 */
public class BookMarkTest {

    @Test
    public void testParseQueryString() throws MalformedURLException {
        String name = "testBookMark";
        String comment = "";
        String url = "http://localhost?x=3&z&y=4&y=5";
        Bookmark bookmark = new Bookmark(name, "", url);
        ListMultimap<String, String> props = Bookmark.parseParameters(bookmark.getURL());
        List<String> results = props.get("x");
        assertTrue(results.get(0).equals("3"));
        results = props.get("z");
        assertTrue(results.get(0).equals(""));
        results = props.get("y");
        assertTrue(results.get(0).equals("4"));
        assertTrue(results.get(1).equals("5"));
    }

    @Test
    public void constructURLTest() throws UnsupportedEncodingException {
        ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.<String, String>builder();
        builder.put(Bookmark.SEQID, "testSeqID");
        builder.put(Bookmark.VERSION, "C_briggsae_Jan_2007");
        builder.put(Bookmark.START, "0");
        builder.put(Bookmark.END, "1000");
        String bookmarkUrl = Bookmark.constructURL(builder.build());
        assertEquals("http://localhost:7085/IGBControl?seqid=testSeqID&version=C_briggsae_Jan_2007&start=0&end=1000", bookmarkUrl);
    }
}
