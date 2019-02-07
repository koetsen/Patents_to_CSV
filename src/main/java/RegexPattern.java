import java.util.regex.Pattern;


import java.util.ArrayList;
import java.util.List;


public class RegexPattern {

    static public Pattern DIGIT() {
        return Pattern.compile("\\p{Digit}+");
    }

    static public Pattern PUNCT() {
        return Pattern.compile("\\p{Punct}+");
    }

    static public Pattern GREEK() {
        return Pattern.compile("\\p{InGreek}+");
    }

    static public Pattern SPACE() {
        return Pattern.compile("\\p{Space}{2,}");
    }

    static public Pattern ONE_CHAR() {
        return Pattern.compile("\\s+\\w\\s+");
    }

    public static ArrayList<Pattern> getRegex() {

        ArrayList<Pattern> regexList = new ArrayList<>();
        regexList.add(DIGIT());
        regexList.add(PUNCT());
        regexList.add(GREEK());
        return regexList;
    }

    public static String getListAsConcatenatedString(List<String> strList) {

        String singleSpace = " ";
        StringBuilder returnString = new StringBuilder();

        for (String inString : strList) {
            String stringForArray = inString;

            // TODO: vielleicht chemische Formeln erkennen und drin lassen
            /*
             * ersetzten von - durch singleSpace, damit durch Bindestriche getrennte Worte
             * nicht zusammenfallen
             */
            stringForArray = stringForArray.toLowerCase();
            stringForArray = stringForArray.replace("-", singleSpace);
            stringForArray = stringForArray.replace("°C", "");
            returnString.append(cleanString(stringForArray, RegexPattern.getRegex()));
            stringForArray = RegexPattern.SPACE().matcher(stringForArray).replaceAll(singleSpace);
            stringForArray = RegexPattern.ONE_CHAR().matcher(stringForArray).replaceAll(singleSpace);
        }

        return returnString.toString();
    }

    private static String cleanString(String input, List<Pattern> pattern) {

        String returnString = input;
        for (Pattern regex : pattern) {
            returnString = regex.matcher(returnString).replaceAll("");
        }
        return returnString;
    }

}