package com.affymetrix.igb.view.ucsc;




import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URL;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;


/**
 * UCSC View Component. 
 * Shows the UCSC plot for the current region.
 * 
 * 
 * 
 * @author Ido M. Tamir
 */
public final class UCSCView extends JComponent {
        private static final String UCSCSETTINGSNODE = "ucscSettings";
        private static final String UCSCUSERID = "hguid";
        private SwingWorker<Image, Void> worker = null;

		private String userId = "";

		private final SeqMapView gviewer = Application.getSingleton().getMapView();
		private final JButton update_button = new JButton("update");
        private final JButton ucscSettingsButton = new JButton("UCSC settings");
        private final UCSCImage ucscImage = new UCSCImage();
        private final JScrollPane scroll = new JScrollPane();
		
        /**
         * Panel for UCSC Settings: hguid selection 
         * Shows the UCSC plot for the current region.
         *
         **/ 
        public class UCSCHelper extends JDialog {

            private final JButton okButton = new JButton("submit");
            private final JButton ucscInfo = new JButton("UCSC info");
            private final JTextField userIdField = new JTextField(userId,15);
            
            
            public UCSCHelper(Window window, String string) {
                super(window, string);
                CookieHandler.setDefault(null);
    
                this.setLayout(new BorderLayout());
                final JTextPane pane = new JTextPane();
                pane.setContentType("text/html");
               
                String text = "<h1>Setting the UCSC user id</h1><p>Using the UCSC user id you can customize the UCSC Viewer settings with your browser.</p>";
                text += "<ol><li><p>Obtain your user id by clicking on the \"UCSC info\" button.</p><p>Or open <a href=\"http://genome.ucsc.edu/cgi-bin/cartDump\">http://genome.ucsc.edu/cgi-bin/cartDump</a> in your browser</p></li>";
                text += "<li>Then scroll down in the opened window and copy the value of hguid into the \"UCSC user id\" field.</li>";
                text += "<li>Click the submit button.</li>";
				text += "<li>Your IGB UCSC View is now synchronized with you browser track configuration.</br>";
				text += "The settings in your browser now change the view.</li></ol>";
                pane.setText(text);                
                pane.setEditable(false);
                final JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
                panel.add(ucscInfo);
                panel.add(Box.createHorizontalGlue());
                panel.add(Box.createHorizontalStrut(5));
                panel.add(new JLabel("UCSC user id (hguid):"));
                panel.add(Box.createHorizontalStrut(5));
                panel.add(userIdField);
                panel.add(Box.createHorizontalStrut(5));
                panel.add(Box.createHorizontalGlue());
                panel.add(okButton);

                okButton.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e) {
                        userId = userIdField.getText();
                        Preferences ucscSettingsNode = UnibrowPrefsUtil.getTopNode().node(UCSCSETTINGSNODE);
                        ucscSettingsNode.put(UCSCUSERID, userId);
                        dispose();
                    }
       
                });
                okButton.setToolTipText("Set your UCSC id for the session");
                ucscInfo.addActionListener(new ActionListener(){

                    public void actionPerformed(ActionEvent e) {
                        GeneralUtils.browse("http://genome.ucsc.edu/cgi-bin/cartDump");
                    }
                    
                });
                ucscInfo.setToolTipText("<html>Opens the browser with the UCSC user info from cookie.</br>Type the number at the bottom of the screen</br>where it says \"hguid=...\" into the text box and click submit.</html>");
                getContentPane().add("Center", pane);
                getContentPane().add("South", panel);
            }

        }

        
        
        
	public UCSCView() {
		        super();
                final Preferences ucscSettingsNode = UnibrowPrefsUtil.getTopNode().node(UCSCSETTINGSNODE);
                userId = ucscSettingsNode.get(UCSCUSERID, "");
                
        	    this.setLayout(new BorderLayout());
                final JPanel buttonPanel = new JPanel();
                buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
                
                scroll.setViewportView(ucscImage);
                scroll.getVerticalScrollBar().setEnabled(true);
                scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                this.add("Center", scroll);
                this.add("South", buttonPanel);
                    
                buttonPanel.add(ucscSettingsButton);
                buttonPanel.add(Box.createHorizontalGlue());
                buttonPanel.add(update_button);

				update_button.setToolTipText("update the view");
                update_button.addActionListener(new ActionListener(){

                    public void actionPerformed(ActionEvent e) {
						if(worker != null){
							worker.cancel(true);
						}
						final String msg = "Updating UCSC View...";
						Application.getSingleton().addNotLockedUpMsg(msg);
						final int pixWidth = scroll.getViewport().getWidth();
						worker = new SwingWorker<Image, Void>() {
							@Override
							public Image doInBackground() {
								return getImage(pixWidth);
							}

							@Override
							public void done() {
								try {
									Application.getSingleton().removeNotLockedUpMsg(msg);
									Application.getSingleton().setStatus("",false);
									Image image = get();
									ucscImage.setImage(image);
								}
								catch (InterruptedException ignore) {}
							    catch (CancellationException ignore){}
								catch (java.util.concurrent.ExecutionException e) {
									String why = null;
									Throwable cause = e.getCause();
									if (cause != null) {
										why = cause.getMessage();
									} else {
										why = e.getMessage();
									}
									Logger.getLogger(UCSCView.class.getName()).log(Level.FINE, why);
								}
							}
						};
						worker.execute();
                    }
                    
                });

				ucscSettingsButton.setToolTipText("personalize view");
                ucscSettingsButton.addActionListener(new ActionListener() {

                     public void actionPerformed(ActionEvent e) {
                            final Window window = SwingUtilities.getWindowAncestor(UCSCView.this);
                            final UCSCHelper helper = new UCSCHelper(window, "UCSC user id");

                            helper.setSize(500, 400);
                            helper.setModalityType(ModalityType.DOCUMENT_MODAL);

                            helper.setVisible(true);
                    }
                });
	    }

        
	public Image getImage(int pixWidth){
		String url = getUrlForView(pixWidth);
		if(url.startsWith("http")){
			final UCSCLoader loader = new UCSCLoader();
			url = loader.getImageUrl(url, userId);
			if(url.startsWith("http")){
				try {
					return ImageIO.read(new URL(url));
				}
				catch (IOException e) {
					Logger.getLogger(UCSCView.class.getName()).log(Level.FINE, "url was : " + url, e);
				}	
			}
		}
		return createErrorImage(url, pixWidth);
        }

		
		public Image createErrorImage(String error, int pixWidth){
			final BufferedImage image = new BufferedImage(pixWidth, 70, BufferedImage.TYPE_3BYTE_BGR);
		    image.createGraphics();
            final Graphics2D g = (Graphics2D)image.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, pixWidth, 70);
			final Font font = new Font("Serif", Font.PLAIN, 12);
			g.setFont(font);
			g.setColor(Color.BLACK);
			g.setFont(font);
			g.drawString(error, 30,20);
			return image;
		}


        public String getUrlForView(int pixWidth){
            final String query = gviewer.getUCSCQuery();
            if(query.startsWith("db")) {
                 String width = "pix=" + pixWidth + "&";
                 return "http://genome.ucsc.edu/cgi-bin/hgTracks?" + width + query;
            }else{
				return query;
			}
        }
        


  }

