/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot;

import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.UniFileChooser;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.bioviz.protannot.interproscan.InterProscanTranslator;
import org.bioviz.protannot.interproscan.api.InterProscanService;
import org.bioviz.protannot.interproscan.api.InterProscanService.Status;
import org.bioviz.protannot.interproscan.api.Job;
import org.bioviz.protannot.interproscan.api.JobRequest;
import org.bioviz.protannot.interproscan.api.JobSequence;
import org.bioviz.protannot.interproscan.appl.model.ParameterType;
import org.bioviz.protannot.model.Dnaseq;
import org.bioviz.protannot.model.ProtannotParser;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 *
 * @author jeckstei
 */
@aQute.bnd.annotation.component.Component(provide = SequenceService.class)
public class SequenceService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SequenceService.class);

    private InterProscanService interProscanService;

    private InterProscanTranslator interProscanTranslator;

    private static final String EMAIL_PATTERN
            = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private final Pattern pattern;
    private Matcher matcher;

    private JLabel infoLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JTextField email;
    private Timer resultFetchTimer;
    private JDialog dialog;
    private JPanel parentPanel;
    private final Set<String> inputAppl;
    private JPanel configParentPanel;
    private ProtannotParser parser;
    private final List<String> defaultApplications;

    public SequenceService() throws JAXBException {
        inputAppl = Sets.newConcurrentHashSet();
        defaultApplications = Lists.newArrayList("PfamA", "TMHMM", "SignalP");
        pattern = Pattern.compile(EMAIL_PATTERN);
    }

    private void initEmail() {
        email = new JTextField();
    }

    private void initInfoLabel(String text) {
        if (infoLabel == null) {
            infoLabel = new JLabel(text);
        } else {
            infoLabel.setText(text);
        }
        
    }
    
    private void initStatusLabel(String text) {
        if(statusLabel == null) {
            statusLabel = new JLabel(text);
        } else {
            statusLabel.setText(text);
        }
    }

    private void initProgressBar() {
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
    }

    private boolean isDefaultApplication(String application) {
        return defaultApplications.contains(application);
    }

    private boolean showApplicationOptionsLoadingModal() {
        createLoadApplicationsThread();

        parentPanel = new JPanel(new MigLayout());
        initInfoLabel("Loading InterProScan Options. Please wait...");
        parentPanel.add(infoLabel, "wrap");

        initProgressBar();
        parentPanel.add(progressBar, "align center, wrap");

        final JComponent[] inputs = new JComponent[]{
            parentPanel
        };
        Object[] options = {"Cancel"};

        Object selectedValue = showOptionPane(inputs, options, "Loading InterProScan Options");
        return processApplicationLoadingSelection(selectedValue, options);
    }

    private Object showOptionPane(final JComponent[] inputs, Object[] options, String message) throws HeadlessException {
        JOptionPane pane = new JOptionPane(inputs, JOptionPane.PLAIN_MESSAGE, JOptionPane.CANCEL_OPTION,
                null,
                options,
                null);
        pane.setInitialValue(null);
        dialog = pane.createDialog(message);
        dialog.show();
        dialog.dispose();
        return pane.getValue();
    }

    private boolean processApplicationLoadingSelection(Object selectedValue, Object[] options) {
        if (selectedValue != null && selectedValue.equals(options[0])) {
            LOG.debug("cancelling request");
            return false;
        }
        return true;
    }

    private void createLoadApplicationsThread() {
        CThreadWorker< Void, Void> worker = new CThreadWorker<Void, Void>("Loading InterProScan Options") {
            @Override
            protected Void runInBackground() {
                ParameterType applications = interProscanService.getApplications();
                applications.getValues().getValue().forEach(vt -> {
                    JCheckBox applCheckBox = new JCheckBox(vt.getLabel());
                    applCheckBox.setName(vt.getValue());
                    if (isDefaultApplication(vt.getValue())) {
                        applCheckBox.setSelected(true);
                    } else {
                        applCheckBox.setSelected(false);
                    }
                    configParentPanel.add(applCheckBox);
                });
                dialog.dispose();
                return null;
            }
            
            @Override
            protected void finished() {
            }
        };
        CThreadHolder.getInstance().execute(this, worker);
    }

    private void showResultLoadingModal() {
        parentPanel = new JPanel(new MigLayout());

        initInfoLabel(LOADING_IPS_DATA);
        initStatusLabel( "Initializing ...");
        parentPanel.add(infoLabel, "wrap");
        parentPanel.add(statusLabel, "wrap");

        initProgressBar();
        parentPanel.add(progressBar, "align center, wrap");

        final JComponent[] inputs = new JComponent[]{
            parentPanel
        };
        Object[] options = {"Cancel"};

        Object selectedValue = showOptionPane(inputs, options, "Loading InterProScan Data");
        if (selectedValue != null && selectedValue.equals(options[0])) {
            LOG.debug("cancelling request");
            resultFetchTimer.cancel();
        }
    }
    private static final String LOADING_IPS_DATA = "Loading InterProScan data, Please wait...";

    private boolean showSetupModal() {
        inputAppl.clear();
        configParentPanel = new JPanel(new MigLayout(new LC().wrapAfter(3)));
        configParentPanel.add(new JLabel("Select the applications to run."), "wrap");
        initEmail();
        JPanel emailPanel = new JPanel(new MigLayout());
        emailPanel.add(new JLabel("Email:"));
        emailPanel.add(email, "width :125:");
        configParentPanel.add(emailPanel, "wrap");
        if (!showApplicationOptionsLoadingModal()) {
            return false;
        }

        final JComponent[] inputs = new JComponent[]{
            configParentPanel
        };
        Object[] options = {"Run", "Cancel"};
        int optionChosen = JOptionPane.showOptionDialog(null, inputs, "InterProScan Job Configuration", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        return processSetupOption(optionChosen);
    }

    private boolean processSetupOption(int optionChosen) {
        if (optionChosen == 0) {
            matcher = pattern.matcher(email.getText());
            if (!matcher.matches()) {
                ModalUtils.infoPanel("Please enter a valid email address.");
                return false;
            }
            for (java.awt.Component c : configParentPanel.getComponents()) {
                if (c instanceof JCheckBox) {
                    if (((JCheckBox) c).isSelected()) {
                        String value = ((JCheckBox) c).getName();
                        inputAppl.add(value);
                    }
                }
            }
            return true;
        }
        return false;
    }

    public void asyncLoadSequence(Callback callback) {
        if (showSetupModal()) {
            CThreadWorker< Void, Void> worker = new CThreadWorker<Void, Void>("Loading InterProScan") {
                @Override
                protected Void runInBackground() {
                    loadSequence(callback);
                    return null;
                }

                @Override
                protected void finished() {
                }
            };
            CThreadHolder.getInstance().execute(this, worker);
            showResultLoadingModal();
        }
    }

    private JobRequest createJobRequest() {
        JobRequest request = new JobRequest();
        request.setEmail(email.getText());

        request.setSignatureMethods(Optional.of(inputAppl));
        request.setTitle(Optional.empty());
        request.setGoterms(Optional.empty());
        request.setPathways(Optional.empty());
        for (Object obj : parser.getDnaseq().getMRNAAndAaseq()) {
            if (obj instanceof Dnaseq.MRNA) {
                String proteinSequence = null;
                String sequenceName = null;
                for (Dnaseq.Descriptor d : ((Dnaseq.MRNA) obj).getDescriptor()) {
                    if (d.getType().equals("protein sequence")) {
                        proteinSequence = d.getValue();
                    }
                    if (d.getType().equals("protein_product_id")) {
                        sequenceName = d.getValue();
                    }
                }

                request.getJobSequences().add(new JobSequence(sequenceName, proteinSequence));
            }
        }
        return request;
    }

    private void processJobResults(final List<Job> successfulJobs, Callback callback) {
        Dnaseq original = parser.getDnaseq();
        for (Job job : successfulJobs) {
            Optional<Document> doc = interProscanService.result(job.getId());
            if (doc.isPresent()) {
                Dnaseq.Aaseq aaseq = interProscanTranslator.translateFromResultDocumentToModel(job.getSequenceName(), doc.get());
                original.getMRNAAndAaseq().add(aaseq);
            }
        }
        callback.execute(original);
        dialog.dispose();
        resultFetchTimer.cancel();
    }

    private TimerTask buildTimerTask(final List<Job> jobs, Callback callback) {
        final List<Job> successfulJobs = new ArrayList<>();
        return new TimerTask() {

            @Override
            public void run() {

                int failed = 0;

                Iterator<Job> it = jobs.iterator();
                while (it.hasNext()) {
                    Job job = it.next();
                    Status status = interProscanService.status(job.getId());
                    LOG.info(status.toString());
                    if (status.equals(Status.FINISHED)) {
                        successfulJobs.add(job);
                        it.remove();
                    }
                    if (status.equals(Status.ERROR)) {
                        failed++;
                        it.remove();
                    }
                    if (status.equals(Status.FAILURE)) {
                        failed++;
                        it.remove();
                    }
                    if (status.equals(Status.NOT_FOUND)) {
                        //TODO: Notify user
                        it.remove();
                    }
                }
                initStatusLabel(jobs.size() + " Running, " + successfulJobs.size() + " Successful, " + failed + " Failed ");
                dialog.pack();
                dialog.repaint();
                if (jobs.isEmpty()) {
                    processJobResults(successfulJobs, callback);
                }
            }
        };
    }

    public void loadSequence(Callback callback) {
        JobRequest jobRequest = createJobRequest();
        final List<Job> jobs = interProscanService.run(jobRequest);
        if (LOG.isDebugEnabled()) {
            jobs.stream().forEach((job) -> {
                LOG.debug(job.getId());
            });
        }
        resultFetchTimer = new Timer();
        resultFetchTimer.schedule(buildTimerTask(jobs, callback), new Date(), 1000);

    }

    public void exportAsXml(Component component) {
        JFileChooser chooser = new UniFileChooser("PAXML File", "paxml");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.rescanCurrentDirectory();
        int option = chooser.showSaveDialog(component);
        if (option == JFileChooser.APPROVE_OPTION) {
            File exportFile = chooser.getSelectedFile();
            Dnaseq dnaseq = parser.getDnaseq();
            JAXBContext jaxbContext;
            try {
                jaxbContext = JAXBContext.newInstance(Dnaseq.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                jaxbMarshaller.marshal(dnaseq, exportFile);
            } catch (JAXBException ex) {
                LOG.error(ex.getMessage(), ex);
            }

        }
    }

    @Reference
    public void setInterProscanService(InterProscanService interProscanService) {
        this.interProscanService = interProscanService;
    }

    @Reference
    public void setParser(ProtannotParser parser) {
        this.parser = parser;
    }

    @Reference
    public void setInterProscanTranslator(InterProscanTranslator interProscanTranslator) {
        this.interProscanTranslator = interProscanTranslator;
    }

    public interface Callback {

        public void execute(Dnaseq dnaseq);
    }

}
