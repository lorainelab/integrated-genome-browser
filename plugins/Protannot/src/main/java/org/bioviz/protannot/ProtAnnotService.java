/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.UniFileChooser;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
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
import org.bioviz.protannot.event.StatusSetEvent;
import org.bioviz.protannot.event.StatusStartEvent;
import org.bioviz.protannot.event.StatusTerminateEvent;
import org.bioviz.protannot.interproscan.InterProscanTranslator;
import org.bioviz.protannot.interproscan.api.InterProscanService;
import org.bioviz.protannot.interproscan.api.InterProscanService.Status;
import org.bioviz.protannot.interproscan.api.Job;
import org.bioviz.protannot.interproscan.api.JobRequest;
import org.bioviz.protannot.interproscan.api.JobSequence;
import org.bioviz.protannot.interproscan.appl.model.ParameterType;
import org.bioviz.protannot.interproscan.appl.model.ValueType;
import org.bioviz.protannot.model.Dnaseq;
import org.bioviz.protannot.model.ProtannotParser;
import org.bioviz.protannot.view.StatusBar;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 *
 * @author jeckstei
 */
@aQute.bnd.annotation.component.Component(provide = ProtAnnotService.class, factory = "protannot.service.factory.provider")
public class ProtAnnotService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ProtAnnotService.class);

    private static final Icon INFO_ICON = CommonUtils.getInstance().getIcon("16x16/actions/info.png");

    private InterProscanService interProscanService;

    private InterProscanTranslator interProscanTranslator;

    private static final String SELECT_ALL = "Select all";
    private static final String UNSELECT_ALL = "Unselect all";

    private static final String LOADING_IPS_DATA = "Loading InterProScan data, Please wait...";

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
    private JLabel selectAllLabel;
    private JPanel applicationsPanel;
    private final Preferences protAnnotPreferencesNode;
    private Map<String, Object> properties;
    private Dnaseq dnaseq;
    private CThreadWorker< Void, Void> applicationLoadingWorker;
    private CThreadWorker<Void, Void> loadResultsWorker;
    private EventBus eventBus;
    private ProtAnnotEventService eventService;
    private volatile boolean interProScanRunning;
    private volatile String id;

    @Reference
    public void setEventService(ProtAnnotEventService eventService) {
        this.eventService = eventService;
    }

    public ProtAnnotService() throws JAXBException {
        inputAppl = Sets.newConcurrentHashSet();
        defaultApplications = Lists.newArrayList("PfamA", "TMHMM", "SignalP");
        pattern = Pattern.compile(EMAIL_PATTERN);
        protAnnotPreferencesNode = PreferenceUtils.getProtAnnotNode();
        dnaseq = new Dnaseq();
        interProScanRunning = false;
    }

    public boolean isInterProScanRunning() {
        return interProScanRunning;
    }

    public void cancelBackgroundTasks() {
        eventBus.post(new StatusTerminateEvent(id));
        interProScanRunning = false;
        if (applicationLoadingWorker != null) {
            try {
                applicationLoadingWorker.cancelThread(true);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        if (loadResultsWorker != null) {
            try {
                loadResultsWorker.cancelThread(true);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        if (gview != null) {
            gview.getIpsTableModel().cancelAllJobs();
        }
        LOG.info("All jobs cancelled");
    }
    GenomeView gview;

    public Dnaseq getDnaseq() {
        return dnaseq;
    }

    public void setDnaseq(Dnaseq dnaseq) {
        this.dnaseq = dnaseq;
    }

    @Activate
    public void activate(Map<String, Object> properties) {
        this.properties = properties;
        eventBus = eventService.getEventBus();
        eventBus.register(this);
        id = (String) properties.get("id");
    }

    private void initEmail() {
        email = new JTextField();
        email.setText(protAnnotPreferencesNode.get(PreferenceUtils.PROTANNOT_IPS_EMAIL, ""));
    }

    private void initInfoLabel(String text) {
        if (infoLabel == null) {
            infoLabel = new JLabel(text);
        } else {
            infoLabel.setText(text);
        }

    }

    private void initStatusLabel(String text) {
        if (statusLabel == null) {
            statusLabel = new JLabel(text);
        } else {
            statusLabel.setText(text);
        }
        eventBus.post(new StatusSetEvent(text, StatusBar.ICONS.INFO, true, id));
    }

    private void initProgressBar() {
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
    }

    private boolean isDefaultApplication(String application) {
        return defaultApplications.contains(application);
    }

    private boolean isPreviousSelectedApplication(String application) {
        return inputAppl.contains(application);
    }

    private boolean showApplicationOptionsLoadingModal() {
        applicationLoadingWorker = createLoadApplicationsThread();

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
            //applicationLoadingWorker.cancelThread(true);
            cancelBackgroundTasks();
            return false;
        }
        return true;
    }

    private boolean isAllApplicationsSelected() {
        if (applicationsPanel == null) {
            return false;
        }
        boolean isAllSelected = true;
        for (java.awt.Component parent : applicationsPanel.getComponents()) {
            if (parent instanceof JPanel) {
                for (java.awt.Component child : ((JPanel) parent).getComponents()) {
                    if (child instanceof JCheckBox) {
                        if (!((JCheckBox) child).isSelected()) {
                            isAllSelected = false;
                            break;
                        }
                    }
                }
            }
        }
        return isAllSelected;
    }

    private void initApplicationListener(JCheckBox applCheckBox) {
        applCheckBox.addActionListener((ActionEvent e) -> {
            if (isAllApplicationsSelected()) {
                setSelectAllText(UNSELECT_ALL);
            } else {
                setSelectAllText(SELECT_ALL);
            }
        });
    }

    private void initApplicationToolTip(JPanel container, JCheckBox applCheckBox, ValueType vt) {
        if (vt.getProperties() != null
                && vt.getProperties().getProperty() != null
                && vt.getProperties().getProperty().getKey() != null
                && vt.getProperties().getProperty().getKey().equals("description")) {
            JLabel icon = new JLabel();
            icon.setIcon(INFO_ICON);
            String originalToolTip = vt.getProperties().getProperty().getValue();
            StringBuilder sb = new StringBuilder("<html>");
            for (int i = 0; i < originalToolTip.length(); i += 50) {
                if ((originalToolTip.length() - i) < 50) {
                    sb.append(originalToolTip.substring(i));
                } else {
                    sb.append(originalToolTip.substring(i, i + 50));
//                    if(originalToolTip.length() > (i + 51 - 1) 
//                            && originalToolTip.charAt(i+50) != ' ' 
//                            && originalToolTip.charAt(i+51) != ' ') {
//                        sb.append("-");
//                    }
                    sb.append("<br />");
                }

            }
            sb.append("</html>");
            icon.setToolTipText(sb.toString());
            container.add(icon, "left");
        }
    }

    private void initApplicationCheckboxValues(JPanel container, JCheckBox applCheckBox, ValueType vt) {
        applCheckBox.setName(vt.getValue());
        container.add(applCheckBox, "left");
    }

    private void initApplicationCheckboxSelection(JCheckBox applCheckBox, ValueType vt) {
        if (inputAppl.isEmpty() && isDefaultApplication(vt.getValue())) {
            applCheckBox.setSelected(true);
        } else if (!inputAppl.isEmpty() && isPreviousSelectedApplication(vt.getValue())) {
            applCheckBox.setSelected(true);
        } else {
            applCheckBox.setSelected(false);
        }
    }

    private void buildInterProscanApplications() {
        ParameterType applications = interProscanService.getApplications();
        applicationsPanel = new JPanel(new MigLayout(new LC().wrapAfter(3)));
        applications.getValues().getValue().forEach(vt -> {
            JPanel container = new JPanel(new MigLayout());
            JCheckBox applCheckBox = new JCheckBox(vt.getLabel());
            initApplicationListener(applCheckBox);
            initApplicationCheckboxValues(container, applCheckBox, vt);
            initApplicationToolTip(container, applCheckBox, vt);
            initApplicationCheckboxSelection(applCheckBox, vt);
            applicationsPanel.add(container);
        });
        configParentPanel.add(applicationsPanel, "wrap");
        if (isAllApplicationsSelected()) {
            setSelectAllText(UNSELECT_ALL);
        }
    }

    private CThreadWorker< Void, Void> createLoadApplicationsThread() {
        CThreadWorker< Void, Void> worker = new CThreadWorker<Void, Void>("Loading InterProScan Options") {
            @Override
            protected Void runInBackground() {
                try {
                    buildInterProscanApplications();
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                } finally {
                    dialog.dispose();
                }
                return null;
            }

            @Override
            public boolean cancelThread(boolean b) {
                return this.cancel(b);
            }

            @Override
            protected void finished() {
            }
        };
        CThreadHolder.getInstance()
                .execute(this, worker);
        return worker;
    }

    private void showResultLoadingModal(GenomeView gview) {
        parentPanel = new JPanel(new MigLayout());
        initInfoLabel(LOADING_IPS_DATA);
        initStatusLabel("Initializing ...");
        parentPanel.add(infoLabel, "wrap");
        parentPanel.add(statusLabel, "wrap");

        initProgressBar();
        parentPanel.add(progressBar, "align center, wrap");

        final JComponent[] inputs = new JComponent[]{
            parentPanel
        };
        Object[] options = {"Run in background", "Cancel"};

        Object selectedValue = showOptionPane(inputs, options, "Loading InterProScan Data");
        if (selectedValue != null && selectedValue.equals(options[1])) {
            LOG.info("cancelling result request");
            //loadResultsWorker.cancelThread(true);
            cancelBackgroundTasks();
        } else {
            gview.getTabbedPane().setSelectedIndex(1);
        }
    }

    private void setSelectAllText(String text) {
        selectAllLabel.setText("<html><font color='blue'>" + text + "</font></html>");
    }

    private void initSelectAll() {
        selectAllLabel = new JLabel();
        setSelectAllText("Select all");
        selectAllLabel.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                boolean isAllSelected = isAllApplicationsSelected();
                if (isAllSelected) {
                    setSelectAllText(SELECT_ALL);
                } else {
                    setSelectAllText(UNSELECT_ALL);
                }

                for (java.awt.Component parent : applicationsPanel.getComponents()) {
                    if (parent instanceof JPanel) {
                        for (java.awt.Component child : ((JPanel) parent).getComponents()) {
                            if (child instanceof JCheckBox) {
                                if (isAllSelected) {
                                    ((JCheckBox) child).setSelected(false);
                                } else {
                                    ((JCheckBox) child).setSelected(true);
                                }
                            }
                        }
                    }

                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
    }

    private boolean showSetupModal() {

        configParentPanel = new JPanel(new MigLayout());
        configParentPanel.add(new JLabel("<html>ProtAnnot uses the free InterProScan Web service hosted<br />"
                + "at the European Bioinformatics Institute (EBI) to search for<br />"
                + "domains and motifs in your protein sequences.<br />"
                + "To get started, select InterProScan resources to search:<html>"), "wrap");
        initEmail();
        JPanel emailPanel = new JPanel(new MigLayout());
        emailPanel.add(new JLabel("Email:"));
        emailPanel.add(email, "width :300:");
        configParentPanel.add(emailPanel, "wrap");
        configParentPanel.add(new JLabel("The InterProScan Web service requires an email address."), "wrap");
        initSelectAll();
        configParentPanel.add(selectAllLabel, "wrap");
        if (!showApplicationOptionsLoadingModal()) {
            return false;
        }
        JPanel linkPanel = new JPanel(new MigLayout());
        linkPanel.add(new JLabel("For more information,"), "left");
        JLabel hyperlink = new JLabel("<html><a href='#'>visit the InterPro Web page at EBI</a>.</html>");
        hyperlink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        hyperlink.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(new URI("http://www.ebi.ac.uk/interpro/about.html"));
                    } catch (IOException | URISyntaxException ex) {
                        LOG.error("Error navigating to hyperlink in about IGB window", ex);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        linkPanel.add(hyperlink, "left");
        configParentPanel.add(linkPanel);
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
            inputAppl.clear();
            for (java.awt.Component parent : applicationsPanel.getComponents()) {
                if (parent instanceof JPanel) {
                    for (java.awt.Component child : ((JPanel) parent).getComponents()) {
                        if (child instanceof JCheckBox) {
                            if (((JCheckBox) child).isSelected()) {
                                String value = ((JCheckBox) child).getName();
                                inputAppl.add(value);
                            }
                        }
                    }
                }
            }
            matcher = pattern.matcher(email.getText());
            if (!matcher.matches()) {
                ModalUtils.infoPanel("To run a search, enter an email address.");
                return false;
            }
            protAnnotPreferencesNode.put(PreferenceUtils.PROTANNOT_IPS_EMAIL, email.getText());
            return true;
        }
        return false;
    }

    public void asyncLoadSequence(Callback callback, GenomeView gview) {
        this.gview = gview;
        interProScanRunning = true;
        eventBus.post(new StatusStartEvent(id));
        if (showSetupModal()) {
            resultFetchTimer = new Timer();
            loadResultsWorker = new CThreadWorker<Void, Void>("Loading InterProScan") {
                @Override
                protected Void runInBackground() {
                    try {
                        loadSequence(callback, gview);
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                    return null;
                }

                @Override
                public boolean cancelThread(boolean b) {
                    try {
                        if (resultFetchTimer != null) {
                            resultFetchTimer.cancel();
                        }
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                    return true;
                }

                @Override
                protected void finished() {

                }
            };
            CThreadHolder.getInstance().execute(this, loadResultsWorker);
            showResultLoadingModal(gview);
        } else {
            interProScanRunning = false;
            eventBus.post(new StatusTerminateEvent(id));
        }
    }

    private JobRequest createJobRequest() {
        JobRequest request = new JobRequest();
        request.setEmail(email.getText());

        request.setSignatureMethods(Optional.of(inputAppl));
        request.setTitle(Optional.empty());
        request.setGoterms(Optional.empty());
        request.setPathways(Optional.empty());
        for (Object obj : getDnaseq().getMRNAAndAaseq()) {
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

    private void processJobResults(final List<Job> jobs, Callback callback) {
        Dnaseq original = getDnaseq();
        Iterator it = original.getMRNAAndAaseq().iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof Dnaseq.Aaseq) {
                it.remove();
            }
        }

        jobs.stream().filter(job -> job.getStatus().equals(Status.FINISHED)).forEach(job -> {
            Optional<Document> doc = interProscanService.result(job.getId());
            if (doc.isPresent()) {
                Dnaseq.Aaseq aaseq = interProscanTranslator.translateFromResultDocumentToModel(job.getSequenceName(), doc.get());
                original.getMRNAAndAaseq().add(aaseq);
            }
        });
        callback.execute(original);
        dialog.dispose();
        resultFetchTimer.cancel();
    }

    private TimerTask buildTimerTask(final List<Job> jobs, Callback callback, GenomeView gview) {

        return new TimerTask() {

            @Override
            public void run() {

                int failed = (int) jobs.stream().filter(job -> (!job.getStatus().equals(Status.RUNNING)
                        && !job.getStatus().equals(Status.FINISHED))).count();;
                int successful = (int) jobs.stream().filter(job -> job.getStatus().equals(Status.FINISHED)).count();;
                int running = (int) jobs.stream().filter(job -> job.getStatus().equals(Status.RUNNING)).count();
                for (int i = 0; i < jobs.size(); i++) {
                    Job job = jobs.get(i);
                    Status status = job.getStatus();
                    if (!status.equals(Status.RUNNING)) {
                        continue;
                    }
                    status = interProscanService.status(job.getId());
                    job.setStatus(status);
                    LOG.info(job.getId() + " " + status.toString());

                    if (status.equals(Status.RUNNING)) {
                        //running++;
                    } else if (status.equals(Status.FINISHED)) {
                        running--;
                        successful++;
                    } else {
                        running--;
                        failed++;
                    }
                    initStatusLabel(running + " Running, " + successful + " Successful, " + failed + " Failed ");
                    gview.getIpsTableModel().updateModel(jobs);
                }
                if (anyJobRunning(jobs)) {
                    initStatusLabel(running + " Running, " + successful + " Successful, " + failed + " Failed ");
                } else {
                    initStatusLabel("Fetching results from InterProscan");
                }
                gview.getIpsTableModel().updateModel(jobs);
                if (!anyJobRunning(jobs)) {
                    processJobResults(jobs, callback);
                    interProScanRunning = false;
                    eventBus.post(new StatusTerminateEvent(id));
                }
            }
        };
    }

    private boolean anyJobRunning(final List<Job> jobs) {
        for (Job job : jobs) {
            if (job.getStatus().equals(Status.RUNNING)) {
                return true;
            }
        }
        return false;
    }

    public void loadSequence(Callback callback, GenomeView gview) {
        JobRequest jobRequest = createJobRequest();
        final List<Job> jobs = interProscanService.run(jobRequest);
        if (LOG.isDebugEnabled()) {
            jobs.stream().forEach((job) -> {
                LOG.debug(job.getId());
            });
        }

        resultFetchTimer.schedule(buildTimerTask(jobs, callback, gview), new Date(), 1000);

    }

    public void exportAsXml(Component component) {
        JFileChooser chooser = new UniFileChooser("PAXML File", "paxml");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.rescanCurrentDirectory();
        int option = chooser.showSaveDialog(component);
        if (option == JFileChooser.APPROVE_OPTION) {
            File exportFile = chooser.getSelectedFile();
            Dnaseq dnaseq = getDnaseq();
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
