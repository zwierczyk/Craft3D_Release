package craft3dmodern.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimalny, samowystarczalny parser JSON (bez zewnetrznych bibliotek).
 * Wynik: Map<String,Object>, List<Object>, String, Double, Boolean lub null.
 */
public final class Json {
    private final String s;
    private int pos;

    private Json(String s) {
        this.s = s;
    }

    public static Object parse(String text) {
        Json p = new Json(text);
        Object v = p.value();
        p.ws();
        if (p.pos != p.s.length()) throw new IllegalArgumentException("trailing json at " + p.pos);
        return v;
    }

    public static Object parseFile(Path file) throws IOException {
        return parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object o) {
        return (List<Object>) o;
    }

    private void ws() {
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') pos++;
            else break;
        }
    }

    private Object value() {
        ws();
        if (pos >= s.length()) throw new IllegalArgumentException("unexpected end");
        char c = s.charAt(pos);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': literal("true"); return Boolean.TRUE;
            case 'f': literal("false"); return Boolean.FALSE;
            case 'n': literal("null"); return null;
            default: return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        pos++; // {
        ws();
        if (pos < s.length() && s.charAt(pos) == '}') { pos++; return m; }
        while (true) {
            ws();
            String k = string();
            ws();
            if (pos >= s.length() || s.charAt(pos) != ':') throw new IllegalArgumentException("missing ':' at " + pos);
            pos++;
            m.put(k, value());
            ws();
            if (pos >= s.length()) throw new IllegalArgumentException("unterminated object");
            char c = s.charAt(pos);
            if (c == ',') { pos++; continue; }
            if (c == '}') { pos++; return m; }
            throw new IllegalArgumentException("bad object char " + c + " at " + pos);
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<Object>();
        pos++; // [
        ws();
        if (pos < s.length() && s.charAt(pos) == ']') { pos++; return l; }
        while (true) {
            l.add(value());
            ws();
            if (pos >= s.length()) throw new IllegalArgumentException("unterminated array");
            char c = s.charAt(pos);
            if (c == ',') { pos++; continue; }
            if (c == ']') { pos++; return l; }
            throw new IllegalArgumentException("bad array char " + c + " at " + pos);
        }
    }

    private String string() {
        if (pos >= s.length() || s.charAt(pos) != '"') throw new IllegalArgumentException("expected string at " + pos);
        pos++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= s.length()) throw new IllegalArgumentException("unterminated string");
            char c = s.charAt(pos);
            if (c == '"') { pos++; return sb.toString(); }
            if (c == '\\') {
                pos++;
                if (pos >= s.length()) throw new IllegalArgumentException("bad escape");
                char e = s.charAt(pos);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 >= s.length()) throw new IllegalArgumentException("bad unicode escape");
                        sb.append((char) Integer.parseInt(s.substring(pos + 1, pos + 5), 16));
                        pos += 4;
                        break;
                    default: throw new IllegalArgumentException("bad escape \\" + e);
                }
                pos++;
            } else {
                sb.append(c);
                pos++;
            }
        }
    }

    private void literal(String lit) {
        if (!s.startsWith(lit, pos)) throw new IllegalArgumentException("bad literal at " + pos);
        pos += lit.length();
    }

    private Double number() {
        int start = pos;
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') pos++;
            else break;
        }
        if (start == pos) throw new IllegalArgumentException("bad number at " + pos);
        return Double.parseDouble(s.substring(start, pos));
    }
}
