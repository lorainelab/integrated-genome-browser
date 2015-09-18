package com.lorainelab.feedback;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.JFrame;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author dcnorris
 */
public class FeedbackWidget extends JFrame {

    private JFXPanel panel;
    private MigLayout layout;

    public FeedbackWidget() {
        initializeFrame();
        initializeLayout();
        initComponents();
        this.add(panel);
    }

    private void initializeFrame() {
        setName("IGB Feedback");
        setTitle("IGB Feedback");
        setSize(825, 700);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    private void initializeLayout() {
        layout = new MigLayout("fill", "[grow 100,fill]", "[grow 100,fill]");
        setLayout(layout);
    }

    private void initComponents() {
        panel = new JFXPanel();
        Platform.runLater(() -> {
            final WebView browser = new WebView();
            final WebEngine webEngine = browser.getEngine();
            // Layout logic
            VBox root = new VBox(5);
            root.getChildren().setAll(browser);
            root.setPrefSize(800, 400);
            VBox.setVgrow(browser, Priority.ALWAYS);
            try {
                webEngine.loadContent(CharStreams.toString(new InputStreamReader(FeedbackWidget.class.getClassLoader().getResourceAsStream("bugReport.html"))));
            } catch (IOException ex) {
                Logger.getLogger(FeedbackWidget.class.getName()).log(Level.SEVERE, null, ex);
            }
            Scene scene = new Scene(root);
            panel.setScene(scene);
            panel.setVisible(true);
        });

    }

    public void showPanel() {

        Platform.runLater(() -> {
            setVisible(true);
        });

    }

    public static void main(String[] args) {
        FeedbackWidget c = new FeedbackWidget();
        c.setVisible(true);
    }

}
