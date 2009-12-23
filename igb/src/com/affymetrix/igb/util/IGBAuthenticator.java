package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.StringUtils;
import com.affymetrix.igb.general.ServerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;

/**
 * An Authenticator class for IGB.  It is designed to make it easier for a
 * user to authenticate to a server as well as letting a user use a server
 * anonymously.
 *
 * TODO:
 *  - detect when a login fails
 *  - detect difference between optional and required authentication
 *  - use this class to authenticate old-style genoviz DAS2 login
 *  - integrate this class with Server Preferences
 *  - transition away from using guest:guest for authentication
 *
 * @author sgblanch
 * @version $Id$
 */
public class IGBAuthenticator extends Authenticator {
	private static enum AuthType { ASK, ANONYMOUS, AUTHENTICATE };
	
	private static final String[] OPTIONS = { "Login", "Cancel" };
	private static final String SAVE_PASSWORD = "Save Password";
	private static final String ALWAYS_ANON = "Always use anonymous access";
	private static final String EMPTY_STRING = "";
	private static final String GUEST = "guest";
	private static final String PREF_AUTH_TYPE = "authentication type";
	private static final String PREF_REMEMBER = "remember authentication";

	private static final String MESSAGE_OPTIONAL = "The server '%s' requested "
			+ "authentication.  Authentication to this server is optional "
			+ "however accessing this server anonymously will limit what data "
			+ "sets you can access.";

	private final JFrame parent;
	private JPanel dialog;
	private JPanel messageContainer;
	private JLabel server;
	private JTextField     username;
	private JPasswordField password;
	private JRadioButton anon;
	private JRadioButton auth;
	private JCheckBox remember;

