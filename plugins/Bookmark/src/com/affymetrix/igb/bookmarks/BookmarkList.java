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
package com.affymetrix.igb.bookmarks;

import com.affymetrix.common.CommonUtils;
import java.io.*;
import java.util.Enumeration;
import java.util.StringTokenizer;
import javax.swing.tree.*;

/**
 * Holds a list of bookmarks.
 * This is a simple extension of DefaultMutableTreeNode that only allows
 * certain types of objects to be put in the list.
 * The userObject can be either a Bookmark, a Separator or a String.
 * If the userObject is a String, that implies that the item is a list that
 * can contain other bookmarks; otherwise it is not a list and will not allow children.
 * This class also contains methods for pretty-printing
 * and output formatting.
 *
 * @author  ed
 * @version $Id: BookmarkList.java 5912 2010-05-10 19:42:58Z sgblanch $
 */
public final class BookmarkList extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 1L;
	public static final String NETSCAPE_BOOKMARKS_DOCTYPE = "<!DOCTYPE NETSCAPE-Bookmark-file-1>";
	private String COMMENT;

	public BookmarkList(String s) {
		super(s, true);
	}

	public BookmarkList(String s, String comment) {
		super(s, true);
		COMMENT = comment;
	}

	public BookmarkList(Bookmark b) {
		super(b, false);
	}

	public BookmarkList(Separator s) {
		super(s, false);
	}

	public BookmarkList addBookmark(Bookmark bookmark) {
		BookmarkList bl = new BookmarkList(bookmark);
		add(bl);
		return bl;
	}

	public void addSeparator() {
		add(new BookmarkList(new Separator()));
	}

	public void addSublist(BookmarkList sublist) {
		add(sublist);
	}

	/** Overriden to return getAllowsChildren().  Thus BookmarkLists which <b>can</b> contain
	 *  other items return false even if they don't currently contain other items.
	 *  This is important so that a JTree will display
	 *  BookmarkLists with a folder icon, not a leaf icon.
	 */
	@Override
	public boolean isLeaf() {
		return (!getAllowsChildren());
	}

	/** Overridden to insure that all children are instances of BookmarkList.
	 *  @throws IllegalArgumentException
	 */
	@Override
	public void insert(MutableTreeNode item, int index) {
		if (!(item instanceof BookmarkList)) {
			throw new IllegalArgumentException("All children of BookmarkList must be instances of BookmarkList");
		}
		super.insert(item, index);
	}

	/** Not recommended.  Set the object during the constructor, then leave it alone. */
	@Override
	public void setUserObject(Object o) throws IllegalArgumentException {
		if (o instanceof String) {
			setAllowsChildren(true);
		} else if (o instanceof Bookmark) {
			setAllowsChildren(false);
		} else if (o instanceof Separator) {
			setAllowsChildren(false);
		} else {
			throw new IllegalArgumentException("Cannot accept object of type: " + o.getClass());
		}
		super.setUserObject(o);
	}

	public String getName() {
		return this.toString();
	}
	
	public String getComment() {
		return COMMENT;
	}

	public void setComment(String comment) {
		COMMENT = comment;
	}
	
	/** Finds a sublist with the given name.
	 *  @param create if true, will create the sublist if it doesn't exist.
	 */
	private BookmarkList getSubListByName(String name, boolean create) {
		BookmarkList result = null;
		@SuppressWarnings("unchecked")
		Enumeration<BookmarkList> childs = children();
		while (childs.hasMoreElements()) {
			BookmarkList tn = childs.nextElement();
			Object o = tn.getUserObject();
			if (o instanceof String) {
				if (name.equals(o)) {
					result = tn;
					break;
				}
			}
		}
		if (create && result == null) {
			result = new BookmarkList(name);
			this.add(result);
		}
		return result;
	}

	/** Finds or creates a sublist with a given path.
	 *  Example getSublistByPath("aaa/bbb/ccc", "/", true) will find or create
	 *  a BookmarkList called "aaa" containing a BookmarkList "bbb" containing
	 *  a BookmarkList "ccc".
	 */
	public BookmarkList getSubListByPath(String path, String delimiter, boolean create) {
		StringTokenizer st = new StringTokenizer(path, delimiter);
		BookmarkList current_list = this;
		while (current_list != null && st.hasMoreElements()) {
			String name = st.nextToken();
			current_list = current_list.getSubListByName(name, create);
		}
		return current_list;
	}

	/** Prints a description of the list to a PrintStream, for debugging. */
	public void printText(PrintStream out) {
		printTextRecursively(out, "");
	}

	// not a public method.  Use printText(PrintStream)
	private void printTextRecursively(PrintStream out, String indent) {
		out.println(indent + "Bookmark List: '" + this.toString() + "'  length: " + this.getChildCount());
		@SuppressWarnings("unchecked")
		Enumeration<BookmarkList> e = children();
		while (e.hasMoreElements()) {
			BookmarkList btn = e.nextElement();
			Object o = btn.getUserObject();
			if (o instanceof String) {
				btn.printTextRecursively(out, indent + " -> ");
			} else if (o instanceof Separator) {
				out.println(indent + "  --------------------------");
			} else if (o instanceof Bookmark) {
				Bookmark b = (Bookmark) o;
				out.println(indent + "  Bookmark: '" + b.getName() + "' -> '" + b.getURL().toExternalForm() + "'");
			} else {
				out.println(indent + "  Unknown object: " + o);
			}
		}
	}
	
	/** Returns true only if the two objects are the same identical object. */
	@Override
	public boolean equals(Object o) {
		return (this == o);
	}
	
	@Override
	public String toString() {
		Object o = getUserObject();
		if (o instanceof String) {
			return ((String) o);
		} else if (o instanceof Bookmark) {
			return ((Bookmark) o).getName();
		} else if (o instanceof Separator) {
			return "-----";
		} else {
			assert false; // should not get here
			return "" + o; // but if you do....
		}
	}
	
	/** Exports the BookmarkList in the Netscape/Mozilla/Firebird bookmark list
	 *  format.  Microsoft IE can also read this format.
	 */
	public static void exportAsHTML(BookmarkList list, File fil) throws IOException {
		String appName = CommonUtils.getInstance().getAppName();
		String appVersion = CommonUtils.getInstance().getAppVersion();
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			fw = new FileWriter(fil);
			bw = new BufferedWriter(fw);
			bw.write(NETSCAPE_BOOKMARKS_DOCTYPE + "\n");
			bw.write("<!-- This file was generated by " + appName + " " + appVersion + "\n");
			bw.write("     You may import and export these with a web browser.\n");
			bw.write("     DO NOT EDIT BY HAND! \n");
			bw.write("     Doing so could make the file unusable. -->\n");
			bw.write("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n");
			bw.write("<TITLE>Bookmarks</TITLE>\n");
			bw.write("<H1>Bookmarks</H1>\n");

			bw.write("<DL><p>\n");
			exportAsHTML_recursive(list, bw, " ", true);
			bw.write("</DL><p>\n");
			bw.close();
		} finally {
			if (bw != null) {
				bw.close();
			}
			if (fw != null) {
				fw.close();
			}
		}
	}
	
	public static void exportAsTEXT(BookmarkList list, File fil) throws IOException{
		String appName = CommonUtils.getInstance().getAppName();
		String appVersion = CommonUtils.getInstance().getAppVersion();
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			fw = new FileWriter(fil);
			bw = new BufferedWriter(fw);
			bw.write("Bookmarks\n");
			exportAsTEXT_recursive(list, bw, "\n", true);
			bw.write("\n");
			bw.close();
		} finally {
			if (bw != null) {
				bw.close();
			}
			if (fw != null) {
				fw.close();
			}
		}
	}

	// Used by exportAsNetscapeHTML
	private static void exportAsTEXT_recursive(BookmarkList list, Writer bw, String indent, boolean isSelectedNode) throws IOException {
		// Note: the H3 here could have also ADD_DATE, LAST_MODIFIED and ID attributes
		@SuppressWarnings("unchecked")
		Enumeration<BookmarkList> e = list.children();
		Object currentNode = list.getUserObject();

		if (list.getChildCount() == 0 && currentNode instanceof Bookmark) { // Export the selected single bookmark
			Bookmark bm = (Bookmark) currentNode;
			bw.write(bm.getName());
			bw.write(indent);
			bw.write(indent + "COMMENT =" + formatComment(bm.getComment()) + "");
			bw.write("\n\n");
		} else if (currentNode instanceof String) { // Export selected folder
			
			if (isSelectedNode && !currentNode.toString().equals("Bookmarks")) { // Not for the 'Bookmark'
				bw.write(indent + " " + currentNode + " " + list.getComment() + "\n");
			}

			while (e.hasMoreElements()) { // Export the children
				BookmarkList btn = e.nextElement();
				Object o = btn.getUserObject();
				if (o instanceof String) {
					bw.write(indent + " " + o + " " + btn.getComment() + "\n");
					bw.write(indent + "\n");
					exportAsTEXT_recursive(btn, bw, indent + "\t", false);
					bw.write(indent + "\n");
				} else if (o instanceof Bookmark) {
					Bookmark bm = (Bookmark) o;
					bw.write(bm.getName());
					bw.write(indent);
					bw.write(indent + "COMMENT =" + formatComment(bm.getComment()) + "");
					bw.write("\n\n");
				} else if (o instanceof Separator) {
					bw.write(indent + "\n");
				}
			}
		}
	}
	// Used by exportAsNetscapeHTML
	private static void exportAsHTML_recursive(BookmarkList list, Writer bw, String indent, boolean isSelectedNode) throws IOException {
		// Note: the H3 here could have also ADD_DATE, LAST_MODIFIED and ID attributes
		@SuppressWarnings("unchecked")
		Enumeration<BookmarkList> e = list.children();
		Object currentNode = list.getUserObject();
		
		if (list.getChildCount() == 0 && currentNode instanceof Bookmark) { // Export the selected single bookmark
			Bookmark bm = (Bookmark) currentNode;
			bw.write(indent + "<DT><A HREF=\"" + bm.getURL().toExternalForm() + "\"");
			bw.write(indent + "COMMENT=\"" + formatComment(bm.getComment()) + "\">");
			bw.write(bm.getName());
			bw.write("</A>\n");
		} else if(currentNode instanceof String) { // Export selected folder

			if (isSelectedNode && !currentNode.toString().equals("Bookmarks")) { // Not for the 'Bookmark'
				bw.write(indent + "<DT><H3>" + currentNode + "</H3>" + "<DD>" + (list.getComment()==null?" ":list.getComment()) + "</DD>" + "\n");//tK
				bw.write(indent + "<DL><p>\n");
				bw.write(indent + "</DL><p>\n");
			}

			while (e.hasMoreElements()) { // Export the children
				BookmarkList btn = e.nextElement();
				Object o = btn.getUserObject();
				if (o instanceof String) {
					bw.write(indent + "<DT><H3>" + o + "</H3>" + "<DD>" + (btn.getComment()==null?" ":btn.getComment()) + "</DD>" + "\n");//tK 
					bw.write(indent + "<DL><p>\n");
					exportAsHTML_recursive(btn, bw, indent + "  ", false);
					bw.write(indent + "</DL><p>\n");
				} else if (o instanceof Bookmark) {
					Bookmark bm = (Bookmark) o;
					bw.write(indent + "<DT><A HREF=\"" + bm.getURL().toExternalForm() + "\"");
					bw.write(indent + "COMMENT=\"" + formatComment(bm.getComment()) + "\">");
					bw.write(bm.getName());
					bw.write("</A>\n");
				} else if (o instanceof Separator) {
					bw.write(indent + "<HR>\n");
				}
			}
		}
	}

	private static String formatComment(String comment) {
		return comment.replaceAll("\n", "&newLine");
	}
}
