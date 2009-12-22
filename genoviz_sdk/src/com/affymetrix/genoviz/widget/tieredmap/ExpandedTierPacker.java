/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.widget.tieredmap;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.util.*;
import com.affymetrix.genoviz.util.*;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class ExpandedTierPacker implements PaddedPackerI, NeoConstants  {

	protected boolean DEBUG = true;
	protected boolean DEBUG_CHECKS = false;
	protected double coord_fuzziness = 1;
	protected double spacing = 2;
	protected int movetype;
	protected Rectangle2D.Double before = new Rectangle2D.Double();

	boolean STRETCH_HORIZONTAL = true;
	boolean STRETCH_VERTICAL = true;
	boolean USE_NEW_PACK = true;
	boolean use_search_nodes = false;

	/**
	 * Parent_spacer is <em>not</em> the same as AbstractCoordPacker.spacing.
	 * Spacing is between each child.
	 * parent_spacer is padding added to parent above and below the
	 * extent of all the children.
	 */
	protected double parent_spacer;

	/**
	 * Constructs a packer that moves glyphs away from the horizontal axis.
	 */
	public ExpandedTierPacker() {
		this(DOWN);
	}

	/**
	 * Constructs a packer with a given direction to move glyphs.
	 *
	 * @param movetype indicates which direction the glyph_to_move should move.
	 * @see #setMoveType
	 */
	public ExpandedTierPacker(int movetype) {
		setMoveType(movetype);
	}

	/**
	 * Sets the direction this packer should move glyphs.
	 *
	 * @param movetype indicates which direction the glyph_to_move should move.
	 *                 It must be one of {@link #UP}, {@link #DOWN}, {@link #LEFT}, {@link #RIGHT},
	 *                 {@link #MIRROR_VERTICAL}, or {@link #MIRROR_HORIZONTAL}.
	 *                 The last two mean "away from the orthoganal axis".
	 */
	public void setMoveType(int movetype) {
		this.movetype = movetype;
	}

	/**
	 *     Sets the fuzziness of hit detection in layout.
	 *     This is the minimal distance glyph coordboxes need to be separated by
	 *     in order to be considered not overlapping.
	 * <p> <em>WARNING: better not make this greater than spacing.</em>
	 * <p> Note that since Rectangle2D does not consider two rects
	 *     that only share an edge to be intersecting,
	 *     will need to have a coord_fuzziness &gt; 0
	 *     in order to consider these to be overlapping.
	 */
	public void setCoordFuzziness(double fuzz) {
		if (fuzz > spacing) {
			throw new IllegalArgumentException
				("Can't set packer fuzziness greater than spacing");
		}
		else {
			coord_fuzziness = fuzz;
		}
	}

	public double getCoordFuzziness() {
		return coord_fuzziness;
	}

	/**
	 * Sets the spacing desired between glyphs.
	 * If glyphB is found to hit glyphA,
	 * this is the distance away from glyphA's coordbox
	 * that glyphB's coord box will be moved.
	 */
	public void setSpacing(double sp) {
		if (sp < coord_fuzziness) {
			throw new IllegalArgumentException
				("Can't set packer spacing less than fuzziness");
		}
		else {
			spacing = sp;
		}
	}

	public double getSpacing() {
		return spacing;
	}

	/**
	 * Moves one glyph to avoid another.
	 * This is called from subclasses
	 * in their <code>pack(parent, glyph, view)</code> methods.
	 *
	 * @param glyph_to_move
	 * @param glyph_to_avoid
	 * @param movetype indicates which direction the glyph_to_move should move.
	 * @see #setMoveType
	 */
	public void moveToAvoid(
			GlyphI glyph_to_move, GlyphI glyph_to_avoid, int movetype) {
		Rectangle2D.Double movebox = glyph_to_move.getCoordBox();
		Rectangle2D.Double avoidbox = glyph_to_avoid.getCoordBox();
		if ( ! movebox.intersects ( avoidbox ) ) return;
		if (movetype == MIRROR_VERTICAL) {
			if (movebox.y < 0) {
				glyph_to_move.moveAbsolute(movebox.x,
						avoidbox.y - movebox.height - spacing);
			}
			else {
				glyph_to_move.moveAbsolute(movebox.x,
						avoidbox.y + avoidbox.height + spacing);
			}
		}
		else if (movetype == MIRROR_HORIZONTAL) {
			if (movebox.x < 0) {
				glyph_to_move.moveAbsolute(avoidbox.x - movebox.width - spacing,
						movebox.y);
			}
			else {
				glyph_to_move.moveAbsolute(avoidbox.x + avoidbox.width + spacing,
						movebox.y);
			}
		}
		else if (movetype == DOWN) {
			glyph_to_move.moveAbsolute(movebox.x,
					avoidbox.y + avoidbox.height + spacing);
		}
		else if (movetype == UP) {
			glyph_to_move.moveAbsolute(movebox.x,
					avoidbox.y - movebox.height - spacing);
		}
		else if (movetype == RIGHT) {
			glyph_to_move.moveAbsolute(avoidbox.x + avoidbox.width + spacing,
					movebox.y);
		}
		else if (movetype == LEFT) {
			glyph_to_move.moveAbsolute(avoidbox.x - movebox.width - spacing,
					movebox.y);
		}
		else {
			throw new IllegalArgumentException
				("movetype must be one of UP, DOWN, LEFT, RIGHT, MIRROR_HORIZONTAL, or MIRROR_VERTICAL");
		}
			}

	public void setParentSpacer(double spacer) {
		this.parent_spacer = spacer;
	}

	public double getParentSpacer() {
		return parent_spacer;
	}

	public void setStretchHorizontal(boolean b) {
		STRETCH_HORIZONTAL = b;
	}

	public boolean getStretchHorizontal(boolean b) {
		return STRETCH_HORIZONTAL;
	}

	public Rectangle pack(GlyphI parent, ViewI view) {
		Vector<GlyphI> sibs;
		GlyphI child;

		sibs = parent.getChildren();
		if (sibs == null) { return null; }

		/**
		 *  child packing
		 */
		Rectangle2D.Double cbox;
		Rectangle2D.Double pbox = parent.getCoordBox();

		// resetting height of parent to just spacers
		parent.setCoords(pbox.x, pbox.y, pbox.width, 2 * parent_spacer);

		// trying to fix an old packing bug...
		// this may also speed things up a bit...
		// BUT, this is probably NOT THREADSAFE!!!
		// (if for example another thread was adding / removing glyphs from the parent...)
		//
		// might be more threadsafe to make a new pack(parent, child, view, vector) method
		//   that can take a vector argument to use as children to check against, rather than
		//   always checking against all the children of parent?  And then make a new vector
		//   and keep adding to it rather than removing all the children like current solution
		//      [but then what to do about using glyph searchnodes?]
		//
		// an easier (but probably less efficient) way to make this threadsafe is to
		//    synchronize on the children glyph Vector (sibs), so that no other thread
		//    can muck with it while children are being removed then added back in this array
		//    However, this may become even less efficient depending on what the performance
		//    hit is for sorting the glyphs into GlyphSearchNodes as they are being added
		//    back...
		//
		// trying synchronization to ensure this method is threadsafe
		if (USE_NEW_PACK) {
			synchronized(sibs) {  // testing synchronizing on sibs vector...
				GlyphI[] sibarray = new GlyphI[sibs.size()];
				sibs.copyInto(sibarray);
				sibs.removeAllElements(); // sets parent.getChildren() to empty Vector
				int sibs_size = sibarray.length;
				for (int i=0; i<sibs_size; i++) {
					child = sibarray[i];
					sibs.add(child);  // add children back in one at a time
					pack(parent, child, view);
					if (DEBUG_CHECKS)  { System.out.println(child); }
				}
			}
		}
		else {  // old way
			int sibs_size = sibs.size();
			for (int i=0; i<sibs_size; i++) {
				child = sibs.get(i);
				cbox = child.getCoordBox();
				// MUST CALL moveAbsolute!!!
				// setCoords() does not guarantee that coord changes will recurse
				// down through descendants of child!

				// GAH 10-4-99 deal with expansion of parent within child pack...
				pack(parent, child, view);
				if (DEBUG_CHECKS)  { System.out.println(child); }
			}
		}

		/*
		 * Now that child packing is done, need to ensure
		 * that parent is expanded/shrunk vertically to just fit its
		 * children, plus spacers above and below.
		 */
		sibs = parent.getChildren();
		pbox = parent.getCoordBox();

		if (sibs == null || sibs.size() <= 0) {
			parent.setCoords(pbox.x, pbox.y, pbox.width, parent_spacer);
			return null;
		}
		Rectangle2D.Double newbox = new Rectangle2D.Double();
		Rectangle2D.Double tempbox = new Rectangle2D.Double();
		child = sibs.get(0);
		newbox.setRect(pbox.x, child.getCoordBox().y,
				pbox.width, child.getCoordBox().height);
		int sibs_size = sibs.size();
		if (STRETCH_HORIZONTAL && STRETCH_VERTICAL) {
			for (int i=1; i<sibs_size; i++) {
				child = sibs.get(i);
				GeometryUtils.union(newbox, child.getCoordBox(), newbox);
			}
		}
		else if (STRETCH_VERTICAL) {
			for (int i=1; i<sibs_size; i++) {
				child = sibs.get(i);
				Rectangle2D.Double childbox = child.getCoordBox();
				tempbox.setRect(newbox.x, childbox.y, newbox.width, childbox.height);
				GeometryUtils.union(newbox, tempbox, newbox);
			}
		}
		else if (STRETCH_HORIZONTAL) {  // NOT YET TESTED
			for (int i=1; i<sibs_size; i++) {
				child = sibs.get(i);
				Rectangle2D.Double childbox = child.getCoordBox();
				tempbox.setRect(childbox.x, newbox.y, childbox.width, newbox.height);
				GeometryUtils.union(newbox, tempbox, newbox);
			}
		}
		newbox.y = newbox.y - parent_spacer;
		newbox.height = newbox.height + (2 * parent_spacer);
		parent.setCoords(newbox.x, newbox.y, newbox.width, newbox.height);

		return null;
	}


	/**
	 * Packs a child.
	 * This adjusts the child's offset
	 * until it no longer reports hitting any of its siblings.
	 */
	public Rectangle pack(GlyphI parent, GlyphI child, ViewI view) {
		Rectangle2D.Double childbox, siblingbox;
		Rectangle2D.Double pbox = parent.getCoordBox();
		childbox = child.getCoordBox();
		if (movetype == UP) {
			child.moveAbsolute(childbox.x,
					pbox.y + pbox.height - childbox.height - parent_spacer);
		}
		else {
			// assuming if movetype != UP then it is DOWN
			//    (ignoring LEFT, RIGHT, MIRROR_VERTICAL, etc. for now)
			child.moveAbsolute(childbox.x, pbox.y+parent_spacer);
		}
		childbox = child.getCoordBox();

		Vector<? extends GlyphI> sibs = parent.getChildren();
		if (sibs == null) { return null; }

		Vector<GlyphI> sibsinrange;

		if (parent instanceof MapTierGlyph && use_search_nodes) {
			sibsinrange = ((MapTierGlyph)parent).getOverlappingSibs(child);
		}
		else {
			sibsinrange = new Vector<GlyphI>();
			int sibs_size = sibs.size();
			for (int i=0; i<sibs_size; i++) {
				GlyphI sibling = (GlyphI)sibs.get(i);
				siblingbox = sibling.getCoordBox();
				if (!(siblingbox.x > (childbox.x+childbox.width) ||
							((siblingbox.x+siblingbox.width) < childbox.x)) ) {
					sibsinrange.add(sibling);
							}
			}
			if (DEBUG_CHECKS)  { System.out.println("sibs in range: " + sibsinrange.size()); }
		}

		this.before.x = childbox.x;
		this.before.y = childbox.y;
		this.before.width = childbox.width;
		this.before.height = childbox.height;
		boolean childMoved = true;
		while (childMoved) {
			childMoved = false;
			int sibsinrange_size = sibsinrange.size();
			for (int j=0; j<sibsinrange_size; j++) {
				GlyphI sibling = sibsinrange.get(j);
				if (sibling == child) { continue; }
				siblingbox = sibling.getCoordBox();
				if (DEBUG_CHECKS)  { System.out.println("checking against: " + sibling); }
				if (child.hit(siblingbox, view) ) {
					if (DEBUG_CHECKS)  { System.out.println("hit sib"); }
					if ( child instanceof com.affymetrix.genoviz.glyph.LabelGlyph ) {
						/* LabelGlyphs cannot be so easily moved as other glyphs.
						 * They will immediately snap back to the glyph they are labeling.
						 * This can cause an infinite loop here.
						 * What's worse is that the "snapping back" may happen outside the loop.
						 * Hence the checking with "before" done below may not always work
						 * for LabelGlyphs.
						 * Someday, we might try changing the LabelGlyph's orientation
						 * to its labeled glyph.
						 * i.e. move it to the other side or inside its labeled glyph.
						 */
					}
					else {
						Rectangle2D.Double cb = child.getCoordBox();
						this.before.x = cb.x;
						this.before.y = cb.y;
						this.before.width = cb.width;
						this.before.height = cb.height;
						moveToAvoid(child, sibling, movetype);
						childMoved = childMoved || ! before.equals(child.getCoordBox());
					}
				}
			}
		}

		// adjusting tier bounds to encompass child (plus spacer)
		childbox = child.getCoordBox();
		//     if first child, then shrink to fit...
		if (parent.getChildren().size() <= 1) {
			pbox.y = childbox.y - parent_spacer;
			pbox.height = childbox.height + 2 * parent_spacer;
		}
		else {
			if (pbox.y > (childbox.y - parent_spacer)) {
				double yend = pbox.y + pbox.height;
				pbox.y = childbox.y - parent_spacer;
				pbox.height = yend - pbox.y;
			}
			if ((pbox.y+pbox.height) < (childbox.y + childbox.height + parent_spacer)) {
				double yend = childbox.y + childbox.height + parent_spacer;
				pbox.height = yend - pbox.y;
			}
		}

		return null;
	}

}
