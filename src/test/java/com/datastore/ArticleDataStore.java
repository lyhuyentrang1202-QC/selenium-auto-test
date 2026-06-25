package com.datastore;

import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.*;
import java.util.zip.*;

/**
 * Singleton. Load toàn bộ JSON từ ZIP một lần duy nhất vào RAM.
 * Cung cấp lookup nhanh O(1) theo title hoặc type.
 */
public class ArticleDataStore {

    private static volatile ArticleDataStore instance;

    // Lookup chính: normalized title → article
    private final Map<String, JsonObject> byNormalizedTitle = new HashMap<>();

    // Lookup phụ: type → list articles
    private final Map<String, List<JsonObject>> byType = new LinkedHashMap<>();

    // Toàn bộ articles (dedup theo id)
    private final Map<String, JsonObject> byId = new LinkedHashMap<>();

    private static final String ZIP_DIR =
        "src/test/resources/testdata/lsn-sogd-crawler/lsn-sogd-crawler";

    // ── Singleton ─────────────────────────────────────────────────────────────

    private ArticleDataStore() {
        long start = System.currentTimeMillis();
        loadAll();
        System.out.printf("[DataStore] Loaded %d articles in %dms%n",
            byId.size(), System.currentTimeMillis() - start);
    }

    public static ArticleDataStore get() {
        if (instance == null) {
            synchronized (ArticleDataStore.class) {
                if (instance == null) instance = new ArticleDataStore();
            }
        }
        return instance;
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private void loadAll() {
        try {
            List<Path> zips = Files.walk(Paths.get(ZIP_DIR))
                .filter(p -> p.toString().endsWith(".zip"))
                .sorted()
                .collect(Collectors.toList());

            for (Path zip : zips) readZip(zip);

            for (JsonObject article : byId.values()) {
                String type = str(article, "type", "unknown");
                byType.computeIfAbsent(type, k -> new ArrayList<>()).add(article);

                String title = str(article, "title", "");
                if (!title.isEmpty()) {
                    byNormalizedTitle.put(normalize(title), article);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Không load được ZIP data: " + e.getMessage(), e);
        }
    }

    private void readZip(Path zipPath) {
        try (ZipInputStream zis = new ZipInputStream(
                new FileInputStream(zipPath.toFile()), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().endsWith("article.json")) { zis.closeEntry(); continue; }
                try {
                    byte[] bytes = zis.readAllBytes();
                    JsonObject obj = JsonParser.parseString(
                        new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                    String id = str(obj, "id", null);
                    if (id != null) byId.putIfAbsent(id, obj);
                } catch (Exception ignored) {}
                zis.closeEntry();
            }
        } catch (Exception ignored) {}
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Tìm article theo title từ UI (normalize để so khớp mềm). */
    public Optional<JsonObject> findByTitle(String uiTitle) {
        return Optional.ofNullable(byNormalizedTitle.get(normalize(uiTitle)));
    }

    /** Tất cả articles theo type. */
    public List<JsonObject> getByType(String type) {
        return byType.getOrDefault(type, Collections.emptyList());
    }

    /** Tổng số articles theo type trong JSON. */
    public int countByType(String type) {
        return getByType(type).size();
    }

    /** Tổng số unique articles. */
    public int total() { return byId.size(); }

    /** Tất cả types có trong data. */
    public Set<String> types() { return byType.keySet(); }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Normalize title để so khớp mềm giữa UI và JSON.
     * NFC normalization trước để thống nhất Unicode composition.
     * Giữ nguyên dấu tiếng Việt vì cả 2 phía đều có.
     */
    public static String normalize(String s) {
        if (s == null) return "";
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        s = s.replaceAll("\\p{Cf}", "");
        return s.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\"’“”…]", "")
                .replaceAll("\\s*[–—-]\\s*", "-");
    }

    public static String str(JsonObject obj, String key, String def) {
        JsonElement el = obj == null ? null : obj.get(key);
        if (el == null || el.isJsonNull()) return def;
        return el.isJsonPrimitive() ? el.getAsString() : def;
    }

    public static int intVal(JsonObject obj, String key) {
        JsonElement el = obj == null ? null : obj.get(key);
        if (el == null || el.isJsonNull()) return 0;
        try { return el.getAsInt(); } catch (Exception e) { return 0; }
    }

    public static boolean boolVal(JsonObject obj, String key) {
        JsonElement el = obj == null ? null : obj.get(key);
        if (el == null || el.isJsonNull()) return false;
        try { return el.getAsBoolean(); } catch (Exception e) { return false; }
    }

    public static int arraySize(JsonObject obj, String key) {
        JsonElement el = obj == null ? null : obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonArray()) return 0;
        return el.getAsJsonArray().size();
    }

    public static String nestedStr(JsonObject obj, String parent, String child) {
        JsonElement p = obj == null ? null : obj.get(parent);
        if (p == null || p.isJsonNull() || !p.isJsonObject()) return "";
        return str(p.getAsJsonObject(), child, "");
    }
}
