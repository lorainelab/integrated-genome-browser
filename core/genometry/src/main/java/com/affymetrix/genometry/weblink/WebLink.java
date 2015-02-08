package com.affymetrix.genometry.weblink;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.google.common.base.Strings;
import com.lorainelab.igb.preferences.model.AnnotationUrl;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @version $Id: WebLink.java 11425 2012-05-04 15:54:04Z lfrohman $
 */
public final class WebLink implements Comparable<WebLink> {

    public static final String LOCAL = "local";
    private static final String separator = System.getProperty("line.separator");
    private static final Pattern DOUBLE_DOLLAR_PATTERN = Pattern.compile("[$][$]");	//A pattern that matches the string "$$"
    private static final Pattern DOLLAR_GENOME_PATTERN = Pattern.compile("[$][:]genome[:][$]");	// A pattern that matches the string "$:genome:$"

    public enum RegexType {

        ANNOTATION_NAME, ANNOTATION_ID
    }

    private String url;
    private String name;
    private String species;
    private String id_field_name;
    private String original_regex;
    private String type; // server or local source
    private RegexType regexType = RegexType.ANNOTATION_NAME;
    private String imageIconPath;
    private Pattern pattern;

    public WebLink() {
    }

    public WebLink(AnnotationUrl annotationUrl) {
        name = annotationUrl.getName();
        url = annotationUrl.getUrl();
        id_field_name = annotationUrl.getIdField();
        type = annotationUrl.getType();
        species = annotationUrl.getSpecies();
        imageIconPath = annotationUrl.getImageIconPath();
        if (Strings.isNullOrEmpty(annotationUrl.getAnnotTypeRegex())) {
            regexType = WebLink.RegexType.ANNOTATION_ID;
            setRegex(annotationUrl.getAnnotIdRegex());
        } else {
            setRegex(annotationUrl.getAnnotTypeRegex());
        }
    }

    WebLink(String name, String regex, String url, RegexType regexType) throws PatternSyntaxException {
        setName(name);
        setRegex(regex);
        setUrl(url);
        setRegexType(regexType);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        if (name == null || "null".equals(name)) {
            this.name = "";
        } else {
            this.name = name;
        }
    }

    public void setIDField(String IDField) {
        this.id_field_name = IDField;
    }

    public String getIDField() {
        return this.id_field_name;
    }

    public String getSpeciesName() {
        return this.species;
    }

    public void setSpeciesName(String name) {
        if (name == null || "null".equals(name)) {
            this.species = "";
        } else {
            this.species = name;
        }
    }

    public String getRegex() {
        return original_regex;
    }

    /**
     * Sets the regular expression that must be matched. The special value
     * <b>null</b> is also allowed, and matches every String. If the Regex does
     * not begin with "(?i)", then this will be pre-pended automatically to
     * generate a case-insensitive pattern. If you want a case-sensitive
     * pattern, start your regex with "(?-i)" and this will cancel-out the
     * effect of the "(?i)" flag.
     */
    public void setRegex(String regex) throws PatternSyntaxException {
        if (regex == null || ".*".equals(regex) || "(?i).*".equals(regex)) {
            pattern = null;
            original_regex = regex;
            return;
        }

        // delete any double, triple, etc., "(?i)" strings caused by a bug in a previous version
        while (regex.startsWith("(?i)(?i)")) {
            regex = regex.substring(4);
        }
        if (!regex.startsWith("(?i)") && !type.equals(LOCAL)) {
            regex = "(?i)" + regex; // force all server web link matches to be case-insensitive
        }
        original_regex = regex;
        pattern = Pattern.compile(regex);

    }

    /**
     * Return the compiled form of the regular expression.
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Returns the URL (or URL pattern) associated with this WebLink. If the URL
     * pattern contains any "$$" characters, those should be replaced with
     * URL-Encoded annotation IDs to get the final URL. Better to use
     * {@link #getURLForSym(SeqSymmetry)}.
     */
    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRegexType(RegexType regexType) {
        this.regexType = regexType;
    }

    public RegexType getRegexType() {
        return this.regexType;
    }

    public boolean matches(String s) {
        return (pattern == null
                || pattern.matcher(s).matches());
    }

    /**
     * replace all "$$" in the url pattern with the given id, URLEncoded
     *
     * @param url
     * @param id
     * @return url
     */
    public static String replacePlaceholderWithId(String url, String id) {
        if (url == null || id == null) {
            return url;
        }
        String encoded_id = "";
        try {
            encoded_id = URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WebLink.class.getName()).log(Level.SEVERE, null, ex);
        }
        url = DOUBLE_DOLLAR_PATTERN.matcher(url).replaceAll(encoded_id);

        return url;
    }

    /**
     * replace all "$:genome:$" in the url pattern with the current seqGroup id,
     * URLEncoded
     *
     * @param url
     * @return url
     */
    public static String replaceGenomeId(String url) {
        if (url == null) {
            return url;
        }
        GenometryModel gmodel = GenometryModel.getInstance();
        AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
        if (group != null) {
            String encoded_id = "";
            try {
                encoded_id = URLEncoder.encode(group.getID(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(WebLink.class.getName()).log(Level.SEVERE, null, ex);
            }
            url = DOLLAR_GENOME_PATTERN.matcher(url).replaceAll(encoded_id);
        }

        return url;
    }

    public String getURLForSym(SeqSymmetry sym) {
        String url2 = getURLForSym_(sym);
        return replaceGenomeId(url2);
    }

    private String getURLForSym_(SeqSymmetry sym) {
        // Currently this just replaces any "$$" with the ANNOTATION_ID, but it could
        // do something more sophisticated later, like replace "$$" with
        // some other sym property.
        if (id_field_name == null || id_field_name.trim().length() <= 0) {
            return replacePlaceholderWithId(getUrl(), sym.getID());
        }

        Object field_value = null;
        if (sym instanceof SymWithProps) {
            field_value = ((SymWithProps) sym).getProperty(id_field_name);
        }

        if (field_value == null) {
            if (id_field_name != null && id_field_name.trim().length() > 0) {
                Logger.getLogger(WebLink.class.getName()).log(Level.WARNING,
                        "Selected item has no value for property ''{0}'' which is needed to construct the web link.", id_field_name);
            }
            return replacePlaceholderWithId(getUrl(), "");
        }
        return replacePlaceholderWithId(getUrl(), field_value.toString());
    }

    public String getImageIconPath() {
        return imageIconPath;
    }

    public void setImageIconPath(String imageIconPath) {
        this.imageIconPath = imageIconPath;
    }

    @Override
    public String toString() {
        return "WebLink: name=" + name
                + ", regex=" + getRegex()
                + ", regexType=" + this.regexType.toString()
                + ", url=" + url
                + ", id_field_name=" + id_field_name
                + ", image_icon_path=" + imageIconPath;
    }

    /**
     * Used to compute the hashCode and in the equals() method.
     */
    private String toComparisonString() {
        // Do NOT consider the "name" in tests of equality.
        // We do not want to allow two links that are identical except for name.
        // This is important in allowing users to over-ride the default links.
        return original_regex + ", " + url.toString() + ", " + id_field_name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WebLink)) {
            return false;
        }
        WebLink w = (WebLink) o;
        return toComparisonString().equals(w.toComparisonString());
    }

    @Override
    public int hashCode() {
        return toComparisonString().hashCode();
    }

    @Override
    public int compareTo(WebLink link) {
        return (sortString(this).compareTo(sortString(link)));
    }

    private static String sortString(WebLink wl) {
        return wl.getName() + ", " + wl.getRegex() + ", " + wl.getUrl() + ", " + wl.getIDField();
    }

}
