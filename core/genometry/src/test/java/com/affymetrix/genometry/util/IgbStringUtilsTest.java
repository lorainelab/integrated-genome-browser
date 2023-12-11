package com.affymetrix.genometry.util;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 *
 * @author sgblanch
 * @version $Id: StringUtilsTest.java 9793 2012-01-13 16:11:27Z hiralv $
 */
public final class IgbStringUtilsTest {

    private Canvas canvas;
    private static final String loremIpsum
            = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut "
            + "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco "
            + "laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in "
            + "voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat "
            + "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    @BeforeEach
    public void setUp() {
        this.canvas = new Canvas();
    }

    @Disabled
    @Test
    public void testWrap_3args() {
        String toWrap = loremIpsum;
        FontMetrics metrics = this.canvas.getFontMetrics(new Font("sansserif", Font.PLAIN, 12));
        int pixels;
        String[] result;

        // Normal case
        pixels = 300;
        result = IgbStringUtils.wrap(toWrap, metrics, pixels);

        verifyWrap(result, metrics, pixels);
        Assertions.assertTrue(result[result.length - 1].endsWith("laborum. "), "Wrapped text does not appear to be complete");

        // Extreme test
        pixels = 50;
        result = IgbStringUtils.wrap(toWrap, metrics, pixels);

        verifyWrap(result, metrics, pixels);
        Assertions.assertTrue(result[result.length - 1].endsWith("laborum. "), "Wrapped text does not appear to be complete");
    }

    /**
     * Test of wrap method, of class StringUtils.
     */
    @Test
    public void testWrap_4args() {
        String toWrap = loremIpsum;
        FontMetrics metrics = this.canvas.getFontMetrics(new Font("sansserif", Font.PLAIN, 12));
        int pixels = 300;
        int maxLines;
        String[] result;

        // Normal case
        maxLines = 3;
        result = IgbStringUtils.wrap(toWrap, metrics, pixels, maxLines);

        Assertions.assertEquals(maxLines, result.length, "Wrong number of lines in wrapped text");
        verifyWrap(result, metrics, pixels);

        String lastLine = result[result.length - 1];
        Assertions.assertEquals('\u2026', lastLine.charAt(lastLine.length() - 1), "Wrapped text does not end with ellipsis (\u2026)");

        // One line case
        maxLines = 1;
        result = IgbStringUtils.wrap(toWrap, metrics, pixels, maxLines);

        Assertions.assertEquals(maxLines, result.length, "Wrong number of lines in wrapped text");
        verifyWrap(result, metrics, pixels);

        lastLine = result[result.length - 1];
        Assertions.assertEquals('\u2026', lastLine.charAt(lastLine.length() - 1), "Wrapped text does not end with ellipsis (\u2026)");
    }

    private static void verifyWrap(String[] result, FontMetrics metrics, int pixels) {
        String current;
        int width;
        int i = 0;
        String[] words = loremIpsum.split("\\s+");
        for (String line : result) {
            if (line.endsWith(" ")) {
                current = line.substring(0, line.length() - 1);
            } else {
                current = line;
            }
            width = metrics.stringWidth(current);
            Assertions.assertFalse(current.contains(" ") && width > pixels, "Current line is larger than wrap width ");

            for (String word : line.split("\\s+")) {
                if ("\u2026".equals(word)) {
                    break;
                }
                Assertions.assertEquals(words[i], word, "Found incorrect word in text");
                i++;
            }
        }
    }
}
