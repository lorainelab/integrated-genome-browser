package com.affymetrix.igb.survey;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;

/**
 *
 * @author hiralv
 */
public class Activator extends ServiceRegistrar implements BundleActivator {

	@Override
	protected ServiceRegistration<?>[] registerService(IGBService igbService) throws Exception {
		ResourceBundle BUNDLE = ResourceBundle.getBundle("survey");
		
		InputStream inputStream = null;
		//inputStream = Activator.class.getResourceAsStream("/surveys.xml");
		//inputStream = LocalUrlCacher.getInputStream(BUNDLE.getString("surveys"));
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
				if (today.compareTo(survey.getStart()) >= 0 && 
					today.compareTo(survey.getEnd()) < 0  && 
					!PreferenceUtils.getBooleanParam(survey.getId(), false)) {
					ShowSurvey.show(survey);
					break;
				}
			}
		}
		return null;
	}
}