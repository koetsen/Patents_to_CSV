import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;


public class PatentHelper {

    private static LanguageDetector langDetector = new OptimaizeLangDetector().loadModels();


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
        return null;
    }

    public static String getDate(String prioDate) {

        DateTimeFormatter dateFormater = DateTimeFormatter.ofPattern("dd-MM-YYYY");
        LocalDate date = LocalDate.parse(prioDate.trim(), DateTimeFormatter.ISO_DATE_TIME);
        return dateFormater.format(date);
    }

    public static String[] getArrayForOutput(HashMap<String, String> inHash) {

        String[] fieldnames = PatentHelper.getPatenFields();
        String[] csvArray = new String[fieldnames.length];
        for (int i = 0; i < fieldnames.length; i++) {
            String result = inHash.get(fieldnames[i]);
            if (result == null) {
                csvArray[i] = "";
            } else {
                csvArray[i] = inHash.get(fieldnames[i]);
            }
        }
        return csvArray;
    }

    public static boolean isPatentNumConsistent(String documentNumber) {

        Matcher match = RegexPattern.PATENT_NUMBER().matcher(documentNumber);
        if (!match.matches()) {

            return false;
        }
        return true;
    }

    public static void outputErrorPatentNum(String documentNumber) {
        String errorString = String.format("%s scheint eine komische Patentnummer zu sein!", documentNumber);
        System.err.println(errorString);
    }

    public static Charset getCharsetFromFile(Path inPath) {

        byte[] fis = null;
        try {
            fis = Files.readAllBytes(inPath);
        } catch (IOException e) {
            System.out.println(String.format("Konnte Datei %s nicht in ByteSream umwandel", inPath.toString()));
            return Charset.defaultCharset();
        }
        CharsetDetector detector = new CharsetDetector();
        detector.setText(fis);
        CharsetMatch charsetMatch = detector.detect();
        if (charsetMatch != null) {
            return Charset.forName(charsetMatch.getName());
        } else {
            System.out.println(String.format("Kein Charset für %s gefunden", inPath.toString()));
            return Charset.defaultCharset();
        }
    }

    public static String getLanguage(String doc) {

        String lang = langDetector.detect(doc).getLanguage();
        if (lang == null) {
            return "";
        }
        return lang;
    }

    public enum PatentFieldName {

        PATENT_NUMMER, PRIORITY_DATE, TITLE,
        // das was vor description steht
        INVENTION_TITLE, ABSTRACT, DESCRIPTION, CLAIMS, FAMILY, ANMELDER_PERSON, ANMELDER_FIRMA, CPC, LANGUAGE
    }
}