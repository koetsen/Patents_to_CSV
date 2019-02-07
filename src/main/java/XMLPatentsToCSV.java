import com.opencsv.CSVWriter;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.*;

import java.util.HashMap;
import java.util.regex.Pattern;


public class XMLPatentsToCSV implements CSV_Writable {

    private PatentHelper.PatentFieldName recentElement = null;

    /* Hierbei handelt es sich nicht um die Patentnummer, sondern um eine Nummer
    ohne die letzten beiden Ziffern, also z.B. ohne A2, B3 oder A1 */
    private String documentNumber;

    private Reader infileReader;
    private CSVWriter csvWriter;
    private HashMap<String, String> csvHash;
    private StringBuilder resultString;

    public XMLPatentsToCSV(Reader ifr) {
        this.infileReader = ifr;
        this.csvHash = new HashMap<String, String>();
        this.resultString = new StringBuilder("");
    }

    //entry point of class
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

                        this.setFlag(event.asStartElement().getName().getLocalPart());
                        break;

                    case XMLStreamConstants.CHARACTERS:

                        if (this.recentElement != null) {
                            this.resultString.append(event.asCharacters().getData());
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:

                        String endElement = event.asEndElement().getName().getLocalPart();
                        if (endElement == "HitlistXMLExportTable") {
                            writeOutput(endElement);

                            //clean up
                            this.csvHash.clear();
                            this.resultString.setLength(0);
                            this.recentElement = null;

                        }
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
                Hierbei handelt es sich nicht um die Patentnummer, sondern um eine Nummer
                ohne die letzten beiden Ziffern, also z.B. ohne A2, B3 oder A1;
                 */
                this.documentNumber = resultString.toString();
                break;

            case "Title":
                String title = getTitle(recentElement.toString());
                csvHash.put(PatentHelper.PatentFieldName.TITLE.toString(), title);
                break;

            case "OfficalAbstract":
                
                break;

            case "PriorityDate":
                break;

            case "LargestFamily":
                break;

            case "description":
                break;

            case "claim":
                break;

            case "FAN":
                break;

            case "OS":
                break;

            case "IPC8_AN":
                break;

        }
    }

    private String getTitle(String rawTitle) {

        /*
        check for appropriate language of title
         */
        String[] titleList = rawTitle.split(Pattern.quote(";"));
        String[] languages = {"en", "de"};
        for (String lang : languages) {

            for (String title : titleList) {
                if (getLanguage(title) == lang) {
                    return title.toLowerCase();
                }
            }
        }
        return "";
    }

    private void setFlag(String startElm) {

        switch (startElm) {
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

    private String getLanguage(String doc) {
        LanguageDetector langDetector = new OptimaizeLangDetector().loadModels();
        LanguageResult result = langDetector.detect(doc);
        return result.getLanguage();
    }

}
