package org.example.horse_racing.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader/writer for the app's data files (no external dependencies).
 * <p>
 * Values map to: object → {@code Map<String,Object>}, array → {@code List<Object>}, string → {@code String},
 * number → {@code Long} or {@code Double}, {@code true/false} → {@code Boolean}, {@code null} → {@code null}.
 */
public final class Json {

    private Json() {}

    // ------------------------------------------------------------------ files

    public static Object read(Path file) throws IOException {
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    public static void write(Path file, Object value) throws IOException {
        Files.writeString(file, stringify(value), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ writing

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, 0);
        return sb.append('\n').toString();
    }

    private static void write(StringBuilder sb, Object v, int indent) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            quote(sb, s);
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> m) {
            if (m.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                pad(sb, indent + 1);
                quote(sb, String.valueOf(e.getKey()));
                sb.append(": ");
                write(sb, e.getValue(), indent + 1);
                if (++i < m.size()) sb.append(',');
                sb.append('\n');
            }
            pad(sb, indent);
            sb.append('}');
        } else if (v instanceof Iterable<?> it) {
            List<Object> items = new ArrayList<>();
            it.forEach(items::add);
            if (items.isEmpty()) { sb.append("[]"); return; }
            sb.append("[\n");
            for (int i = 0; i < items.size(); i++) {
                pad(sb, indent + 1);
                write(sb, items.get(i), indent + 1);
                if (i < items.size() - 1) sb.append(',');
                sb.append('\n');
            }
            pad(sb, indent);
            sb.append(']');
        } else {
            quote(sb, v.toString());
        }
    }

    private static void pad(StringBuilder sb, int indent) {
        sb.append("  ".repeat(indent));
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------ parsing

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWs();
        Object v = p.value();
        p.skipWs();
        if (!p.done()) throw p.error("Unexpected trailing content");
        return v;
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            // Tolerate a UTF-8 byte-order mark.
            this.s = s.startsWith("﻿") ? s.substring(1) : s;
        }

        boolean done() { return i >= s.length(); }

        IllegalArgumentException error(String msg) {
            return new IllegalArgumentException(msg + " at position " + i);
        }

        void skipWs() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        Object value() {
            if (done()) throw error("Unexpected end of input");
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) yield number();
                    throw error("Unexpected character '" + c + "'");
                }
            };
        }

        Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // {
            skipWs();
            if (peek('}')) { i++; return m; }
            while (true) {
                skipWs();
                if (!peek('"')) throw error("Expected property name");
                String key = string();
                skipWs();
                expect(':');
                skipWs();
                m.put(key, value());
                skipWs();
                if (peek(',')) { i++; continue; }
                expect('}');
                return m;
            }
        }

        List<Object> array() {
            List<Object> list = new ArrayList<>();
            i++; // [
            skipWs();
            if (peek(']')) { i++; return list; }
            while (true) {
                skipWs();
                list.add(value());
                skipWs();
                if (peek(',')) { i++; continue; }
                expect(']');
                return list;
            }
        }

        String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (done()) throw error("Unterminated string");
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                if (done()) throw error("Bad escape");
                char e = s.charAt(i++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw error("Bad unicode escape");
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw error("Bad escape '\\" + e + "'");
                }
            }
        }

        Number number() {
            int start = i;
            if (peek('-')) i++;
            while (i < s.length() && "0123456789.eE+-".indexOf(s.charAt(i)) >= 0) i++;
            String n = s.substring(start, i);
            try {
                if (n.contains(".") || n.contains("e") || n.contains("E")) return Double.parseDouble(n);
                return Long.parseLong(n);
            } catch (NumberFormatException ex) {
                throw error("Bad number '" + n + "'");
            }
        }

        Object literal(String word, Object v) {
            if (!s.startsWith(word, i)) throw error("Expected " + word);
            i += word.length();
            return v;
        }

        boolean peek(char c) {
            return i < s.length() && s.charAt(i) == c;
        }

        void expect(char c) {
            if (!peek(c)) throw error("Expected '" + c + "'");
            i++;
        }
    }
}
