package com.gene.tutorialhelper;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.igb.swing.JRPWidget;
import com.affymetrix.igb.swing.JRPWidgetDecorator;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JComponent;

@Component(name = WidgetIdTooltip.COMPONENT_NAME, immediate = true, provide = JRPWidgetDecorator.class)
public class WidgetIdTooltip implements JRPWidgetDecorator {

    public static final String COMPONENT_NAME = "WidgetIdTooltip";

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
