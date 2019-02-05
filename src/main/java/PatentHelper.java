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

	/*public HashMap<String, Integer> getPatPos() {
		for (PatentFieldName item : PatentFieldName.values()) {
			patPos.put(item.toString(), PatentFieldName.valueOf(item.toString()).ordinal());
		}
		return patPos;
	}*/

    public enum PatentFieldName {

        PATENT_NUMMER, PRIORITY_DATE, TITLE,
        // das was vor description steht
        INVENTION_TITLE, ABSTRACT, DESCRIPTION, CLAIMS, FAMILY, ANMELDER_PERSON, ANMELDER_FIRMA, CPC,
    }
}
