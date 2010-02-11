package com.affymetrix.genometryImpl.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple helper function to build URLs with query components from a base URL.
 * 
 * @author sgblanch
 * @version $Id$
 */
public class QueryBuilder {
	private final Map<String, String> parameters = new LinkedHashMap<String, String>();
	private final URL u;

	public QueryBuilder(URL u) {
		this.u = u;
	}

	public void add(String key, String value) {
		parameters.put(GeneralUtils.URLEncode(key), GeneralUtils.URLEncode(value));
	}

	public URL build() throws MalformedURLException {
		StringBuilder query = new StringBuilder();

		query.append(u.getPath());
		query.append("?");
		for (Map.Entry<String, String> parameter : parameters.entrySet()) {
			query.append(parameter.getKey());
			query.append("=");
			query.append(parameter.getValue());
			query.append(";");
		}

		return new URL(u, query.toString());
	}
}
