package com.affymetrix.igb.action;

import static com.affymetrix.common.CommonUtils.APP_NAME;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.swing.JFrame;
import net.miginfocom.swing.MigLayout;
import netscape.javascript.JSObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Open a window showing information about Integrated Genome Browser.
 *
 * @author aloraine
 */
public class AboutIGBAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final String HTML_SOURCE = "about/AboutIGB.html";

    private static final String ABOUT_CSS = "about/about.css";
    private static final String BOOTSTRAP_CSS = "about/bootstrap.min.css";
    private static final String IGBL_LOGO = "about/igb.png";

    private static final Set<String> HTML_RESOURCES = Sets.newHashSet(
            ABOUT_CSS,
            BOOTSTRAP_CSS,
            IGBL_LOGO
    );

    private static final AboutIGBAction ACTION = new AboutIGBAction();
    private static final Logger logger = LoggerFactory.getLogger(AboutIGBAction.class);
    private static final String PERIOD = ".";
    private JFXPanel panel;
    private JFrame frame;

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static AboutIGBAction getAction() {
        return ACTION;
    }

    private AboutIGBAction() {
        super(MessageFormat.format(BUNDLE.getString("about"), APP_NAME), null,
                "16x16/actions/about_igb.png",
                "22x22/actions/about_igb.png",
                KeyEvent.VK_A, null, false);
        this.ordinal = 100;
        frame = new JFrame("About Integrated Genome Browser");
        MigLayout layout = new MigLayout("fill", "[grow 100,fill]", "[grow 100,fill]");
        frame.setLayout(layout);
        frame.setSize(new Dimension(555, 461));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        Platform.runLater(() -> {
            if (panel == null) {
                initJFXPanel();
            }
            frame.setVisible(true);
        });
    }

    private void initJFXPanel() {
        panel = new JFXPanel();
        buildScene(panel);
        frame.add(panel);
    }

    private void buildScene(JFXPanel panel) {
        try {
            final WebView browser = new WebView();
            final WebEngine webEngine = browser.getEngine();
            // Layout logic
            VBox root = new VBox(5);
            root.getChildren().setAll(browser);
            root.setPrefSize(800, 400);
            VBox.setVgrow(browser, Priority.ALWAYS);
            File tempDestinationDir = Files.createTempDir();
            tempDestinationDir.deleteOnExit();

            HTML_RESOURCES.forEach(source -> {
                try {
                    File sourceFile = new File(tempDestinationDir, source);
                    FileUtils.copyURLToFile(AboutIGBAction.class.getClassLoader().getResource(source), sourceFile);
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            });
            File htmlFile = new File(tempDestinationDir, HTML_SOURCE);
            String htmlString = getClassPathResourceAsString(HTML_SOURCE);
            String dataDir = PreferenceUtils.getAppDataDirectory();
            htmlString = htmlString.replaceFirst("\\$\\{dataDirectory\\}", dataDir);
            Files.write(htmlString, htmlFile, Charsets.UTF_8);

            webEngine.load(htmlFile.toURI().toURL().toExternalForm());
            webEngine.documentProperty().addListener((ObservableValue<? extends Document> prop, Document oldDoc, Document newDoc) -> {
                setupWebResources(webEngine, panel);
            });
            Scene scene = new Scene(root);
            panel.setScene(scene);
            panel.setVisible(true);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private static String getClassPathResourceAsString(String resourcePath) {
        try {
            return CharStreams.toString(new InputStreamReader(AboutIGBAction.class.getClassLoader().getResourceAsStream(resourcePath)));
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return "";
    }

    private void setupWebResources(final WebEngine engine, JFXPanel panel) {
        JSObject jsobj = (JSObject) engine.executeScript("window");
        jsobj.setMember("logger", new JSLogger());
        engine.setOnAlert((WebEvent<String> event) -> {
            Platform.runLater(() -> {
                Stage popup = new Stage();
                popup.initOwner(panel.getScene().getWindow());
                popup.initStyle(StageStyle.UTILITY);
                popup.initModality(Modality.WINDOW_MODAL);
                StackPane content = new StackPane();
                content.getChildren().setAll(
                        new Label(event.getData())
                );
                content.setPrefSize(200, 100);
                popup.setScene(new Scene(content));
                popup.showAndWait();
            });

        });
    }

    public class JSLogger {

        public void log(String message) {
            Platform.runLater(() -> {
                logger.info(message);
            });
        }
    }

}
