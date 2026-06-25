package com.utilities;

import com.google.gson.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.util.zip.*;

/**
 * Đọc toàn bộ file ZIP cào từ langson.edu.vn → xuất TOÀN BỘ fields ra Excel.
 *
 * Cách chạy:
 *   mvn test-compile exec:java
 *
 * Output: output/export.xlsx
 *   - Sheet "Tổng hợp": thống kê
 *   - 1 sheet / loại article, 1 dòng / bài, cột = mọi field có trong JSON
 */
public class JsonToExcelExporter {

    // Tên tiếng Việt cho sheet
    private static final Map<String, String> TYPE_LABELS = new LinkedHashMap<>();
    static {
        TYPE_LABELS.put("Article.News",                    "Tin tức");
        TYPE_LABELS.put("Article.LegalDocument",           "Văn bản pháp quy");
        TYPE_LABELS.put("Article.Download",                "Tài liệu tải về");
        TYPE_LABELS.put("Article.Video",                   "Video");
        TYPE_LABELS.put("Article.PhotoAlbum",              "Album ảnh");
        TYPE_LABELS.put("Article.AdministrativeProcedure", "Thủ tục hành chính");
        TYPE_LABELS.put("Article.Person",                  "Cán bộ - Nhân sự");
        TYPE_LABELS.put("Article.UnitDirectory",           "Danh mục đơn vị");
        TYPE_LABELS.put("Article.PDF",                     "PDF");
        TYPE_LABELS.put("Article.Infographic",             "Infographic");
    }

