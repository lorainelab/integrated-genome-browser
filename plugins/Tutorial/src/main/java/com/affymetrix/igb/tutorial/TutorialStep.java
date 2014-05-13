package com.affymetrix.igb.tutorial;

public class TutorialStep {

    public static class TutorialExecute {

        private String name;
        private float amount;
        private String param;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public float getAmount() {
            return amount;
        }

        public void setAmount(float amount) {
            this.amount = amount;
        }

        public String getParam() {
            return param;
        }

        public void setParam(String param) {
            this.param = param;
        }
    }
    private String text;
    private String[] highlight;
    private int timeout;
    private String trigger;
    private String waitFor;
    private TutorialStep.TutorialExecute execute;
    private String[] tab;
    private TutorialStep[] subTutorial;
    private String script;
    private String checkServer;

    public String getText() {
        return text;
    }

    public String[] getTab() {
        return tab;
    }

    public void setTab(String[] tab) {
        this.tab = tab;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getCheckServer() {
        return checkServer;
    }

    public void setCheckServer(String checkServer) {
        this.checkServer = checkServer;
    }

    public String[] getHighlight() {
        return highlight;
    }

    public void setHighlight(String[] highlight) {
        this.highlight = highlight;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getWaitFor() {
        return waitFor;
    }

    public void setWaitFor(String waitFor) {
        this.waitFor = waitFor;
    }

    public TutorialStep.TutorialExecute getExecute() {
        return execute;
    }

    public void setExecute(TutorialStep.TutorialExecute execute) {
        this.execute = execute;
    }

    public TutorialStep[] getSubTutorial() {
        return subTutorial;
    }

    public void setSubTutorial(TutorialStep[] subTutorial) {
        this.subTutorial = subTutorial;
    }
}
