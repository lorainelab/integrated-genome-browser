/**
 * Main class. This builds the GUI components and establishes their
 * behavior.
 */
package org.bioviz.protannot;

import org.freehep.util.export.ExportDialog;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.util.ComponentPagePrinter;
import com.affymetrix.genoviz.swing.ColorTableCellEditor;
import com.affymetrix.genoviz.swing.ColorTableCellRenderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * @see     com.affymetrix.genometryImpl.BioSeq
 * @see     com.affymetrix.genoviz.util.ComponentPagePrinter
 */

final class ProtAnnotMain implements WindowListener {
    // where the application is first invoked
    private static String user_dir = System.getProperty("user.dir");
    // used for choosing new files to load
    private JFileChooser chooser = null;
    // for printing
    private ComponentPagePrinter print_panel = null;
    // for choosing sample file from server
    private JFrame sampleChooser = null;
    // for choosing colors
    private JFrame colorChooser = null;
    // the JFrame containing all the widgets
    private JFrame frm;
    // has NeoMaps and PropertySheet (JTable)
    private GenomeView gview;
    // the genome_seq we're looking at
    private BioSeq genome_seq;
    // is populated from prefs_file
    private Hashtable<String,Color> prefs_hash;
    // for storing user prefrences
    private Preferences prefs;
    // width of the user's screen
    private Dimension screen;
    private int frm_width;// = (int) (screen.width * .8f);
    private int frm_height;// = (int) (screen.height * .8f);
    private static final boolean testmode = false;
    private enum Arguments {
        SERVER,
        FILENAME;

        public static Arguments getValue(String s)
        {
           if(s.equalsIgnoreCase("-s"))
               return SERVER;
           else if(s.equalsIgnoreCase("-f"))
               return FILENAME;
           
           return null;
        }

    };
    private final Hashtable<Arguments,String> ArgumentValues = new Hashtable<Arguments,String>();
    
    public static void main(String[] args) {
        ProtAnnotMain test = new ProtAnnotMain();
        test.parseArguments(args);
        test.loadPrefs();
        test.start(args);
    }

    /**
     * Unloads everything from GnomeView if unable to read the selected file.
     */
    private void no_data() {
        frm.setTitle(" ProtAnnot");
        gview.setTitle("");
        gview.no_data();
    }

