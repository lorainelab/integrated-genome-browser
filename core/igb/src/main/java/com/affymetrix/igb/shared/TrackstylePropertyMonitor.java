package com.affymetrix.igb.shared;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 *
 * @author hiralv
 */
public class TrackstylePropertyMonitor implements TableModelListener, ActionListener {

    private static TrackstylePropertyMonitor singleton = new TrackstylePropertyMonitor();
    private final Set<TrackStylePropertyListener> listeners = new CopyOnWriteArraySet<TrackStylePropertyListener>();

    private TrackstylePropertyMonitor() {
    }

    public static TrackstylePropertyMonitor getPropertyTracker() {
        return singleton;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        firePropertyChanged(e);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        firePropertyChanged(e);
    }

    public void addPropertyListener(TrackStylePropertyListener listener) {
        listeners.add(listener);
    }

    public void removePropertyListener(TrackStylePropertyListener listener) {
        listeners.remove(listener);
    }

    private void firePropertyChanged(EventObject eo) {
        for (TrackStylePropertyListener listener : listeners) {
            listener.trackstylePropertyChanged(eo);
        }
    }

    public interface TrackStylePropertyListener {

        public void trackstylePropertyChanged(EventObject eo);
    }
}
