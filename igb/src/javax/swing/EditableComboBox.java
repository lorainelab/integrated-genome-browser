package javax.swing;

import java.awt.Component;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class EditableComboBox extends JComboBox {

  public EditableComboBox() {
    reset();
  }

  public void reset() {
    setModel(new FilterableComboBoxModel(new ArrayList()));
    setEditor(new MyEditor());
    setEditable(true);
    removeAllItems();
  }

  public final class FilterableComboBoxModel extends AbstractListModel
    implements MutableComboBoxModel {

    private List items;
    private Filter filter;
    private List filteredItems;
    private Object selectedItem;

    public FilterableComboBoxModel(List items) {
      this.items = new ArrayList(items);
      filteredItems = new ArrayList(items.size());
      updateFilteredItems();
    }

    public void addElement( Object obj ) {
      items.add(obj);
      updateFilteredItems();
    }

    public void removeElement( Object obj ) {
      items.remove(obj);
      updateFilteredItems();
    }

    public void removeElementAt(int index) {
      items.remove(index);
      updateFilteredItems();
    }

    public void insertElementAt( Object obj, int index ) {}

    public void setFilter(Filter filter) {
      this.filter = filter;
      updateFilteredItems();
    }

    protected void updateFilteredItems() {
      fireIntervalRemoved(this, 0, filteredItems.size());
      filteredItems.clear();

      if (filter == null)
	filteredItems.addAll(items);
      else {
	for (Iterator iterator = items.iterator(); iterator.hasNext();) {
	  Object item = iterator.next();
	  if (filter.accept(item))
	    filteredItems.add(item);
	}
      }
      fireIntervalAdded(this, 0, filteredItems.size());
    }

    public int getSize() {
      return filteredItems.size();
    }

    public Object getElementAt(int index) {
      return filteredItems.get(index);
    }

    public Object getSelectedItem() {
      return selectedItem;
    }

    public void setSelectedItem(Object val) {
       if ((selectedItem == null) && (val == null))
	return;
       if ((selectedItem != null) && selectedItem.equals(val))
	return;
       if ((val != null) && val.equals(selectedItem))
	return;
       selectedItem = val;
      fireContentsChanged(this, -1, -1);
    }
  }

  public static interface Filter {
    public boolean accept(Object obj);
  }

  class StartsWithFilter implements Filter {
    private String prefix;
    public StartsWithFilter(String prefix) { this.prefix = prefix; }
    public boolean accept(Object o) {
        if (o == null)  { return false; }
        return o.toString().startsWith(prefix); }
  }

  public final class MyEditor implements ComboBoxEditor, DocumentListener {
    public JTextField text;
    private volatile boolean filtering = false;
    private volatile boolean setting = false;

    public MyEditor() {
      text = new JTextField(15);
      text.getDocument().addDocumentListener(this);
    }

    public Component getEditorComponent() { return text; }

    public void setItem(Object item) {
      if(filtering)
	return;

      setting = true;
      String newText = (item == null) ? "" : item.toString();
      text.setText(newText);
      setting = false;
    }

    public Object getItem() {
      return text.getText();
    }

    public void selectAll() { text.selectAll(); }

    public void addActionListener(ActionListener l) {
      text.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
      text.removeActionListener(l);
    }

    public void insertUpdate(DocumentEvent e) { handleChange(); }
    public void removeUpdate(DocumentEvent e) { handleChange(); }
    public void changedUpdate(DocumentEvent e) { }

    protected void handleChange() {
      //      System.out.println("in MyEditor.handleChange()");
      if (setting)
	return;
      filtering = true;
      Filter filter = null;
      if (text.getText().length() > 0)
	filter = new StartsWithFilter(text.getText());
      ((FilterableComboBoxModel) getModel()).setFilter(filter);
      // A bit nasty but it seems to get the popup validated properly
      setPopupVisible(false);
      setPopupVisible(true);
      filtering = false;
    }
  }
}
