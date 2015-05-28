package com.lorainelab.das.parser;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.parsers.Parser;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sgblanch
 * @version $Id: DASFeatureParser.java 11467 2012-05-08 20:44:01Z hiralv $
 */
public final class DASFeatureParser implements Parser {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DASFeatureParser.class);

    static enum Orientation {

        UNKNOWN, FORWARD, REVERSE
    }

    private static enum Elements {

        DASGFF, GFF, SEGMENT, FEATURE, TYPE, METHOD, START, END, SCORE, ORIENTATION, PHASE, NOTE, LINK, TARGET, GROUP
    }

    private static enum Attr {

        version, href, id, start, stop, type, label, category, reference
    }

    private BioSeq sequence;
    private String note;
    private boolean annotateSeq = true;

    public void setAnnotateSeq(boolean annotateSeq) {
        this.annotateSeq = annotateSeq;
    }

    public List<DASSymmetry> parse(InputStream s, GenomeVersion genomeVersion) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(s);
        XMLEvent current;
        Deque<StartElement> stack = new ArrayDeque<>();
        FeatureBean feature = new FeatureBean();
        LinkBean link = new LinkBean();
        GroupBean group = new GroupBean();
        TargetBean target = new TargetBean();
        Map<String, DASSymmetry> groupMap = new HashMap<>();
        try {
            while (reader.hasNext() && !Thread.currentThread().isInterrupted()) {
                current = reader.nextEvent();
                switch (current.getEventType()) {
                    case XMLEvent.START_ELEMENT:
                        startElement(current.asStartElement(), feature, link, group, target, genomeVersion);
                        stack.push(current.asStartElement());
                        break;
                    case XMLEvent.CHARACTERS:
                        characters(current.asCharacters(), stack.peek(), feature, link, target);
                        break;
                    case XMLEvent.END_ELEMENT:
                        stack.pop();
                        endElement(current.asEndElement(), stack.peek(), groupMap, feature, link, group, target, genomeVersion);
                        break;
                }
            }
        } catch (XMLStreamException ex) {
            logger.warn("Invalid XML received from the Das server, unable to parse invalid xml. IGB will show this region as empty.", ex);
            return Collections.<DASSymmetry>emptyList();
        }
        if (Thread.currentThread().isInterrupted()) {
            return Collections.<DASSymmetry>emptyList();
        }

        return new ArrayList<>(groupMap.values());
    }

    /**
     * Handle an XML start tag. This creates various storage beans when
     * necessary and stores XML attributes in the appropriate bean.
     *
     * @param current
     * @param genomeVersion
     */
    private void startElement(StartElement current, FeatureBean feature, LinkBean link, GroupBean group, final TargetBean target, GenomeVersion genomeVersion) {
        switch (Elements.valueOf(current.getName().getLocalPart())) {
            case SEGMENT:
                sequence = genomeVersion.addSeq(getAttribute(current, Attr.id),
                        Integer.valueOf(getAttribute(current, Attr.stop)));
                break;
            case FEATURE:
                feature.clear();
                feature.setID(getAttribute(current, Attr.id));
                feature.setLabel(getAttribute(current, Attr.label));
                break;
            case TYPE:
                feature.setTypeID(getAttribute(current, Attr.id));
                feature.setTypeCategory(getAttribute(current, Attr.category));
                feature.setTypeReference(getAttribute(current, Attr.reference));
                break;
            case METHOD:
                feature.setMethodID(getAttribute(current, Attr.id));
                break;
            case LINK:
                link.clear();
                link.setURL(getAttribute(current, Attr.href));
                break;
            case TARGET:
                target.clear();
                target.setID(getAttribute(current, Attr.id));
                target.setStart(getAttribute(current, Attr.start));
                target.setStop(getAttribute(current, Attr.stop));
                break;
            case GROUP:
                group.clear();
                group.setID(getAttribute(current, Attr.id));
                group.setLabel(getAttribute(current, Attr.label));
                group.setType(getAttribute(current, Attr.type));
        }
    }

    /**
     * Handle XML character data. This stores the data in the correct bean,
     * depending on what tag we are processing.
     *
     * @param current
     * @param parent
     */
    private void characters(Characters current, StartElement parent, FeatureBean feature, LinkBean link, TargetBean target) {
        switch (Elements.valueOf(parent.getName().getLocalPart())) {
            case TYPE:
                feature.setTypeLabel(current.getData());
                break;
            case METHOD:
                feature.setMethodLabel(current.getData());
                break;
            case START:
                feature.setStart(current.getData());
                break;
            case END:
                feature.setEnd(current.getData());
                break;
            case SCORE:
                feature.setScore(current.getData());
                break;
            case ORIENTATION:
                feature.setOrientation(current.getData());
                break;
            case PHASE:
                feature.setPhase(current.getData());
                break;
            case NOTE:
                note = current.getData();
                break;
            case LINK:
                link.setTitle(current.getData());
                break;
            case TARGET:
                target.setName(current.getData());
                break;
        }
    }

    /**
     * Handle an XML end tag. This stores certain child beans in their parent
     * beans. It will also create a SeqSymmetry when finished with a feature
     * tag.
     *
     * @param current
     * @param parent
     */
    private void endElement(EndElement current, StartElement parent, Map<String, DASSymmetry> groupMap, FeatureBean feature, LinkBean link, GroupBean group, TargetBean target, GenomeVersion genomeVersion) {
        Elements p = null;
        if (parent != null) {
            p = Elements.valueOf(parent.getName().getLocalPart());
        }
        switch (Elements.valueOf(current.getName().getLocalPart())) {
            case FEATURE:
                DASSymmetry groupSymmetry;
                DASSymmetry featureSymmetry = new DASSymmetry(feature, sequence);

                if (feature.getGroups().isEmpty()) {
                    if (annotateSeq) {
                        sequence.addAnnotation(featureSymmetry);
                    }
                    groupMap.put(featureSymmetry.getID(), featureSymmetry);
                } else {
                    for (GroupBean groupBean : feature.getGroups()) {
                        groupSymmetry = getGroupSymmetry(groupMap, feature, groupBean, genomeVersion);
                        groupSymmetry.addChild(featureSymmetry);
                    }
                }
                break;
            case NOTE:
                if (p == Elements.FEATURE) {
                    feature.addNote(note);
                } else if (p == Elements.GROUP) {
                    group.addNote(note);
                }
                break;
            case LINK:
                if (p == Elements.FEATURE) {
                    feature.addLink(link);
                } else if (p == Elements.GROUP) {
                    group.addLink(link);
                }
                break;
            case TARGET:
                if (p == Elements.FEATURE) {
                    feature.addTarget(target);
                } else if (p == Elements.GROUP) {
                    group.addTarget(target);
                }
                break;
            case GROUP:
                feature.addGroup(group);
                break;
        }
    }

    private static String getAttribute(StartElement current, Attr attr) {
        QName qName = new QName(current.getName().getNamespaceURI(), attr.toString());
        Attribute attribute = current.getAttributeByName(qName);
        return attribute == null ? "" : attribute.getValue();
    }

    private DASSymmetry getGroupSymmetry(Map<String, DASSymmetry> groupMap, FeatureBean feature, GroupBean group, GenomeVersion genomeVersion) {
        /* Do we have a groupSymmetry for ID stored in parser */
        if (groupMap.containsKey(group.getID())) {
            return groupMap.get(group.getID());
        }

        /* Is there a groupSymmetry for ID on this sequence */
        for (SeqSymmetry sym : genomeVersion.findSyms(group.getID())) {
            if (sym instanceof DASSymmetry && sym.getSpan(sequence) != null) {
                groupMap.put(sym.getID(), (DASSymmetry) sym);
                return (DASSymmetry) sym;
            }
        }

        /* Create a new groupSymmetry for ID */
        DASSymmetry groupSymmetry = new DASSymmetry(group, feature, sequence);
        if (annotateSeq) {
            sequence.addAnnotation(groupSymmetry);
        }
//		genomeVersion.addToIndex(groupSymmetry.getName(), groupSymmetry);
        groupMap.put(groupSymmetry.getID(), groupSymmetry);

        return groupSymmetry;
    }

    @Override
    public List<? extends SeqSymmetry> parse(InputStream is,
            GenomeVersion genomeVersion, String nameType, String uri,
            boolean annotate_seq) throws Exception {
        setAnnotateSeq(annotate_seq);
        try {
            return parse(is, genomeVersion);
        } catch (XMLStreamException ex) {
            Logger.getLogger(DASFeatureParser.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
