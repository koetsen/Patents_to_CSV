import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import javax.xml.stream.*;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XML_Patent_Devide_Claims {

    private boolean foundDescription = false;
    private boolean foundStartDocument = false;
    private boolean foundClaims = false;
    private boolean foundDocumentNumber = false;


    private StringBuilder stringBldr;

    public XML_Patent_Devide_Claims() {
        this.stringBldr = new StringBuilder();
    }

    public void devideDescriptionAndClaims(String infile, String outFile) {


        BufferedReader br = null;
        BufferedWriter bWr = null;
        XMLInputFactory xmlFac = XMLInputFactory.newInstance();
        XMLOutputFactory outfac = XMLOutputFactory.newInstance();

        try {

            br = Files.newBufferedReader(Paths.get(infile), StandardCharsets.UTF_8);
            XMLEventReader xmlEventReader = xmlFac.createXMLEventReader(br);
            bWr = new BufferedWriter(new FileWriter(outFile));
            XMLStreamWriter xmlWriter = outfac.createXMLStreamWriter(bWr);

            while (xmlEventReader.hasNext()) {
                XMLEvent event = xmlEventReader.nextEvent();

                switch (event.getEventType()) {

                    case XMLStreamConstants.START_DOCUMENT:
                        if (!this.foundStartDocument) {
                            xmlWriter.writeStartDocument("UTF-8", "1.0");
                            this.foundStartDocument = true;
                        }
                        break;

                    case XMLStreamConstants.START_ELEMENT:

                        StartElement elm = event.asStartElement();
                        String elementAsString = elm.getName().getLocalPart();
                        if (elementAsString.equalsIgnoreCase("Description")) {
                            this.foundDescription = true;
                        } else if (elementAsString.equalsIgnoreCase("Claims")) {
                            this.foundClaims = true;
                        } else if (elementAsString.equalsIgnoreCase("DocumentNumber")) {
                            this.foundDocumentNumber = true;
                        }
                        xmlWriter.writeStartElement(elementAsString);
                        break;

                    case XMLStreamConstants.CHARACTERS:
                        String contentAsString = event.asCharacters().getData();
                        if (this.foundDescription || this.foundClaims) {
                            if (!contentAsString.isEmpty()) {
                                // TODO: Escape HTML
                                this.stringBldr.append(contentAsString);
                            }
                            break;
                        } else if (foundDocumentNumber) {
                            System.out.printf("Working on: %s\n", contentAsString);
                            this.foundDocumentNumber = false;
                        }
                        xmlWriter.writeCharacters(contentAsString);
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        if (this.foundDescription) {
                            this.devideAndWrite(xmlWriter, "Description", "description");
                            this.stringBldr.setLength(0);
                            this.foundDescription = false;
                        }
                        if (this.foundClaims) {
                            this.devideAndWrite(xmlWriter, "Claims", "claim");
                            this.stringBldr.setLength(0);
                            this.foundClaims = false;
                        }
                        xmlWriter.writeEndElement();
                        break;

                    case XMLStreamConstants.END_DOCUMENT:
                        // xmlWriter.writeEndDocument();
                        break;
                }
            }

            xmlWriter.writeEndDocument();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XMLStreamException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } finally {
            try {
                br.close();
                bWr.flush();
                bWr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void devideAndWrite(XMLStreamWriter xmlWriter, String startOfStringMatcher, String elementName) {

        /*
         * startOfString -> wonach soll getrennt werden: z.B. Description oder Claims
         * elementName -> wie soll das Element unter Description heißen -> <descr
         * lang=en nr=WO2017125773A1> -> <claim lang=en nr=WO2017125773A1>
         */

        // get Patent Numbers
        ArrayList<String> docNumbers = new ArrayList<>();
        Matcher matcher = getPattern(startOfStringMatcher).matcher(this.stringBldr.toString());
        while (matcher.find()) {
            docNumbers.add(this.normalizePatentNr(matcher.group(1)));
        }

        // split claims of document text according by paten or claim numbers
        ArrayList<String> docTexts = getDocTexts(startOfStringMatcher);

        if (docNumbers.size() != docTexts.size()) {
            System.err.println("Document Numbers and Document Test Numbers do not match for " + startOfStringMatcher);
        }

        // xmlOutput schreiben
        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < docNumbers.size(); i++) {
            String docNumber = docNumbers.get(i);
            // es sollen keine Elemente doppelt gezählt werden
            if (!set.contains(docNumber)) {
                try {
                    xmlWriter.writeStartElement(elementName);
                    xmlWriter.writeAttribute("patentNr", docNumbers.get(i));
                    xmlWriter.writeAttribute("lang", getLanguage(docTexts.get(i)));
                    xmlWriter.writeCharacters(docTexts.get(i));
                    xmlWriter.writeEndElement();
                } catch (XMLStreamException e) {
                    e.printStackTrace();
                }
                set.add(docNumber);
            }
        }
    }

    private ArrayList<String> getDocTexts(String startOfStringMatcher) {

        String[] splitResult = this.stringBldr.toString().split(getRegexAsString(startOfStringMatcher));
        ArrayList<String> docTexts = new ArrayList<>();
        for (String element : splitResult) {
            if (!element.isEmpty()) {
                docTexts.add(element);
            }
        }
        return docTexts;
    }

    private Pattern getPattern(String startOfStringMatcher) {
        return Pattern.compile(getRegexAsString(startOfStringMatcher));
    }

    private String getRegexAsString(String startOfStringMatcher) {
        return startOfStringMatcher + " of (\\w{2}\\s*\\w+\\s*\\(?\\w+\\)?)";
    }

    private String normalizePatentNr(String number) {
        return number.replaceAll("[\\s+\\(\\)]", "");
    }

    private String getLanguage(String doc) {
        LanguageDetector langDetector = new OptimaizeLangDetector().loadModels();
        LanguageResult result = langDetector.detect(doc);
        return result.getLanguage();
    }

}