	public IGBAuthenticator(JFrame parent) {
		this.parent = parent;

		/* Ensure construction happens on event queue */
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dialog = new JPanel();
				messageContainer = new JPanel();
				server = new JLabel();
				username = new JTextField();
				password = new JPasswordField();
				anon = new JRadioButton("Use anonymous login");
				auth = new JRadioButton("Authenticate to Server");
				remember = new JCheckBox();

				buildDialog();
			}
		});
	}

	/**
	 * Constructs the dialog that is presented to the user when IGB recieves an
	 * authentication request from a server.
	 */
	private void buildDialog() {
		JLabel s = new JLabel("Server ");
		final JLabel u = new JLabel("Username ");
		final JLabel p = new JLabel("Password ");
		ButtonGroup group = new ButtonGroup();
		GroupLayout layout = new GroupLayout(dialog);

		dialog.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.linkSize(SwingConstants.HORIZONTAL, s, u, p);

		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(messageContainer)
				.addComponent(anon)
				.addComponent(auth)
				.addGroup(layout.createSequentialGroup()
					.addComponent(s)
					.addComponent(server))
				.addGroup(layout.createSequentialGroup()
					.addComponent(u)
					.addComponent(username))
				.addGroup(layout.createSequentialGroup()
					.addComponent(p)
					.addComponent(password))
				.addComponent(remember));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(messageContainer)
				.addComponent(anon)
				.addComponent(auth)
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(s)
					.addComponent(server))
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(u)
					.addComponent(username))
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(p)
					.addComponent(password))
				.addComponent(remember));

		ActionListener radioListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				u.setEnabled(auth.isSelected());
				p.setEnabled(auth.isSelected());
				username.setEnabled(auth.isSelected());
				password.setEnabled(auth.isSelected());

				if (anon.isSelected()) {
					remember.setText(ALWAYS_ANON);
				} else {
					remember.setText(SAVE_PASSWORD);
				}
			}
		};

		group.add(anon);
		group.add(auth);
		anon.addActionListener(radioListener);
		auth.addActionListener(radioListener);
		anon.setSelected(true);
		radioListener.actionPerformed(null);

		messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));
	}

	/**
	 * Request credentials to authenticate to the server.  First consults the
	 * preferences and then prompts the user.
	 * 
	 * @return a PasswordAuthentication to use against the server
	 */
	@Override
	public PasswordAuthentication getPasswordAuthentication() {
		GenericServer serverObject = null;
		try {
			serverObject = ServerList.getServer(this.getRequestingURL());
		} catch (URISyntaxException ex) {
			Logger.getLogger(IGBAuthenticator.class.getName()).log(Level.SEVERE, "Problem translating URL '" + this.getRequestingURL().toString() + "' to server", ex);
		}

		String url = serverObject != null ? serverObject.URL : this.getRequestingURL().toString();

		Preferences serverNode = UnibrowPrefsUtil.getServersNode().node(GeneralUtils.URLEncode(url));
		AuthType authType = AuthType.valueOf(serverNode.get(PREF_AUTH_TYPE, AuthType.ASK.toString()));
		String userFromPrefs = serverObject.login != null ? serverObject.login : EMPTY_STRING;
		String passFromPrefs = serverObject.password != null ? serverObject.password : EMPTY_STRING;

		if (authType == AuthType.AUTHENTICATE && !userFromPrefs.equals(EMPTY_STRING) && !passFromPrefs.equals(EMPTY_STRING)) {
			return new PasswordAuthentication(userFromPrefs, passFromPrefs.toCharArray());
		} else if (authType == AuthType.ANONYMOUS) {
			return doAnonymous();
		} else {
			return displayDialog(serverNode, serverObject, url);
		}
	}

	/**
	 * Returns 'anonymous' credentials for authenticating against a genopub
	 * server.
	 *
	 * @return a PasswordAuthentication with the username and password set to 'guest'
	 */
	private PasswordAuthentication doAnonymous() {
		return new PasswordAuthentication(GUEST, GUEST.toCharArray());
	}

	/**
	 * Prompt the user on how to authenticate to the server.
	 * 
	 * @param serverNode
	 * @param serverObject
	 * @param url
	 * @return
	 */
	private PasswordAuthentication displayDialog(final Preferences serverNode, final GenericServer serverObject, final String url) {
		setMessage(serverObject.serverName);
		server.setText(url);
		anon.setSelected(true);
		remember.setEnabled(serverNode.parent().getBoolean(PREF_REMEMBER, true));

		int result = JOptionPane.showOptionDialog(parent, dialog, null, OK_CANCEL_OPTION, PLAIN_MESSAGE, null, OPTIONS, OPTIONS[0]);

		if (result == OK_OPTION) {
			if (remember.isSelected()) {
				savePreferences(serverNode, serverObject);
			}

			if (auth.isSelected()) {
				return new PasswordAuthentication(username.getText(), password.getPassword());
			} else {
				return doAnonymous();
			}
		}

		/* User cancelled or quit login prompt */
		/*
		 * We really want to return null here, but there is a bug in
		 * Das2ServerInfo: getSources() will call initialize() every time
		 * if the login() fails.  Currently, this occurs 4 times on startup.
		 */
		return doAnonymous();
	}

	/**
	 * Formats and word wraps the message of the authentication dialog.
	 * 
	 * @param serverName friendly name of the server that requested authentication
	 */
	private void setMessage(String serverName) {
		/* instantiante current simply to steal FontMetrics from it */
		JLabel current = new JLabel();
		String[] message = StringUtils.wrap(
				String.format(MESSAGE_OPTIONAL, serverName),
				current.getFontMetrics(current.getFont()),
				500);

		messageContainer.removeAll();

		for (String line : message) {
			current = new JLabel(line);
			messageContainer.add(current);
		}
	}

	/**
	 * Writes the user's choices out to the preferences.
	 * 
	 * @param serverNode the preferences node for this server
	 * @param serverObject the GenericServer object for this server
	 */
	private void savePreferences(Preferences serverNode, GenericServer serverObject) {
		AuthType authType = anon.isSelected() ? AuthType.ANONYMOUS : AuthType.AUTHENTICATE;
		serverNode.put(PREF_AUTH_TYPE, authType.toString());
		serverNode.parent().putBoolean(PREF_REMEMBER, remember.isSelected());
		if (authType == authType.AUTHENTICATE) {
			serverObject.login = username.getText();
			serverObject.password = new String(password.getPassword());
			ServerList.addServerToPrefs(serverObject);
		}
	}
}
