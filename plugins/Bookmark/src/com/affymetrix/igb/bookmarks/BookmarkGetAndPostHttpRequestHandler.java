/*
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
package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Eric Blossom
 */
class BookmarkGetAndPostHttpRequestHandler implements Runnable {

	private final Socket socket;
	private final IGBService igbService;
	private static final Logger ourLogger = Logger.getLogger(BookmarkGetAndPostHttpRequestHandler.class.getPackage().getName());

	public BookmarkGetAndPostHttpRequestHandler(IGBService igbService, Socket socket) {
		this.socket = socket;
		this.igbService = igbService;
	}

	@Override
	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Split and HTTP GET or POST request.
	 *
	 * @return the middle part between the command and "HTTP/1.n".
	 */
	private String getResourceID(String theLine) {
		String answer = "";
		String[] getCommandPart = theLine.split(" ");
		if (1 < getCommandPart.length) {
			answer = getCommandPart[1];
		}
		return answer;
	}

	/**
	 * Parse the headers for an HTTP request.
	 *
	 * @param theReader from the socket the client opened to us.
	 */
	private Map<String, String> getRequestHeaders(BufferedReader theReader) throws IOException {
		Map<String, String> answer = new HashMap<String, String>();
		String line = theReader.readLine();
		while (null != line && (0 < line.trim().length())) {
			String[] word = line.split(": ");
			if (2 <= word.length) {
				StringBuilder b = new StringBuilder(word[1]);
				int i = 2;
				while (i < word.length) {
					b.append(": ").append(word[i]);
					++i;
				}
				answer.put(word[0], b.toString());
			} else {
				ourLogger.log(Level.WARNING, "Expected a header. Got: {0}", line);
			}
			line = theReader.readLine();
		}
		return answer;
	}

	/**
	 * Get the body of an HTTP request.
	 *
	 * @param theReader probably the same one used to
	 * {@link #getRequestHeaders()}.
	 * @param theBodySize probably the value of the Content-Size header.
	 */
	private String getRequestBody(BufferedReader theReader, int theBodySize) throws IOException {
		char[] buf = new char[theBodySize];
		int n = theReader.read(buf, 0, theBodySize);
		if (-1 == n) {
			return "";
		}
		return new String(buf);
	}

	/**
	 * @param theScript can be multiple lines.
	 */
	private void runIGBScript(String theScript) {
		String[] commands = theScript.split("\n");
		for (String s : commands) {
			igbService.runScriptString(s.trim() + "\n", "igb");
		}

	}

	/**
	 * Turn a URL query string into a map of decoded parameters. BUG: Each
	 * parameter gets the last value. It should get a list of values.
	 *
	 * @param theQuery URL encoded query string.
	 * @return Map of decoded parameters. Maybe this should be a Properties
	 * instead.
	 * @throws UnsupportedEncodingException if your platform cannot do UTF-8.
	 */
	private Map<String, String> parsedQuery(String theQuery)
			throws UnsupportedEncodingException {
		Map<String, String> answer = new HashMap<String, String>();
		for (String s : theQuery.split("&")) {
			String[] p = s.split("=");
			answer.put(p[0], URLDecoder.decode(p[1], "UTF-8"));
		}
		return answer;
	}

	private String notFound(String theProtocol, String theResource) {
		int length = theResource.length() + 15;
		StringBuffer answer = new StringBuffer(theProtocol)
				.append(" 404 Not Found\n")
				.append("Server: Integrated Genome Browser\n")
				.append("Content-Type: text/plain\n")
				.append("Content-Length: ").append(length).append("\n")
				.append("Connection: close\n")
				.append("\n")
				.append("404 ").append(theResource).append(" Not Found\n");
		System.out.println(answer.toString());
		return answer.toString();
	}

