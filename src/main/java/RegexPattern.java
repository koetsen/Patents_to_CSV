import java.util.regex.Pattern;

public class RegexPattern {

    static public Pattern PATTERN_BRACET_NUMBER() {
        return Pattern.compile("\\[\\d+\\]");
    }

    static public Pattern PATTERN_BRACES_ANY() {
        return Pattern.compile("\\[.*\\]");
    }

    static public Pattern PATTERN_DIGIT_POINT_START() {
        return Pattern.compile("^\\d+\\.*");
    }




}