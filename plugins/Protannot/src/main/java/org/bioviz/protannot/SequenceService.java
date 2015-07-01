/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.miginfocom.swing.MigLayout;
import org.bioviz.protannot.interproscan.api.InterProscanService;
import org.bioviz.protannot.interproscan.api.InterProscanService.Status;
import org.bioviz.protannot.interproscan.api.JobRequest;
import org.bioviz.protannot.interproscan.appl.model.ParameterType;
import org.bioviz.protannot.interproscan.appl.model.ValueType;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
@Component(provide = SequenceService.class)
public class SequenceService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SequenceService.class);

    private InterProscanService interProscanService;

    private JLabel infoLabel;
    private JProgressBar progressBar;
    private JLabel showDetailLabel;
    private JTextArea detailText;
    private JScrollPane areaScrollPane;
    Timer timer;
    JDialog dialog;
    JPanel parentPanel;

    private void initInfoLabel() {
        infoLabel = new JLabel("Loading InterProscan data, Please wait...");
    }

    private void initProgressBar() {
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
    }

    private void initShowDetailLabel() {
        showDetailLabel = new JLabel("+ show detail");
        showDetailLabel.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (areaScrollPane.isVisible()) {
//                    showDetailLabel.setText("+ show detail");
//                    parentPanel.remove(areaScrollPane);
//                    areaScrollPane.setVisible(false);
                } else {
//                    showDetailLabel.setText("- hide detail");
//                    parentPanel.add(areaScrollPane, "grow, height 200");
//                    areaScrollPane.setVisible(true);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                //
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                //
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                //
            }

            @Override
            public void mouseExited(MouseEvent e) {
                //
            }
        });
    }

    private void initDetailText() {
        detailText = new JTextArea();
        detailText.setEditable(false);
        detailText.setLineWrap(true);

    }

    private void initAreaScrollPane() {
        initDetailText();
        areaScrollPane = new JScrollPane(detailText);
        areaScrollPane.setVisible(false);
        areaScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    }

    private void showLoadingModal() {
        parentPanel = new JPanel(new MigLayout());

        initInfoLabel();
        parentPanel.add(infoLabel, "wrap");

        initProgressBar();
        parentPanel.add(progressBar, "align center, wrap");

        //initShowDetailLabel();
        //parentPanel.add(showDetailLabel, "left, wrap");
        //initAreaScrollPane();
        //parentPanel.add(areaScrollPane, "grow, height 200");
        final JComponent[] inputs = new JComponent[]{
            parentPanel
        };
        Object[] options = {"Cancel"};

        JOptionPane pane = new JOptionPane(inputs, JOptionPane.CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                null);

        pane.setInitialValue(null);

        dialog = pane.createDialog("Loading InterProscan Data");

        dialog.show();
        dialog.dispose();
        Object selectedValue = pane.getValue();
        if (selectedValue != null && selectedValue.equals(options[0])) {
            LOG.debug("cancelling request");
            timer.cancel();
        }
    }

    public void asyncLoadSequence() {
        CThreadWorker< Void, Void> worker = new CThreadWorker<Void, Void>("Loading InterProscan") {
            @Override
            protected Void runInBackground() {
                loadSequence();
                return null;
            }

            @Override
            protected void finished() {
            }
        };
        CThreadHolder.getInstance().execute(this, worker);
        showLoadingModal();
    }

    public void loadSequence() {

        //For testing
        JobRequest request = new JobRequest();
        request.setEmail("tmall@uncc.edu");
        ParameterType applParameters = interProscanService.getApplications();
        List<ValueType> applValues = applParameters.getValues().getValue();
        Set<String> inputApplSet = new HashSet<>();
        for (ValueType valueType : applValues) {
            inputApplSet.add(valueType.getValue());
        }

        request.setSignatureMethods(Optional.of(inputApplSet));
        request.setTitle(Optional.empty());
        request.setGoterms(Optional.empty());
        request.setPathways(Optional.empty());
        request.setSequence(Optional.of("MSKLPRELTRDLERSLPAVASLGSSLSHSQSLSSHLLPPPEKRRAISDVRRTFCLFVTFDLLFISLLWIIELNTNTGIRKNLEQEIIQYNFKTSFFDIFVLAFFRFSGLLLGYAVLRLRHWWVIALLSKGAFGYLLPIVSFVLAWLETWFLDFKVLPQEAEEERWYLAAQVAVARGPLLFSGALSEGQFYSPPESFAGSDNESDEEVAGKKSFSAQEREYIRQGKEATAVVDQILAQEENWKFEKNNEYGDTVYTIEVPFHGKTFILKTFLPCPAELVYQEVILQPERMVLWNKTVTACQILQRVEDNTLISYDVSAGAAGGVVSPRDFVNVRRIERRRDRYLSSGIATSHSAKPPTHKYVRGENGPGGFIVLKSASNPRVCTFVWILNTDLKGRLPRYLIHQSLAATMFEFAFHLRQRISELGARA"));
        Optional<String> jobId = interProscanService.run(request);

        LOG.info(jobId.get());

//        timer = new Timer(1000, (ActionEvent e) -> {
//            Status status = interProscanService.status(jobId.get());
//            LOG.debug(status.toString());
//            if (status.equals(Status.FINISHED)) {
//                dialog.dispose();
//                timer.stop();
//            }
//            if (status.equals(Status.ERROR)) {
//                dialog.dispose();
//                timer.stop();
//                //TODO: Notify user
//            }
//            if (status.equals(Status.FAILURE)) {
//                dialog.dispose();
//                timer.stop();
//                //TODO: Notify user
//            }
//            if (status.equals(Status.NOT_FOUND)) {
//                dialog.dispose();
//                timer.stop();
//                //TODO: Notify user
//            }
//        });
//        timer.setDelay(1000);
//        timer.start();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                Status status = interProscanService.status(jobId.get());
                LOG.debug(status.toString());
                if (status.equals(Status.FINISHED)) {
                    dialog.dispose();
                    timer.cancel();
                }
                if (status.equals(Status.ERROR)) {
                    dialog.dispose();
                    timer.cancel();
                    //TODO: Notify user
                }
                if (status.equals(Status.FAILURE)) {
                    dialog.dispose();
                    timer.cancel();
                    //TODO: Notify user
                }
                if (status.equals(Status.NOT_FOUND)) {
                    dialog.dispose();
                    timer.cancel();
                    //TODO: Notify user
                }
            }
        }, 1000);

    }

    @Reference
    public void setInterProscanService(InterProscanService interProscanService) {
        this.interProscanService = interProscanService;
    }

}