    /**
     * Loads prefrences from the a file.
     * @param   prefs_name  File name containing prefrences.
     * @return              Returns a hashtable with name as key and Color as a value.
     */
    private Hashtable<String,Color> loadPrefs() {
        Hashtable<String,Color> phash = new Hashtable<String,Color>();

        prefs = Preferences.userNodeForPackage(ProtAnnotMain.class);

        try
        {
            phash.put(GenomeView.COLORS.BACKGROUND.toString(), new Color(prefs.getInt(GenomeView.COLORS.BACKGROUND.toString(), GenomeView.COLORS.BACKGROUND.getRGB())));
            phash.put(GenomeView.COLORS.FRAME0.toString(), new Color(prefs.getInt(GenomeView.COLORS.FRAME0.toString(), GenomeView.COLORS.FRAME0.getRGB())));
            phash.put(GenomeView.COLORS.FRAME1.toString(), new Color(prefs.getInt(GenomeView.COLORS.FRAME1.toString(), GenomeView.COLORS.FRAME1.getRGB())));
            phash.put(GenomeView.COLORS.FRAME2.toString(), new Color(prefs.getInt(GenomeView.COLORS.FRAME2.toString(), GenomeView.COLORS.FRAME2.getRGB())));
            phash.put(GenomeView.COLORS.TRANSCRIPT.toString(), new Color(prefs.getInt(GenomeView.COLORS.TRANSCRIPT.toString(), GenomeView.COLORS.TRANSCRIPT.getRGB())));
            phash.put(GenomeView.COLORS.DOMAIN.toString(), new Color(prefs.getInt(GenomeView.COLORS.DOMAIN.toString(), GenomeView.COLORS.DOMAIN.getRGB())));
            phash.put(GenomeView.COLORS.EXONSUMMARY.toString(), new Color(prefs.getInt(GenomeView.COLORS.EXONSUMMARY.toString(), GenomeView.COLORS.EXONSUMMARY.getRGB())));
            updatePrefs(phash);
        } catch (Exception ex) {
            Logger.getLogger(ProtAnnotMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        prefs_hash = phash;
        return prefs_hash;
    }

    /**
     * Loads the file selected in the file browser.
     * @param   seqfile - Name of file to be loaded
     */
    private void load(File seqfile) {

        FileInputStream fistr = null;
        try {
            fistr = new FileInputStream(seqfile);
            load(fistr,seqfile.getName());
        }catch(Exception e)
        {
            Reporter.report("Couldn't read file: " + e.getMessage(), e, false, false, true);
        } finally
        {
            GeneralUtils.safeClose(fistr);
        }
    }

    private void load(String file)
    {
        if(file.startsWith("/"))
                load(new File(file));
        else
        {
            URLConnection conn = null;
            try
            {
                String path = getArgumentValue(Arguments.SERVER) + file;
                URL url = new URL(path);
                conn = url.openConnection();
                conn.setConnectTimeout(1000 * 10);
                conn.setReadTimeout(1000 * 10);
                load(conn.getInputStream(),url.toString());
            }catch(Exception e)
            {
                Reporter.report("Couldn't read file: " + e.getMessage(), e, false, false, true);
            }
        }

    }

    private void load(InputStream fistr, String filename)
    {
        BufferedInputStream bistr = null;
        try
        {
            bistr = new BufferedInputStream(fistr);
            Xml2GenometryParser parser = new Xml2GenometryParser();
            genome_seq = parser.parse(bistr);
            gview.setTitle("viewing file: " + filename);
            gview.setBioSeq(genome_seq,true);
            frm.setTitle(" ProtAnnot: " + filename);
        } catch (Exception ex) {
            Reporter.report("Couldn't read file: " + filename,
                    ex, false, false, true);
            no_data();
        } finally
        {
            GeneralUtils.safeClose(bistr);
        }
    }

    /**
     * Setup the outer frame.
     * @param   args    - optional file name as a parameter.
     */
    private void start(String[] args) {
        frm = new JFrame(" ProtAnnot");
        screen = frm.getToolkit().getScreenSize();
        frm_width = (int) (screen.width * .8f);
        frm_height = (int) (screen.height * .8f);
        frm.setSize(frm_width, frm_height);
        frm.setLocation((int) (screen.width * .1f), (int) (screen.height * .05f));
        setUpPanels();
        setUpMenus();
        setupColorChooser();
        if(getArgumentValue(Arguments.SERVER)!=null)
            setupSamplesFromServer();
       
        frm.addWindowListener(this);
        frm.setVisible(true);
        if(getArgumentValue(Arguments.FILENAME)!=null)
            load(getArgumentValue(Arguments.FILENAME));
    }

    /**
     * One of part in setting up the outer frame. Sets up panels.
     */
    private void setUpPanels() {
        Container cpane = frm.getContentPane();
        cpane.setLayout(new BorderLayout());
        gview = new GenomeView(prefs_hash);
        cpane.add("Center", gview);
        print_panel = new ComponentPagePrinter(gview);
    }

    /**
     * One of part in setting up the outer frame. Sets up menus.
     */
    private void setUpMenus() {
        JMenuBar mbar = new JMenuBar();
        JMenu menu = new JMenu("File");
        addFileActions(menu);
        mbar.add(menu);
        menu = new JMenu("View");
        addViewActions(menu);
        mbar.add(menu);
        frm.setJMenuBar(mbar);
        if(testmode)
        {
            addQuickLaunch(mbar);
        }
    }

    /**
     * Action perfomed when a file is seleced in the file browser. Calls up load(name) to load the file.
     */
    private void doLoadFile() {
        if (this.chooser == null) {
            this.chooser = new JFileChooser(user_dir);
        }
        int option = this.chooser.showOpenDialog(frm);
        if (option == JFileChooser.APPROVE_OPTION) {
            File cfil = this.chooser.getSelectedFile();
            load(cfil);
        }
    }

    /**
     * Component Listener implementation
     */
    public void windowActivated(WindowEvent evt) {
    }

    /** Component Listener implementation */
    public void windowDeactivated(WindowEvent evt) {
    }

    /** Component Listener implementation */
    public void windowDeiconified(WindowEvent evt) {
    }

    /** Component Listener implementation */
    public void windowIconified(WindowEvent evt) {
    }

    /** Component Listener implementation */
    public void windowOpened(WindowEvent evt) {
    }

    /** Close everything and exit upon closing the window */
    public void windowClosing(WindowEvent evt) {
        //    System.out.println(evt);
        if (evt.getSource() == frm) {
            updatePrefs(gview.getColorPrefs());
            System.exit(0);
        }
    }

    /** Close everything and exit upon closing the window */
    public void windowClosed(WindowEvent evt) {
        if (evt.getSource() == frm) {
            updatePrefs(gview.getColorPrefs());
            System.exit(0);
        }
    }

    /**
     * Adds menu item to View menu. Adds open browser action to it.
     * @param   menu    Menu name to which submenus should be added.
     */
    private void addViewActions(JMenu menu) {
        JMenuItem menuitem;
        OpenBrowserAction b_action = new OpenBrowserAction(this.gview);
        menuitem = menu.add(b_action);
        menuitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0));
        b_action.setEnabled(false);

        gview.popup.add(b_action);
    }

