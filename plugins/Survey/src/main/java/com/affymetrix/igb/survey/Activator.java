package com.affymetrix.igb.survey;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.igb.swing.JRPMenu;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;
import static com.affymetrix.igb.survey.ShowSurvey.*;

/**
 *
 * @author hiralv
 */
public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, IGBService igbService) throws Exception {
        ResourceBundle BUNDLE = ResourceBundle.getBundle("survey");
        JRPMenu surveysMenu = new JRPMenu("Survey_surveysMenu", "News and Surveys");

        InputStream inputStream = null;
        //inputStream = Activator.class.getResourceAsStream("/surveys.xml");
        inputStream = LocalUrlCacher.getInputStream(BUNDLE.getString("surveys"));
        if (inputStream != null) {
            List<Survey> surveys = SurveyParser.parse(inputStream);
            GeneralUtils.safeClose(inputStream);

            Collections.sort(surveys, new Comparator<Survey>() {
                public int compare(Survey o1, Survey o2) {
                    return o1.getEnd().compareTo(o2.getEnd());
                }
            });

            Date today = Calendar.getInstance().getTime();
            for (final Survey survey : surveys) {
                if (today.compareTo(survey.getStart()) >= 0
                        && today.compareTo(survey.getEnd()) < 0) {
                    JMenuItem item = new JMenuItem(
                            new GenericAction(survey.getName(), null, null) {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    super.actionPerformed(e);
                                    GeneralUtils.browse(survey.getLink());
                                }
                            }
                    );
                    surveysMenu.add(item);
                }
            }
            for (final Survey survey : surveys) {
                if (today.compareTo(survey.getStart()) >= 0
                        && today.compareTo(survey.getEnd()) < 0
                        && !PreferenceUtils.getBooleanParam(survey.getId(), false)) {

                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            showSurvey(survey);
                        }
                    }, 10000);

                    break;
                }
            }
        }

        if (surveysMenu.getItemCount() > 0) {
            return new ServiceRegistration<?>[]{bundleContext.registerService(AMenuItem.class, new AMenuItem(surveysMenu, "help"), null)};
        }

        return null;
    }
}
