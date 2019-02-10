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

    static public Pattern MULTIPLE_SPACE() {
        return Pattern.compile("\\p{Space}{2,}");
    }

    static public Pattern ONE_CHAR() {
        return Pattern.compile("\\s+\\w\\s+");
    }

    public static Pattern PATENT_NUMBER() {
        return Pattern.compile("([A-Z]{2,})\\d+([A-Z]\\d){0,1}");
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
             * ersetzten von '-' durch singleSpace, damit durch Bindestriche getrennte Worte
             * nicht zusammenfallen
             */
            stringForArray = stringForArray.toLowerCase();
            stringForArray = stringForArray.replace("-", singleSpace);
            stringForArray = stringForArray.replace("°C", "");
            //returnString.append(cleanString(stringForArray, RegexPattern.getRegex()));
            stringForArray = cleanString(stringForArray, RegexPattern.getRegex());
            stringForArray = RegexPattern.MULTIPLE_SPACE().matcher(stringForArray).replaceAll(singleSpace);
            stringForArray = RegexPattern.ONE_CHAR().matcher(stringForArray).replaceAll(singleSpace);
            returnString.append(stringForArray);
        }

        return returnString.toString();
    }

    public static String cleanInputString(String input) {

        ArrayList<String> inStringList = new ArrayList<>();
        inStringList.add(input);
        return getListAsConcatenatedString(inStringList);
    }

    private static String cleanString(String input, List<Pattern> pattern) {

        String returnString = input;
        for (Pattern regex : pattern) {
            returnString = regex.matcher(returnString).replaceAll("");
        }
        return returnString;
    }

}