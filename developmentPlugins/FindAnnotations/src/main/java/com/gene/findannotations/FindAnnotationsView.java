package com.gene.findannotations;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.window.tabs.IgbTabPanelI;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import com.lorainelab.igb.services.search.IStatus;

@Component(name = FindAnnotationsView.COMPONENT_NAME, provide = IgbTabPanelI.class, immediate = true)
public class FindAnnotationsView extends FindAnnotationsGUI {

    public static final String COMPONENT_NAME = "FindAnnotationsView";
    private static final long serialVersionUID = 1L;
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("findannotations");
    private FindAnnotationsAction findAnnotationsAction;

    private IgbService igbService;

    public FindAnnotationsView() {
    }

    @Activate
    public void activate() {
        searchTable.setModel(new AnnotationsTableModel());
        searchTable.setAutoCreateRowSorter(true);
        searchTable.addMouseListener(new FindAnnotationsSelectListener(searchTable, igbService));
        final IStatus status = new IStatus() {
            @Override
            public void setStatus(String text) {
                igbService.setStatus(text);
            }
        };
        findAnnotationsAction = new FindAnnotationsAction(igbService, searchText, selectedTracksCB, searchTable, trackFromHitsButton, status);
        searchText.addKeyListener(
                new KeyAdapter() {
                    public void keyTyped(KeyEvent e) {
                        if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                            findAnnotationsAction.actionPerformed(null);
                        }
                    }
                }
        );
        goButton.addActionListener(findAnnotationsAction);
        trackFromHitsButton.addActionListener(new TrackFromHitsAction(igbService, searchText, (AnnotationsTableModel) searchTable.getModel()));
        trackFromHitsButton.setEnabled(false);
    }

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }
}
