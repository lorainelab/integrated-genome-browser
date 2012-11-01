package com.affymetrix.genoviz.swing;

import javax.swing.JMenuItem;

/**
 *
 * @author hiralv
 */
public class AMenuItem {
	
	private String parentMenu;
	private JMenuItem menuItem;
	private int location;
	
	/**
	 * @param menuItem Actual menu item to be inserted. It cannot be null.
	 * @param parentMenu Name of parent menu into which @menuItem should be inserted. It cannot be null.
	 */
	public AMenuItem(JMenuItem menuItem, String parentMenu){
		this(menuItem, parentMenu, -1);
	}
	
	/**
	 * @param menuItem Actual menu item to be inserted. It cannot be null.
	 * @param parentMenu Name of parent menu into which @menuItem should be inserted. It cannot be null.
	 * @param location Location at which menu item should be inserted.
	 */
	public AMenuItem(JMenuItem menuItem, String parentMenu, int location){
		if(parentMenu == null || menuItem == null){
			throw new IllegalArgumentException("Neither menuItem or parentMenu cannot be null.");
		}
		this.menuItem = menuItem;
		this.parentMenu = parentMenu;
		this.location = location;
	}

	public JMenuItem getMenuItem() {
		return menuItem;
	}
	
	public String getParentMenu() {
		return parentMenu;
	}

	public int getLocation() {
		return location;
	}

}
