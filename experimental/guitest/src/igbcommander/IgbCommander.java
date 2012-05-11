package igbcommander;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

//import com.affymetrix.igb.menuitem.LoadFileAction;

public class IgbCommander extends JFrame {
	static IgbCommander ic = null;
	JButton execute, save, load, clear, clearHistory;
	JTextField inputField = new JTextField(45);
	JTextArea historyArea = new JTextArea(10, 50);
	JFileChooser fc;
	
	public static void main(final String[] args) {
		ic = new IgbCommander();
		ic.createUI();
	}
	
	private void createUI() {
		fc = new JFileChooser();
		ButtonHandler handler = new ButtonHandler();
		
		JPanel buttonContent = new JPanel();
		buttonContent.setLayout(new GridLayout(2,2));
		
		execute = new JButton("Execute Command");
		save = new JButton("Save to File");
		clearHistory = new JButton("Clear History           ");
		load = new JButton("Run a File");
		execute.addActionListener(handler);
		save.addActionListener(handler);
		clearHistory.addActionListener(handler);
		load.addActionListener(handler);
		
		buttonContent.add(execute);
		buttonContent.add(save);
		buttonContent.add(clearHistory);
		buttonContent.add(load);
				
		Box horizontalBox = Box.createHorizontalBox();
		JLabel inputLabel = new JLabel("   Enter Command: ");
		clear = new JButton("Clear");
		clear.addActionListener(handler);
		horizontalBox.add(inputLabel);
	    horizontalBox.add(inputField);
	    horizontalBox.add(clear);
		
	    Box historyContent = Box.createHorizontalBox();
		JLabel historyLabel = new JLabel("Command History: ");
		historyArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(historyArea,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		historyContent.add(historyLabel);
		historyContent.add(scrollPane);
		
		JPanel commandContent = new JPanel();
		commandContent.setLayout(new BorderLayout());
		commandContent.add(horizontalBox, BorderLayout.NORTH);
		commandContent.add(historyContent, BorderLayout.CENTER);

		JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        content.add(commandContent , BorderLayout.CENTER);
        content.add(buttonContent , BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(execute);
        //setIconImage(new ImageIcon("igb.gif").getImage());
        
        BufferedImage image = null;       
        try {           
        	image = ImageIO.read(this.getClass().getResource("igb.gif"));         
        } catch (IOException e) {           
        	e.printStackTrace();       
        }        
        this.setIconImage(image); 

        setTitle("IGB Commander");
        pack();
        setVisible(true);
	}
	
	private class ButtonHandler implements ActionListener {
		public void actionPerformed (ActionEvent e) {
			if (e.getSource() == execute) {
				String command = inputField.getText();
				if(command.trim().equals("")) {
					JOptionPane.showMessageDialog(ic, "The command cann't be empty!", "Empty box warning", JOptionPane.WARNING_MESSAGE);
				} else {
					executeOneCommand(command);
					historyArea.append(command + "\n");
					inputField.setText("");
				}
			} else if(e.getSource() == save) {
				if(historyArea.getText().trim().equals("")) {
					JOptionPane.showMessageDialog(ic, "The history cann't be empty to save a file!", "Empty history warning", JOptionPane.WARNING_MESSAGE);
				} else {
					int returnVal = fc.showSaveDialog(IgbCommander.this);
					if(returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						//System.out.println("Saving: " + file.getName());
						writeCommandFile(file);
	                
						historyArea.setText("");
					}
				}
			} else if(e.getSource() == load) {
				int returnVal = fc.showOpenDialog(IgbCommander.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fc.getSelectedFile();
	                
	                //System.out.println("Opening: " + file.getName());
	                executeCommandFromFile(file, historyArea);
	            }
			} else if(e.getSource() == clear) {
				inputField.setText("");
			} else if(e.getSource() == clearHistory) {
				historyArea.setText("");
			}
		}
	}
	
	private void executeOneCommand(String strLine) {
		PrintWriter out = null;
		try {
		    Socket sock = new Socket("localhost", 7085);
			out = new PrintWriter(sock.getOutputStream(), true); 
		    //System.out.println("-----line: " + strLine);
		    if(!strLine.trim().equals("")) {
		    	out.println(strLine);
		    	out.flush();
		    	Thread.currentThread().sleep(1000);
		    }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(out != null) out.close();
		}
	}
	
	private void executeCommandFromFile(File inFile, JTextArea historyArea) {
		BufferedReader br = null;
		PrintWriter out = null;
		
		try {
			String strLine = "";
		    br = new BufferedReader(new InputStreamReader(new FileInputStream(inFile)));

			Socket sock = new Socket("localhost", 7085);
			out = new PrintWriter(sock.getOutputStream(), true); 
			while ((strLine = br.readLine()) != null)   {
		    	//System.out.println("-----line: " + strLine);
		    	if(!strLine.trim().equals("")) {
		    		out.println(strLine);
		    		out.flush();
		    		Thread.currentThread().sleep(1000);
		    		historyArea.append(strLine + "\n");
		    	}
		    }
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(br != null) br.close();
				if(out != null) out.close();
			} catch(IOException ioe) {}
		}
	}
	
	private void writeCommandFile(File file) {
		DataOutputStream dos = null;
		try {
            dos = new DataOutputStream(new FileOutputStream(file));
            dos.writeBytes(historyArea.getText());
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(dos != null) dos.close();
			} catch(IOException ioe) {}
		}
	}
}
