package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

import com.affymetrix.igb.view.UnibrowHairline;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
/**
 *
 * @author hiralv
 */
public class ToggleHairlineAction extends GenericAction {
	private static final long serialVersionUID = 1;
	private static final ToggleHairlineAction ACTION = new ToggleHairlineAction();
	
	private ToggleHairlineAction() {
		super(BUNDLE.getString("toggleHairline"),
				"16x16/actions/show_zoom_stripe.png",
				"22x22/actions/show_zoom_stripe.png");
		//this.putValue(MNEMONIC_KEY, java.awt.event.KeyEvent.VK_H);
		/* TODO: This is only correct for English Locale" */
		//this.putValue(DISPLAYED_MNEMONIC_INDEX_KEY, 5);
		this.ordinal = -4003000;
	}
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		PreferenceUtils.saveToPreferences(UnibrowHairline.PREF_KEEP_HAIRLINE_IN_VIEW, UnibrowHairline.default_keep_hairline_in_view, ACTION);
	}
	
	public static ToggleHairlineAction getAction() {
		return ACTION;
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
