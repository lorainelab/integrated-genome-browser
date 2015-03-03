/**
 * Copyright (c) 2001-2004 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.bookmarks;

import com.affymetrix.igb.bookmarks.model.Bookmark;
import com.google.common.collect.ListMultimap;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 *
 * @author ed
 */
public final class BookmarkTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 1L;
    private int underlined_row = -1;
    private int outlined_row = -1;

    private boolean is_separator = false;
    private boolean is_underline = false;
    private boolean is_outline = false;

    public void setUnderlinedRow(int row) {
        underlined_row = row;
    }

    public void setOutlinedRow(int row) {
        outlined_row = row;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
            Object value, boolean sel, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object user_object = node.getUserObject();
        setFont(getFont().deriveFont(Font.PLAIN));

        // by default, don't give underline or outline feedback
        is_underline = false;
        is_outline = false;
        is_separator = false;

        if (leaf) {
            if (user_object instanceof Separator) {
                is_separator = true;
                setIcon(null);

                // set a longish string to force the component to have a reasonable width
                // (setting an empty or null string will make a zero-size component: not good!)
                setText("                                 ");
                setToolTipText("Separator");
            } else if (user_object instanceof Bookmark) {
                Bookmark b = (Bookmark) user_object;
                if (b.isValidBookmarkFormat()) {
                    setIcon(BookmarkIcon.UNIBROW_CONTROL_ICON);
                } else {
                    setIcon(BookmarkIcon.EXTERNAL_ICON);
                    setFont(getFont().deriveFont(Font.ITALIC));
                }
                setText(b.getName());
                setToolTipText(getToolTip(b));
            } else {
                setToolTipText(getText());
            }
        } else { // not a leaf, thus must be a folder
            if (user_object instanceof String) {
                setText((String) user_object);
                setToolTipText(getText());
            }
            // leave the icon at the default: an open or closed folder icon
        }

        is_underline = (row == underlined_row);
        is_outline = (row == outlined_row);
        return this;
    }

    private String getToolTip(Bookmark bm) {
        StringBuilder toolTipSB = new StringBuilder();
        ListMultimap<String, String> parameters = Bookmark.parseParameters(bm.getURL());
        BookmarkUnibrowControlServlet bucs = BookmarkUnibrowControlServlet.getInstance();
        String seqid = bucs.getFirstValueEntry(parameters, Bookmark.SEQID);
        if (seqid != null) {
            toolTipSB.append(seqid);
            String start_param = bucs.getFirstValueEntry(parameters, Bookmark.START);
            if (start_param != null) {
                toolTipSB.append(" : ");
                toolTipSB.append(start_param);
                String end_param = bucs.getFirstValueEntry(parameters, Bookmark.END);
                if (end_param != null) {
                    toolTipSB.append(" - ");
                    toolTipSB.append(end_param);
                }
            }
        }
        return toolTipSB.toString();
    }

    // This is a copy of a private method in DefaultTreeCellRenderer.
    private int myGetLabelStart() {
        Icon currentI = getIcon();
        if (currentI != null && getText() != null) {
            return currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1);
        }
        return 0;
    }

    /**
     * Overridden to draw Separators in a special way and to outline or
     * underline a chosen row.
     *
     * @see #setUnderlinedRow(int)
     * @see #setOutlinedRow(int)
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (is_separator) {
            int offset = myGetLabelStart();
            g.setColor(getForeground());
            if (getComponentOrientation().isLeftToRight()) {
                g.drawLine(offset, getHeight() / 2, getX() + getWidth() - offset, getHeight() / 2);
            } else {
                g.drawLine(0, getHeight() / 2, getWidth() - 1 - offset - getIconTextGap(), getHeight() / 2);
            }
        }
        if (is_outline) {
            g.setColor(getBorderSelectionColor());
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        } else if (is_underline) {
            g.setColor(getBorderSelectionColor());
            g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        }
    }

}
