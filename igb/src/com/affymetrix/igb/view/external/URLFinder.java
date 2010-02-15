package com.affymetrix.igb.view.external;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Parses a html page for the image URL
 *
 * @author Ido M. Tamir
 */
public interface URLFinder {

	public String findUrl(BufferedReader reader) throws IOException;
}
