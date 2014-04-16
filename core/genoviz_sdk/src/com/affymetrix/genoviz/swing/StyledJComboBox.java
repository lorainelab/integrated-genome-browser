package com.affymetrix.genoviz.swing;

import java.awt.Component;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 *
 * @author nick
 */
public class StyledJComboBox extends JComboBox {

    private final String separator = ":"; //Use to separate actual value and display value

    public StyledJComboBox() {
        setRenderer(new ItemRenderer());
    }

    @SuppressWarnings("unchecked")
    public void initModel(String[] list) {
        ArrayList<Item> model = new ArrayList<Item>();
        String actualValue = "";
        String displayValue = "";

        for (String values : list) {
            actualValue = values.split(separator)[0];
            displayValue = values.split(separator)[1];
            model.add(new Item(actualValue, displayValue));
        }

        setModel(new DefaultComboBoxModel(model.toArray()));
    }

    public void addItem(String item) {
        String actualValue = item.split(separator)[0];
        String displayValue = item.split(separator)[1];

        this.addItem(new Item(actualValue, displayValue));
    }

    public void setSelectedItem(String value) {
        for (int i = 0; i < this.getItemCount(); i++) {
            if (this.getItemAt(i).toString().equals(value)) {
                this.setSelectedItem(this.getItemAt(i));
            };
        }
    }

    public String getSelectedValue() {
        return ((Item) this.getSelectedItem()).getActualValue();
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

        private String actualValue;
        private String displayValue;

        public Item(String actualValue, String displayValue) {
            this.actualValue = actualValue;
            this.displayValue = displayValue;
        }

        public String getActualValue() {
            return actualValue;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        public String toString() {
            return actualValue;
        }
    }
}
