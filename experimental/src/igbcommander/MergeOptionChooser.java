package igbcommander;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;

public final class MergeOptionChooser extends JFileChooser implements ActionListener{
	private static final long serialVersionUID = 1L;

	private static final String SELECT_SPECIES = "Species";
	private static final String CHOOSE = "Choose";
	public final Box box, outerBox;
	public final JComboBox speciesCB = new JComboBox();
	public final JComboBox versionCB = new JComboBox();
	private JFrame frame = null;

	public MergeOptionChooser() {
		super();
		
		speciesCB.addActionListener(this);
		versionCB.addActionListener(this);
		
		box = new Box(BoxLayout.X_AXIS);
		box.setBorder(BorderFactory.createEmptyBorder(5, 5, 8, 5));

		box.add(new JLabel(CHOOSE + ":"));
		box.add(Box.createHorizontalStrut(5));
		box.add(speciesCB);

		box.add(Box.createHorizontalStrut(5));
		box.add(versionCB);
		
		//start max
		Box box0 = new Box(BoxLayout.X_AXIS);
		box0.setBorder(BorderFactory.createEmptyBorder(5, 5, 8, 5));
		
		//java.net.URL imgURL = com.affymetrix.igb.IGB.class.getResource("info_icon.gif");
		BufferedImage image = null;       
	    try {           
	        image = ImageIO.read(this.getClass().getResource("info_icon.gif"));         
	    } catch (IOException e) {           
	        e.printStackTrace();       
	    }     
		
		ImageIcon infoIcon = new ImageIcon(image);
		JLabel iconLabel = new JLabel(infoIcon);
		//iconLabel.setToolTipText("IGB's data sources already provide IGB with informatin about many species and their various genome assemblies, alsos called \"versions\". If youwant to open a local file containing annotations and other data for one of these genme versions, select it using the species and genome version chooser menus");
		iconLabel.addMouseListener(new IconListener());
		
		box0.add(Box.createHorizontalStrut(5));
		box0.add(new JLabel("Choose existing or enter new species and genome version"));
		box0.add(Box.createHorizontalStrut(5));
		box0.add(iconLabel);
		JLabel emptyText2 = new JLabel();
		emptyText2.setText("                                                       ");
		emptyText2.setFont(new Font("Serif", Font.PLAIN, 12));
		emptyText2.setForeground(Color.blue);
		box0.add(emptyText2);
		
		outerBox = new Box(BoxLayout.Y_AXIS);
		outerBox.add(box0);
		outerBox.add(box);
		//end max
	}

	@Override
	protected JDialog createDialog(Component parent) throws HeadlessException {
		JDialog dialog = super.createDialog(parent);

		refreshSpeciesList();
		dialog.getContentPane().add(outerBox, BorderLayout.SOUTH);
		return dialog;
	}

	public void refreshSpeciesList(){
		speciesCB.removeAllItems();
		
		List<String> speciesList = new ArrayList<String>();
		speciesList.add("Arabidopsis thaliana");
		speciesList.add("Bos taurus");
		
		for(String species : speciesList) {
			speciesCB.addItem(species);
		}
		speciesCB.setSelectedItem("Arabidopsis thaliana");

		List<String> versionList = new ArrayList<String>();
		versionList.add("A_thaliana_Jun_2009");
		versionList.add("A_thaliana_Apr_2008");
		versionList.add("A_thaliana_Jan_2004");
		
		for(String version : versionList) {
			versionCB.addItem(version);
		}
		versionCB.setSelectedItem("A_thaliana_Jun_2009");
	}

	public void actionPerformed(ActionEvent e) {
		if(e == null)
			return;
	}
	
	private class IconListener implements MouseListener {
		public void mouseClicked(MouseEvent e) {
			displayTip();
			/*
			JOptionPane.showMessageDialog(parent,
				    "IGB's data sources already provide IGB with information about many species\n " +
					"and their various genome assemblies, also called \"versions\". If you want\n " +
					"to open a local file containing annotations and other data for one of these\n " +
					"genme versions, select it using the species and genome version chooser menus\n\n" +
					"If you are working with something new that isn't listed, then type in the \n" +
					"species and genome version, select the file you want, and click \"Open\"");
			*/
		}
		
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		
		public void displayTip() {
			frame = new JFrame("Show Tip");

			JPanel content = new JPanel();
			content.setLayout(new BorderLayout());
			
			JTextArea tipArea = new JTextArea(20, 100);
			tipArea.setText("IGB's data sources already provide IGB with information about many species\n " +
					"and their various genome assemblies, also called \"versions\". If you want\n " +
					"to open a local file containing annotations and other data for one of these\n " +
					"genme versions, select it using the species and genome version chooser menus\n\n" +
					"If you are working with something new that isn't listed, then type in the \n" +
					"species and genome version, select the file you want, and click \"Open\"");
			content.add(tipArea, BorderLayout.CENTER);

			frame.setContentPane(content);
        
			frame.setSize(500,300);
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		}
	}
	
	public void closeFrame() {
		if(frame != null) {
			frame.setVisible(false);
			frame.dispose();
		}
	}
}
