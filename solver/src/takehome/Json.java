package takehome;

import java.util.*;

/** 极简 JSON 解析器（够本题目使用），返回 Map/List/String/Double/Boolean */
public class Json {
    private final String s;
    private int i = 0;

    private Json(String s) { this.s = s; }

    public static Object parse(String text) {
        Json p = new Json(text);
        p.ws();
        return p.value();
    }

    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

    private Object value() {
        ws();
        char c = s.charAt(i);
        if (c == '{') return obj();
        if (c == '[') return arr();
        if (c == '\"') return str();
        if (c == 't') { i += 4; return Boolean.TRUE; }
        if (c == 'f') { i += 5; return Boolean.FALSE; }
        if (c == 'n') { i += 4; return null; }
        return num();
    }

    private Map<String, Object> obj() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++; ws();
        if (s.charAt(i) == '}') { i++; return m; }
        while (true) {
            ws();
            String k = str();
            ws(); i++; // :
            Object v = value();
            m.put(k, v);
            ws();
            char c = s.charAt(i++);
            if (c == '}') break;
        }
        return m;
    }

    private List<Object> arr() {
        List<Object> l = new ArrayList<>();
        i++; ws();
        if (s.charAt(i) == ']') { i++; return l; }
        while (true) {
            l.add(value());
            ws();
            char c = s.charAt(i++);
            if (c == ']') break;
        }
        return l;
    }

    private String str() {
        StringBuilder sb = new StringBuilder();
        i++; // opening quote
        while (s.charAt(i) != '\"') {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
                char e = s.charAt(i);
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '\"': sb.append('\"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    default: sb.append(e);
                }
            } else sb.append(c);
            i++;
        }
        i++; // closing quote
        return sb.toString();
    }

    private Double num() {
        int st = i;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '-' || c == '+' || c == '.' || (c >= '0' && c <= '9')
                    || c == 'e' || c == 'E') i++;
            else break;
        }
        return Double.parseDouble(s.substring(st, i));
    }
}
