import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.SplittableRandom;

public class TokenMap {
    enum TYPE{
        LETTER, DIGIT, QUOTE, OPERATOR, DELIMITER, OTHER
    }
    private static final ArrayList<String> keywordList = new ArrayList<String>(){{
        add("main");
        add("const");
        add("break");
        add("continue");
        add("int");
        add("void");
        add("if");
        add("else");
        add("while");
        add("return");
        add("getint");
        add("printf");
    }};
    private static final HashMap<String, String> tokenMap = new HashMap<>();
    private static final HashMap<Character, TYPE> typeMap = new HashMap<>();
    static {
        //typeMap添加字符对应的类别
        for (int i = 1; i <= 32; i++) {
            typeMap.put((char)i, TYPE.DELIMITER);
        }
        for (int i = 33; i < 127; i++) {
            char c = (char)i;
            if (Character.isLetter(c) || c == '_') {
                typeMap.put(c, TYPE.LETTER);
            } else if (Character.isDigit(c)) {
                typeMap.put(c, TYPE.DIGIT);
            } else if (c == '"') {
                typeMap.put(c, TYPE.QUOTE);
            } else if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '=' || c == '<' || c == '>' || c == '!' || c == '&' || c == '|'
                    || c == ';' || c == ',' || c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}') {
                typeMap.put(c, TYPE.OPERATOR);
            } else {
                typeMap.put(c, TYPE.OTHER);
            }
        }
        //tokenMap添加关键字对应的tokenCode
        for (String keyword : keywordList) {
            tokenMap.put(keyword, keyword.toUpperCase() + "TK");
        }
        tokenMap.put("!", "NOT");
        tokenMap.put("&&", "AND");
        tokenMap.put("||", "OR");
        tokenMap.put("==", "EQL");
        tokenMap.put("!=", "NEQ");
        tokenMap.put("<", "LSS");
        tokenMap.put("<=", "LEQ");
        tokenMap.put(">", "GRE");
        tokenMap.put(">=", "GEQ");
        tokenMap.put("+", "PLUS");
        tokenMap.put("-", "MINU");
        tokenMap.put("*", "MULT");
        tokenMap.put("/", "DIV");
        tokenMap.put("%", "MOD");
        tokenMap.put("=", "ASSIGN");
        tokenMap.put(";", "SEMICN");
        tokenMap.put(",", "COMMA");
        tokenMap.put("(", "LPARENT");
        tokenMap.put(")", "RPARENT");
        tokenMap.put("[", "LBRACK");
        tokenMap.put("]", "RBRACK");
        tokenMap.put("{", "LBRACE");
        tokenMap.put("}", "RBRACE");
    }
    public static TYPE getType(char c) {
        return typeMap.get(c);
    }
    public static String getTokenCode(String tokenValue) {
        return tokenMap.get(tokenValue);
    }
    public static boolean isKeyword(String tokenValue) {
        return keywordList.contains(tokenValue);
    }
}
