import com.opencsv.CSVWriter;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class XMLPatentsToCSV implements CSV_Writable {

    private PatentHelper.PatentFieldName recentElement = null;

    /*
     * Hierbei handelt es sich nicht um die Patentnummer, sondern um eine Nummer
     * ohne die letzten beiden Ziffern, also z.B. ohne A2, B3 oder A1
     */
    private String documentNumber;

    private Reader infileReader;
    private CSVWriter csvWriter;
    private HashMap<String, String> csvHash;
    private HashMap<String, HashMap<String, String>> descriptionOrClaim;
    private StringBuilder resultString;

    // die Attribute der description oder claim tags
    private String patentNum = null;
    private String language = null;

    public XMLPatentsToCSV(Reader ifr) {
        this.infileReader = ifr;
        this.csvHash = new HashMap<String, String>();
        this.resultString = new StringBuilder("");
        this.descriptionOrClaim = new HashMap<>();
    }

    // entry point of class
    public void writeCSV(CSVWriter wr) {
        this.csvWriter = wr;
        convertXMLToCSV();
    }

    private void convertXMLToCSV() {

        XMLInputFactory xmlFac = XMLInputFactory.newInstance();

        try {

            XMLEventReader xmlEventReader = xmlFac.createXMLEventReader(infileReader);

            while (xmlEventReader.hasNext()) {

                XMLEvent event = xmlEventReader.nextEvent();
                switch (event.getEventType()) {

                    case XMLStreamConstants.START_DOCUMENT:
                        break;

                    case XMLStreamConstants.START_ELEMENT:

                        // this.setFlag(event.asStartElement().getName().getLocalPart());
                        this.setFlag(event.asStartElement());
                        break;

                    case XMLStreamConstants.CHARACTERS:

                        if (this.recentElement != null) {
                            this.resultString.append(event.asCharacters().getData());
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:

                        String endElement = event.asEndElement().getName().getLocalPart();
                        if (endElement == "HitlistXMLExportTable") {

                            /* TODO:
                             * 1. Inhalt aus csvHash ins String[] umwandeln 2. Inhalt von String[] auf wr
                             * schreiben 3 Aufräumen
                             */

                            /*
                             * writeOutput(endElement);
                             *
                             * // clean up
                             *
                             * this.csvHash.clear(); this.resultString.setLength(0); this.recentElement =
                             * null;
                             */

                        }

                        writeOutput(endElement);
                        break;

                    case XMLStreamConstants.END_DOCUMENT:
                        /*
                         * Nüscht machen
                         */
                        break;
                }
            }

        } catch (XMLStreamException e1) {
            e1.printStackTrace();
        }
    }

    private void writeOutput(String endElement) {

        switch (endElement) {
            case "DocumentNumber":
                /*
                 * Hierbei handelt es sich nicht um die Patentnummer, sondern um eine Nummer
                 * ohne die letzten beiden Ziffern, also z.B. ohne A2, B3 oder A1;
                 */
                this.documentNumber = resultString.toString();
                this.checkPatentNumber(this.documentNumber);
                break;

            case "Title":
                String title = getTitle(recentElement.toString());
                csvHash.put(PatentHelper.PatentFieldName.TITLE.toString(), title);
                break;

            case "OfficalAbstract":
                String oAbstract = getAbstract(recentElement.toString());
                csvHash.put(PatentHelper.PatentFieldName.ABSTRACT.toString(), oAbstract);
                break;

            case "PriorityDate":
                LocalDate prioDate = PatentHelper.getDate(recentElement.toString());
                csvHash.put(PatentHelper.PatentFieldName.PRIORITY_DATE.toString(), prioDate.toString());
                break;

            case "LargestFamily":
                csvHash.put(PatentHelper.PatentFieldName.FAMILY.toString(), resultString.toString());
                break;

            // inneres Elemente
            case "description":
                this.setDescriptionOrClaim();
                break;

            // Äußeres Element
            case "Description":
                this.selectAndSetDescriptionOrClaim();
                break;

            case "claim":
                break;

            case "Claims":

                break;

            case "FAN":
                break;

            case "OS":
                break;

            case "IPC8_AN":
                break;

        }
    }

    private String checkPatentNumber(String documentNumber) {

        Matcher match = RegexPattern.PATENT_NUMBER().matcher(documentNumber);
        if (! match.matches()){
            String errorString = String.format("%s scheint eine komische Patentnummer zu sein!", documentNumber);
            System.err.println(errorString);
            return "";
        }


        return documentNumber;
    }

    private void selectAndSetDescriptionOrClaim() {

    }

    private void setDescriptionOrClaim() {

        /*
         HashMap<String, HashMap<String, String>> claims or describtion
         */
        HashMap<String, String> documentHash = new HashMap<>();
        documentHash.put("lang", language);
        documentHash.put("doc", RegexPattern.cleanInputString(recentElement.toString()));
        this.descriptionOrClaim.put(patentNum, documentHash);
        //Aufräumen
        this.patentNum = null;
        this.language = null;
    }

    private String getAbstract(String rawAbstract) {

        return StringEscapeUtils.unescapeXml(rawAbstract);
    }

    private String getTitle(String rawTitle) {

        /*
         * check for appropriate language of title
         */
        String[] titleList = rawTitle.split(Pattern.quote(";"));
        String[] languages = { "en", "de" };
        for (String lang : languages) {

            for (String raw : titleList) {
                String title = StringEscapeUtils.unescapeXml(raw);
                if (getLanguage(title) == lang) {
                    return title.toLowerCase();
                }
            }
        }
        return "";
    }

    private void setFlag(StartElement startElement) {

        switch (startElement.getName().getLocalPart()) {
            case "DocumentNumber":
                this.recentElement = PatentHelper.PatentFieldName.PATENT_NUMMER;
                break;

            case "Title":
                this.recentElement = PatentHelper.PatentFieldName.TITLE;
                break;

            case "OfficalAbstract":
                this.recentElement = PatentHelper.PatentFieldName.ABSTRACT;
                break;

            case "PriorityDate":
                this.recentElement = PatentHelper.PatentFieldName.PRIORITY_DATE;
                break;

            case "LargestFamily":
                this.recentElement = PatentHelper.PatentFieldName.FAMILY;
                break;

            case "description":
                this.recentElement = PatentHelper.PatentFieldName.DESCRIPTION;
                this.setPatNrAndLang(startElement);
                break;

            case "claim":
                this.recentElement = PatentHelper.PatentFieldName.CLAIMS;
                break;

            case "FAN":
                this.recentElement = PatentHelper.PatentFieldName.ANMELDER_PERSON;
                break;

            case "OS":
                this.recentElement = PatentHelper.PatentFieldName.ANMELDER_FIRMA;
                break;

            case "IPC8_AN":
                this.recentElement = PatentHelper.PatentFieldName.CPC;
                break;
            default:
                // kein Element gefunden
                this.recentElement = null;
                break;

        }

    }

    private void setPatNrAndLang(StartElement startElement) {

        if ((this.patentNum == null) && (this.language == null)) {

            // TODO: check PatentNumber consitency
            Iterator iter = startElement.getAttributes();
            while (iter.hasNext()) {
                Attribute attr = (Attribute) iter.next();
                switch (attr.getName().getLocalPart()) {
                    case "patentNr":
                        this.patentNum = attr.getValue();
                        break;
                    case "lang":
                        this.language = attr.getValue();
                        break;
                }
            }
        }
    }

    private String getLanguage(String doc) {
        LanguageDetector langDetector = new OptimaizeLangDetector().loadModels();
        LanguageResult result = langDetector.detect(doc);
        return result.getLanguage();
    }
}
