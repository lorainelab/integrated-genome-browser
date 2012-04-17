package com.affymetrix.genoviz.swing;

import java.awt.Component;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 *
 * @author nick
 */
public class StyledJComboBox extends JComboBox {

	private final String separator = ":";

	public StyledJComboBox() {
		setRenderer(new ItemRenderer());
	}

	@SuppressWarnings("unchecked")
	public void initModel(String[] list) {
		Vector model = new Vector();
		String selectedValue = "", displayValue = "";

		for (String values : list) {
			selectedValue = values.split(separator)[0];
			displayValue = values.split(separator)[1];
			model.addElement(new Item(selectedValue, displayValue));
		}

		setModel(new DefaultComboBoxModel(model));
	}

	public void addItem(String item) {
		String selectedValue = item.split(separator)[0];
		String displayValue = item.split(separator)[1];

		this.addItem(new Item(selectedValue, displayValue));
	}

	public void setSelectedItem(String value) {
		for (int i = 0; i < this.getItemCount(); i++) {
			if (this.getItemAt(i).toString().equals(value)) {
				this.setSelectedItem(this.getItemAt(i));
			};
		}
	}

	public String getSelectedValue() {
		return ((Item) this.getSelectedItem()).getSelectedValue();
	}

	class ItemRenderer extends BasicComboBoxRenderer {

		public Component getListCellRendererComponent(
				JList list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index,
					isSelected, cellHasFocus);

			if (value != null) {
				Item item = (Item) value;
				setText(item.getDisplayValue());
			}

			return this;
		}
	}

	class Item {

		private String selectedValue;
		private String displayValue;

		public Item(String selectedValue, String displayValue) {
			this.selectedValue = selectedValue;
			this.displayValue = displayValue;
		}

		public String getSelectedValue() {
			return selectedValue;
		}

		public String getDisplayValue() {
			return displayValue;
		}

		public String toString() {
			return selectedValue;
		}
	}
}
