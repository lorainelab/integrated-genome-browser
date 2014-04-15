package cytoscape.visual.ui.editors.continuous;

public class Main {

	public static void main(String[] args) {
//		JFrame frame = new JFrame("Test");
//		frame.setSize(800, 400);
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//		JXMultiThumbSlider<Color> slider = new JXMultiThumbSlider<Color>();
//		final MultiColorThumbModel model = new MultiColorThumbModel();
//		final ColorInterpolator colorInterpolator = new GradientColorInterpolator(model);
//		slider.setModel(model);
//		slider.setTrackRenderer(new CyGradientTrackRenderer());
//		slider.setThumbRenderer(new TriangleThumbRenderer());
//
//		//slider.setMinimumValue(100.0F);
//		slider.setMaximumValue(100.0F);
//
//		slider.getModel().addThumb(20.0f, Color.cyan);
//		slider.getModel().addThumb(50.0f, Color.green);
//		slider.getModel().addThumb(80.0f, Color.magenta);
//
//		final JLabel label = new JLabel("Color at 150");
//		label.setMaximumSize(new Dimension(80, 20));
//
//		slider.addMultiThumbListener(new ThumbListener() {
//			@Override
//			public void thumbMoved(int thumb, float pos) {
//				label.setForeground(colorInterpolator.getColor(50));
//			}
//
//			@Override
//			public void thumbSelected(int thumb) {
//			}
//
//			@Override
//			public void mousePressed(MouseEvent evt) {
//				if (evt.getClickCount() > 1) {
//					JOptionPane.showConfirmDialog(null, "Selected Color");
//				}
//			}
//		});
//
//		Box box = Box.createVerticalBox();
//		box.add(slider);
//		box.add(label);
//		frame.getContentPane().add(box);
//
//		frame.setLocationRelativeTo(null);
//		frame.setVisible(true);

		GradientEditorPanel.showDialog(600, 300, "Test");
	}
}
