
package com.affymetrix.genoviz.swing;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author hiralv
 */
public class JTextButtonCellRenderer extends AbstractCellEditor implements 
		TableCellEditor, ActionListener, TableCellRenderer, MouseListener {
	public static final long serialVersionUID = 1l;
	private final JLabel field;
	private final JButton button;
	private final JPanel panel;
	private final JFrame frame;
	private String temp;
	
	public JTextButtonCellRenderer(final JFrame frame){
		super();
		panel = new JPanel();
		field = new JLabel();
		button = new JButton(" ... ");
		this.frame = frame;
		button.addActionListener(this);
		field.addMouseListener(this);
		panel.addMouseListener(this);
		
		field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
		

		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(field, c);

		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.LINE_END;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		panel.add(button, c);

		panel.setBackground(Color.WHITE);
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		field.setText(value.toString());
		
		return panel;
	}

	 //Implement the one CellEditor method that AbstractCellEditor doesn't.
    public Object getCellEditorValue() {
        return temp;
    }
	
	public void actionPerformed(ActionEvent e) {
		final JTextArea tfa = new JTextArea();
		final JButton copy = new JButton("Copy & Close");
		final JButton ok = new JButton("Ok");
		tfa.setEditable(false);
		final JDialog dialog = new JDialog(frame, "Value"){
			
			@Override
			public void dispose(){
				super.dispose();
				field.setText(tfa.getText());
				fireEditingStopped();
			}
			
		};
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		ok.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		
		copy.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringBuffer hackbuf = new StringBuffer(temp);
				String hackstr = new String(hackbuf);
				StringSelection data = new StringSelection(hackstr);
				clipboard.setContents(data, null);
				dialog.dispose();
			}
		});
		
		tfa.setColumns(12);
		tfa.setRows(6);
		tfa.setText(temp);
		
		Box box = Box.createHorizontalBox();
		box.add(Box.createGlue());
		box.add(copy);
		box.add(ok);
		dialog.getContentPane().add(tfa, "Center");
		dialog.getContentPane().add(box, "South");

		//dialog.setUndecorated(true);

		dialog.validate();
		dialog.pack();

		Point location = frame.getLocation();
		dialog.setLocation(location.x + frame.getWidth() / 2 - dialog.getWidth() / 2, location.y + frame.getHeight() / 2 - dialog.getHeight() / 2);

		dialog.setVisible(true);
	}

    //Implement the one method defined by TableCellEditor.
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
			                                     int column) {
		temp = value.toString();
        return panel;
    }

	public void mouseReleased(MouseEvent e) {
		fireEditingCanceled();
	}
	
	public void mouseClicked(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }

}
