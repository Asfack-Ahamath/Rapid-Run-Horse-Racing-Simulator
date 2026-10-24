package org.example.horse_racing;

import org.example.horse_racing.service.Json;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonTest {

    @Test
    void roundTrip_preservesValues() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", "002");
        m.put("name", "Quote \" and \\ slash, ünïcödé");
        m.put("age", 7);
        m.put("time", 68.42);
        m.put("ok", true);
        m.put("none", null);
        m.put("list", List.of(1, 2, 3));
        Object back = Json.parse(Json.stringify(List.of(m)));
        Map<?, ?> r = (Map<?, ?>) ((List<?>) back).get(0);
        assertEquals("002", r.get("id"));
        assertEquals("Quote \" and \\ slash, ünïcödé", r.get("name"));
        assertEquals(7L, r.get("age"));
        assertEquals(68.42, r.get("time"));
        assertEquals(Boolean.TRUE, r.get("ok"));
        assertTrue(r.containsKey("none"));
        assertEquals(3, ((List<?>) r.get("list")).size());
    }

    @Test
    void parse_jacksonStyleFile() {
        Object v = Json.parse("[ {\n  \"id\" : \"002\",\n  \"age\" : 7\n} ]");
        assertEquals("002", ((Map<?, ?>) ((List<?>) v).get(0)).get("id"));
    }

    @Test
    void parse_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("[1, 2"));
    }
}
