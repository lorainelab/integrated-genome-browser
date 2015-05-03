package com.affymetrix.igb.tutorial;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionDoneCallback;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.event.GenericActionListener;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.LoadUtils;
import com.affymetrix.igb.shared.IGBScriptAction;
import com.affymetrix.igb.swing.JRPWidget;
import com.affymetrix.igb.swing.script.ScriptManager;
import com.affymetrix.igb.window.service.IWindowService;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.window.tabs.IgbTabPanel;
import furbelow.AbstractComponentDecorator;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public class TutorialManager implements GenericActionListener, GenericActionDoneCallback {

    private final TutorialNavigator tutorialNavigator;
    protected final IgbService igbService;
    private boolean tutorialDisplayed = false;
    private TutorialStep[] tutorial = null;
    private String waitFor = null;
    private boolean isRunning = false;
    private Map<String, TutorialStep[]> triggers = new HashMap<>();
    private Map<String, AbstractComponentDecorator> decoratorMap = new HashMap<>();
    private MenuListener menuListener = new MenuListener() {

        @Override
        public void menuSelected(MenuEvent e) {
            advanceStep();
            ((com.affymetrix.igb.swing.JRPMenu) e.getSource()).removeMenuListener(this);
        }

        @Override
        public void menuDeselected(MenuEvent e) {
        }

        @Override
        public void menuCanceled(MenuEvent e) {
        }
    };
    private ActionListener menuItemListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            advanceStep();
            ((com.affymetrix.igb.swing.JRPMenuItem) e.getSource()).removeActionListener(this);
        }
    };
    private int stepIndex = 0;

    public TutorialManager(IgbService igbService, IWindowService windowService) {
        super();
        addRecordPlayback(igbService);
        this.tutorialNavigator = new TutorialNavigator(new TutorialBackAction(this), new TutorialNextAction(this), new TutorialCancelAction(this));
        this.igbService = igbService;
        windowService.setTopComponent1(tutorialNavigator);
        tutorialNavigator.setVisible(false);
        tutorialDisplayed = false;
        TweeningZoomAction.getAction();
        VerticalStretchZoomAction.getAction();
        initListeners();
    }

    private void initListeners() {
        GenometryModel.getInstance().addGroupSelectionListener(evt -> {
            GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
            String species = "";
            if (genomeVersion != null && genomeVersion.getSpeciesName() != null) {
                species = "." + genomeVersion.getSpeciesName();
            }
            String version = "";
            if (genomeVersion != null && genomeVersion.getName() != null) {
                version = "." + genomeVersion.getName();
            }
            doWaitFor("groupSelectionChanged" + species + version);
        });

        igbService.addSpeciesItemListener(ie -> {
            if (ie.getItem() == null || ie.getStateChange() == ItemEvent.DESELECTED) {
                return;
            }

            String species = "";
            species = "." + ie.getItem().toString();
            doWaitFor("speciesSelectionChanged" + species);
        });
        igbService.addPartialResiduesActionListener(ae -> doWaitFor("LoadResidueAction"));
    }

    public void setTutorialDisplayed(boolean tutorialDisplayed) {
        this.tutorialDisplayed = tutorialDisplayed;
    }

    private JComponent getWidget(String widgetId) {
        int pos = widgetId.indexOf('.');
        if (pos == -1) {
            return (JComponent) ScriptManager.getInstance().getWidget(widgetId);
        } else {
            String mainWidgetId = widgetId.substring(0, pos);
            return (JComponent) ScriptManager.getInstance().getWidget(mainWidgetId);
        }
    }

    private com.affymetrix.igb.swing.SubRegionFinder getSubRegionFinder(String widgetId) {
        JComponent mainWidget = getWidget(widgetId);
        if (mainWidget == null) {
            return null;
        }
        int pos = widgetId.indexOf('.');
        if (pos == -1) {
            return null;
        }
        if (!(mainWidget instanceof com.affymetrix.igb.swing.JRPHierarchicalWidget)) {
            ErrorHandler.errorPanel("Tutorial Error", "error in tutorial, widget " + widgetId + " is incorrect, not hierarchical.", Level.WARNING);
            return null;
        }
        String subId = widgetId.substring(pos + 1);
        return ((com.affymetrix.igb.swing.JRPHierarchicalWidget) mainWidget).getSubRegionFinder(subId);
    }

    private boolean highlightWidget(String[] widgetId) {
        for (String s : widgetId) {
            JComponent widget = getWidget(s);
            if (widget == null) {
                return false;
            }
            com.affymetrix.igb.swing.SubRegionFinder subRegionFinder = getSubRegionFinder(s);
            Marquee m = new Marquee(widget, subRegionFinder);
            decoratorMap.put(s, m);
        }
//		saveBackgroundColor = widget.getBackground();
//		widget.setBackground(HIGHLIGHT_COLOR);
//		widget.requestFocusInWindow();
        return true;
    }

    private void unhighlightWidget(String[] widgetId) {
        for (String s : widgetId) {
            AbstractComponentDecorator decorator = decoratorMap.get(s);
            if (decorator != null) {
                decorator.setVisible(false);
                decorator.dispose();

                decoratorMap.remove(s);
            }
        }
//		JComponent widget = getWidget(widgetId);
//		if (widget != null) {
//			widget.setBackground(saveBackgroundColor);
//		}
    }

    private boolean runTutorialStep(TutorialStep step) {
        if (!tutorialDisplayed) {
            tutorialNavigator.setVisible(true);
            tutorialDisplayed = true;
        }
        if (step.getText() == null) {
            tutorialNavigator.getInstructions().setText("");
        } else {
            tutorialNavigator.getInstructions().setText(step.getText());
        }
        if (step.getScript() != null) {
            IGBScriptAction.executeScriptAction(step.getScript());
        }
        if (step.getCheckServer() != null) {
            if (!checkServer(step.getCheckServer())) {
                stop();
            }
        }
        if (step.getTab() != null) {
            setTab(step.getTab());
        }
        if (step.getHighlight() != null) {
            if (!highlightWidget(step.getHighlight())) {
                ErrorHandler.errorPanel("Tutorial Error", "error in tutorial, unable to find widget " + Arrays.toString(step.getHighlight()), Level.SEVERE);
            }
        }
        if (step.getExecute() != null) {
            GenericAction action = GenericActionHolder.getInstance().getGenericAction(step.getExecute().getName());
            if (action instanceof IAmount) {
                ((IAmount) action).setAmount(step.getExecute().getAmount());
            }
            action.addDoneCallback(this);
            action.actionPerformed(null);
        }
        if (step.getTrigger() != null) {
            if (step.getSubTutorial() == null) {
                ErrorHandler.errorPanel("Tutorial Error", "error in tutorial, no sub tutorial for trigger " + step.getTrigger(), Level.WARNING);
            } else {
                triggers.put(step.getTrigger(), step.getSubTutorial());
            }
        } else if (step.getSubTutorial() != null) {
            runTutorial(step.getSubTutorial());
        } else if (step.getTimeout() > 0) {
            try {
                Thread.sleep(step.getTimeout() * 1000);
            } catch (InterruptedException x) {
            }
        } else if (step.getWaitFor() == null) {
            waitFor = TutorialNextAction.class.getName(); // default
//			highlightWidget("TutorialNavigator_next");
            return false;
        } else {
            String waitForItem = step.getWaitFor();
            JRPWidget widget = ScriptManager.getInstance().getWidget(waitForItem);
            if (widget instanceof com.affymetrix.igb.swing.JRPMenu) {
                ((com.affymetrix.igb.swing.JRPMenu) widget).addMenuListener(menuListener);
            } else if (widget instanceof com.affymetrix.igb.swing.JRPMenuItem) {
                ((com.affymetrix.igb.swing.JRPMenuItem) widget).addActionListener(menuItemListener);
            } else {
                waitFor = waitForItem;
            }
            return false;
        }
        return true;
    }

    public void runTutorial(TutorialStep[] tutorial) {
        if (isRunning) {
            stop();
        }
        isRunning = true;
        waitFor = null;
        stepIndex = 0;
        this.tutorial = tutorial;
        nextStep();

    }

    private void nextStep() {
        if (stepIndex >= tutorial.length) {
            tutorialDone();
        } else {
            SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() throws Exception {
                    boolean finished = runTutorialStep(tutorial[stepIndex]);
                    if (finished) {
                        advanceStep();
                    }
                    return null;
                }
            };
            sw.execute();
        }
    }

    private void advanceStep() {
        TutorialStep step = tutorial[stepIndex];
        if (step.getHighlight() != null) {
            unhighlightWidget(step.getHighlight());
        }

        waitFor = null;
        stepIndex++;
        nextStep();
    }

    public void tutorialDone() {
        waitFor = null;
        stepIndex = 0;
        tutorial = null;
        tutorialNavigator.setVisible(false);
        tutorialDisplayed = false;
        triggers.clear();
        isRunning = false;
    }

    public void back() {
    }

    public void next() {
    }

    public void stop() {
        TutorialStep step = tutorial[stepIndex];
        if (step.getHighlight() != null) {
            unhighlightWidget(step.getHighlight());
        }
        tutorialDone();
    }

    private void addRecordPlayback(IgbService igbService) {
    }

    public void addJComponent(String id, JComponent comp) {
        ScriptManager.getInstance().addWidget(new com.affymetrix.igb.swing.JRPWrapper(id, comp));
    }

    public void removeJComponent(String id) {
        ScriptManager.getInstance().removeWidget(id);
    }

    @Override
    public void onCreateGenericAction(GenericAction genericAction) {
    }

    @Override
    public void notifyGenericAction(GenericAction genericAction) {
        String id = genericAction.getId();
        if (genericAction.getExtraInfo() != null) {
            id += "." + genericAction.getExtraInfo();
        }
        doWaitFor(id);
    }

    private void doWaitFor(String id) {
        if (id.equals(waitFor)) {
            advanceStep();
        } else {
            TutorialStep[] tutorial = triggers.get(id);
            if (tutorial != null) {
                runTutorial(tutorial);
            }
        }
    }

    @Override
    public void actionDone(GenericAction action) {
        advanceStep();
        action.removeDoneCallback(this);
    }

    public void loadState() {
        igbService.loadState();
    }

    private void setTab(String[] tabs) {
        for (String tab : tabs) {
            IgbTabPanel panel = igbService.getTabPanelFromDisplayName(tab);
            igbService.selectTab(panel);
        }
    }

    //Manual check to ensure IGB Quickload is Enabled
    private boolean checkServer(String serverName) {
        Set<DataProvider> enabledServers = igbService.getEnabledServerList();
        for (DataProvider server : enabledServers) {
            if (server.getName().equalsIgnoreCase(serverName)) {
                return true;
            }
        }
        //if it is not enabled offer option to enable or cancel tutorial
        Container frame = igbService.getApplicationFrame();
        String message = "IGB Quickload is required for this tutorial, but it is not enabled. Would you like to enable it and continue?";
        Object[] params = {message};
        int yes = JOptionPane.showConfirmDialog(frame, params, "Continue?", JOptionPane.YES_NO_OPTION);
        if (yes == JOptionPane.YES_OPTION) {
            for (DataProvider server : igbService.getAllServersList()) {
                if (server.getName().equalsIgnoreCase(serverName)) {
                    server.setStatus(LoadUtils.ResourceStatus.NotInitialized);
                    return true;
                }
            }
            ErrorHandler.errorPanel("Tutorial Error", "error in tutorial, could not find " + serverName + " in serves.", Level.SEVERE);
        }
        return false;
    }
}
