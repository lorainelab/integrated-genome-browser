/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.Timer;

/**
 *
 * @author tarun
 */
public class SeqMapMouseListener extends MouseAdapter {

    private Timer timer;
    private final int TIMER_DURATION = 200;

    @Override
    public void mousePressed(MouseEvent e) {
        if (!(e.getSource() instanceof JButton)
                || ((JButton) e.getSource()).getAction() == null) {
            return;
        }

        timer = new Timer(TIMER_DURATION, ((JButton) e.getSource()).getAction());
        timer.start();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (timer != null) {
            timer.stop();
        }
    }
}