	/**
	 * Handle an HTTP request or IGBScript command. Note that this violates the
	 * HTTP. At this point only GET and POST are recognized as HTTP requests.
	 * Anything else is assumed to be an IGBScript command. It is used as an
	 * expedient. A script can be submitted in a answer with "lang=IGBScript"
	 * and "code"=<var>script</var>.
	 */
	private void processRequest() throws IOException {
		BufferedReader reader = null;
		OutputStream output = null;

		String line;
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = socket.getOutputStream();
			while ((line = reader.readLine()) != null && line.trim().length() > 0) {
				// we need to process only the GET header line of the input, which will
				// look something like this:
				// 'GET /IGBControl?version=hg18&seqid=chr17&start=43966897&end=44063310 HTTP/1.1'
				String[] word = line.split(" ");
				if (0 < word.length) {
					String cmd = word[0].toUpperCase();
					if ("GET".equals(cmd)) {
						String resource = getResourceID(line);
						// Consume the headers and then fall through to legacy code.
						Map<String, String> headers = getRequestHeaders(reader);
						//bring IGB to front? (shouldn't this be "line.contains"?)
						if (resource.contains("bringIGBToFront=true")) {
							igbService.getFrame().toFront();
							igbService.getFrame().repaint();
						} else if (resource.startsWith("/IGBScript?")) {
							String[] part = resource.split("\\?");
							if (1 < part.length) {
								Map<String, String> form = parsedQuery(part[1]);
								runIGBScript(form.get("code"));
							}
						} else if (resource.startsWith("/Script?")) {
							String[] part = resource.split("\\?");
							if (1 < part.length) {
								Map<String, String> form = parsedQuery(part[1]);
								String lang = form.get("lang");
								if ("IGBScript".equalsIgnoreCase(lang)) {
									runIGBScript(form.get("code"));
								}
							}
						} else {
							parseAndGoToBookmark(resource);
						}
						output.write(SimpleBookmarkServer.http_response.getBytes());
						output.flush();
					} else if ("POST".equals(cmd)) {
						Map<String, String> headers = getRequestHeaders(reader);
						String resource = getResourceID(line);
						String body = getRequestBody(reader,
								Integer.parseInt(headers.get("Content-Length")));
						String ourResponse = SimpleBookmarkServer.http_response;
						if (resource.equals("/IGBScript")) {
							Map<String, String> form = parsedQuery(body);
							runIGBScript(form.get("code"));
						} else if (resource.equals("/Script")) {
							String contentType = headers.get("Content-Type");
							if ("application/x-www-form-urlencoded".equals(contentType)) {
								Map<String, String> form = parsedQuery(body);
								if ("IGBScript".equalsIgnoreCase(form.get("lang"))) {
									runIGBScript(form.get("code"));
								}
							}
						} else {
							String protocol = "HTTP/1.1";
							if (2 < word.length) {
								protocol = word[2];
							}
							ourResponse = notFound(protocol, resource);
						}
						// It might be nice to also support other content types
						// like application/x-igbscript or application/x-javascript.
						output.write(ourResponse.getBytes());
						output.flush();
					} /* Other HTTP requests that are not (yet) supported are:
					 * HEAD, PUT (maybe more appropriate for scripting),
					 * DELETE, TRACE, OPTIONS, CONNECT, and PATCH.
					 * Note that if IGBScript supports one of the above
					 * then there could be conflict down the line.
					 */ else { // it's not an HTTP request that we recognize.
						// Assume it's an IGBScript command.
						igbService.runScriptString(line, "igb");
//						output.write(SimpleBookmarkServer.prompt);
//						output.flush();
					}
				}
			}
		} finally {

			GeneralUtils.safeClose(output);
			GeneralUtils.safeClose(reader);
			try {
				socket.close();
			} catch (Exception e) {
				// do nothing
			}
		}

	}

	private void parseAndGoToBookmark(String command) throws NumberFormatException {
		ourLogger.log(Level.FINE, "Command = {0}", command);
		// at this point, the command will look something like this:
		// '/IGBControl?version=hg18&seqid=chr17&start=43966897&end=44063310'
		//TODO: We could check to see that the command is "IGBControl" or "UnibrowControl",
		// but since that is the only command we ever expect, we can just assume for now.
		int index = command.indexOf('?');
		if (index >= 0 && index < command.length()) {
			String params = command.substring(index + 1);
			Map<String, String[]> paramMap = new HashMap<String, String[]>();
			Bookmark.parseParametersFromQuery(paramMap, params, true);
			BookmarkUnibrowControlServlet.getInstance().goToBookmark(igbService, paramMap);
		}
	}
}
