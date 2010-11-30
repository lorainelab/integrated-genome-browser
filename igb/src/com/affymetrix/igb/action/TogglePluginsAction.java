package com.affymetrix.igb.action;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGBServiceImpl;

public class TogglePluginsAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	public static final String USE_PLUGINS = "use plugins";
	private static final TogglePluginsAction ACTION = new TogglePluginsAction();

	private boolean use_plugins;

	private TogglePluginsAction() {
		super(BUNDLE.getString("pluginsUseLabel"));

		use_plugins = PreferenceUtils.getTopNode().getBoolean(USE_PLUGINS, true);
		this.putValue(SELECTED_KEY, use_plugins);
	}

	public static TogglePluginsAction getAction() {
		return ACTION;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		use_plugins = !use_plugins;
		if (use_plugins) {
			IGBServiceImpl.startOSGi();
		}
		else {
			IGBServiceImpl.stopOSGi();
		}
		PreferenceUtils.getTopNode().putBoolean(
				USE_PLUGINS,
				use_plugins);
	}

}
