package com.affymetrix.igb.util;

import com.affymetrix.genometry.data.DataProvider;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.LOGIN;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.PASSWORD;
import static com.affymetrix.genometry.general.DataProviderPrefKeys.REMEMBER_CREDENTIALS;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genometry.util.StringEncrypter;
import static com.affymetrix.genometry.util.StringEncrypter.DESEDE_ENCRYPTION_SCHEME;
import com.affymetrix.igb.general.DataProviderManager;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.Timer;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class IGBAuthenticator extends Authenticator {

    private static final Logger logger = LoggerFactory.getLogger(IGBAuthenticator.class);
    private static final Set<String> HOSTIGNORELIST = Sets.newHashSet();
    private static final StringEncrypter ENCRYPTER = new StringEncrypter(DESEDE_ENCRYPTION_SCHEME);
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("igb");
    private static final String ERROR_LOGIN = BUNDLE.getString("errorLogin");
    private static int loginAttempts = 0;

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (HOSTIGNORELIST.contains(getRequestingHost())) {
            return null;
        }
        Optional<DataProvider> dataProvider = DataProviderManager.getServerFromUrlStatic(this.getRequestingURL().toString());
        final JPanel panel = new JPanel(new MigLayout("wrap 2"));
        if (dataProvider.isPresent()) {
            panel.add(new JLabel(dataProvider.get().getUrl() + " asks for authentication:"), "span 2");
        } else {
            panel.add(new JLabel(getRequestingHost() + " asks for authentication:"), "span 2");
        }
        final JLabel errorLoginLabel = new JLabel("");
        panel.add(errorLoginLabel, "span 2");
        errorLoginLabel.setForeground(Color.red);
        panel.add(new JLabel("User:"));
        final JTextField user = new JTextField(20);
        panel.add(user);
        panel.add(new JLabel("Password:"));
        final JPasswordField password = new JPasswordField(20);
        panel.add(password);
        final JCheckBox showPassword = new JCheckBox();
        showPassword.addItemListener(e -> {
            if (showPassword.isSelected()) {
                password.setEchoChar((char) 0);
            } else {
                password.setEchoChar('\u2022');
            }
        });
        showPassword.setText("Show Password");
        panel.add(showPassword, "wrap");
        final JCheckBox rememberCredentials = new JCheckBox("Save Password");

        if (loginAttempts > 0) {
            errorLoginLabel.setText(ERROR_LOGIN);
        } else {
            loginAttempts++;
        }
        if (dataProvider.isPresent()) {
            Preferences dataProviderNode = PreferenceUtils.getDataProviderNode(dataProvider.get().getUrl());
            final boolean currentRememberStatus = dataProviderNode.getBoolean(REMEMBER_CREDENTIALS, false);
            String userName = null;
            String prefPwd = null;
            if (currentRememberStatus) {
                userName = dataProviderNode.get(LOGIN, null);
                prefPwd = dataProviderNode.get(PASSWORD, null);
                if (!prefPwd.isEmpty()) {
                    prefPwd = ENCRYPTER.decrypt(prefPwd);
                }
            } else if (dataProvider.get().getLogin().isPresent()) {
                userName = dataProvider.get().getLogin().get();
                if (dataProvider.get().getPassword().isPresent()) {
                    prefPwd = dataProvider.get().getPassword().get();
                }
            }
            if (!Strings.isNullOrEmpty(userName) || !Strings.isNullOrEmpty(prefPwd)) {
                try {
                    PasswordAuthentication persistedCredentials = validateAuthentication(userName, new JPasswordField(prefPwd).getPassword());
                    return persistedCredentials;
                } catch (FileNotFoundException ex) {
                    //do nothing
                    return null;
                } catch (IOException ex) {
                    logger.error(BUNDLE.getString("invalidCredentials"));
                    dataProviderNode.putBoolean(REMEMBER_CREDENTIALS, false);
                }
            }
            rememberCredentials.setSelected(currentRememberStatus);
            panel.add(rememberCredentials);
        }

        int option = JOptionPane.showConfirmDialog(null, panel, getRequestingPrompt(), JOptionPane.OK_CANCEL_OPTION);
        // work around Java's internal ISO-8859-1 encoding
        if (option == JOptionPane.OK_OPTION) {
            final String string = new String(password.getPassword());
            final byte[] bytes;
            try {
                bytes = string.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            final char[] chars = new char[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                chars[i] = (char) (bytes[i] & 0xff);
            }
            String username = user.getText();
            String passwordPlainText = new String(chars);
            if (dataProvider.isPresent() && rememberCredentials.isSelected()) {
                Preferences dataProviderNode = PreferenceUtils.getDataProviderNode(dataProvider.get().getUrl());
                dataProviderNode.putBoolean(REMEMBER_CREDENTIALS, true);
            }
            dataProvider.get().setLogin(username);
            dataProvider.get().setPassword(passwordPlainText);
            if (Strings.isNullOrEmpty(username) || chars.length == 0) {
                return null;
            }
            return new PasswordAuthentication(username, chars);
        } else {
            temporarilyIgnoreHost();
            loginAttempts = 0;
            return null;
        }
    }

    private void temporarilyIgnoreHost() {
        //track host for a few seconds to prevent recurring popups
        String currentRequestingHost = getRequestingHost();
        HOSTIGNORELIST.add(currentRequestingHost);
        Timer timer = new Timer(3000, event -> {
            HOSTIGNORELIST.remove(currentRequestingHost);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private PasswordAuthentication validateAuthentication(final String username, final char[] pwd) throws IOException {
        PasswordAuthentication pa = new PasswordAuthentication(username, pwd);
        Authenticator.setDefault(new Authenticator() {
            int authenticationValidationCount = 0;

            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                if (authenticationValidationCount > 0) {
                    return null;
                }
                authenticationValidationCount++;
                return pa;
            }
        });
        try (InputStream temp = getRequestingURL().openStream()) {
        } catch (IOException ex) {
            throw ex;
        } finally {
            Authenticator.setDefault(this);
        }
        return pa;
    }

    public static void resetAuthentication(DataProvider dataProvider) {
        Preferences dataProviderNode = PreferenceUtils.getDataProviderNode(dataProvider.getUrl());
        dataProviderNode.putBoolean(REMEMBER_CREDENTIALS, false);
        dataProvider.setLogin(null);
        dataProvider.setPassword(null);
        loginAttempts = 0;
    }
}
