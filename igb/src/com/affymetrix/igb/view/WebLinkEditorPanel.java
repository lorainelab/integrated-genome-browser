package com.affymetrix.igb.view;

import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.genoviz.swing.recordplayback.JRPTextField;
import com.affymetrix.genoviz.util.ErrorHandler;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.*;
import javax.swing.border.*;

public final class WebLinkEditorPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private WebLink webLink = null;
	private JRPTextField name_tf = new JRPTextField("WebLinkEditorPanel_name_tf", "");
	private JRPTextField url_tf = new JRPTextField("WebLinkEditorPanel_url_tf", "");
	private JRPTextField regex_tf = new JRPTextField("WebLinkEditorPanel_regex_tf", "");
	private final JRadioButton name_b = new JRadioButton("Track Name");
	private final JRadioButton id_b = new JRadioButton("Annotation ID");
	private final ButtonGroup but_group_1 = new ButtonGroup();

	public WebLinkEditorPanel() {
		Box container = Box.createVerticalBox();

		Box row1 = Box.createHorizontalBox();
		JLabel name_l = new JLabel("Name:");
		row1.add(name_l);
		row1.add(Box.createRigidArea(new Dimension(6, 0)));
		row1.add(name_tf);
		name_tf.setPreferredSize(new Dimension(500, name_tf.getPreferredSize().height));

		Box row2 = Box.createHorizontalBox();
		JLabel url_l = new JLabel("URL:");
		row2.add(url_l);
		row2.add(Box.createRigidArea(new Dimension(18, 0)));
		row2.add(url_tf);

		Box row3 = Box.createHorizontalBox();
		row3.add(name_b);
		row3.add(id_b);
		row3.add(Box.createRigidArea(new Dimension(6, 0)));
		row3.add(regex_tf);

		but_group_1.add(name_b);
		but_group_1.add(id_b);
		name_b.setSelected(true);

		container.add(row1);
		container.add(Box.createRigidArea(new Dimension(0, 6)));
		container.add(row2);
		container.add(Box.createRigidArea(new Dimension(0, 6)));
		container.add(row3);

		name_b.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				regex_tf.grabFocus();
			}
		});
		id_b.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				regex_tf.grabFocus();
			}
		});

		container.setBorder(new EmptyBorder(new java.awt.Insets(1, 1, 1, 1)));
		this.add(container);
	}

	public void setWebLink(WebLink link) {
		this.webLink = link;

		name_tf.setText("");
		url_tf.setText("");
	}

	private static boolean isEmpty(String s) {
		return (s == null || s.trim().length() == 0);
	}

	public boolean setLinkPropertiesFromGUI() {
		try {
			setLinkPropertiesFromGUIx();
			return true;
		} catch (Exception e) {
			ErrorHandler.errorPanel("Error", "Problem setting web link properties\n", name_tf, e);
		}
		return false;
	}

	/* If the data has been pre-validated with validateUserInput(),
	 *  there should never be an exception here.
	 */
	private void setLinkPropertiesFromGUIx() throws IllegalArgumentException, PatternSyntaxException {
		if (isEmpty(name_tf.getText()) || isEmpty(url_tf.getText())) {
			throw new IllegalArgumentException("Name and URL must not be blank.");
		}
		webLink.setName(name_tf.getText());
		webLink.setUrl(url_tf.getText());
		if (id_b.isSelected()) {
			webLink.setRegexType(WebLink.RegexType.ID);
		} else {
			webLink.setRegexType(WebLink.RegexType.TYPE);
		}

		webLink.setRegex(regex_tf.getText());
	}

	public boolean showDialog(JFrame frame) {

		final JOptionPane opt_pane = new JOptionPane(this,
				JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);
		final JDialog dialog = new JDialog(frame, "Web Link Editor", true);
		dialog.setContentPane(opt_pane);

		opt_pane.addPropertyChangeListener(new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				String prop = evt.getPropertyName();
				if (!dialog.isVisible() || evt.getSource() != opt_pane || !prop.equals(JOptionPane.VALUE_PROPERTY)) {
					return;
				}

				Object value = opt_pane.getValue();
				boolean ok = false;

				if (value instanceof Integer) {
					int value_int = ((Integer) value).intValue();
					if (value_int == JOptionPane.YES_OPTION) {
						ok = validateUserInput();
						if (ok) {
							dialog.setVisible(false);
						}
						// Keep window open if user selected ok, but validation failed
					} else {
						// User selected "cancel", so simply close the window
						dialog.setVisible(false);
					}
				}

				//Reset the JOptionPane's value.
				//Otherwise, if the user presses the same button again, no event will be fired.
				if (!ok) {
					opt_pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
				}
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);

		Object choice_obj = opt_pane.getValue();
		int choice = JOptionPane.CANCEL_OPTION;
		if (choice_obj instanceof Integer) {
			choice = ((Integer) choice_obj).intValue();
		}

		// only continue if user clicked OK button
		return (choice == JOptionPane.OK_OPTION);
	}

	/** Checks that all user-entered values are OK.  Pops-up error messages if not.
	 *  @return true if all data is OK.
	 */
	private boolean validateUserInput() {
		if (isEmpty(name_tf.getText())) {
			ErrorHandler.errorPanel("The name cannot be blank");
			name_tf.grabFocus();
			return false;
		}

		if (isEmpty(url_tf.getText())) {
			ErrorHandler.errorPanel("The URL cannot be blank");
			url_tf.grabFocus();
			return false;
		}

		try {
			new URL(url_tf.getText());
		} catch (MalformedURLException e) {
			ErrorHandler.errorPanel("Malformed URL",
					"The given URL appears to be invalid.\n" + e.getMessage(),
					url_tf);
			return false;
		}

		if (isEmpty(regex_tf.getText())) {
			ErrorHandler.errorPanel("The regular expression cannot be blank");
			regex_tf.grabFocus();
			return false;
		}

		try {
			Pattern.compile(regex_tf.getText());
		} catch (PatternSyntaxException pse) {
			ErrorHandler.errorPanel("Bad Regular Expression",
					"Error in regular expression:\n" + pse.getMessage(), regex_tf);
			regex_tf.grabFocus();
			return false;
		}

		return true;
	}
}
