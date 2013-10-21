package com.affymetrix.igb.action;

import java.awt.event.KeyEvent;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.SeqMapView;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class ToggleToolTipAction extends GenericAction {
	private static final long serialVersionUID = 1;
	private static final ToggleToolTipAction ACTION = new ToggleToolTipAction();

	private ToggleToolTipAction() {
		super(BUNDLE.getString("togglePropertiesTooltip"), null,
				"16x16/actions/speech-bubble.png",
				"22x22/actions/speech-bubble.png", // for tool bar
				KeyEvent.VK_H);
		this.ordinal = 160;
		/* TODO: This is only correct for English Locale" */
		this.putValue(DISPLAYED_MNEMONIC_INDEX_KEY, 5);
	}
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		PreferenceUtils.saveToPreferences(SeqMapView.PREF_SHOW_TOOLTIP, SeqMapView.default_show_prop_tooltip, ACTION);
	}
	
	public static ToggleToolTipAction getAction() {
		return ACTION;
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
