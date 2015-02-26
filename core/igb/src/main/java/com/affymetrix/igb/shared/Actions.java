package com.affymetrix.igb.shared;

import com.affymetrix.genometry.event.ParameteredAction;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.igb.action.ChangeBackgroundColorAction;
import com.affymetrix.igb.action.ChangeExpandMaxAction;
import com.affymetrix.igb.action.ChangeForegroundColorAction;
import com.affymetrix.igb.action.ChangeForwardColorAction;
import com.affymetrix.igb.action.ChangeLabelColorAction;
import com.affymetrix.igb.action.ChangeReverseColorAction;
import com.affymetrix.igb.action.ChangeTierHeightAction;
import com.affymetrix.igb.action.FloatTiersAction;
import com.affymetrix.igb.action.LabelGlyphAction;
import com.affymetrix.igb.action.RenameTierAction;
import com.affymetrix.igb.action.SetDirectionStyleArrowAction;
import com.affymetrix.igb.action.SetDirectionStyleColorAction;
import com.affymetrix.igb.action.ShowOneTierAction;
import com.affymetrix.igb.action.ShowTwoTiersAction;
import com.affymetrix.igb.action.TierFontSizeAction;
import com.affymetrix.igb.action.UnFloatTiersAction;
import com.affymetrix.igb.action.UnsetDirectionStyleArrowAction;
import com.affymetrix.igb.action.UnsetDirectionStyleColorAction;
import java.awt.Color;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
public class Actions {

    public static void setForegroundColor(Color color) {
        ParameteredAction action = ChangeForegroundColorAction.getAction();
        action.performAction(color);
    }

    public static void setBackgroundColor(Color color) {
        ParameteredAction action = ChangeBackgroundColorAction.getAction();
        action.performAction(color);
    }

    public static void setLabelColor(Color color) {
        ParameteredAction action = ChangeLabelColorAction.getAction();
        action.performAction(color);
    }

    public static void setTierFontSize(int fontSize) {
        ParameteredAction action = TierFontSizeAction.getAction();
        action.performAction(fontSize);
    }

    public static void setStackDepth(int depth) {
        ParameteredAction action = ChangeExpandMaxAction.getAction();
        action.performAction(depth);
    }

    public static void setLabelField(String labelField) {
        ParameteredAction action = LabelGlyphAction.getAction();
        action.performAction(labelField);
    }

    public static void setStrandsReverseColor(Color color) {
        if (color == null) {
            return;
        }

        ParameteredAction action = ChangeReverseColorAction.getAction();
        action.performAction(color);
    }

    public static void setStrandsForwardColor(Color color) {
        if (color == null) {
            return;
        }

        ParameteredAction action = ChangeForwardColorAction.getAction();
        action.performAction(color);
    }

    /**
     * @param showOneTier Show one tier
     */
    public static void showOneTwoTier(boolean showOneTier, ActionEvent e) {
        GenericAction action = !showOneTier
                ? ShowOneTierAction.getAction() : ShowTwoTiersAction.getAction();

        action.actionPerformed(e);
    }

    /**
     * @param showArrow Show arrow
     */
    public static void showArrow(boolean showArrow, ActionEvent e) {
        GenericAction action = showArrow
                ? SetDirectionStyleArrowAction.getAction() : UnsetDirectionStyleArrowAction.getAction();
        action.actionPerformed(e);
    }

    /**
     * @param showColor Show color
     */
    public static void showStrandsColor(boolean showColor, ActionEvent e) {
        GenericAction action = showColor
                ? SetDirectionStyleColorAction.getAction() : UnsetDirectionStyleColorAction.getAction();
        action.actionPerformed(e);
    }

    public static void setLockedTierHeight(int height) {
        ParameteredAction action = ChangeTierHeightAction.getAction();
        action.performAction(height);
    }

    /**
     * @param floatTier Float tier
     */
    public static void setFloatTier(boolean floatTier, ActionEvent e) {
        GenericAction action = floatTier
                ? FloatTiersAction.getAction() : UnFloatTiersAction.getAction();
        action.actionPerformed(e);
    }

    public static void setRenameTier(ITrackStyleExtended style, String name) {
        ParameteredAction action = RenameTierAction.getAction();
        action.performAction(style, name);
    }

    private Actions() {
    }

}