    // Thứ tự ưu tiên cột — những key này ra trước, còn lại ra sau theo alphabet
    private static final List<String> COL_PRIORITY = Arrays.asList(
        "id", "type", "title", "subTitle",
        "editorialStatus", "displayStatus",
        "publishedAt", "pinDate", "crawledAt",
        "author", "source", "creator", "tenant",
        "viewCount", "wordCount", "imageCount",
        "category", "keywords",
        "publicUrl", "editUrl",
        "thumbnail.url", "thumbnail.localPath", "thumbnail.width", "thumbnail.height", "thumbnail.alt",
        "flags.isFeatured", "flags.isFocus", "flags.syncNews", "flags.requireLogin",
        "seo.title", "seo.description", "seo.keywords",
        "video.url", "video.uploadType", "video.isExternal",
        "legalDocument.code", "legalDocument.documentType", "legalDocument.organization",
        "legalDocument.signer", "legalDocument.element", "legalDocument.scope",
        "legalDocument.issueTime", "legalDocument.effectiveTime", "legalDocument.expirationDate",
        "administrativeProcedure.number", "administrativeProcedure.fieldProcedure",
        "administrativeProcedure.perform", "administrativeProcedure.termOfSettlement",
        "administrativeProcedure.addressReceiving", "administrativeProcedure.result",
        "administrativeProcedure.chargeFee", "administrativeProcedure.numberOfRecord",
        "attachments#count", "attachments#names", "attachments#urls", "attachments#localPaths",
        "images#count", "images#urls",
        "gallery#count", "gallery#titles", "gallery#urls",
        "relatedNews#count", "relatedNews#titles",
        "errors#count", "errors#messages",
        "contentHtml"
    );

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        String inputDir   = args.length > 0 ? args[0]
            : "src/test/resources/testdata/lsn-sogd-crawler/lsn-sogd-crawler";
        String outputPath = args.length > 1 ? args[1] : "output/export.xlsx";
        new JsonToExcelExporter().run(inputDir, outputPath);
    }

    public void run(String inputDir, String outputPath) throws Exception {
        System.out.println("Đang đọc ZIP từ: " + inputDir);

        // 1. Đọc & dedup
        Map<String, JsonObject> byId = new LinkedHashMap<>();
        List<Path> zips = Files.walk(Paths.get(inputDir))
            .filter(p -> p.toString().endsWith(".zip"))
            .sorted()
            .collect(Collectors.toList());
        System.out.println("Tìm thấy " + zips.size() + " file ZIP");

        for (Path zipPath : zips) {
            readArticlesFromZip(zipPath, byId);
        }
        System.out.println("Tổng cộng " + byId.size() + " bài viết (unique)");

        // 2. Group theo type
        Map<String, List<JsonObject>> byType = new LinkedHashMap<>();
        for (JsonObject article : byId.values()) {
            String type = str(article, "type", "unknown");
            byType.computeIfAbsent(type, k -> new ArrayList<>()).add(article);
        }
        byType.forEach((t, list) ->
            System.out.printf("  %-45s : %4d bài%n", t, list.size()));

        // 3. Xuất Excel
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            buildSummarySheet(wb, st, byType);
            for (Map.Entry<String, List<JsonObject>> e : byType.entrySet()) {
                buildTypeSheet(wb, st, e.getKey(), e.getValue());
            }
            Path out = Paths.get(outputPath);
            if (out.getParent() != null) Files.createDirectories(out.getParent());
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
        }
        System.out.println("\nXuất xong: " + new File(outputPath).getAbsolutePath());
    }

    // ── Đọc ZIP ───────────────────────────────────────────────────────────────

    private void readArticlesFromZip(Path zipPath, Map<String, JsonObject> byId) {
        try (ZipInputStream zis = new ZipInputStream(
                new FileInputStream(zipPath.toFile()), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().endsWith("article.json")) { zis.closeEntry(); continue; }
                try {
                    byte[] bytes = zis.readAllBytes();
                    JsonObject obj = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8))
                                               .getAsJsonObject();
                    String id = str(obj, "id", null);
                    if (id != null && !byId.containsKey(id)) {
                        byId.put(id, obj);
                    }
                } catch (Exception e) {
                    System.err.println("  Lỗi đọc " + entry.getName() + " trong "
                        + zipPath.getFileName() + ": " + e.getMessage());
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            System.err.println("Lỗi mở ZIP " + zipPath.getFileName() + ": " + e.getMessage());
        }
    }

    // ── Flatten article → Map<String, String> ────────────────────────────────

    /**
     * Chuyển một JsonObject thành map phẳng key→value để ghi vào Excel.
     *
     * Quy tắc:
     *   Primitive       → giữ nguyên (contentHtml truncate 800 ký tự)
     *   Object đơn      → flatten với prefix "key.subKey"
     *   Mảng string     → join với "; "
     *   Mảng object     → key#count + join các sub-field quan trọng
     */
    private Map<String, String> flatten(JsonObject article) {
        Map<String, String> flat = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : article.entrySet()) {
            flattenField(e.getKey(), e.getValue(), flat);
        }
        return flat;
    }

    private void flattenField(String key, JsonElement val, Map<String, String> out) {
        if (val == null || val.isJsonNull()) {
            out.put(key, "");
            return;
        }
        if (val.isJsonPrimitive()) {
            String s = val.getAsString();
            if (key.equals("contentHtml")) s = truncate(s, 800);
            out.put(key, s);
            return;
        }
        if (val.isJsonObject()) {
            flattenObject(key, val.getAsJsonObject(), out);
            return;
        }
        if (val.isJsonArray()) {
            flattenArray(key, val.getAsJsonArray(), out);
        }
    }

    private void flattenObject(String prefix, JsonObject obj, Map<String, String> out) {
        if (obj.size() == 0) return;
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            String childKey = prefix + "." + e.getKey();
            JsonElement child = e.getValue();
            if (child == null || child.isJsonNull()) {
                out.put(childKey, "");
            } else if (child.isJsonPrimitive()) {
                out.put(childKey, child.getAsString());
            } else if (child.isJsonObject()) {
                flattenObject(childKey, child.getAsJsonObject(), out);
            } else if (child.isJsonArray()) {
                flattenArray(childKey, child.getAsJsonArray(), out);
            }
        }
    }

    private void flattenArray(String key, JsonArray arr, Map<String, String> out) {
        if (arr.size() == 0) {
            out.put(key + "#count", "0");
            return;
        }

        // Mảng primitive → join thẳng
        if (arr.get(0).isJsonPrimitive()) {
            List<String> items = new ArrayList<>();
            for (JsonElement e : arr) {
                if (!e.isJsonNull()) items.add(e.getAsString());
            }
            out.put(key, String.join("; ", items));
            return;
        }

        // Mảng object → count + join các sub-field quan trọng
        out.put(key + "#count", String.valueOf(arr.size()));

        switch (key) {
            case "attachments": {
                List<String> names  = new ArrayList<>();
                List<String> urls   = new ArrayList<>();
                List<String> locals = new ArrayList<>();
                for (JsonElement e : arr) {
                    if (!e.isJsonObject()) continue;
                    JsonObject o = e.getAsJsonObject();
                    names.add(strEl(o,  "name",      ""));
                    urls.add(strEl(o,   "url",       ""));
                    locals.add(strEl(o, "localPath", ""));
                }
                out.put("attachments#names",      truncate(String.join("; ", names),  1500));
                out.put("attachments#urls",       truncate(String.join("; ", urls),   1500));
                out.put("attachments#localPaths", truncate(String.join("; ", locals), 1500));
                break;
            }
            case "images": {
                List<String> urls = new ArrayList<>();
                for (JsonElement e : arr) {
                    if (e.isJsonObject()) urls.add(strEl(e.getAsJsonObject(), "url", ""));
                }
                out.put("images#urls", truncate(String.join("; ", urls), 1500));
                break;
            }
            case "gallery": {
                List<String> titles = new ArrayList<>();
                List<String> urls   = new ArrayList<>();
                for (JsonElement e : arr) {
                    if (!e.isJsonObject()) continue;
                    JsonObject o = e.getAsJsonObject();
                    titles.add(strEl(o, "title", ""));
                    urls.add(strEl(o,   "url",   ""));
                }
                out.put("gallery#titles", truncate(String.join("; ", titles), 1500));
                out.put("gallery#urls",   truncate(String.join("; ", urls),   1500));
                break;
            }
            case "relatedNews": {
                List<String> titles = new ArrayList<>();
                for (JsonElement e : arr) {
                    if (e.isJsonObject()) titles.add(strEl(e.getAsJsonObject(), "title", ""));
                }
                out.put("relatedNews#titles", truncate(String.join("; ", titles), 1500));
                break;
            }
            case "errors": {
                List<String> msgs = new ArrayList<>();
                for (JsonElement e : arr) {
                    if (e.isJsonObject()) {
                        msgs.add(strEl(e.getAsJsonObject(), "message", e.toString()));
                    } else if (!e.isJsonNull()) {
                        msgs.add(e.getAsString());
                    }
                }
                out.put("errors#messages", truncate(String.join("; ", msgs), 800));
                break;
            }
            default: {
                // Mảng object khác chưa biết: flatten item đầu tiên để capture schema
                JsonElement first = arr.get(0);
                if (first.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> e : first.getAsJsonObject().entrySet()) {
                        if (e.getValue().isJsonPrimitive()) {
                            out.put(key + "[0]." + e.getKey(), e.getValue().getAsString());
                        }
                    }
                }
            }
        }
    }

    // ── Xây dựng danh sách cột ────────────────────────────────────────────────

    private List<String> buildColOrder(List<JsonObject> articles) {
        // Collect tất cả keys thực tế trong bộ dữ liệu
        Set<String> allKeys = new LinkedHashSet<>();
        for (JsonObject a : articles) {
            allKeys.addAll(flatten(a).keySet());
        }

        // Priority trước, còn lại alphabet
        List<String> ordered = new ArrayList<>();
        for (String p : COL_PRIORITY) {
            if (allKeys.remove(p)) ordered.add(p);
        }
        List<String> remaining = new ArrayList<>(allKeys);
        Collections.sort(remaining);
        ordered.addAll(remaining);
        return ordered;
    }

    // ── Sheet tổng hợp ────────────────────────────────────────────────────────

    private void buildSummarySheet(XSSFWorkbook wb, Styles st,
                                   Map<String, List<JsonObject>> byType) {
        XSSFSheet sh = wb.createSheet("Tổng hợp");
        sh.setColumnWidth(0, 6 * 256);
        sh.setColumnWidth(1, 42 * 256);
        sh.setColumnWidth(2, 14 * 256);
        sh.setColumnWidth(3, 36 * 256);

        int r = 0;
        titleRow(sh, st, r++, "TỔNG HỢP DỮ LIỆU CÀO - LANGSON.EDU.VN", 4);
        Row hdr = sh.createRow(r++);
        cell(hdr, 0, "STT",             st.header);
        cell(hdr, 1, "Loại bài",        st.header);
        cell(hdr, 2, "Số lượng",        st.header);
        cell(hdr, 3, "Sheet tương ứng", st.header);

        int stt = 1;
        for (Map.Entry<String, List<JsonObject>> e : byType.entrySet()) {
            Row row = sh.createRow(r++);
            String label = TYPE_LABELS.getOrDefault(e.getKey(), e.getKey());
            cell(row, 0, String.valueOf(stt++), st.dataEven);
            cell(row, 1, label,                 st.dataEven);
            cell(row, 2, String.valueOf(e.getValue().size()), st.dataEven);
            cell(row, 3, sheetName(e.getKey()), st.dataEven);
        }
        sh.createFreezePane(0, 2);
    }

    // ── Sheet theo loại ───────────────────────────────────────────────────────

    private void buildTypeSheet(XSSFWorkbook wb, Styles st,
                                String type, List<JsonObject> articles) {
        String sName  = sheetName(type);
        String sTitle = TYPE_LABELS.getOrDefault(type, type).toUpperCase();
        XSSFSheet sh  = wb.createSheet(sName);

        List<String> cols = buildColOrder(articles);

        // Độ rộng cột
        for (int c = 0; c < cols.size(); c++) {
            sh.setColumnWidth(c, colWidth(cols.get(c)));
        }

        // Dòng tiêu đề
        int r = 0;
        titleRow(sh, st, r++, sTitle + " (" + articles.size() + " bài)", cols.size());

        // Dòng header = tên key JSON
        Row hdrRow = sh.createRow(r++);
        hdrRow.setHeightInPoints(18);
        for (int c = 0; c < cols.size(); c++) {
            cell(hdrRow, c, cols.get(c), st.header);
        }

        // Dữ liệu
        for (int i = 0; i < articles.size(); i++) {
            Map<String, String> flat = flatten(articles.get(i));
            Row row = sh.createRow(r++);
            row.setHeightInPoints(15);
            CellStyle cs = (i % 2 == 0) ? st.dataEven : st.dataOdd;
            for (int c = 0; c < cols.size(); c++) {
                cell(row, c, flat.getOrDefault(cols.get(c), ""), cs);
            }
        }

        // Freeze: 2 cột đầu (id, type/title) + 2 dòng header
        sh.createFreezePane(2, 2);
        System.out.printf("  Sheet %-32s : %d cột, %d dòng%n",
            "\"" + sName + "\"", cols.size(), articles.size());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String str(JsonObject obj, String key, String def) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return def;
        return el.isJsonPrimitive() ? el.getAsString() : def;
    }

    private String strEl(JsonObject obj, String key, String def) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return def;
        return el.isJsonPrimitive() ? el.getAsString() : def;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private String sheetName(String type) {
        String label = TYPE_LABELS.getOrDefault(type, type);
        String safe  = label.replaceAll("[\\[\\]:*?/\\\\]", " ").trim();
        return safe.length() > 31 ? safe.substring(0, 31) : safe;
    }

    private int colWidth(String key) {
        if (key.equals("id"))              return 32 * 256;
        if (key.equals("title") || key.equals("subTitle")) return 55 * 256;
        if (key.equals("contentHtml"))     return 80 * 256;
        if (key.contains("#names") || key.contains("#titles")
         || key.contains("#urls")  || key.contains("#messages")
         || key.contains("#localPaths"))   return 60 * 256;
        if (key.contains("url") || key.contains("Url") || key.contains("Path")) return 50 * 256;
        if (key.contains("At") || key.contains("Time") || key.contains("Date")) return 22 * 256;
        if (key.contains("Status") || key.contains("status"))  return 24 * 256;
        if (key.contains("#count") || key.contains("Count"))   return 12 * 256;
        return 22 * 256;
    }

    // ── Excel helpers ─────────────────────────────────────────────────────────

    private void titleRow(XSSFSheet sh, Styles st, int rowNum, String text, int span) {
        Row row = sh.createRow(rowNum);
        row.setHeightInPoints(24);
        Cell c = row.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(st.title);
        if (span > 1) sh.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, span - 1));
    }

    private void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        c.setCellStyle(style);
    }

    // ── Styles ────────────────────────────────────────────────────────────────

    private static class Styles {
        final CellStyle title, header, dataEven, dataOdd;

        Styles(XSSFWorkbook wb) {
            title    = make(wb, new int[]{31,  73, 125}, true,  13, true,  HorizontalAlignment.CENTER);
            header   = make(wb, new int[]{68, 114, 196}, true,  10, true,  HorizontalAlignment.CENTER);
            dataEven = make(wb, new int[]{255, 255, 255}, false, 10, false, HorizontalAlignment.LEFT);
            dataOdd  = make(wb, new int[]{235, 241, 252}, false, 10, false, HorizontalAlignment.LEFT);
        }

        private CellStyle make(XSSFWorkbook wb, int[] bg, boolean whiteFg,
                               int sz, boolean bold, HorizontalAlignment align) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setFillForegroundColor(
                new XSSFColor(new byte[]{(byte) bg[0], (byte) bg[1], (byte) bg[2]}, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont f = wb.createFont();
            f.setFontHeightInPoints((short) sz);
            f.setBold(bold);
            if (whiteFg) f.setColor(IndexedColors.WHITE.getIndex());
            s.setFont(f);
            s.setAlignment(align);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setBorderTop(BorderStyle.THIN);    s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);   s.setBorderRight(BorderStyle.THIN);
            s.setWrapText(false);
            return s;
        }
    }
}
