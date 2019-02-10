import org.jsoup.nodes.Element;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class PatentHelper {

    private HashMap<String, Integer> patPos = null;

    public static String[] getPatenFields() {

        String[] retArray = new String[PatentFieldName.values().length];
        for (int i = 0; i < PatentFieldName.values().length; i++) {
            retArray[i] = PatentFieldName.values()[i].toString();
        }
        return retArray;
    }

    public static HashMap<String, String> initCSVHash() {

        // Aufbau eines Hashs in dem die Felder für jedes Patent gesoeichert
        // werden sollen
        HashMap<String, String> csvInitializedHash = new HashMap<>();
        for (String fieldName : getPatenFields()) {
            csvInitializedHash.put(fieldName, "");
        }

        return csvInitializedHash;

    }

    public static LocalDate getDate(ArrayList<Element> prioDates) {

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

    public static LocalDate getDate(String prioDate) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return LocalDate.parse(prioDate, formatter);
    }

    public enum PatentFieldName {

        PATENT_NUMMER, PRIORITY_DATE, TITLE,
        // das was vor description steht
        INVENTION_TITLE, ABSTRACT, DESCRIPTION, CLAIMS, FAMILY, ANMELDER_PERSON, ANMELDER_FIRMA, CPC,
    }

}
