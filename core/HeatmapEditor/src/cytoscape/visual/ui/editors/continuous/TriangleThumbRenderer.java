package cytoscape.visual.ui.editors.continuous;

import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.ThumbRenderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

import javax.swing.JComponent;

/**
 * DOCUMENT ME!
 * 
 * @author $author$
 */
public class TriangleThumbRenderer extends JComponent implements ThumbRenderer {
	private static final long serialVersionUID = 1L;

	private static final Color SELECTED_COLOR = Color.red;
	private static final Color DEFAULT_COLOR = Color.DARK_GRAY;
	
	@Override
	protected void paintComponent(Graphics g) {
		/*
		 * Enable anti-aliasing
		 */
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		/*
		 * Draw small triangle
		 */
		final Polygon thumb = new Polygon();
		thumb.addPoint(0, 0);
		thumb.addPoint(10, 0);
		thumb.addPoint(5, 10);
		g.setColor(getForeground());
		g.fillPolygon(thumb);

		/*
		 * Draw triangle outline
		 */
		final Polygon outline = new Polygon();
		outline.addPoint(0, 0);
		outline.addPoint(9, 0);
		outline.addPoint(5, 9);
		((Graphics2D) g).setStroke(new BasicStroke(1.0f));
		g.setColor(getBackground());
		g.drawPolygon(outline);

	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param slider
	 *            DOCUMENT ME!
	 * @param index
	 *            DOCUMENT ME!
	 * @param selected
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public JComponent getThumbRendererComponent(JXMultiThumbSlider slider,
			int index, boolean selected) {
		
		Object obj = slider.getModel().getThumbAt(index).getObject();
		this.setForeground((Color) obj);
		//Use background color as border color
		if(selected){
			this.setBackground(SELECTED_COLOR);
		}else{
			this.setBackground(DEFAULT_COLOR);
		}
		
		return this;
	}

	

}
