package org.lorainelab.igb.plugin.manager;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.commonmark.node.Node;
import org.jmarkdownviewer.jmdviewer.parser.MarkdownParser;
import org.lorainelab.igb.plugin.manager.model.PluginListItemMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlPane extends JEditorPane {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlPane.class);
    Node document;

    public HtmlPane() {
        setEditable(false);
        createPane();
    }

    private void createPane() {
        HTMLEditorKit kit = new HTMLEditorKit();
        setEditorKit(kit);
        StyleSheet stylesheet = kit.getStyleSheet();
        try {
            Font robotoFont = Font.createFont(Font.TRUETYPE_FONT,
                    HtmlPane.class.getClassLoader().getResourceAsStream("Roboto-Regular.ttf")).deriveFont(16f);
            setFont(robotoFont);
        } catch (FontFormatException | IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        stylesheet.importStyleSheet(HtmlPane.class.getClassLoader().getResource("github.css"));
        Document doc = kit.createDefaultDocument();
        setDocument(doc);
        setText(DEFAULT_HTML);

    }

    public void load(PluginListItemMetadata selectedPlugin) {
        HTMLDocument doc = (HTMLDocument) getDocument();
        setDocument(doc);
        setText(getDescriptionHtml(selectedPlugin));
        setCaretPosition(0);
    }

    private String getDescriptionHtml(PluginListItemMetadata selectedPlugin) {
        if (selectedPlugin == null) {
            return DEFAULT_HTML;
        }
        return formatHtmlTemplate(selectedPlugin.getPluginName(), selectedPlugin.getVersion(), getDescriptionHtml(selectedPlugin.getDescription()));
    }
    private static final String DEFAULT_HTML = """
                                                <html>
                                                     <body>
                                                       
                                                     </body>
                                                   </html>
                                               """;

    private String getDescriptionHtml(String markdownText) {
        MarkdownParser parser = new MarkdownParser();
        parser.parse(markdownText);
        document = parser.getDocument();
        String html = parser.getHTML();
        return html;
    }

    public static String formatHtmlTemplate(String nameLine, String versionLine, String descriptionHtml) {
        String htmlTemplate = """
    <html>
      <body>
        <h1 style="margin-top: 0;">%s - %s</h1>  <!-- nameLine -->  <!-- versionLine -->
        <div style="margin-top: 15px;">
          %s  <!-- descriptionHtml -->
        </div>
      </body>
    </html>
    """;

        return String.format(htmlTemplate, nameLine, versionLine, descriptionHtml);
    }

}
