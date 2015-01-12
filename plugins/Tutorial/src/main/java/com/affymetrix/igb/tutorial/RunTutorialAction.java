package com.affymetrix.igb.tutorial;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.map.ObjectMapper;

public class RunTutorialAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final String TUTORIAL_EXT = "txt";
    private static final String SESSION_EXT = "xml";
    private static final Logger ourLogger
            = Logger.getLogger(RunTutorialAction.class.getPackage().getName());

    private final TutorialManager tutorialManager;
    private String name;
    private String uri;

    public RunTutorialAction(TutorialManager tutorialManager, String name, String uri) {
        super(name, null, null);
        this.tutorialManager = tutorialManager;
        this.name = name;
        this.uri = uri;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            //URL sessionUrl = new URL(uri + "." + SESSION_EXT);
            //PreferenceUtils.importPreferences(sessionUrl);
            //Validate Enabled Servers

            tutorialManager.loadState();

        } catch (Exception x) {
            // OK if session not loaded
        }
        try {
            URL tutorialUrl = new URL(uri + "." + TUTORIAL_EXT);
            try (BufferedReader rdr = new BufferedReader(new InputStreamReader(tutorialUrl.openStream()))) {
                if (rdr != null) {
                    TutorialStep[] tutorial = loadTutorial(rdr);
                    if (tutorial != null) {
                        tutorialManager.runTutorial(tutorial);
                    }
                }
            }
        } catch (Exception x) {
            ourLogger.log(Level.SEVERE, "Unable to load tutorial.", x);
            ErrorHandler.errorPanel("Tutorial Error", "Unable to load tutorial " + uri, Level.SEVERE);
        }
    }

    private TutorialStep[] loadTutorial(Reader reader) {
        TutorialStep[] tutorial = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            tutorial = mapper.readValue(reader, TutorialStep[].class);
        } catch (Exception x) {
            ourLogger.log(Level.SEVERE, "Unable to load tutorial.", x);
            ErrorHandler.errorPanel("Tutorial Error", "Unable to load tutorial " + uri, Level.SEVERE);
        }
        return tutorial;
    }
}
