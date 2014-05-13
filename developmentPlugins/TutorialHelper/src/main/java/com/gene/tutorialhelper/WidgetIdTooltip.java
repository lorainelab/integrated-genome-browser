package com.gene.tutorialhelper;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;

import com.affymetrix.igb.swing.JRPWidget;
import com.affymetrix.igb.swing.JRPWidgetDecorator;

public class WidgetIdTooltip implements JRPWidgetDecorator {

    @Override
    public void widgetAdded(final JRPWidget widget) {
        processWidget(widget);
        ((JComponent) widget).addMouseListener(
                new MouseListener() {
                    @Override
                    public void mouseReleased(MouseEvent e) {
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        processWidget(widget);
                    }
                }
        );
    }

    private void processWidget(JRPWidget widget) {
        final JComponent comp = (JComponent) widget;
        if (!widget.getId().equals(comp.getToolTipText())) {
            comp.setToolTipText("id=" + widget.getId());
        }
    }
}