    /**
     * Adds menu item to File menu. Adds Load,print and quit to it.
     * @param   file_menu   Menu name to which submenus should be added.
     */
    private void addFileActions(JMenu file_menu) {
        JMenuItem menuitem;

        AbstractAction load_action = new AbstractAction("Open File ...") {

            public void actionPerformed(ActionEvent e) {
                    doLoadFile();
            }
        };
        load_action.setEnabled(true);
        menuitem = file_menu.add(load_action);
        menuitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, 0));

        if(getArgumentValue(Arguments.SERVER)!=null)
        {
            AbstractAction server_load_action = new AbstractAction("Open from server ...") {

                public void actionPerformed(ActionEvent e) {
                        sampleChooser.setVisible(true);
                }
            };
            server_load_action.setEnabled(true);
            menuitem = file_menu.add(server_load_action);
            menuitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));
        }

        AbstractAction print_action = new AbstractAction("Print") {

            public void actionPerformed(ActionEvent e) {
                try {
                    print_panel.print();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        print_action.setEnabled(true);
        menuitem = file_menu.add(print_action);
        menuitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));

        AbstractAction export_action = new AbstractAction("Export as ...") {

            public void actionPerformed(ActionEvent e) {
                try {
                    ExportDialog export = new ExportDialog();
                    export.showExportDialog(gview, "Export view as ...", gview, "export");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        export_action.setEnabled(true);
        menuitem = file_menu.add(export_action);
        menuitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0));

        AbstractAction preference = new AbstractAction("Set Color Preferences ...") {

            public void actionPerformed(ActionEvent e) {
                colorChooser.setVisible(true);
            }
        };
        preference.setEnabled(true);
        menuitem = file_menu.add(preference);
        menuitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

        AbstractAction quit_action = new AbstractAction("Quit") {

            public void actionPerformed(ActionEvent e) {
                updatePrefs(gview.getColorPrefs());
                System.exit(0);
            }
        };
        quit_action.setEnabled(true);
        menuitem = file_menu.add(quit_action);
        menuitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0));

    }

    /**
     * In testmode, adds 3 buttons in menubar to load files.
     * @param   mbar
     */
    private void addQuickLaunch(JMenuBar mbar) {
        JLabel test = new JLabel("                || Chose Test file :-");
        JButton low = new JButton("Low");
        JButton med = new JButton("Medium");
        JButton high = new JButton("High");
        mbar.add(test);
        mbar.add(low);
        mbar.add(med);
        mbar.add(high);

        low.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    load(new File(user_dir + "/samples/AF113210.1.paxml"));
                }
            });

        med.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    load(new File(user_dir + "/samples/ADARB1.paxml"));
                }
            });

        high.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    load(new File(user_dir +"/samples/ABCB4.paxml"));
                }
            });
    }

    /**
     * Sets up interface to select file from the server.
     */
    private void setupSamplesFromServer()
    {

        sampleChooser = new JFrame("Choose a file ...");
        sampleChooser.setSize(250, 250);
        sampleChooser.setLocation((int) (screen.width * .4f), (int) (screen.height * .15f));
        sampleChooser.setLayout(new BoxLayout(sampleChooser.getContentPane(), BoxLayout.Y_AXIS));

        String files[] = getSamplesFromServer();
        final JList filesList = new JList(files);
        filesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(filesList);

        sampleChooser.add(scrollPane);

        JPanel buttonpanel = new JPanel();
        buttonpanel.setLayout(new FlowLayout());
        JButton open   = new JButton(" Open ");
        JButton cancel = new JButton("Cancel");
        buttonpanel.add(open);
        buttonpanel.add(cancel);

        open.addActionListener(new ActionListener(){

                public void actionPerformed(ActionEvent e) {
                    load(filesList.getSelectedValue().toString());
                    sampleChooser.setVisible(false);
                }
            });

        cancel.addActionListener(new ActionListener(){

                public void actionPerformed(ActionEvent e) {
                    sampleChooser.setVisible(false);
                }
            });

        if(files.length <= 0)
            open.setEnabled(false);
            
        sampleChooser.add(buttonpanel);

    }

    /**
     * Loads all samples if server name is provided.
     */
    private String[] getSamplesFromServer()
    {
        List<String> ret = new ArrayList<String>();
        String page = loadPage();
        Pattern pattern = Pattern.compile("<a.+href=\"(.+paxml)\"");
        Matcher matcher = pattern.matcher(page);
        while (matcher.find()) {
            ret.add(matcher.group(1));
        }
        return (String[])ret.toArray(new String[0]);
    }

    /**
     * Parses command line argument and adds valid arguments to the argument dictionary.
     * @param   args    Command line arguments
     */
    private void parseArguments(String[] args) {

       if(args.length  == 1)
       {
           if(args[0].startsWith("http:/") || args[0].startsWith("https:/"))
           {
               if(args[0].endsWith(".paxml"))
               {
                   String file = args[0].substring(args[0].lastIndexOf("/")+1);
                   String server = args[0].replace(file, "");
                   System.out.println(file);
                   System.out.println(server);
                   addToArgumentDictionary(new String[]{"-s",server,"-f",file});
               }
               else
               {
                   addToArgumentDictionary(new String[]{"-s",args[0]});
               }
           }else if(args[0].startsWith("/"))
           {
               if(args[0].endsWith(".paxml"))
               {
                    addToArgumentDictionary(new String[]{"-f",args[0]});
               }
               else
                   JOptionPane.showMessageDialog(new JFrame(),"Invalid Argument","",JOptionPane.ERROR_MESSAGE);

           }else
                   JOptionPane.showMessageDialog(new JFrame(),"Invalid Argument","",JOptionPane.ERROR_MESSAGE);

       }else if(args.length%2 == 0)
       {
           addToArgumentDictionary(args);
       }
       else
           JOptionPane.showMessageDialog(new JFrame(),"Invalid Arguments","",JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Adds argument to dictionary
     * @param   args    Argument pair to be inserted in dictionary.
     */
    private void addToArgumentDictionary(String[] args) {
       
       for(int i=0; i<args.length; i+=2)
       {
            if(Arguments.getValue(args[i])!=null)
                ArgumentValues.put(Arguments.getValue(args[i]), args[i+1]);
       }
    }

    /**
     * Gets value for the given argument.
     * @param   arg     Argument name
     * @return  String  Returns argument value
     */
    private String getArgumentValue(Arguments arg)
    {
        return ArgumentValues.get(arg);
    }

    /**
     * Loads the sample directory from the server
     * @return  String  Returns directory listing in string format.
     */
    private String loadPage() {
        StringBuffer output = new StringBuffer(2000);
        BufferedReader buff = null;
        try {
            URL url = new URL(getArgumentValue(Arguments.SERVER));
            URLConnection conn = url.openConnection();
            
            // setting these timeouts ensures the client does not deadlock indefinitely
            // when the server has problems.
            conn.setConnectTimeout(1000 * 10);
            conn.setReadTimeout(1000 * 10);

            buff = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            boolean eof = false;
            while (!eof) {
                String line = buff.readLine();
                if (line == null) {
                    eof = true;
                } else {
                    output.append(line + "\n");
                }
            }
        } catch (IOException e) {
            System.out.println("Error -- " + e.toString());
        } finally {
            GeneralUtils.safeClose(buff);
        }
        return output.toString();
    }

    /**
     * Updates users color preferences
     * @param   hash    Hashtable containing color name and color value pairs.
     */
    private void updatePrefs(Hashtable<String,Color> hash)
    {
        prefs = Preferences.userNodeForPackage(org.bioviz.protannot.ProtAnnotMain.class);
        Enumeration e = hash.keys();

        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            prefs.putInt(key, hash.get(key).getRGB());
        }
    }

    /**
     * Sets up interface to choose color preferences.
     */
    private void setupColorChooser()
    {
        colorChooser = new JFrame("Color Preference");
        colorChooser.setSize(375, 175);
        colorChooser.setLocation((int) (screen.width * .4f), (int) (screen.height * .15f));
        colorChooser.setLayout(new BorderLayout());
        //colorChooser.setResizable(false);

        final ColorTableModel model = new ColorTableModel();
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setLayout(new BorderLayout());
        table.setDefaultRenderer(Color.class, new ColorTableCellRenderer(true));
        table.setDefaultEditor(Color.class, new ColorTableCellEditor());
        table.setFillsViewportHeight(true);
       
        JPanel buttonpanel = new JPanel();
        buttonpanel.setLayout(new GridLayout(1,4));

        JButton defaults = new JButton("Defaults");
        JButton apply  = new JButton("Apply ");
        JButton save   = new JButton(" Save ");
        JButton cancel = new JButton("Cancel");

        buttonpanel.add(defaults);
        buttonpanel.add(apply);
        buttonpanel.add(save);
        buttonpanel.add(cancel);
        

        apply.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                gview.tempChangePreference(model.colorList());
            }
        });

        save.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                gview.changePreference(model.colorList());
                model.setValues(gview.getColorPrefs());
                colorChooser.setVisible(false);
            }
        });

        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                gview.cancelChangePrefernce();
                model.setValues(gview.getColorPrefs());
                colorChooser.setVisible(false);
            }
        });

        defaults.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                gview.changePreference(GenomeView.COLORS.defaultColorList());
                model.setValues(gview.getColorPrefs());
                colorChooser.setVisible(false);
            }
        });
        
        colorChooser.add("Center",table);
        colorChooser.add("South",buttonpanel);

    }

    /**
     * Returns color preferences in two dimentional object.
     * @param prefs_col     Hashtable<String,Color>
     * @return  Object[][]  Returns color preferences in two dimentional object.
     */
    private Object[][] getData(Hashtable<String, Color> prefs_col)
    {
                Object[][] colordata = new Object[prefs_col.size()][2];
                int i=0;
                Enumeration e = prefs_col.keys();

                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    colordata[i++] = new Object[]{key,prefs_col.get(key)};
                }
                return colordata;

     }

    /**
     * Table model for color prefrences
     */
    private class ColorTableModel extends AbstractTableModel{

            final String[] col_headings = {"Name","Color"};
            Object[][] data;

            /**
             * Initialized data with default color values
             */
            ColorTableModel()
            {
                setValues(gview.getColorPrefs());
            }

            /**
             * Gets number of rows.
             * @return  int     Returns number of rows.
             */
            public int getRowCount() {
                return data.length;
            }

            /**
             * Gets number of columns.
             * @return  int     Returns number of columns.
             */
            public int getColumnCount() {
                return col_headings.length;
            }

            /**
             * Returns value at rowIndex and columnIndex.
             * @param rowIndex      Row number
             * @param columnIndex   Column number
             * @return  Object      Returns value at rowIndex and columnIndex.
             */
            public Object getValueAt(int rowIndex, int columnIndex) {
                return data[rowIndex][columnIndex];
            }

            /**
             * Tells if the cell is editable at row and column.
             * @param row   Row number
             * @param col   Column number
             * @return  boolean     true if cell is editable else false
             */
            @Override
            public boolean isCellEditable(int row, int col) {
                if (col > 0) {
                    return true;
                } else {
                    return false;
                }
            }

            /**
             * Set cell value at row and col.
             * @param value     Value to be set
             * @param row       Row number
             * @param col       Column number
             */
            @Override
            public void setValueAt(Object value, int row, int col) {
                data[row][col] = value;
                fireTableCellUpdated(row, col);
            }

            /**
             * Returns Class of given column number c.
             * @param c     Column number
             * @return  Class    Class of column c.
             */
            @Override
            public Class getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }

            /**
             * Returns table value in form of hashtable
             * @return  Hashtable<String,Color> Hashtable of values in rows.
             */
            public Hashtable<String,Color> colorList()
            {
                Hashtable<String,Color> colorhash = new Hashtable<String,Color>();

                for(int i=0; i<data.length; i++)
                    colorhash.put((String)data[i][0], (Color)data[i][1]);
                
                return colorhash;
            }

            /**
             * Sets all values of the data
             * @param prefs_cols
             */
            public void setValues(Hashtable<String,Color> prefs_cols)
            {
                data = getData(prefs_cols);
            }

        };

}
