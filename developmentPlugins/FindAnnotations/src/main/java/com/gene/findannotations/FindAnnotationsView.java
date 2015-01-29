package com.gene.findannotations;

import com.affymetrix.igb.service.api.IgbService;
import com.affymetrix.igb.service.api.IgbTabPanelI;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import com.affymetrix.igb.shared.IStatus;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

@Component(provides = IgbTabPanelI.class)
public class FindAnnotationsView extends FindAnnotationsGUI {

    private static final long serialVersionUID = 1L;
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("findannotations");
    private FindAnnotationsAction findAnnotationsAction;

    @ServiceDependency
    private IgbService igbService;

    public FindAnnotationsView() {
        super();

    }

    @Start
    private void init() {
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
}
