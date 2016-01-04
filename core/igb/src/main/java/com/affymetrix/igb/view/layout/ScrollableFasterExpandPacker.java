package com.affymetrix.igb.view.layout;

import com.affymetrix.igb.view.layout.FasterExpandPacker;
import cern.colt.list.DoubleArrayList;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import org.lorainelab.igb.igb.genoviz.extensions.glyph.TierGlyph;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class ScrollableFasterExpandPacker extends FasterExpandPacker {

    @Override
    public Rectangle pack(GlyphI parent, ViewI view) {
        Rectangle2D.Double pbox = parent.getCoordBox();
        // resetting height of parent to just spacers
        parent.setCoords(pbox.x, 0, pbox.width, 2 * parent_spacer);

        int child_count = parent.getChildCount();
        if (child_count == 0) {
            return null;
        }

        /*  A potential improvement to this layout algorithm is to also keep track of
         *  the the _minimum_ xmax (prev_min_xmax) of all slots that were checked for the
         *  previous child being packed (including the new xmax of the slot the prev child
         *  was placed in), as well as the index of the slot the prev child was placed in
         *  (prev_slot_index)
         */
        Rectangle2D.Double cbox;
        double ymin = Double.POSITIVE_INFINITY;
        DoubleArrayList slot_maxes = new DoubleArrayList(1000);
        double slot_height = getMaxChildHeight(parent) + 2 * spacing;
        double prev_min_xmax = Double.POSITIVE_INFINITY;
        int min_xmax_slot_index = 0;	//index of slot with max of prev_min_xmax
        int prev_slot_index = 0;
//		GlyphI layeredChild = null;

        for (int i = 0; i < child_count; i++) {
            GlyphI child = parent.getChild(i);
//			child.setVisibility(true);
            child.setOverlapped(false);
            cbox = child.getCoordBox();
            double child_min = cbox.x;
            double child_max = child_min + cbox.width;
            boolean child_placed = false;
            int start_slot_index = 0;
            if (prev_min_xmax >= child_min) {
                // no point in checking slots prior to and including prev_slot_index, so
                //  modify start_slot_index to be prev_slot_index++;
                start_slot_index = prev_slot_index + 1;
            }
            int slot_count = slot_maxes.size();
            for (int slot_index = start_slot_index; slot_index < slot_count; slot_index++) {
                double slot_max = slot_maxes.get(slot_index);
                if (slot_max < prev_min_xmax) {
                    min_xmax_slot_index = slot_index;
                    prev_min_xmax = slot_max;
                }
                if (slot_max < child_min) {
                    double new_ycoord = determineYCoord(this.getMoveType(), slot_index, slot_height, spacing);
                    child.moveAbsolute(child_min, new_ycoord);
                    child_placed = true;
                    slot_maxes.set(slot_index, child_max);
                    prev_slot_index = slot_index;
                    if (slot_index == min_xmax_slot_index) {
                        prev_slot_index = 0;
                        min_xmax_slot_index = 0;
                        prev_min_xmax = slot_maxes.get(0);
                    } else if (child_max < prev_min_xmax) {
                        prev_min_xmax = child_max;
                        min_xmax_slot_index = slot_index;
                    }
                    break;
                }
            }
            if (!child_placed) {
                // make new slot for child (unless already have max number of slots allowed,
                //   in which case layer at top/bottom depending on movetype
                double new_ycoord = determineYCoord(this.getMoveType(), slot_maxes.size(), slot_height, spacing);
                child.moveAbsolute(child_min, new_ycoord);

//				if (max_slots_allowed > 0 && slot_maxes.size() >= max_slots_allowed) {
//					child.setOverlapped(true);
//				}
//
                slot_maxes.add(child_max);
                int slot_index = slot_maxes.size() - 1;
                if (child_max < prev_min_xmax) {
                    min_xmax_slot_index = slot_index;
                    prev_min_xmax = child_max;
                }
                prev_slot_index = slot_index;

            }
            ymin = Math.min(cbox.y, ymin);
        }

        /*
         *  now that child packing is done, need to ensure
         *  that parent is expanded/shrunk vertically to just fit its
         *  children, plus spacers above and below
         *
         *  maybe can get rid of this, since also handled for each child pack
         *     in pack(parent, child, view);
         *
         */
        // move children so "top" edge (y) of top-most child (ymin) is "bottom" edge
        //    (y+height) of bottom-most (ymax) child is at
        for (GlyphI child : parent.getChildren()) {
            child.moveRelative(0, parent_spacer - ymin);
        }

        packParent(parent);

        slot_maxes.trimToSize();

        setActualSlots(slot_maxes.size());

        if (getMaxSlots() != 0) {
            parent.setCoords(parent.getCoordBox().x, parent.getCoordBox().y,
                    parent.getCoordBox().width, (Math.min(getActualSlots(), getMaxSlots()) + 1) * slot_height);
        }

        // Make sure the parent is not too short.
        // This was needed for tiers in tiered maps.
        int minPixelHeight = ((Glyph) parent).getMinPixelsHeight();
        // P.S. Why isn't getMinPixelsHeight in GlyphI?
        int currentPixelHeight = parent.getPixelBox(view).height;
        if (currentPixelHeight < minPixelHeight) {
            if (parent instanceof TierGlyph) {
                TierGlyph g = (TierGlyph) parent;
                // Only do this for resizable tiers for now.
                // It would screw up the axis tier, for one.
                if (g.isManuallyResizable()) {
                    Rectangle2D.Double oldBox = parent.getCoordBox();
                    Rectangle r = parent.getPixelBox(view);
                    r.height = minPixelHeight; // Make it tall enough.
                    view.transformToCoords(r, oldBox);
                    parent.setCoords(oldBox.x, oldBox.y, oldBox.width, oldBox.height);
                }
            }
        }

        return null;
    }

    @Override
    protected void setActualSlots(int theNumber) {
        actual_slots = theNumber;
    }

}
