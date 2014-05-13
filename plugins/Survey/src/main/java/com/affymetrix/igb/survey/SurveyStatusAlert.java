package com.affymetrix.igb.survey;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.StatusAlert;
import javax.swing.Icon;

import static com.affymetrix.igb.survey.ShowSurvey.*;

/**
 *
 * @author hiralv
 */
public class SurveyStatusAlert implements StatusAlert {

    private static final String TOOLTIP = "A new survey is available";
    private static final String DISPLAYMESSAGE = "Survey Pending";
    private static final String ICONPATH = "16x16/actions/stop_hex.gif";
    private final Survey survey;

    public SurveyStatusAlert(Survey survey) {
        this.survey = survey;
    }

    public Icon getIcon() {
        return CommonUtils.getInstance().getIcon(ICONPATH);
    }

    public String getDisplayMessage() {
        return DISPLAYMESSAGE;
    }

    public String getToolTip() {
        return TOOLTIP;
    }

    public int actionPerformed() {
        return showSurvey(survey) ? StatusAlert.HIDE_ALERT : StatusAlert.KEEP_ALERT;
    }
}
