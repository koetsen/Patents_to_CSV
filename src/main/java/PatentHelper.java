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

    public enum PatentFieldName {

        PATENT_NUMMER, PRIORITY_DATE, TITLE,
        // das was vor description steht
        INVENTION_TITLE, ABSTRACT, DESCRIPTION, CLAIMS, FAMILY, ANMELDER_PERSON, ANMELDER_FIRMA, CPC,
    }


}
