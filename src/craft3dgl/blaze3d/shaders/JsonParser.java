package craft3dgl.blaze3d.shaders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prosty parser JSON dla shader configów MC.
 * Wspiera: obiekty {}, tablice [], string "..", liczby, true/false/null.
 * BEZ zewnętrznych deps (Gson) - self-contained, ~200 linii.
 *
 * Zwraca strukturę:
 *   Object = Map<String, Object> (obiekt JSON)
 *   Object = List<Object> (tablica)
 *   Object = String, Number, Boolean, null
 */
public class JsonParser {

    private final String src;
    private int pos;

    private JsonParser(String s) { this.src = s; this.pos = 0; }

    /** Parse JSON string, returns Map/List/String/Number/Boolean/null. */
    public static Object parse(String src) {
        JsonParser p = new JsonParser(src);
        p.skipWs();
        Object result = p.parseValue();
        p.skipWs();
        return result;
    }

    private void skipWs() {
        while (pos < src.length() && (src.charAt(pos) == ' ' || src.charAt(pos) == '\t' ||
                                       src.charAt(pos) == '\n' || src.charAt(pos) == '\r')) {
            pos++;
        }
    }

    private Object parseValue() {
        skipWs();
        if (pos >= src.length()) throw new RuntimeException("Unexpected end of JSON");
        char c = src.charAt(pos);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == 't' || c == 'f') return parseBool();
        if (c == 'n') return parseNull();
        if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
        throw new RuntimeException("Unexpected char '" + c + "' at pos " + pos);
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> obj = new LinkedHashMap<>();
        pos++; // skip {
        skipWs();
        if (pos < src.length() && src.charAt(pos) == '}') { pos++; return obj; }
        while (true) {
            skipWs();
            String key = parseString();
            skipWs();
            if (src.charAt(pos) != ':') throw new RuntimeException("Expected ':' at pos " + pos);
            pos++;
            Object value = parseValue();
            obj.put(key, value);
            skipWs();
            if (pos >= src.length()) throw new RuntimeException("Unexpected end of object");
            char c = src.charAt(pos);
            if (c == ',') { pos++; continue; }
            if (c == '}') { pos++; return obj; }
            throw new RuntimeException("Expected ',' or '}' at pos " + pos + " got '" + c + "'");
        }
    }

    private List<Object> parseArray() {
        List<Object> arr = new ArrayList<>();
        pos++; // skip [
        skipWs();
        if (pos < src.length() && src.charAt(pos) == ']') { pos++; return arr; }
        while (true) {
            arr.add(parseValue());
            skipWs();
            if (pos >= src.length()) throw new RuntimeException("Unexpected end of array");
            char c = src.charAt(pos);
            if (c == ',') { pos++; continue; }
            if (c == ']') { pos++; return arr; }
            throw new RuntimeException("Expected ',' or ']' at pos " + pos);
        }
    }

    private String parseString() {
        if (src.charAt(pos) != '"') throw new RuntimeException("Expected '\"' at pos " + pos);
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '"') { pos++; return sb.toString(); }
            if (c == '\\') {
                pos++;
                char esc = src.charAt(pos);
                switch (esc) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        String hex = src.substring(pos + 1, pos + 5);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                        break;
                    default: sb.append(esc);
                }
                pos++;
            } else {
                sb.append(c);
                pos++;
            }
        }
        throw new RuntimeException("Unterminated string");
    }

    private Number parseNumber() {
        int start = pos;
        if (src.charAt(pos) == '-') pos++;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                pos++;
            } else break;
        }
        String num = src.substring(start, pos);
        if (num.contains(".") || num.contains("e") || num.contains("E")) {
            return Double.parseDouble(num);
        }
        try {
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            return Double.parseDouble(num);
        }
    }

    private Boolean parseBool() {
        if (src.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
        if (src.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
        throw new RuntimeException("Expected true/false at pos " + pos);
    }

    private Object parseNull() {
        if (src.startsWith("null", pos)) { pos += 4; return null; }
        throw new RuntimeException("Expected null at pos " + pos);
    }

    // === HELPERS - typed access (jak GsonHelper w MC) ===

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getAsObject(Object o, String key) {
        if (o instanceof Map) {
            Object v = ((Map<String, Object>) o).get(key);
            if (v instanceof Map) return (Map<String, Object>) v;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getAsArray(Object o, String key) {
        if (o instanceof Map) {
            Object v = ((Map<String, Object>) o).get(key);
            if (v instanceof List) return (List<Object>) v;
        }
        return null;
    }

    public static String getAsString(Object o, String key) {
        if (o instanceof Map) {
            Object v = ((Map<String, Object>) o).get(key);
            if (v instanceof String) return (String) v;
        }
        return null;
    }

    public static String getAsString(Object o, String key, String def) {
        String s = getAsString(o, key);
        return s == null ? def : s;
    }

    public static int getAsInt(Object o, String key, int def) {
        if (o instanceof Map) {
            Object v = ((Map<String, Object>) o).get(key);
            if (v instanceof Number) return ((Number) v).intValue();
        }
        return def;
    }

    public static float getAsFloat(Object o, String key, float def) {
        if (o instanceof Map) {
            Object v = ((Map<String, Object>) o).get(key);
            if (v instanceof Number) return ((Number) v).floatValue();
        }
        return def;
    }

    public static boolean getAsBoolean(Object o, String key, boolean def) {
        if (o instanceof Map) {
            Object v = ((Map<String, Object>) o).get(key);
            if (v instanceof Boolean) return (Boolean) v;
        }
        return def;
    }

    public static float asFloat(Object o) {
        if (o instanceof Number) return ((Number) o).floatValue();
        if (o instanceof String) {
            try { return Float.parseFloat((String) o); } catch (Exception ignored) {}
        }
        return 0f;
    }
}
