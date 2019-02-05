import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.opencsv.CSVWriter;

public class ParsePatent implements CSV_Writable {

    private Document doc;
    private Element knowledgeCard;

    public ParsePatent(File fileName) {
        try {
            this.doc = Jsoup.parse(fileName, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<Element> knowledgeArr = this.doc.select(CssLocator.KNOWLEDGE_CARD_CSS());
        this.knowledgeCard = knowledgeArr.get(0);

        if (this.knowledgeCard == null) {
            System.err.printf("No knowledge card was found for %s", fileName.toString());
        }

        if (knowledgeArr != null && knowledgeArr.size() > 1) {
            System.err.printf("No knowledge card was found for %s", fileName.toString());
        }
    }

    public String getTitle() {
        Elements elem = this.doc.select(CssLocator.TITLE_CSS());
        elem.select("overlay-tooltip").remove();
        return elem.text();
    }

    public String getAbstract() {
        Elements elem = this.doc.select(CssLocator.ABSTRACT_CSS());
        return elem.select(CssLocator.ABSTRACT_CSS_DIV()).text();
    }

    public Map<String, String> getCPC() {

        Map<String, String> cpcClassesMap = new LinkedHashMap<String, String>();

        Elements cpcElements = this.doc.select(CssLocator.CPC_CSS()).select(CssLocator.CPC_DIVS_CSS());
        for (Element cpcElem : cpcElements) {
            String cpcClass = cpcElem.select(CssLocator.CPC_Class_CSS()).attr("data-cpc");
            String cpcDescription = cpcElem.select(CssLocator.CPC_DESCRIPTION_CSS()).text();

            if (cpcClass.isEmpty() || cpcDescription.isEmpty()) {
                continue;
            }

            if (cpcClassesMap.containsKey(cpcClass)) {
                continue;
            }
            cpcClassesMap.put(cpcClass, cpcDescription);
        }
        return cpcClassesMap;
    }

    public String getInventionTitle() {
        // INID Code (54)
        // das was nach describtion steht
        return this.doc.select(CssLocator.INVENTION_TITLE_CSS()).text();
    }

    public ArrayList<String> getInventors() {

        ArrayList<String> inventors = new ArrayList<>();
        for (Element elm : this.knowledgeCard.select(CssLocator.IVENTOR_CSS())) {
            inventors.add(elm.attr("data-inventor"));
        }
        return inventors;
    }

    public ArrayList<String> getDescription() {

        // der erste CSS-Locator trifft nicht alle Elemente
        Elements descriptionText = this.doc.select(CssLocator.DESCRIPTION_BODY_CSS())
                .select(CssLocator.DESCRIPTION_TEXT_CSS());

        if (descriptionText.size() == 0) {
            descriptionText = this.doc.select(CssLocator.DESCRIPTION_BODY_CSS())
                    .select(CssLocator.DESCRIPTION_TEXT_ALT_CSS());
        }

        ArrayList<String> returnList = new ArrayList<>();
        for (Element elem : descriptionText) {
            if (elem.text().isEmpty()) {
                continue;
            }
            returnList.add(cleanString(elem.text(), getAllRegex()));
        }
        return returnList;
    }

    public String getPatentNumber() {
        return this.knowledgeCard.select(CssLocator.PATENT_NR_CSS()).text();
    }

    public ArrayList<String> getAssignee() {

        HashSet<String> assignee = new HashSet<>();
        // ArrayList<String> assignee = new ArrayList<>();
        for (Element elm : this.knowledgeCard.select(CssLocator.ASSIGNEE_CSS())) {
            assignee.add(elm.attr("data-assignee"));
        }
        return new ArrayList<>(assignee);
    }

    public LocalDate getPriorityDate() {

        ArrayList<Element> prioDates = this.knowledgeCard.select(CssLocator.PRIORITY_DATE_CSS());

        LocalDate prioDate = this.getDate(prioDates);
        if (prioDate == null) {
            System.err.println("No Priority Date was found");
        }
        return prioDate;
    }

    public String prioDateAsString() {

        DateTimeFormatter dateFormater = DateTimeFormatter.ofPattern("dd-MM-YYYY");

        String dateAsString = dateFormater.format(getPriorityDate());
        return dateAsString;
    }

    // TODO: Implement family download
    public ArrayList<String> getFamily() {
        return null;
    }

    public ArrayList<String> getClaims() {

        ArrayList<String> claims = new ArrayList<>();
        for (Element element : this.doc.select(CssLocator.CLAIMS_TEXT_CSS())) {

            /*
             * Bei einigen Claims werden die claims doppelt gezählt, falls die Abfrage nicht
             * stattfindet
             */
            if (!element.parentNode().attr("num").isEmpty()) {
                claims.add(cleanString(element.text(), this.getAllRegex()));
            }
        }
        return claims;
    }

    private LocalDate getDate(ArrayList<Element> prioDates) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate prioDate = null;

        /*
         * Die List enthält normalerweise 2 Elemente; es soll aber nur das Element
         * betrachtet werden, dass nicht das Attribut "data-keywords" enthält;
         */
        for (Element elem : prioDates) {
            if (!elem.attr("data-keywords").isEmpty()) {
                continue;
            }
            String rawTime = elem.attr("data-before");
            prioDate = LocalDate.parse(rawTime, formatter);
            return prioDate;
        }
        return prioDate;
    }

    private String cleanString(String input, List<Pattern> pattern) {

        String returnString = input;
        for (Pattern regex : pattern) {
            returnString = regex.matcher(returnString).replaceAll("");
        }
        return returnString;
    }

    private ArrayList<Pattern> getAllRegex() {

        // falls es mal mehr als ein regex geben sollte
        ArrayList<Pattern> regexList = new ArrayList<>();
        regexList.add(RegexPattern.PATTERN_BRACET_NUMBER());
        regexList.add(RegexPattern.PATTERN_BRACES_ANY());
        regexList.add(RegexPattern.PATTERN_DIGIT_POINT_START());

        return regexList;
    }

    public void writeCSV(CSVWriter wr) {

        HashMap<String, String> csvHash = getCVSHash_To_write();

    }


    private HashMap<String, String> getCVSHash_To_write() {

        // einzelne Hashelement füllen
        HashMap<String, String> csvHash = initCSVHash();

        for (PatentHelper.PatentFieldName patField :
                PatentHelper.PatentFieldName.values()) {

            // Has
            switch (patField) {
                case PATENT_NUMMER:
                    csvHash.put(patField.toString(), getPatentNumber());
                    break;
                case PRIORITY_DATE:
                    csvHash.put(patField.toString(), prioDateAsString());
                    break;
                case TITLE:
                    csvHash.put(patField.toString(), getTitle());
                    break;
                case INVENTION_TITLE:
                    csvHash.put(patField.toString(), getInventionTitle());
                    break;
                case ABSTRACT:
                    csvHash.put(patField.toString(), getAbstract());
                    break;

            }
        }


        return csvHash;

    }

    private HashMap<String, String> initCSVHash() {

        // Aufbau eines Hashs in dem die Felder für jedes Patent gesoeichert
        // werden sollen
        HashMap<String, String> csvInitializedHash = new HashMap<>();
        for (String fieldName : PatentHelper.getPatenFields()) {
            csvInitializedHash.put(fieldName, "");
        }

        return csvInitializedHash;

    }

}