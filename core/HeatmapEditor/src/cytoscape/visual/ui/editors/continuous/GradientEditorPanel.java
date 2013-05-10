package cytoscape.visual.ui.editors.continuous;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;

/**
 * Gradient editor.
 *
 * @version 0.7
 * @since Cytoscpae 2.5
 * @author kono
 */
public class GradientEditorPanel extends ContinuousMappingEditorPanel {

	private static final long serialVersionUID = -7645303507318540305L;
	// For presets
	private static final Color DEF_LOWER_COLOR = Color.BLACK;
	private static final Color DEF_UPPER_COLOR = Color.WHITE;
	
	private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
		/**
		 * DOCUMENT ME!
		 *
		 * @param e DOCUMENT ME!
		 */
		public void propertyChange(PropertyChangeEvent e) {
			if (e.getPropertyName().equals(BelowAndAbovePanel.COLOR_CHANGED)) {
				String sourceName = ((BelowAndAbovePanel) e.getSource()).getName();

				if (sourceName.equals("abovePanel")) {
					((MultiColorThumbModel) slider.getModel()).setAboveColor((Color) e.getNewValue());
				} else {
					((MultiColorThumbModel) slider.getModel()).setBelowColor((Color) e.getNewValue());
				}

				repaint();
			}
		}
	};
	
	/**
	 * Creates a new GradientEditorPanel object.
	 *
	 * @param type DOCUMENT ME!
	 */
	public GradientEditorPanel() {
		this(null);
		
	}

	public GradientEditorPanel(Window window) {
		super(window);
		iconPanel.setVisible(false);
		initSlider();

		belowPanel.addPropertyChangeListener(propertyChangeListener);
		abovePanel.addPropertyChangeListener(propertyChangeListener);
		//if(mapping != null && mapping.getPointCount() == 0)
	}
	
	/**
	 * DOCUMENT ME!
	 *
	 * @param width DOCUMENT ME!
	 * @param height DOCUMENT ME!
	 * @param title DOCUMENT ME!
	 * @param type DOCUMENT ME!
	 */
	@SuppressWarnings("unchecked")
	public static Object showDialog(final int width, final int height, final String title) {
		ContinuousMappingEditorPanel editor = new GradientEditorPanel();
		editor.slider.getModel().addThumb(10f, DEF_LOWER_COLOR);
		editor.slider.getModel().addThumb(90f, DEF_UPPER_COLOR);
		
		final Dimension size = new Dimension(width, height);
		editor.slider.setPreferredSize(size);
		editor.setPreferredSize(size);

		editor.setTitle(title);
		editor.setAlwaysOnTop(true);
//		editor.setLocationRelativeTo(Cytoscape.getDesktop());
		editor.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		editor.setVisible(true);

		return editor;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void addButtonActionPerformed(ActionEvent evt) {

		// Add a new white thumb in the min.
		// slider.getModel().addThumb(100f, Color.white);
		// Add a new white thumb near the middle
		slider.getModel().addThumb(51f, Color.white);

		// Make this slider the selected one
		selectThumbAtPosition(51f);

		slider.repaint();
		repaint();
	}

	public VirtualRange getVirtualRange(){
		return (VirtualRange)slider.getModel();
	}
	
	/**
	 * DOCUMENT ME!
	 */
	private void initSlider() {
		slider.updateUI();
		slider.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) {
					if (slider.getSelectedIndex() >= 0) {
						if (e.getClickCount() == 2) {
							colorButtonActionPerformed();
						}
					}
				}
			}
		});

		if (true) {
			int no_of_points = 0;
			if (no_of_points != 0) {
				//below = (Color) allPoints.get(0).getRange().lesserValue;
				//above = (Color) allPoints.get(allPoints.size() - 1).getRange().greaterValue;
			} else {
				//below = Color.black;
				//above = Color.white;
			}

			setSidePanelIconColor(((MultiColorThumbModel)slider.getModel()).getBelowColor(), ((MultiColorThumbModel)slider.getModel()).getAboveColor());
		}

		TriangleThumbRenderer thumbRend = new TriangleThumbRenderer();

		CyGradientTrackRenderer gRend = new CyGradientTrackRenderer();
		//updateBelowAndAbove();
		slider.setThumbRenderer(thumbRend);
		slider.setTrackRenderer(gRend);
		slider.addMouseListener(new ThumbMouseListener());

		/*
		 * Set tooltip for the slider.
		 */
		slider.setToolTipText("Double-click handles to edit boundary colors.");
	}

	@SuppressWarnings("unchecked")
	public void setVirtualRange(float[] values, Color[] colors){
		((MultiColorThumbModel)slider.getModel()).set(values, colors);
		setSidePanelIconColor(((MultiColorThumbModel)slider.getModel()).getBelowColor(), ((MultiColorThumbModel)slider.getModel()).getAboveColor());
	}
}
