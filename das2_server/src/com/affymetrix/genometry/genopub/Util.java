package com.affymetrix.genometry.genopub;

import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Util {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
    
	
	private static final double    KB = Math.pow(2, 10);
	
	public static Integer getIntegerParameter(HttpServletRequest req, String parameterName) {
		if (req.getParameter(parameterName) != null && !req.getParameter(parameterName).equals("")) {
			return new Integer(req.getParameter(parameterName));
		} else{
			return null;
		}
	}
	
	public static Date getDateParameter(HttpServletRequest req, String parameterName) {
		if (req.getParameter(parameterName) != null && !req.getParameter(parameterName).equals("")) {
			try {
				return parseDate(req.getParameter(parameterName));				
			} catch (ParseException e) {
				return null;
			}
		} else{
			return null;
		}
	}
	
	public static String getFlagParameter(HttpServletRequest req, String parameterName) {
		if (req.getParameter(parameterName) != null && !req.getParameter(parameterName).equals("")) {
			return req.getParameter(parameterName);
		} else{
			return "Y";
		}
	}
	
	
	
	public static String formatDate(Date date) {
		return dateFormat.format(date);
	}
	
	public static Date parseDate(String date) throws ParseException {
		return new Date(dateFormat.parse(date).getTime());
	}
    
	public static long getKilobytes(long bytes) {
		long kb =  Math.round(bytes / KB);
		if (kb == 0) {
			kb = 1;
		}
		return kb;
	}
	
	public static String removeHTMLTags(String buf) {
		if (buf  != null) {
			buf = buf.replaceAll("<(.|\n)+?>", " ");
			buf = Util.escapeHTML(buf);
		}
		return buf;
	}
	
	public static String escapeHTML(String buf) {
		if (buf != null) {
			buf = buf.replaceAll("&", "&amp;");
			buf = buf.replaceAll("<", "&lt;");
			buf = buf.replaceAll(">", "&gt;");			
			buf = buf.replaceAll("\"", "'");			
		}
		
		return buf;
	}
}
