package cytoscape.visual.ui.editors.continuous;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JColorChooser;
import javax.swing.JDialog;


/**
 * This is an annoying re-implementation of JColorChooser.showDialog() that remembers
 * recently used colors between invocations of the chooser dialog.
 */
public class CyColorChooser {
	protected static JColorChooser chooser = new JColorChooser();
	protected static ColorListener listener = new ColorListener();
	protected static Color color;

	/**
	 *  DOCUMENT ME!
	 *
	 * @param component DOCUMENT ME!
	 * @param title DOCUMENT ME!
	 * @param initialColor DOCUMENT ME!
	 *
	 * @return  DOCUMENT ME!
	 */
	public static Color showDialog(Component component, String title, Color initialColor) {
		if (initialColor != null) {
			chooser.setColor(initialColor);
		}

		JDialog dialog = JColorChooser.createDialog(component, title, true, chooser, listener, null);
		dialog.setVisible(true);

		return color;
	}

	static class ColorListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			color = chooser.getColor();
		}
	}
}

