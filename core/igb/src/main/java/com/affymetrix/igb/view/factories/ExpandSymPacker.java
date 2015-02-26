package com.affymetrix.igb.view.factories;

import cern.colt.list.DoubleArrayList;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

public class ExpandSymPacker {

    public static int getMaxSlots(SeqSymmetry parent, BioSeq seq) {
        int child_count = parent.getChildCount();
        if (child_count == 0) {
            return 0;
        }

        final int max_slots_allowed = 1000;
        DoubleArrayList slot_maxes = new DoubleArrayList(1000);
        double prev_min_xmax = Double.POSITIVE_INFINITY;
        int min_xmax_slot_index = 0;	//index of slot with max of prev_min_xmax
        int prev_slot_index = 0;
        SeqSymmetry child;
        SeqSpan span;

        for (int i = 0; i < child_count; i++) {
            child = parent.getChild(i);
            span = child.getSpan(seq);

            if (span == null || span.getLength() == 0) {
                continue;
            }

            double child_min = span.getMinDouble();
            double child_max = span.getMaxDouble();
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
                if (slot_maxes.size() >= max_slots_allowed) {
                    int slot_index = slot_maxes.size() - 1;
                    prev_slot_index = slot_index;
                } else {
                    slot_maxes.add(child_max);
                    int slot_index = slot_maxes.size() - 1;
                    if (child_max < prev_min_xmax) {
                        min_xmax_slot_index = slot_index;
                        prev_min_xmax = child_max;
                    }
                    prev_slot_index = slot_index;
                }
            }
        }

        slot_maxes.trimToSize();

        return slot_maxes.size();
    }
}
