/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.frame;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.lorainelab.igb.services.IgbService;
import static com.lorainelab.igb.services.ServiceComponentNameReference.ALT_SPLICE_VIEW_TAB;
import static com.lorainelab.igb.services.ServiceComponentNameReference.ANNOTATION_TRACK_PANEL_TAB;
import static com.lorainelab.igb.services.ServiceComponentNameReference.APP_MANAGER_TAB;
import static com.lorainelab.igb.services.ServiceComponentNameReference.BOOKMARK_TAB;
import static com.lorainelab.igb.services.ServiceComponentNameReference.DATA_MANAGEMENT_TAB;
import static com.lorainelab.igb.services.ServiceComponentNameReference.EXTERNAL_VIEWER_TAB;
import static com.lorainelab.igb.services.ServiceComponentNameReference.GRAPH_TRACK_PANEL_TAB;
import static com.lorainelab.igb.services.ServiceComponentNameReference.RESTRICTIONS_TAB;
import static com.lorainelab.igb.services.ServiceComponentNameReference.SEARCH_VIEW_TAB;
import static com.lorainelab.igb.services.ServiceComponentNameReference.SELECTION_INFO_TAB;
import static com.lorainelab.igb.services.ServiceComponentNameReference.SEQ_GROUP_TAB;
import com.lorainelab.igb.services.window.tabs.IgbTabPanelI;
import java.awt.event.ActionEvent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true, provide = FrameVisibilityManager.class)
public class FrameVisibilityManager {

    private static final Logger logger = LoggerFactory.getLogger(FrameVisibilityManager.class);

    private IgbTabPanelI dataAccess;
    private IgbTabPanelI altSpliceView;
    private IgbTabPanelI seqGroupView;
    private IgbTabPanelI appManagerTab;
    private IgbTabPanelI bookmarks;
    private IgbTabPanelI advancedSearch;
    private IgbTabPanelI restrictionsTab;
    private IgbTabPanelI selectionsInfo;
    private IgbTabPanelI externalView;
    private IgbTabPanelI graphTrackPanel;
    private IgbTabPanelI annotationTrackPanel;
    private IgbService igbService;

    public FrameVisibilityManager() {
    }

    @Activate
    public void activate() {
        SwingUtilities.invokeLater(() -> {
            igbService.getApplicationFrame().setVisible(true);
        });
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
        //Start anyway if a service fails
        Timer failSafeTimer = new Timer(6000, (ActionEvent e) -> {
            if (!igbService.getApplicationFrame().isVisible()) {
                logger.warn("A fail safe timer was triggered to visualize the main frame, some tabs may be missing.");
                igbService.getApplicationFrame().setVisible(true);
            }
        });
        failSafeTimer.setRepeats(false);
        failSafeTimer.start();
    }

    @Reference(target = "(&(component.name=" + DATA_MANAGEMENT_TAB + "))")
    public void setDataAccess(IgbTabPanelI dataAccess) {
        this.dataAccess = dataAccess;
    }

    @Reference(target = "(&(component.name=" + ALT_SPLICE_VIEW_TAB + "))")
    public void setAltSpliceView(IgbTabPanelI altSpliceView) {
        this.altSpliceView = altSpliceView;
    }

    @Reference(target = "(&(component.name=" + SEQ_GROUP_TAB + "))")
    public void setSeqGroupView(IgbTabPanelI seqGroupView) {
        this.seqGroupView = seqGroupView;
    }

    @Reference(target = "(&(component.name=" + APP_MANAGER_TAB + "))")
    public void setAppManagerTab(IgbTabPanelI appManagerTab) {
        this.appManagerTab = appManagerTab;
    }

    @Reference(target = "(&(component.name=" + BOOKMARK_TAB + "))")
    public void setBookmarks(IgbTabPanelI bookmarks) {
        this.bookmarks = bookmarks;
    }

    @Reference(target = "(&(component.name=" + SEARCH_VIEW_TAB + "))")
    public void setAdvancedSearch(IgbTabPanelI advancedSearch) {
        this.advancedSearch = advancedSearch;
    }

    @Reference(target = "(&(component.name=" + RESTRICTIONS_TAB + "))")
    public void setRestrictionsTab(IgbTabPanelI restrictionsTab) {
        this.restrictionsTab = restrictionsTab;
    }

    @Reference(target = "(&(component.name=" + SELECTION_INFO_TAB + "))")
    public void setSelectionsInfo(IgbTabPanelI selectionsInfo) {
        this.selectionsInfo = selectionsInfo;
    }

    @Reference(target = "(&(component.name=" + EXTERNAL_VIEWER_TAB + "))")
    public void setExternalView(IgbTabPanelI externalView) {
        this.externalView = externalView;
    }

    @Reference(target = "(&(component.name=" + GRAPH_TRACK_PANEL_TAB + "))")
    public void setGraphTrackPanel(IgbTabPanelI graphTrackPanel) {
        this.graphTrackPanel = graphTrackPanel;
    }

    @Reference(target = "(&(component.name=" + ANNOTATION_TRACK_PANEL_TAB + "))")
    public void setAnnotationTrackPanel(IgbTabPanelI annotationTrackPanel) {
        this.annotationTrackPanel = annotationTrackPanel;
    }

}
