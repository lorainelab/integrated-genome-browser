package org.bioviz.protannot;

import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.swing.MenuUtil;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import static org.bioviz.protannot.ProtAnnotAction.APP_NAME;
import static org.bioviz.protannot.ProtAnnotAction.APP_VERSION_FULL;
import static org.bioviz.protannot.ProtAnnotAction.BUNDLE;

/**
 *
 * @author hiralv
 */
class Actions {

    static AbstractAction getLoadAction() {
        AbstractAction load_action = new AbstractAction(MessageFormat.format(
                BUNDLE.getString("menuItemHasDialog"),
                BUNDLE.getString("openFile"))) {
            @Override
                    public void actionPerformed(ActionEvent e) {
                        ProtAnnotAction.getInstance().doLoadFile();
                    }
                };
        load_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_O);
        load_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("openFileTip"));
        return load_action;
    }

    static AbstractAction getAddServerAction() {
        AbstractAction add_server = new AbstractAction(MessageFormat.format(
                BUNDLE.getString("menuItemHasDialog"),
                BUNDLE.getString("addServer"))) {
            @Override
                    public void actionPerformed(ActionEvent e) {
                        ProtAnnotAction.getInstance().addServer();
                    }
                };
        add_server.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_A);
        add_server.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("addServerTip"));
        return add_server;
    }

    static AbstractAction getLoadFromServerAction() {
        final AbstractAction server_load_action = new AbstractAction(MessageFormat.format(
                BUNDLE.getString("menuItemHasDialog"),
                BUNDLE.getString("serverLoad"))) {
            @Override
                    public void actionPerformed(ActionEvent e) {
                        ProtAnnotAction.getInstance().loadFromServer();
                    }
                };
        server_load_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_S);
        server_load_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("serverLoadTip"));

        return server_load_action;
    }

    static AbstractAction getPrintAction() {
        AbstractAction print_action = new AbstractAction(MessageFormat.format(
                BUNDLE.getString("menuItemHasDialog"),
                BUNDLE.getString("print"))) {
            @Override
                    public void actionPerformed(ActionEvent e) {
                        ProtAnnotAction.getInstance().print();
                    }
                };
        print_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_P);
        print_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("printTip"));
        return print_action;
    }

    static AbstractAction getExportAction() {
        AbstractAction export_action = new AbstractAction(MessageFormat.format(
                BUNDLE.getString("menuItemHasDialog"),
                BUNDLE.getString("export"))) {
            @Override
                    public void actionPerformed(ActionEvent e) {
                        ProtAnnotAction.getInstance().export();
                    }
                };
        export_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_T);
        export_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("exportTip"));
        return export_action;
    }

    static AbstractAction getPreferencesAction() {
        AbstractAction preference_action = new AbstractAction(MessageFormat.format(
                BUNDLE.getString("menuItemHasDialog"),
                BUNDLE.getString("preferences"))) {
            @Override
                    public void actionPerformed(ActionEvent e) {
                        ProtAnnotAction.getInstance().colorChooser();
                    }
                };
        preference_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_E);
        preference_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("preferencesTip"));
        return preference_action;
    }

    static AbstractAction getExitAction() {
        AbstractAction quit_action = new AbstractAction(MessageFormat.format(
                BUNDLE.getString("menuItemHasDialog"),
                BUNDLE.getString("exit"))) {
            @Override
                    public void actionPerformed(ActionEvent e) {
                        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                                new WindowEvent(ProtAnnotAction.getInstance().getFrame(),
                                        WindowEvent.WINDOW_CLOSING));
                    }
                };
        quit_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_X);
        quit_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("exitTip"));
        return quit_action;
    }

    static AbstractAction getCopyAction() {
        final AbstractAction copy_action = new AbstractAction(
                BUNDLE.getString("copy")) {
            @Override
                    public void actionPerformed(ActionEvent e) {
                        Properties[] props = ProtAnnotAction.getInstance().getGenomeView().getProperties();
                        Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
                        StringSelection data = new StringSelection(props[0].getProperty("protein sequence"));
                        system.setContents(data, null);
                    }
                };

        MouseListener ml = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (!(e instanceof NeoMouseEvent)) {
                    return;
                }
                Properties[] props = ProtAnnotAction.getInstance().getGenomeView().getProperties();
                if (props != null && props.length == 1) {
                    copy_action.setEnabled(props[0].containsKey("protein sequence"));
                } else {
                    copy_action.setEnabled(false);
                }
            }
        };
        ProtAnnotAction.getInstance().getGenomeView().addMapListener(ml);
        copy_action.setEnabled(false);

        return copy_action;
    }

    /**
     * Asks ProtAnnotMain.getInstance() to open a browser window showing info
     * on the currently selected Glyph.
     */
    static AbstractAction getOpenInBrowserAction() {

        final StringBuilder url = new StringBuilder();

        final AbstractAction open_browser_action = new AbstractAction(BUNDLE.getString("openBrowser"),
                MenuUtil.getIcon("16x16/actions/search.png")) {

            @Override
                    public void actionPerformed(ActionEvent e) {
                        if (url.length() > 0) {
                            GeneralUtils.browse(url.toString());
                        } else {
                            Reporter.report("No URL associated with selected item",
                                    null, false, false, true);
                        }
                    }
                };

        open_browser_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_B);
        open_browser_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("openBrowserTip"));
        open_browser_action.setEnabled(false);

        MouseListener ml = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (!(e instanceof NeoMouseEvent)) {
                    return;
                }
                Properties[] props = ProtAnnotAction.getInstance().getGenomeView().getProperties();
                if (props != null && props.length == 1) {
                    url.delete(0, url.length());
                    url.append(build_url(props[0]));
                } else {
                    url.delete(0, url.length());
                }
                open_browser_action.setEnabled(url.length() > 0 ? true : false);
            }
        };
        ProtAnnotAction.getInstance().getGenomeView().addMapListener(ml);

        return open_browser_action;
    }

    /**
     * Asks ProtAnnotMain.getInstance() to center on the location of the
     * currently selected Glyph.
     */
    static AbstractAction getZoomToFeatureAction() {

        final AbstractAction zoom_to_feature_action = new AbstractAction(BUNDLE.getString("zoomToFeature")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                ProtAnnotAction.getInstance().getGenomeView().zoomToSelection();
            }
        };
        zoom_to_feature_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_Z);
        zoom_to_feature_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("zoomToFeatureTip"));
        zoom_to_feature_action.setEnabled(false);

        MouseListener ml = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (!(e instanceof NeoMouseEvent)) {
                    return;
                }
                List<GlyphI> selected = ProtAnnotAction.getInstance().getGenomeView().getSelected();
                zoom_to_feature_action.setEnabled(selected != null && !selected.isEmpty());
            }
        };
        ProtAnnotAction.getInstance().getGenomeView().addMapListener(ml);

        return zoom_to_feature_action;
    }

    static AbstractAction getToggleHairlineAction() {
        AbstractAction toggle_hairline_action = new AbstractAction(BUNDLE.getString("toggleHairline")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                ProtAnnotAction.getInstance().getGenomeView().toggleHairline();
            }
        };
        toggle_hairline_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_H);
        toggle_hairline_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("toggleHairlineTip"));
        toggle_hairline_action.putValue(AbstractAction.SELECTED_KEY, true);
        return toggle_hairline_action;
    }

    static AbstractAction getToggleHairlineLabelAction() {
        AbstractAction toggle_label_action = new AbstractAction(BUNDLE.getString("toggleHairlineLabel")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                ProtAnnotAction.getInstance().getGenomeView().toggleHairlineLabel();
            }
        };
        toggle_label_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_L);
        toggle_label_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("toggleHairlineLabelTip"));
        toggle_label_action.putValue(AbstractAction.SELECTED_KEY, true);
        return toggle_label_action;
    }

    static AbstractAction getOpenInNewWindow() {
        final AbstractAction newwin_action = new AbstractAction("Open table in new window") {

            @Override
            public void actionPerformed(ActionEvent e) {
                final AbstractAction action = this;
                action.setEnabled(false);

                final JPanel table_panel = ProtAnnotAction.getInstance().getGenomeView().getTablePanel();
                final JSplitPane split_pane = ProtAnnotAction.getInstance().getGenomeView().getSplitPane();
                split_pane.remove(table_panel);

                final JFrame jframe = new JFrame();
                jframe.setMinimumSize(new Dimension(table_panel.getWidth(), 100));
                jframe.setSize(table_panel.getSize());
                jframe.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent evt) {
                        split_pane.add(table_panel);
                        action.setEnabled(true);
                    }
                });
                jframe.add(table_panel);
                jframe.setVisible(true);
            }
        };

        return newwin_action;
    }

    static AbstractAction getAboutAction() {

        final JFrame frm = ProtAnnotAction.getInstance().getFrame();
        AbstractAction about_action = new AbstractAction(MessageFormat.format(
                BUNDLE.getString("menuItemHasDialog"),
                MessageFormat.format(
                        BUNDLE.getString("about"),
                        APP_NAME))) {

            @Override
                    public void actionPerformed(ActionEvent e) {
                        JPanel message_pane = new JPanel();
                        message_pane.setLayout(new BoxLayout(message_pane, BoxLayout.Y_AXIS));
                        JTextArea about_text = new JTextArea();
                        about_text.setEditable(false);

                        String text = APP_NAME + " " + APP_VERSION_FULL + "\n\n"
                        + "Protannot implements many useful features designed for \n"
                        + "understanding how alternative splicing, alternative promoters, \n"
                        + "alternative promoters, and alternative polyadenylation can \n"
                        + "affect the sequence and function of proteins encoded \n"
                        + "by diverse variants expressed from the same gene. \n\n"
                        + "Protannot is a program developed by Hiral Vora, John Nicol\n "
                        + "and Ann Loraine at the University of North Carolina at Charlotte. \n\n"
                        + "For more information, see:\n"
                        + "http://www.bioviz.org/protannot\n";

                        about_text.append(text);
                        message_pane.add(new JScrollPane(about_text));

                        final JOptionPane pane = new JOptionPane(message_pane, JOptionPane.INFORMATION_MESSAGE,
                                JOptionPane.DEFAULT_OPTION);
                        final JDialog dialog = pane.createDialog(frm, MessageFormat.format(BUNDLE.getString("about"), APP_NAME));
                        dialog.setVisible(true);
                    }
                };
        about_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_U);
        return about_action;
    }

    static AbstractAction getReportBugAction() {
        AbstractAction report_bug_action = new AbstractAction(MessageFormat.format(
                BUNDLE.getString("menuItemHasDialog"),
                BUNDLE.getString("reportABug"))) {

            @Override
                    public void actionPerformed(ActionEvent e) {
                        String u = "https://sourceforge.net/tracker/?limit=25&group_id=129420&atid=714744&category=1343170&status=1&category=1343170";
                        GeneralUtils.browse(u);
                    }
                };
        report_bug_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_R);
        return report_bug_action;
    }

    static AbstractAction getFeatureAction() {
        AbstractAction feature_action = new AbstractAction(MessageFormat.format(
                BUNDLE.getString("menuItemHasDialog"),
                BUNDLE.getString("requestAFeature"))) {

            @Override
                    public void actionPerformed(ActionEvent e) {
                        String u = "https://sourceforge.net/tracker/?limit=25&func=&group_id=129420&atid=714747&status=1&category=1449149";
                        GeneralUtils.browse(u);
                    }
                };
        feature_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_F);
        return feature_action;
    }

    static AbstractAction getShowConsoleAction() {
        AbstractAction show_console_action = new AbstractAction(MessageFormat.format(
                BUNDLE.getString("menuItemHasDialog"),
                BUNDLE.getString("showConsole"))) {

            @Override
                    public void actionPerformed(ActionEvent e) {
//                        ConsoleView.showConsole(APP_NAME);
                    }
                };
        show_console_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_C);
        return show_console_action;
    }

    /**
     * Builds url of selected glyphs
     *
     * @param p Property of the selected glyph
     * @return String of build url.
     */
    private static String build_url(Properties p) {
        String val = p.getProperty("URL");
        if (val != null) {
            return val;
        }
        val = p.getProperty("interpro_id");
        if (val != null) {
            return "http://www.ebi.ac.uk/interpro/IEntry?ac=" + val;
        }
        val = p.getProperty("exp_ngi");
        if (val != null) {
            if (val.startsWith("gi:")) {
                val = val.substring(3);
            }

            return "http://www.ncbi.nlm.nih.gov:80/entrez/query.fcgi?cmd=Retrieve&db=nucleotide&list_uids=" + val + "&dopt=GenBank";
        } else {
            return null;
        }
    }

    private Actions() {
    }
}
