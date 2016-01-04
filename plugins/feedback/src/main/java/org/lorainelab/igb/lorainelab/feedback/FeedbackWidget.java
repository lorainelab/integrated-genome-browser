package org.lorainelab.igb.feedback;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 *
 * @author dcnorris
 */
public class FeedbackWidget extends JFrame {

    private JFXPanel panel;
    private MigLayout layout;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FeedbackWidget.class);

    public FeedbackWidget() {
        initializeFrame();
        initializeLayout();
        initComponents();
        this.add(panel);
    }

    private void initializeFrame() {
        setName("IGB Feedback");
        setTitle("IGB Feedback");
        setSize(820, 588);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    private void initializeLayout() {
        layout = new MigLayout("fill", "[grow 100,fill]", "[grow 100,fill]");
        setLayout(layout);
    }

    private void initComponents() {
        panel = new JFXPanel();

    }

    private void buildScene() {
        final WebView browser = new WebView();
        final WebEngine webEngine = browser.getEngine();
        // Layout logic
        VBox root = new VBox(5);
        root.getChildren().setAll(browser);
        root.setPrefSize(800, 400);
        VBox.setVgrow(browser, Priority.ALWAYS);
        webEngine.loadContent(getClassPathResourceAsString("bugReport.html"));
        webEngine.documentProperty().addListener((ObservableValue<? extends Document> prop, Document oldDoc, Document newDoc) -> {
            setupWebResources(webEngine);
        });
        Scene scene = new Scene(root);
        panel.setScene(scene);
        panel.setVisible(true);
    }

    public void showPanel() {

        Platform.runLater(() -> {
            buildScene();
            setVisible(true);
        });

    }

    public void hidePanel() {
        Platform.runLater(() -> {
            setVisible(false);
        });
    }

    public static void main(String[] args) {
        FeedbackWidget c = new FeedbackWidget();
        c.setVisible(true);
    }

    private void setupWebResources(final WebEngine engine) {
        JSObject jsobj = (JSObject) engine.executeScript("window");
        jsobj.setMember("closeTrigger", new CloseTrigger());
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
        engine.setOnVisibilityChanged((WebEvent<Boolean> event) -> {
            FeedbackWidget.this.hidePanel();
        });
        engine.executeScript(getClassPathResourceAsString("jiraCollectorDialog.js"));
        engine.executeScript(getClassPathResourceAsString("closeAction.js"));
    }

    public class CloseTrigger {

        public void hidePanel() {
            Platform.runLater(() -> {
                FeedbackWidget.this.setVisible(false);
            });
        }
    }

    public class JSLogger {

        public void log(String message) {
            Platform.runLater(() -> {
                logger.info(message);
            });
        }
    }

    private static String getClassPathResourceAsString(String resourcePath) {
        try {
            return CharStreams.toString(new InputStreamReader(FeedbackWidget.class.getClassLoader().getResourceAsStream(resourcePath)));
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return "";
    }

}
