package com.affymetrix.igb.tutorial;

public class TutorialStep {
	public static class TutorialExecute {
		private String name;
		private float amount;
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
	}
	private String text;
	private String highlight;
	private int timeout;
	private String trigger;
	private String action;
	private TutorialExecute execute;
	private TutorialStep[] subTutorial;
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getHighlight() {
		return highlight;
	}
	public void setHighlight(String highlight) {
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
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public TutorialExecute getExecute() {
		return execute;
	}
	public void setExecute(TutorialExecute execute) {
		this.execute = execute;
	}
	public TutorialStep[] getSubTutorial() {
		return subTutorial;
	}
	public void setSubTutorial(TutorialStep[] subTutorial) {
		this.subTutorial = subTutorial;
	}
}
