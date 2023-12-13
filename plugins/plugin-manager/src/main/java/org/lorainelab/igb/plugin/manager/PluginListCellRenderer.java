package org.lorainelab.igb.plugin.manager;

import org.lorainelab.igb.plugin.manager.model.PluginListItemMetadata;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.BorderLayout;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class PluginListCellRenderer extends DefaultListCellRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(PluginListCellRenderer.class);

    @Override
    public java.awt.Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof PluginListItemMetadata) {
            PluginListItemMetadata metadata = (PluginListItemMetadata) value;
            label.setText(metadata.getPluginName()); // Assuming getPluginName() returns a String
            label.setHorizontalTextPosition(SwingConstants.LEFT);

            String uninstalledIcon = """
<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="black" viewBox="0 0 16 16">
      <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16"/>
    </svg>
                """;
            String installedIcon = """
<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="green" class="bi bi-circle-fill" viewBox="0 0 16 16">
  <circle cx="8" cy="8" r="8"/>
</svg>
                """;

            String iconString = uninstalledIcon;
            if (((PluginListItemMetadata) value).isInstalled()) {
                iconString = installedIcon;
            }

            try (ByteArrayInputStream svgStream = new ByteArrayInputStream(iconString.getBytes(StandardCharsets.UTF_8))) {
                FlatSVGIcon icon = new FlatSVGIcon(svgStream);
                label.setIcon(icon);
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
            }

            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setIconTextGap(10); 

            // Align icon to the right
            label.setLayout(new BorderLayout());
            JLabel iconLabel = new JLabel(label.getIcon());
            label.add(iconLabel, BorderLayout.EAST);
        }

        return label;
    }
}
