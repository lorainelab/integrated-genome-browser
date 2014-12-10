/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.logging.console;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.JTextArea;

/**
 * A class to help send a PrintStream, such as System.out, to a JTextArea.
 * Adapted from code in the book "Swing Hacks" by Joshua Marinacci and Chris
 * Adamson. This is from hack #95. This sort of use of the code is allowed (even
 * without attribution). See the preface of their book for details.
 */
public class JTextAreaOutputStream extends OutputStream {

    JTextArea ta;
    PrintStream original;

    /**
     * Creates an OutputStream that writes to the given JTextArea.
     *
     * @param echo Can be null, or a PrintStream to which a copy of all output
     * will also by written. Thus you can send System.out to a text area and
     * also still send an echo to the original System.out.
     */
    public JTextAreaOutputStream(JTextArea t, PrintStream echo) {
        this.ta = t;
        this.original = echo;
    }

    public void write(int b) throws IOException {
        write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        ta.append(new String(b, off, len, Charsets.UTF_8));
        if (original != null) {
            original.write(b, off, len);
        }
    }
}
