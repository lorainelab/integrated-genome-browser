package com.gene.findannotations;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.IStatus;

public class FindAnnotationsView extends FindAnnotationsGUI {

    private static final long serialVersionUID = 1L;
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("findannotations");
    private final FindAnnotationsAction findAnnotationsAction;

    public FindAnnotationsView(final IGBService igbService) {
        super();
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
