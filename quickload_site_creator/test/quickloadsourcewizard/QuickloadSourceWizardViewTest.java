/*
 * This is the GUI version.  QuickloadSourceWizardCmd for command line version
 * and open the template in the editor.
 */
package quickloadsourcewizard;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Component;
import javax.swing.JSlider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This is a sample application on testing swing just using jUnit.
 * There are other libraries to make this process less painfull.  These 
 * suits can be studied in the future.
 * @author jfvillal
 */
public class QuickloadSourceWizardViewTest {
    
    public QuickloadSourceWizardViewTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of showAboutBox method, of class QuickloadSourceWizardView.
     */
    @Test
    public void testShowAboutBox() {
        System.out.println("showAboutBox");
         QuickloadSourceWizardApp win_parent = new  QuickloadSourceWizardApp();
        QuickloadSourceWizardView instance = new QuickloadSourceWizardView(win_parent);
        instance.getFrame().show();
        Component[] val = instance.getFrame().getContentPane().getComponents();
        for( int i = 0; i < val.length ; i++){
            System.out.println( val[i].getClass().toString() );
            if( val[i] instanceof JPanel){
                Component[] comp = ((JPanel) val[i]).getComponents();
                for( int k = 0; k < val.length ; k++){        
                    System.out.println( " " + comp[i].getClass().toString() );        
                    if( comp[i] instanceof JButton){
                        assert( ((JButton)comp[i]).getLabel().equals("Create") );
                    }
                }
            }
        }
        
                
     
    }
}
