package com.affymetrix.igb.util;

import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.IgbStringUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.swing.JRPTextField;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.commons.lang3.StringUtils;

/**
 * An Authenticator class for IGB. It is designed to make it easier for a user
 * to authenticate to a server as well as letting a user use a server
 * anonymously.
 *
 * TODO: - detect when a login fails - detect difference between optional and
 * required authentication - use this class to authenticate old-style genoviz
 * DAS2 login - integrate this class with Server Preferences - transition away
 * from using guest:guest for authentication
 *
 * @author sgblanch
 * @version $Id: IGBAuthenticator.java 10143 2012-02-02 21:59:36Z hiralv $
 * updated tkanapar
 */
public class IGBAuthenticator extends Authenticator {

    private static enum AuthType {

        ASK, ANONYMOUS, AUTHENTICATE
    }

    private static boolean authenticationRequestCancelled = false;
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("igb");
    private static final String ERROR_LOGIN = BUNDLE.getString("errorLogin");
    private static final String GUEST = "guest";
    private static final String PREF_AUTH_TYPE = "authentication type";
    private static final String PREF_REMEMBER = "remember authentication";
    private final JFrame parent;

    public IGBAuthenticator(JFrame parent) {
        this.parent = parent;
    }

    /**
     * Constructs the dialog that is presented to the user when IGB recieves an
     * authentication request from a server.
     */
    private static JOptionPane buildDialog(
            final GenericServer serverObject,
            final boolean authOptional,
            final String urlString,
            final String errorString,
            final JRadioButton anon,
            final JRadioButton auth,
            final JRPTextField username,
            final JPasswordField password,
            final JCheckBox remember) {

        final JPanel dialog = new JPanel();
        final JLabel s = new JLabel(BUNDLE.getString("server"));
        final JLabel u = new JLabel(BUNDLE.getString("username"));
        final JLabel p = new JLabel(BUNDLE.getString("password"));
        final JButton login = new JButton(BUNDLE.getString("login"));
        final JButton cancel = new JButton(BUNDLE.getString("cancel"));
        final JButton tryAgain = new JButton(BUNDLE.getString("tryagain"));
        final JCheckBox showPassword = new JCheckBox();
        final JPanel messageContainer = serverObject == null ? new JPanel() : setMessage(serverObject.serverName, authOptional);
        final JLabel error = new JLabel(errorString);
        final JLabel server = new JLabel(urlString);

        Object[] OPTIONS = {login, cancel};
        Object[] OPTIONS2 = {tryAgain, cancel};

        ButtonGroup group = new ButtonGroup();
        GroupLayout layout = new GroupLayout(dialog);
        dialog.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.linkSize(SwingConstants.HORIZONTAL, s, u, p);
        layout.setHorizontalGroup(layout.createParallelGroup().addComponent(messageContainer).addComponent(anon).addComponent(auth).addGroup(layout.createSequentialGroup().addComponent(s).addComponent(server)).addGroup(layout.createSequentialGroup().addComponent(u).addComponent(username)).addGroup(layout.createSequentialGroup().addComponent(p).addComponent(password)).addComponent(error).addComponent(showPassword).addComponent(remember));
        layout.setVerticalGroup(layout.createSequentialGroup().addComponent(messageContainer).addComponent(anon).addComponent(auth).addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(s).addComponent(server)).addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(u).addComponent(username)).addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(p).addComponent(password)).addComponent(error).addComponent(showPassword).addComponent(remember));
        group.add(anon);
        group.add(auth);

        error.setForeground(Color.red);
        remember.setSelected(true);

        JOptionPane optionPane = error.getText() == null ? new JOptionPane(dialog, PLAIN_MESSAGE, OK_CANCEL_OPTION, null, OPTIONS, OPTIONS[0]) : new JOptionPane(dialog, PLAIN_MESSAGE, OK_CANCEL_OPTION, null, OPTIONS2, OPTIONS2[0]);

        showPassword.addItemListener(e -> {
            if (showPassword.isSelected()) {
                password.setEchoChar((char) 0);
            } else {
                password.setEchoChar('\u2022');
            }
        });

        ActionListener radioListener = e -> {
            u.setEnabled(auth.isSelected());
            p.setEnabled(auth.isSelected());
            showPassword.setText("Display Password");
            username.setEnabled(auth.isSelected());
            password.setEnabled(auth.isSelected());
            showPassword.setEnabled(auth.isSelected());
            if (auth.isSelected() && (StringUtils.isBlank(username.getText()) || password.getPassword().length == 0)) {
                login.setEnabled(false);
                tryAgain.setEnabled(false);
            }

            if (anon.isSelected()) {
                login.setEnabled(true);
                tryAgain.setEnabled(true);
                remember.setText(BUNDLE.getString("alwaysAnonymous"));
            } else {
                remember.setText(BUNDLE.getString("savePassword"));
            }
        };
        anon.addActionListener(radioListener);
        auth.addActionListener(radioListener);

        login.addActionListener(new UPActionListener(optionPane, JOptionPane.OK_OPTION));
        tryAgain.addActionListener(new UPActionListener(optionPane, JOptionPane.OK_OPTION));
        cancel.addActionListener(new UPActionListener(optionPane, JOptionPane.CANCEL_OPTION));

        DocumentListener dl = new UPDocumentListener(new JTextField[]{username, password}, login, tryAgain);
        username.getDocument().addDocumentListener(dl);
        password.getDocument().addDocumentListener(dl);

        radioListener.actionPerformed(null);
        dl.changedUpdate(null);
        if (anon.isSelected()) {
            login.setEnabled(true);
            tryAgain.setEnabled(true);
        }
        return optionPane;
    }

    /**
     * Request credentials to authenticate to the server. First consults the
     * preferences and then prompts the user.
     *
     * @return a PasswordAuthentication to use against the server
     */
    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        String urlString = this.getRequestingURL().toString();
        String url = urlString;
        Preferences serverNode = null;
        AuthType authType = AuthType.ASK;
        String userFromPrefs = "";
        String passFromPrefs = "";
        GenericServer serverObject = null;

        try {
            serverObject = ServerList.getServerInstance().getServer(this.getRequestingURL());
        } catch (URISyntaxException ex) {
            Logger.getLogger(IGBAuthenticator.class.getName()).log(Level.SEVERE, "Problem translating URL '" + this.getRequestingURL().toString() + "' to server", ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(IGBAuthenticator.class.getName()).log(Level.WARNING, "URL {0} was not in server list.", this.getRequestingURL());
        }

        if (serverObject != null) {
            url = serverObject.URL;
            serverNode = PreferenceUtils.getServersNode().node(GenericServer.getHash(url));//GeneralUtils.URLEncode(url));
            authType = AuthType.valueOf(serverNode.get(PREF_AUTH_TYPE, AuthType.ASK.toString()));
            if (serverObject.getLogin() != null) {
                userFromPrefs = serverObject.getLogin();
            }
            if (serverObject.getPassword() != null) {
                passFromPrefs = serverObject.getPassword();
            }
        }

        if (authType == AuthType.AUTHENTICATE && userFromPrefs.length() != 0 && passFromPrefs.length() != 0) {
            try {
                return testAuthentication(urlString, userFromPrefs, passFromPrefs.toCharArray());
            } catch (Exception ex) {
                return displayDialog(parent.getFocusOwner(), serverNode, serverObject, url, userFromPrefs, passFromPrefs, ERROR_LOGIN);
            }
        } else if (authType == AuthType.ANONYMOUS) {
            return doAnonymous();
        } else {
            return displayDialog(parent.getFocusOwner(), serverNode, serverObject, url, userFromPrefs, passFromPrefs, null);
        }
    }

    /**
     * Returns 'anonymous' credentials for authenticating against a genopub
     * server.
     *
     * @return a PasswordAuthentication with the username and password set to
     * 'guest'
     */
    private static PasswordAuthentication doAnonymous() {
        return new PasswordAuthentication(GUEST, GUEST.toCharArray());
    }

    /**
     * Prompt the user on how to authenticate to the server.
     *
     * @param serverNode
     * @param serverObject
     * @param url
     * @return Password authentication to the user
     */
    private PasswordAuthentication displayDialog(final Component parent, final Preferences serverNode,
            final GenericServer serverObject, final String urlString, final String usrnmString, final String pwdString, final String errorString) {

        final boolean authOptional = serverObject != null && serverObject.serverType != null && serverObject.serverType.isAuthOptional();
        final JRPTextField username = new JRPTextField("IGBAuthenticator_username", usrnmString);
        final JPasswordField password = new JPasswordField(pwdString);
        final JRadioButton anon = new JRadioButton(BUNDLE.getString("useAnonymousLogin"));
        final JRadioButton auth = new JRadioButton(BUNDLE.getString("authToServer"));
        final JCheckBox remember = new JCheckBox();

        anon.setSelected(authOptional);
        anon.setEnabled(authOptional);
        auth.setSelected(!authOptional);
        remember.setEnabled(serverObject != null && serverNode != null && serverNode.parent().getBoolean(PREF_REMEMBER, true));
        remember.setSelected(!remember.isEnabled() && !authOptional);

        JOptionPane optionPane = buildDialog(serverObject, authOptional, urlString, errorString, anon, auth, username, password, remember);
        JDialog jdg = optionPane.createDialog(parent, null);
        jdg.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                authenticationRequestCancelled = true;
            }
        });
        jdg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        jdg.setVisible(true);
        if (optionPane.getValue() == (Integer) JOptionPane.CANCEL_OPTION) {
            authenticationRequestCancelled = true;
            return null;
        }
        if (optionPane.getValue() == (Integer) JOptionPane.OK_OPTION) {
            authenticationRequestCancelled = false;
            if (auth.isSelected()) {
                try {
                    PasswordAuthentication pa = testAuthentication(urlString, username.getText(), password.getPassword());

                    //Only save correct username and password
                    if (remember.isSelected()) {
                        savePreferences(serverNode, serverObject, username.getText(),
                                password.getPassword(), anon.isSelected(), remember.isSelected());
                    }

                    return pa;
                } catch (Exception ex) {
                    return displayDialog(parent, serverNode, serverObject, urlString, username.getText(), new String(password.getPassword()), ERROR_LOGIN);
                }
            } else {
                // This can be null in case of opening url.
                if (serverNode != null) {
                    serverNode.put(PREF_AUTH_TYPE, AuthType.ANONYMOUS.toString());
                    serverNode.parent().putBoolean(PREF_REMEMBER, true);
                }
                return doAnonymous();
            }
        }

        /* User cancelled or quit login prompt */
        /*
         * We really want to return null here, but there is a bug in
         * Das2ServerInfo: getSources() will call initialize() every time
         * if the login() fails.  Currently, this occurs 4 times on startup.
         */
        return authOptional ? doAnonymous() : null;
    }

    private synchronized PasswordAuthentication testAuthentication(final String urlString, final String usrnmString, final char[] pwd) {
        InputStream temp = null;
        PasswordAuthentication pa = new PasswordAuthentication(usrnmString, pwd);
        try {
            Authenticator.setDefault(new SingleAuthenticator(pa));
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            temp = conn.getInputStream();

            if (temp == null) {
                throw new IllegalArgumentException(ERROR_LOGIN);
            }

        } catch (IOException ex) {
        } finally {
            GeneralUtils.safeClose(temp);
            Authenticator.setDefault(this);
        }
        return pa;
    }

    /**
     * Formats and word wraps the message of the authentication dialog.
     *
     * @param serverName friendly name of the server that requested
     * authentication.
     * @return a JPanel containing the message
     */
    private static JPanel setMessage(String serverName, boolean authOptional) {
        JPanel messageContainer = new JPanel();
        /* instantiante current simply to steal FontMetrics from it */
        JLabel current = new JLabel();
        String[] message = IgbStringUtils.wrap(
                MessageFormat.format(
                        BUNDLE.getString(authOptional ? "authOptional" : "authRequired"),
                        serverName),
                current.getFontMetrics(current.getFont()),
                500);

        messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));

        for (String line : message) {
            current = new JLabel(line);
            messageContainer.add(current);
        }

        return messageContainer;
    }

    /**
     * Writes the user's choices out to the preferences.
     *
     * @param serverNode the preferences node for this server
     * @param serverObject the GenericServer object for this server
     */
    private static void savePreferences(
            Preferences serverNode,
            GenericServer serverObject,
            String username,
            char[] password,
            boolean anon,
            boolean remember) {

        if (serverNode == null || serverObject == null) {
            return;
        }

        AuthType authType = anon ? AuthType.ANONYMOUS : AuthType.AUTHENTICATE;
        serverNode.put(PREF_AUTH_TYPE, authType.toString());
        serverNode.parent().putBoolean(PREF_REMEMBER, remember);
        if (authType == AuthType.AUTHENTICATE) {
            serverObject.setLogin(username);
            serverObject.setPassword(new String(password));
        }
    }

    public static void resetAuth(String url) {
        Preferences serverNode = PreferenceUtils.getServersNode().node(GenericServer.getHash(url));//GeneralUtils.URLEncode(url));
        serverNode.put(PREF_AUTH_TYPE, AuthType.ASK.toString());
    }

    private static class SingleAuthenticator extends Authenticator {

        final PasswordAuthentication pa;

        private SingleAuthenticator(PasswordAuthentication pa) {
            this.pa = pa;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return pa;
        }
    }

    public static boolean authenticationRequestCancelled() {
        return authenticationRequestCancelled;
    }

    public static void resetAuthenticationRequestCancelled() {
        authenticationRequestCancelled = !authenticationRequestCancelled;
    }

    private static class UPDocumentListener implements DocumentListener {

        JTextField[] tfs;
        JComponent[] comps;

        private UPDocumentListener(JTextField[] tfs, JComponent... comps) {
            this.tfs = tfs;
            this.comps = comps;
        }

        @Override
        public void insertUpdate(DocumentEvent de) {
            checkFieldsChange();
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
            checkFieldsChange();
        }

        @Override
        public void changedUpdate(DocumentEvent de) {
            checkFieldsChange();
        }

        private void checkFieldsChange() {
            boolean value = false;
            for (JTextField tf : tfs) {
                if (tf.getText().trim().length() > 0) {
                    value = true;
                } else {
                    value = false;
                    break;
                }
            }
            for (JComponent comp : comps) {
                comp.setEnabled(value);
            }
        }
    }

    private static class UPActionListener implements ActionListener {

        final JOptionPane jop;
        final Integer value;

        private UPActionListener(JOptionPane jop, Integer value) {
            this.jop = jop;
            this.value = value;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            jop.setValue(value);
        }
    }

}
