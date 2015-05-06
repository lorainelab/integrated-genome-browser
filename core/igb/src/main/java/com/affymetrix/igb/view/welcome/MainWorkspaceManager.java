package com.affymetrix.igb.view.welcome;

import be.pwnt.jflow.JFlowPanel;
import be.pwnt.jflow.event.ShapeEvent;
import be.pwnt.jflow.event.ShapeListener;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.igb.EventService;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.swing.JRPJPanel;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.google.common.eventbus.EventBus;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the cover flow visualization. It has a card layout where
 * the welcome panel is held as well as the SeqMapView object.
 *
 * Upon the user's choice (from the cover flow, or from the SeqGroupView combo
 * box)
 * the welcome screen falls out to the background, and the SeqMapView is brought
 * to
 * the foreground.
 *
 *
 * @author jfvillal
 */
public class MainWorkspaceManager extends JRPJPanel implements ItemListener {

    private static final Logger logger = LoggerFactory.getLogger(MainWorkspaceManager.class);
    private static final long serialVersionUID = 1L;
    public static final String WELCOME_PANE = "WelcomePane";
    public static final String SEQ_MAP_PANE = "SeqMapPane";
    private static MainWorkspaceManager singleton;
    private final EventBus eventBus;

    private static final String SELECT_SPECIES = IGBConstants.BUNDLE.getString("speciesCap");

    private final GenometryModel gmodel;

    public static MainWorkspaceManager getWorkspaceManager() {
        if (singleton == null) {
            singleton = new MainWorkspaceManager("MainWorkspaceManager");
        }
        return singleton;
    }

    public MainWorkspaceManager(String id) {
        super(id);
        this.setLayout(new CardLayout());
        gmodel = GenometryModel.getInstance();
        eventBus = EventService.getModuleEventBus();
    }

    public void setSeqMapViewObj(SeqMapView obj) {
        add(new WelcomePage(getWelcomePane()), WELCOME_PANE);
        add(obj, SEQ_MAP_PANE);
    }

    /**
     * @return welcome panel.
     */
    public JPanel getWelcomePane() {
        //return new JPanel();
        final JFlowPanel panel = new JFlowPanel(new GeneConfiguration());
        panel.setPreferredSize(new Dimension(500, 200));
        panel.addListener(new ShapeListener() {
            @Override
            public void shapeClicked(ShapeEvent e) {
                MouseEvent me = e.getMouseEvent();
                if (!me.isConsumed() && me.getButton() == MouseEvent.BUTTON1
                        && me.getClickCount() == 1) {
                    //JOptionPane.showMessageDialog(panel,
                    //		"You clicked on " + e.getShape() + ".",
                    //		"Event Test", JOptionPane.INFORMATION_MESSAGE);
                    CargoPicture pic = (CargoPicture) e.getShape();
                    Object obj = pic.getCargo();

                    if (obj == null) {
                        return;
                    }

                    String speciesName = (String) obj;
                    final List<String> versionNames = SeqGroupView.getInstance().getAllVersions(speciesName);
                    if (versionNames.isEmpty()) {
                        IGB.getInstance().setStatus(speciesName + " Not Available", true);
                        ErrorHandler.errorPanel("NOTICE", speciesName + " not available at this time. "
                                + "Please check that the appropriate data source is available.", Level.WARNING);
                        return;
                    }
                    String versionName = versionNames.get(0);
                    GenomeVersion genomeVersion = gmodel.getSeqGroup(versionName);

                    if (genomeVersion == null || genomeVersion.getAvailableDataContainers().isEmpty()) {
                        IGB.getInstance().setStatus(versionName + " Not Available", true);
                        ErrorHandler.errorPanel("NOTICE", versionName + " not available at this time. "
                                + "Please check that the appropriate data source is available.", Level.WARNING);
                        return;
                    }

                    SeqGroupView.getInstance().setSelectedGenomeVersion(genomeVersion);
                }
            }

            @Override
            public void shapeActivated(ShapeEvent e) {
            }

            @Override
            public void shapeDeactivated(ShapeEvent e) {
            }
        });

        return panel;
    }

    /**
     * Receives state update from the genus/species combo boxes.
     *
     * @param e
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() != ItemEvent.SELECTED || e.getItem() == null) {
            return;
        }

        CardLayout layout = (CardLayout) getLayout();
//		System.out.println("MainWorkspaceManager:itemStateChanged hit");
        String species = e.getItem().toString();
        if (gmodel.getSelectedGenomeVersion() == null && SELECT_SPECIES.equals(species)) {
            layout.show(this, WELCOME_PANE);
        } else {
            layout.show(this, SEQ_MAP_PANE);
        }
    }

}
