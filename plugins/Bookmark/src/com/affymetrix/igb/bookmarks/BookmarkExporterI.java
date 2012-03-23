/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.bookmarks;

import java.io.DataOutputStream;
import javax.swing.JFrame;

/**
 *
 * @author auser
 */
public interface BookmarkExporterI {
	public void exportBookmarkAs(DataOutputStream dos, BookmarkList main_bookmark_list);
	public String getBookmarkExtension();
}
