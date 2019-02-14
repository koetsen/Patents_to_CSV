import com.opencsv.CSVWriter;
import org.apache.commons.text.StringEscapeUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class XMLPatentsToCSV implements CSV_Writable {


    // private static LanguageDetector langDetector = new OptimaizeLangDetector().loadModels();
    private Reader infileReader;
    private CSVWriter csvWriter;
    // diese Elemente nach "HitlistXMLExportTable" leeren
    private PatentHelper.PatentFieldName recentElement = null;

    /*
     * Hierbei handelt es sich nicht um die Patentnummer, sondern um eine Nummer
     * ohne die letzten beiden Ziffern, also z.B. ohne A2, B3 oder A1
     */
    private String documentNumber;
    private HashMap<String, String> csvHash;
    private HashMap<String, HashMap<String, String>> descriptionOrClaim;
    private StringBuilder resultString;
    private String selectedPatentNum = "";

    // die Attribute der description oder claim tags
    private String patentNum = "";
    private String tmpLanguage = "";

    //language actually used
    private String langOfDocumentUsed = "";
    //private PatentHelper.Pat entFieldNamerecentElement;
    // bis hier alles löschen

    public XMLPatentsToCSV(Reader ifr) {
        this.infileReader = ifr;
        this.csvHash = new HashMap<>();
        this.resultString = new StringBuilder();
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
                        this.setFlag(event.asStartElement());
                        break;

                    case XMLStreamConstants.CHARACTERS:

                        if (this.recentElement != null) {
                            this.resultString.append(event.asCharacters().getData());
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:

                        String endElement = event.asEndElement().getName().getLocalPart();
                        if (endElement.equals("HitlistXMLExportTable")) {

                            String[] outArray = PatentHelper.getArrayForOutput(this.csvHash);
                            csvWriter.writeNext(outArray);
                            this.clearUpAll();
                            break;

                        }
                        writeResultStringToCSVHash(endElement);
                        this.resultString.setLength(0);
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

    private void clearUpAll() {

        this.recentElement = null;
        this.documentNumber = "";
        this.csvHash.clear();
        this.descriptionOrClaim.clear();
        this.resultString.setLength(0);
        this.selectedPatentNum = "";
        this.patentNum = "";
        this.tmpLanguage = "";
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
                this.setPatNrAndLang(startElement);
                break;

            case "FAN":
                this.recentElement = PatentHelper.PatentFieldName.FAMILY;
                break;

            case "OS":
                this.recentElement = PatentHelper.PatentFieldName.ANMELDER_PERSON;
                break;

            case "SO":
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

        if ((this.patentNum.isEmpty()) && (this.tmpLanguage.isEmpty())) {

            Iterator iter = startElement.getAttributes();
            while (iter.hasNext()) {

                Attribute attr = (Attribute) iter.next();
                switch (attr.getName().getLocalPart()) {
                    case "patentNr":
                        if (!PatentHelper.isPatentNumConsistent(attr.getValue())) {
                            System.err.print("Method setPatNrAndLang: ");
                            PatentHelper.outputErrorPatentNum(attr.getValue());
                        }
                        this.patentNum = attr.getValue();
                        break;
                    case "lang":
                        this.tmpLanguage = attr.getValue();
                        break;
                }
            }
        }
    }


    private void writeResultStringToCSVHash(String endElement) {

        switch (endElement) {
            case "DocumentNumber":
                /*
                 * Hierbei handelt es sich nicht um die Patentnummer, sondern um eine Nummer
                 * ohne die letzten beiden Ziffern, also z.B. ohne A2, B3 oder A1;
                 */
                this.documentNumber = resultString.toString();
                if (!PatentHelper.isPatentNumConsistent(this.documentNumber)) {
                    System.err.print("Method EndElement DocumentNumber ");
                    PatentHelper.outputErrorPatentNum(this.documentNumber);
                }
                System.out.println("Working on: " + this.documentNumber);
                break;

            case "Title":
                String title = getTitle(resultString.toString());
                csvHash.put(PatentHelper.PatentFieldName.TITLE.toString(), title);
                break;

            case "OfficalAbstract":
                String oAbstract = getAbstract(resultString.toString());
                csvHash.put(PatentHelper.PatentFieldName.ABSTRACT.toString(), oAbstract);
                break;

            case "PriorityDate":
                csvHash.put(PatentHelper.PatentFieldName.PRIORITY_DATE.toString(), PatentHelper.getDate(resultString.toString()));
                break;

            case "LargestFamily":
                csvHash.put(PatentHelper.PatentFieldName.FAMILY.toString(), resultString.toString().trim());
                break;

            // inneres Elemente
            case "description":
                this.saveDescriptionOrClaim();
                break;

            // Äußeres Element
            case "Description":
                this.selectDescriptionOrClaim(PatentHelper.PatentFieldName.DESCRIPTION);
                this.setPatentLang();
                this.setPatenNumber();
                this.cleanUpAfterDescrOrClaim();
                break;

            // inneres Elemente
            case "claim":
                this.saveDescriptionOrClaim();
                break;

            // Äußeres Element
            case "Claims":
                this.selectDescriptionOrClaim(PatentHelper.PatentFieldName.CLAIMS);
                this.setPatentLang();
                this.cleanUpAfterDescrOrClaim();
                break;

            case "FAN":
                this.setFamily(PatentHelper.PatentFieldName.FAMILY.toString());
                break;

            case "OS":
                this.csvHash.put(PatentHelper.PatentFieldName.ANMELDER_PERSON.toString(), this.resultString.toString());
                break;

            case "SO":
                this.csvHash.put(PatentHelper.PatentFieldName.ANMELDER_FIRMA.toString(), this.resultString.toString());
                break;

            case "IPC8_AN":
                this.csvHash.put(PatentHelper.PatentFieldName.CPC.toString(), this.resultString.toString());
                break;

        }
    }

    private void setPatentLang() {

        String langSet = this.csvHash.get(PatentHelper.PatentFieldName.PATENT_NUMMER.toString());
        if (langSet == null){
            this.csvHash.put(PatentHelper.PatentFieldName.PATENT_NUMMER.toString(), "");
        }
        if (!this.langOfDocumentUsed.isEmpty()){
            this.csvHash.put(PatentHelper.PatentFieldName.PATENT_NUMMER.toString(), this.langOfDocumentUsed);
        }

    }

    private void setPatenNumber() {

        if (!this.selectedPatentNum.isEmpty()) {
            csvHash.put(PatentHelper.PatentFieldName.PATENT_NUMMER.toString(), this.selectedPatentNum);
        } else if (!this.documentNumber.isEmpty()) {
            csvHash.put(PatentHelper.PatentFieldName.PATENT_NUMMER.toString(), this.documentNumber);
        } else {
            csvHash.put(PatentHelper.PatentFieldName.PATENT_NUMMER.toString(), "");
        }

    }

    private void setFamily(String fieldname) {

        String strForHash = resultString.toString().replaceAll(",", ";");
        this.csvHash.put(fieldname, strForHash);

    }

    private void cleanUpAfterDescrOrClaim() {

        this.descriptionOrClaim.clear();
    }

    private void selectDescriptionOrClaim(PatentHelper.PatentFieldName fieldName) {

        /*
         * Struktur des Hash "descriptionOrClaim" --> patentNum => {"lang" => xxx, "doc"
         * => xxx}
         *
         * Aus den temporär im Hash "descriptionOrClaim" gespeicherten Dokumenten
         * (Claim/Description) nach folgenden Kriterien eines aussuchen:
         *
         * 1. falls z.B. für den Bereich description schon eine Patentnummer ausgewählt
         * wurde, soll sie auch für die Claims benutzt werden.
         *
         * 2. (documentNumber == patentNum) && (tmpLanguage == en) oder
         *
         * 3. lang == "en" --> dann WO vor EP oder beliebige andere Nummer
         *
         * 4. falls kein "en" lang == "de" --> dann WO vor EP oder beliebige andere
         * Nummer
         *
         * 5. andernfalls "" setzen
         *
         */

        // 1.
        if (descriptionOrClaim.containsKey(selectedPatentNum)) {
            setDescriptionOrClaim(fieldName);
            return;
        }

        // 2.
        String matchingDocument = checkPatNumsAndLang();
        if (!matchingDocument.isEmpty()) {
            setDescriptionOrClaim(fieldName, matchingDocument);
            return;
        }

        // 3.
        matchingDocument = checkForAlternativeDoc("en");
        if (!matchingDocument.isEmpty()) {
            setDescriptionOrClaim(fieldName, matchingDocument);
            return;
        }

        // 4.
        matchingDocument = checkForAlternativeDoc("de");
        if (!matchingDocument.isEmpty()) {
            setDescriptionOrClaim(fieldName, matchingDocument);
            return;
        }

        System.err.println(String.format("No matching Doc for %s", this.documentNumber));
        setDescriptionOrClaim(fieldName, "");
        return;

    }

    private String checkForAlternativeDoc(String language) {

        // patentnummern mit passender sprache raussuchen
        ArrayList<String> matchingNums = new ArrayList<>();
        for (String patNum : descriptionOrClaim.keySet()) {
            if (language.equals(descriptionOrClaim.get(patNum).get("lang"))) {
                matchingNums.add(patNum);
            }
        }

        String[] matchingCodes = {"WO", "EP"};
        // nach WO oder EP suchen
        for (String countyCode : matchingCodes) {
            for (String num : matchingNums) {
                if (num.startsWith(countyCode)) {
                    this.langOfDocumentUsed = descriptionOrClaim.get(num).get("lang");
                    this.selectedPatentNum = num;
                    return descriptionOrClaim.get(num).get("doc");
                }
            }
        }

        // keine WO oder EP aber irgendwas in der Sprache "tmpLanguage"
        for (String docNum : matchingNums) {
            if (language.equals(descriptionOrClaim.get(docNum).get("lang"))) {
                this.langOfDocumentUsed = descriptionOrClaim.get(docNum).get("lang");
                this.selectedPatentNum = docNum;
                return descriptionOrClaim.get(docNum).get("doc");
            }
        }
        return "";
    }

    private void setDescriptionOrClaim(PatentHelper.PatentFieldName fieldName, String matchingDocument) {
        //this.selectedPatentNum = matchingDocument;
        csvHash.put(fieldName.toString(), matchingDocument);
    }

    private void setDescriptionOrClaim(PatentHelper.PatentFieldName fieldName) {
        String doc = this.descriptionOrClaim.get(selectedPatentNum).get("doc");
        csvHash.put(fieldName.toString(), doc);
    }

    private String checkPatNumsAndLang() {

        // passende nummern raussuchen
        ArrayList<String> matchingPatentNumbers = new ArrayList<>();
        for (String num : descriptionOrClaim.keySet()) {
            if (num.startsWith(documentNumber)) {
                matchingPatentNumbers.add(num);
            }
        }

        // ist ein document in englischer Sprache?
        for (String num : matchingPatentNumbers) {
            String lang = descriptionOrClaim.get(num).get("lang");
            if (lang.equals("en")) {
                this.langOfDocumentUsed = lang;
                this.selectedPatentNum = num;
                return descriptionOrClaim.get(num).get("doc");
            }
        }
        return "";
    }

    private void saveDescriptionOrClaim() {

        HashMap<String, String> documentHash = new HashMap<>();

        documentHash.put("lang", tmpLanguage);
        String cleanedResult = RegexPattern.cleanInputString(resultString.toString());
        documentHash.put("doc", cleanedResult);
        if (!PatentHelper.isPatentNumConsistent(this.patentNum)) {
            System.err.print("Method saveDescriptionOrClaim: ");
            PatentHelper.outputErrorPatentNum(this.patentNum);
        }

        this.descriptionOrClaim.put(patentNum, documentHash);
        // Aufräumen
        this.patentNum = "";
        this.tmpLanguage = "";

    }

    private String getAbstract(String rawAbstract) {

        rawAbstract = rawAbstract.trim();
        return StringEscapeUtils.unescapeXml(rawAbstract);
    }

    private String getTitle(String rawTitle) {

        /*
         * clean up raw title string and check for appropriate tmpLanguage of title
         */

        ArrayList<String> titleList = cleanRawTitle(rawTitle);
        String[] languages = {"en", "de"};
        for (String lang : languages) {
            for (String raw : titleList) {
                String title = StringEscapeUtils.unescapeXml(raw);
                if (PatentHelper.getLanguage(title).equals(lang)) {
                    return title;
                }
            }
        }
        return "";
    }

    private ArrayList<String> cleanRawTitle(String rawTitle) {

        ArrayList<String> returnList = new ArrayList<>();
        String[] titleList = rawTitle.split("[;\\n]");
        for (String item : titleList) {
            if (item.isEmpty()) {
                continue;
            }
            item = RegexPattern.MULTIPLE_SPACE().matcher(item).replaceAll(" ");
            // item = RegexPattern.WHITESPACE_AT_START().matcher(item).replaceFirst("");
            returnList.add(item.trim().toLowerCase());
        }
        return returnList;
    }

}
