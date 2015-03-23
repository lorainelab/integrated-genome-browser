package com.lorainelab.image.exporter;

import java.io.File;
import java.io.IOException;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import net.miginfocom.swing.MigLayout;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class JFreeSvgTest {

    private static JPanel createPanel() {
        JPanel panel = new JPanel(new MigLayout());
        panel.add(createLabel("West Panel"), "dock west");
        panel.add(createLabel("North 1 Panel"), "dock north");
        panel.add(createLabel("North 2 Panel"), "dock north");
        panel.add(createLabel("South Panel"), "dock south");
        panel.add(createLabel("East Panel"), "dock east");
        panel.add(createLabel("Center Panel"), "grow, push");
        return panel;
    }

    private static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 10, 5, 10)));
        return label;
    }

    @Test
    public void exportChartAsSVG() throws IOException, InterruptedException {
        JFrame frame = new JFrame("Example");
        frame.getContentPane().add(createPanel());
        frame.pack();
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setVisible(true);
        SVGGraphics2D g2 = new SVGGraphics2D(600, 300);
        frame.paint(g2);
        SVGUtils.writeToSVG(new File("target/lgp-test.svg"), g2.getSVGElement());
    }

}
