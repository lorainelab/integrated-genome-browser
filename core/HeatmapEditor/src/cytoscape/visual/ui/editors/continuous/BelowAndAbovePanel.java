package cytoscape.visual.ui.editors.continuous;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

/**
 * Drawing and updating below & above values in Gradient Editor.
 * 
 * @author $author$
 */
public class BelowAndAbovePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	/**
	 * DOCUMENT ME!
	 */
	public static final String COLOR_CHANGED = "COLOR_CHANGED";
	
	private Color boxColor;
	private boolean below;
	
	/**
	 * Creates a new BelowAndAbovePanel object. This will be used for drawing
	 * below & above triangle
	 * 
	 * @param color
	 *            DOCUMENT ME!
	 * @param below
	 *            DOCUMENT ME!
	 */
	public BelowAndAbovePanel(Color color, boolean below, Component parent) {
		this.boxColor = color;
		this.below = below;

		if (below) {
			this.setToolTipText("Double-click triangle to set below color...");
		}
		else {
			this.setToolTipText("Double-click triangle to set above color...");
		}

		this.addMouseListener(new MouseEventHandler(parent));
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param newColor
	 *            DOCUMENT ME!
	 */
	public void setColor(Color newColor) {
		final Color oldColor = boxColor;
		this.boxColor = newColor;
		this.repaint();
		this.getParent().repaint();

		this.firePropertyChange(COLOR_CHANGED, oldColor, newColor);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param g
	 *            DOCUMENT ME!
	 */
	public void paintComponent(Graphics g) {
		final Graphics2D g2d = (Graphics2D) g;

		final Polygon poly = new Polygon();

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		g2d.setStroke(new BasicStroke(1.0f));
		g2d.setColor(boxColor);

		if (below) {
			poly.addPoint(9, 0);
			poly.addPoint(9, 10);
			poly.addPoint(0, 5);
		} else {
			poly.addPoint(0, 0);
			poly.addPoint(0, 10);
			poly.addPoint(9, 5);
		}

		g2d.fillPolygon(poly);

		g2d.setColor(Color.black);
		g2d.draw(poly);
	}

	class MouseEventHandler extends MouseAdapter {
		private Component caller;

		public MouseEventHandler(Component c) {
			this.caller = c;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {

				Object newValue = CyColorChooser.showDialog(caller,
						"Select new color", boxColor);

				if (newValue == null) {
					return;
				}

				BelowAndAbovePanel.this.setColor((Color) newValue);
				BelowAndAbovePanel.this.repaint();
				BelowAndAbovePanel.this.getParent().repaint();
			}
		}
	}
}
