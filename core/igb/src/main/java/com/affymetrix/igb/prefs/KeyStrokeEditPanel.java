/**
 * Copyright (c) 2001-2004 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.prefs;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.JRPCheckBox;
import com.affymetrix.igb.swing.JRPTextField;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public final class KeyStrokeEditPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final boolean DEBUG = false;
    public final JRPTextField key_field
            = new JRPTextField("KeyStrokeEditPanel_key_field", 20);
    private final JRPCheckBox toolbar_field
            = new JRPCheckBox("KeyStrokeEditPanel_toolbar_field", "Toolbar ?");
    public final JLabel key_label
            = new JLabel("Type a shortcut: ");
    public final JLabel note_label
            = new JLabel();
    public final JRPButton clear_button
            = new JRPButton("KeyStrokeEditPanel_clear_button", "Clear");
    private String key = null;
    private String lastTimeFocusGained = "";
    private String lastCommand = "";
    private FocusListener lois = new FocusListener() {

        @Override
        public void focusGained(java.awt.event.FocusEvent fe) {
            Object o = fe.getSource();
            if (o instanceof JTextField) {
                JTextField tf = (JTextField) o;
                KeyStrokeEditPanel.this.lastTimeFocusGained = tf.getText();
            }
        }

        @Override
        public void focusLost(java.awt.event.FocusEvent evt) {
            if (KeyStrokeEditPanel.this.lastCommand != null) {
                applyAction(KeyStrokeEditPanel.this.lastCommand);
            }
        }

    };

    /**
     * Creates a new instance of KeyStrokesView
     */
    public KeyStrokeEditPanel() {
        key_field.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent evt) {
                int keyCode = evt.getKeyCode();
                int modifiers = evt.getModifiers();
                KeyStroke ks = KeyStroke.getKeyStroke(keyCode, modifiers);
                switch (getKeyText(ks.getKeyCode())) {
                    case "BACK_SPACE":
                    case "DELETE":
                        evt.consume();
                        clearAction();
                        break;
                    case "ENTER":
                    case "TAB":
                        applyAction(key_field.getText().trim());
                        break;
                    default:
                        evt.consume();
                        String command = keyStroke2String(ks);
                        String useCommand = isCommandInUse(command);
                        if (useCommand != null) {
                            // Temorarily remove the focus listener
                            // so that it doesn't try to apply the action
                            // when the confirmation dialog pops up.
                            GenericAction genericAction = GenericActionHolder.getInstance().getGenericAction(useCommand);
                            String actionDisplayName = (genericAction == null) ? "???" : genericAction.getDisplay();
                            key_field.removeFocusListener(lois);
                            if (!ModalUtils.confirmPanel(KeyStrokeEditPanel.this,
                                    "This shortcut is currently in use; \n"
                                            + "reassigning this will remove the shortcut for '"
                                            + actionDisplayName + "'.\n"
                                            + "Do you want to proceed?", null, null, false)) {
                                key_field.setText(lastTimeFocusGained);
                                lastCommand = null;
                            } else { // cancelled
                                PreferenceUtils.getKeystrokesNode().put(useCommand, "");
                                key_field.setText(command);
                                applyAction(key_field.getText().trim());
                            }
                            key_field.addFocusListener(lois);
                            return;
                        }   key_field.setText(command);
                        lastCommand = command;
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent evt) {
                evt.consume();
            }

            @Override
            public void keyTyped(KeyEvent evt) {
                evt.consume();
            }
        });
        key_field.addFocusListener(lois);

        setEnabled(false);
    }

    private String isCommandInUse(String command) {

        for (String key : PreferenceUtils.getKeystrokesNodeNames()) {
            if (command.equalsIgnoreCase(PreferenceUtils.getKeystrokesNode().get(key, ""))) {
                return key;
            }
        }

        return null;
    }

    void setPreferenceKey(String key, String defaultValue) {
        this.key = key;
        if (PreferenceUtils.getKeystrokesNode() == null || key == null) {
            key_label.setText("Make a selection");
            key_field.setText("");
            setEnabled(false);
            return;
        } else {
            key_label.setText("Type a shortcut for \"" + this.key + "\":");
            String value = PreferenceUtils.getKeystrokesNode().get(key, defaultValue);
            key_field.setText(value);
            setEnabled(true);
        }
        boolean isToolbar = PreferenceUtils.getToolbarNode() != null && PreferenceUtils.getToolbarNode().getBoolean(key, false);
        toolbar_field.setSelected(isToolbar);
        key_field.getToolTipText();
    }

    @Override
    public void setEnabled(boolean b) {
        clear_button.setEnabled(b);
        key_field.setEnabled(b);
        toolbar_field.setEnabled(b);
    }

    private void applyAction(String str) {
        if (PreferenceUtils.getKeystrokesNode() == null || key == null) {
            //do nothing except disable clear button
            setEnabled(false);
            return;
        }

        KeyStroke ks = KeyStroke.getKeyStroke(str);

        if (str.length() > 0 && ks == null) {
            ErrorHandler.errorPanel("Unknown Key",
                    "Unknown key code: \"" + str + "\"");
            key_field.setText("");
            return;
        }
        if (str.length() > 0 && (isModifierKey(ks) || (str.contains("unknown")))) {
            ErrorHandler.errorPanel("Bad Keystroke",
                    "Illegal shortcut: \"" + str + "\"");
            key_field.setText("");
            return;
        }
        if (str.length() > 0 && (str.indexOf(' ') <= 0 || str.startsWith("shift "))) {
			// Checking that there is a modifier (ctrl, alt, or meta) present.
            // Without this restriction bad things can happen. For instance,
            // if the user wants to use "Z" too mean "zoom" in the SeqMapView,
            // then any time the letter "Z" is pressed in any input box,
            // a zoom will happen, and the user won't be able to type things
            // like "zebra genome".
            ErrorHandler.errorPanel("Bad Keystroke",
                    "Illegal shortcut: \"" + str + "\"\n"
                    + "Must contain a modifier key (ctrl, alt, ...)");
            key_field.setText("");
            return;
        }
        if (DEBUG) {
            System.out.println("Changing keystroke pref: "
                    + PreferenceUtils.getKeystrokesNode() + ": "
                    + this.key + "  -->  " + str);
        }
        lastCommand = null;
        PreferenceUtils.getKeystrokesNode().put(this.key, str);
		// The following seems to put the accelerator in the menu,
        // but the action does not seem to be invoked by the key stroke.
        Action a = GenericActionHolder.getInstance().getGenericAction(this.key);
        KeyStroke previousKeyStroke = null;
        if (null != a) {
            previousKeyStroke = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
            KeyStroke k = KeyStroke.getKeyStroke(str);
//			if (null != k) {
            a.putValue(Action.ACCELERATOR_KEY, k);
//			}
        }
		// Ah, the above handles things when the action is in a menu
        // for the window (JFrame) with the focus.
        // Here we add "orphan" actions that are not in a window's menu.
        // i.e. the ones in the popup or the tool bar.
        // Should we check and return if it's not "orphaned"?
        javax.swing.JFrame f = IGB.getSingleton().getFrame();
        javax.swing.JPanel p = (javax.swing.JPanel) f.getContentPane();
        InputMap im = p.getInputMap(WHEN_IN_FOCUSED_WINDOW);

        String previousKey = null;
        if (previousKeyStroke != null) {
            previousKey = (String) im.get(previousKeyStroke);
            im.remove(previousKeyStroke);
        }

        ActionMap am = p.getActionMap();
        if (previousKey != null) {
            am.remove(previousKey);
        }

        if (str.length() > 0) {
            im.put(ks, this.key);
            am.put(this.key, a);
        }
    }

    private void clearAction() {
        if (PreferenceUtils.getKeystrokesNode() == null || key == null) {
            // shouldn't happen
            ErrorHandler.errorPanel("ERROR", "No shortcut command selected");
            setEnabled(false);
            return;
        }
        key_field.setText("");
        lastCommand = "";
        toolbar_field.setSelected(false);
//		this.the_keystroke_node.put(this.the_key, "");
        //TO DO:  Fix cell update 
        //KeyStrokesView.getSingleton().model.fireTableCellUpdated(KeyStrokesView.getSingleton().table.getSelectedRow(), KeyStrokesView.KeyStrokeColumn);
    }

    /**
     * Returns true if the primary key code is control, or alt, etc.
     */
    private static boolean isModifierKey(KeyStroke key) {
        int key_code = key.getKeyCode();
        return ((key_code == KeyEvent.VK_ALT)
                || (key_code == KeyEvent.VK_ALT_GRAPH)
                || (key_code == KeyEvent.BUTTON1_DOWN_MASK)
                || (key_code == KeyEvent.BUTTON2_DOWN_MASK)
                || (key_code == KeyEvent.BUTTON3_DOWN_MASK)
                || (key_code == KeyEvent.VK_CONTROL)
                || (key_code == KeyEvent.VK_META)
                || (key_code == KeyEvent.VK_SHIFT));
    }

    /**
     * Convert a KeyStroke to a String in the same format used by
     * KeyStroke.getKeyStroke(String). Modified, originally from the Java
     * Developer's Almanac 1.4.
     * http://javaalmanac.com/egs/javax.swing/Key2Str.html
     */
    public static String keyStroke2String(KeyStroke key) {
        StringBuilder s = new StringBuilder(50);
        int m = key.getModifiers();

        if ((m & (InputEvent.CTRL_DOWN_MASK)) != 0) {
            s.append("ctrl ");
        }
        if ((m & (InputEvent.META_DOWN_MASK)) != 0) {
            s.append("meta ");
        }
        if ((m & (InputEvent.ALT_DOWN_MASK)) != 0) {
            s.append("alt ");
        }
        if ((m & (InputEvent.BUTTON1_DOWN_MASK)) != 0) {
            s.append("button1 ");
        }
        if ((m & (InputEvent.BUTTON2_DOWN_MASK)) != 0) {
            s.append("button2 ");
        }
        if ((m & (InputEvent.BUTTON3_DOWN_MASK)) != 0) {
            s.append("button3 ");
        }
        if ((m & (InputEvent.SHIFT_DOWN_MASK)) != 0) {
			// It is important that the shift key be appended after the others, so that
            // above I can easily check that there is at least one modifier other
            // than "shift" in the keystroke.
            s.append("shift ");
        }

        s.append(getKeyText(key.getKeyCode()));

        return s.toString();
    }

    /**
     * From the Java Developer's Almanac 1.4.
     * http://javaalmanac.com/egs/javax.swing/Key2Str.html
     */
    private static String getKeyText(int keyCode) {
        if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9
                || keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
            return String.valueOf((char) keyCode);
        }

        switch (keyCode) {
            case KeyEvent.VK_COMMA:
                return "COMMA";
            case KeyEvent.VK_PERIOD:
                return "PERIOD";
            case KeyEvent.VK_SLASH:
                return "SLASH";
            case KeyEvent.VK_SEMICOLON:
                return "SEMICOLON";
            case KeyEvent.VK_EQUALS:
                return "EQUALS";
            case KeyEvent.VK_OPEN_BRACKET:
                return "OPEN_BRACKET";
            case KeyEvent.VK_BACK_SLASH:
                return "BACK_SLASH";
            case KeyEvent.VK_CLOSE_BRACKET:
                return "CLOSE_BRACKET";

            case KeyEvent.VK_ENTER:
                return "ENTER";
            case KeyEvent.VK_BACK_SPACE:
                return "BACK_SPACE";
            case KeyEvent.VK_TAB:
                return "TAB";
            case KeyEvent.VK_CANCEL:
                return "CANCEL";
            case KeyEvent.VK_CLEAR:
                return "CLEAR";
            case KeyEvent.VK_SHIFT:
                return "SHIFT";
            case KeyEvent.VK_CONTROL:
                return "CONTROL";
            case KeyEvent.VK_ALT:
                return "ALT";
            case KeyEvent.VK_PAUSE:
                return "PAUSE";
            case KeyEvent.VK_CAPS_LOCK:
                return "CAPS_LOCK";
            case KeyEvent.VK_ESCAPE:
                return "ESCAPE";
            case KeyEvent.VK_SPACE:
                return "SPACE";
            case KeyEvent.VK_PAGE_UP:
                return "PAGE_UP";
            case KeyEvent.VK_PAGE_DOWN:
                return "PAGE_DOWN";
            case KeyEvent.VK_END:
                return "END";
            case KeyEvent.VK_HOME:
                return "HOME";
            case KeyEvent.VK_LEFT:
                return "LEFT";
            case KeyEvent.VK_UP:
                return "UP";
            case KeyEvent.VK_RIGHT:
                return "RIGHT";
            case KeyEvent.VK_DOWN:
                return "DOWN";

            // numpad numeric keys handled below
            case KeyEvent.VK_MULTIPLY:
                return "MULTIPLY";
            case KeyEvent.VK_ADD:
                return "ADD";
            case KeyEvent.VK_SEPARATOR:
                return "SEPARATOR";
            case KeyEvent.VK_SUBTRACT:
                return "SUBTRACT";
            case KeyEvent.VK_DECIMAL:
                return "DECIMAL";
            case KeyEvent.VK_DIVIDE:
                return "DIVIDE";
            case KeyEvent.VK_DELETE:
                return "DELETE";
            case KeyEvent.VK_NUM_LOCK:
                return "NUM_LOCK";
            case KeyEvent.VK_SCROLL_LOCK:
                return "SCROLL_LOCK";

            case KeyEvent.VK_F1:
                return "F1";
            case KeyEvent.VK_F2:
                return "F2";
            case KeyEvent.VK_F3:
                return "F3";
            case KeyEvent.VK_F4:
                return "F4";
            case KeyEvent.VK_F5:
                return "F5";
            case KeyEvent.VK_F6:
                return "F6";
            case KeyEvent.VK_F7:
                return "F7";
            case KeyEvent.VK_F8:
                return "F8";
            case KeyEvent.VK_F9:
                return "F9";
            case KeyEvent.VK_F10:
                return "F10";
            case KeyEvent.VK_F11:
                return "F11";
            case KeyEvent.VK_F12:
                return "F12";
            case KeyEvent.VK_F13:
                return "F13";
            case KeyEvent.VK_F14:
                return "F14";
            case KeyEvent.VK_F15:
                return "F15";
            case KeyEvent.VK_F16:
                return "F16";
            case KeyEvent.VK_F17:
                return "F17";
            case KeyEvent.VK_F18:
                return "F18";
            case KeyEvent.VK_F19:
                return "F19";
            case KeyEvent.VK_F20:
                return "F20";
            case KeyEvent.VK_F21:
                return "F21";
            case KeyEvent.VK_F22:
                return "F22";
            case KeyEvent.VK_F23:
                return "F23";
            case KeyEvent.VK_F24:
                return "F24";

            case KeyEvent.VK_PRINTSCREEN:
                return "PRINTSCREEN";
            case KeyEvent.VK_INSERT:
                return "INSERT";
            case KeyEvent.VK_HELP:
                return "HELP";
            case KeyEvent.VK_META:
                return "META";
            case KeyEvent.VK_BACK_QUOTE:
                return "BACK_QUOTE";
            case KeyEvent.VK_QUOTE:
                return "QUOTE";

            case KeyEvent.VK_KP_UP:
                return "KP_UP";
            case KeyEvent.VK_KP_DOWN:
                return "KP_DOWN";
            case KeyEvent.VK_KP_LEFT:
                return "KP_LEFT";
            case KeyEvent.VK_KP_RIGHT:
                return "KP_RIGHT";

            case KeyEvent.VK_DEAD_GRAVE:
                return "DEAD_GRAVE";
            case KeyEvent.VK_DEAD_ACUTE:
                return "DEAD_ACUTE";
            case KeyEvent.VK_DEAD_CIRCUMFLEX:
                return "DEAD_CIRCUMFLEX";
            case KeyEvent.VK_DEAD_TILDE:
                return "DEAD_TILDE";
            case KeyEvent.VK_DEAD_MACRON:
                return "DEAD_MACRON";
            case KeyEvent.VK_DEAD_BREVE:
                return "DEAD_BREVE";
            case KeyEvent.VK_DEAD_ABOVEDOT:
                return "DEAD_ABOVEDOT";
            case KeyEvent.VK_DEAD_DIAERESIS:
                return "DEAD_DIAERESIS";
            case KeyEvent.VK_DEAD_ABOVERING:
                return "DEAD_ABOVERING";
            case KeyEvent.VK_DEAD_DOUBLEACUTE:
                return "DEAD_DOUBLEACUTE";
            case KeyEvent.VK_DEAD_CARON:
                return "DEAD_CARON";
            case KeyEvent.VK_DEAD_CEDILLA:
                return "DEAD_CEDILLA";
            case KeyEvent.VK_DEAD_OGONEK:
                return "DEAD_OGONEK";
            case KeyEvent.VK_DEAD_IOTA:
                return "DEAD_IOTA";
            case KeyEvent.VK_DEAD_VOICED_SOUND:
                return "DEAD_VOICED_SOUND";
            case KeyEvent.VK_DEAD_SEMIVOICED_SOUND:
                return "DEAD_SEMIVOICED_SOUND";

            case KeyEvent.VK_AMPERSAND:
                return "AMPERSAND";
            case KeyEvent.VK_ASTERISK:
                return "ASTERISK";
            case KeyEvent.VK_QUOTEDBL:
                return "QUOTEDBL";
            case KeyEvent.VK_LESS:
                return "LESS";
            case KeyEvent.VK_GREATER:
                return "GREATER";
            case KeyEvent.VK_BRACELEFT:
                return "BRACELEFT";
            case KeyEvent.VK_BRACERIGHT:
                return "BRACERIGHT";
            case KeyEvent.VK_AT:
                return "AT";
            case KeyEvent.VK_COLON:
                return "COLON";
            case KeyEvent.VK_CIRCUMFLEX:
                return "CIRCUMFLEX";
            case KeyEvent.VK_DOLLAR:
                return "DOLLAR";
            case KeyEvent.VK_EURO_SIGN:
                return "EURO_SIGN";
            case KeyEvent.VK_EXCLAMATION_MARK:
                return "EXCLAMATION_MARK";
            case KeyEvent.VK_INVERTED_EXCLAMATION_MARK:
                return "INVERTED_EXCLAMATION_MARK";
            case KeyEvent.VK_LEFT_PARENTHESIS:
                return "LEFT_PARENTHESIS";
            case KeyEvent.VK_NUMBER_SIGN:
                return "NUMBER_SIGN";
            case KeyEvent.VK_MINUS:
                return "MINUS";
            case KeyEvent.VK_PLUS:
                return "PLUS";
            case KeyEvent.VK_RIGHT_PARENTHESIS:
                return "RIGHT_PARENTHESIS";
            case KeyEvent.VK_UNDERSCORE:
                return "UNDERSCORE";

            case KeyEvent.VK_FINAL:
                return "FINAL";
            case KeyEvent.VK_CONVERT:
                return "CONVERT";
            case KeyEvent.VK_NONCONVERT:
                return "NONCONVERT";
            case KeyEvent.VK_ACCEPT:
                return "ACCEPT";
            case KeyEvent.VK_MODECHANGE:
                return "MODECHANGE";
            case KeyEvent.VK_KANA:
                return "KANA";
            case KeyEvent.VK_KANJI:
                return "KANJI";
            case KeyEvent.VK_ALPHANUMERIC:
                return "ALPHANUMERIC";
            case KeyEvent.VK_KATAKANA:
                return "KATAKANA";
            case KeyEvent.VK_HIRAGANA:
                return "HIRAGANA";
            case KeyEvent.VK_FULL_WIDTH:
                return "FULL_WIDTH";
            case KeyEvent.VK_HALF_WIDTH:
                return "HALF_WIDTH";
            case KeyEvent.VK_ROMAN_CHARACTERS:
                return "ROMAN_CHARACTERS";
            case KeyEvent.VK_ALL_CANDIDATES:
                return "ALL_CANDIDATES";
            case KeyEvent.VK_PREVIOUS_CANDIDATE:
                return "PREVIOUS_CANDIDATE";
            case KeyEvent.VK_CODE_INPUT:
                return "CODE_INPUT";
            case KeyEvent.VK_JAPANESE_KATAKANA:
                return "JAPANESE_KATAKANA";
            case KeyEvent.VK_JAPANESE_HIRAGANA:
                return "JAPANESE_HIRAGANA";
            case KeyEvent.VK_JAPANESE_ROMAN:
                return "JAPANESE_ROMAN";
            case KeyEvent.VK_KANA_LOCK:
                return "KANA_LOCK";
            case KeyEvent.VK_INPUT_METHOD_ON_OFF:
                return "INPUT_METHOD_ON_OFF";

            case KeyEvent.VK_AGAIN:
                return "AGAIN";
            case KeyEvent.VK_UNDO:
                return "UNDO";
            case KeyEvent.VK_COPY:
                return "COPY";
            case KeyEvent.VK_PASTE:
                return "PASTE";
            case KeyEvent.VK_CUT:
                return "CUT";
            case KeyEvent.VK_FIND:
                return "FIND";
            case KeyEvent.VK_PROPS:
                return "PROPS";
            case KeyEvent.VK_STOP:
                return "STOP";

            case KeyEvent.VK_COMPOSE:
                return "COMPOSE";
            case KeyEvent.VK_ALT_GRAPH:
                return "ALT_GRAPH";
        }

        if (keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9) {
            char c = (char) (keyCode - KeyEvent.VK_NUMPAD0 + '0');
            return "NUMPAD" + c;
        }

        return "unknown(0x" + Integer.toString(keyCode, 16) + ")";
    }
}
