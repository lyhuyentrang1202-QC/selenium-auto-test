package com.utilities;

import com.google.gson.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Đọc toàn bộ file JSON crawl từ Orchard Core CMS và xuất ra Excel.
 *
 * Cấu trúc Excel:
 *   - Mỗi sheet = 1 ContentType (tên màn hình)
 *   - Dòng 1 = tiêu đề sheet (merged)
 *   - Dòng 2 = tên các trường (flattened dot-notation)
 *   - Dòng 3+ = giá trị từ từng bài viết
 *
 * Cách chạy:
 *   mvn exec:java -Dexec.mainClass="com.utilities.JsonToExcelExporter"
 *   hoặc: java -cp ... com.utilities.JsonToExcelExporter [inputDir] [output.xlsx]
 */
public class JsonToExcelExporter {

    private static final int MAX_CELL_LEN = 500;

    // Các field ưu tiên hiển thị đầu tiên trong header
    private static final List<String> PRIORITY_KEYS = Arrays.asList(
        "ContentItemId", "ContentType", "DisplayText", "Published", "Latest",
        "TitlePart.Title",
        "EtpArticleSourcePart.Source.Text",
        "EtpTinTucSidebarPart.NgayXuatBan.Value",
        "EtpArticlePromotionPart.IsFeaturedHome.Value"
    );

    public static void main(String[] args) throws Exception {
        String inputDir   = args.length > 0 ? args[0] : "src/test/resources/testdata";
        String outputPath = args.length > 1 ? args[1] : "output/export.xlsx";
        new JsonToExcelExporter().exportAll(inputDir, outputPath);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void exportAll(String inputDir, String outputPath) throws Exception {
        // ContentType → danh sách bài viết (mỗi bài = Map fieldName→value)
        Map<String, List<Map<String, String>>> sheetData = new LinkedHashMap<>();

        List<Path> jsonFiles = Files.walk(Paths.get(inputDir))
            .filter(p -> p.toString().endsWith(".json"))
            .sorted()
            .collect(Collectors.toList());

        if (jsonFiles.isEmpty()) {
            System.out.println("Không tìm thấy file JSON trong: " + inputDir);
            return;
        }

        for (Path jsonFile : jsonFiles) {
            System.out.println("Đang xử lý: " + jsonFile.getFileName());
            List<JsonObject> items = parseOrchardJson(jsonFile.toString());
            System.out.println("  → " + items.size() + " items");

            for (JsonObject item : items) {
                String contentType = getStr(item, "ContentType");
                if (contentType.isEmpty()) {
                    contentType = jsonFile.getFileName().toString().replace(".json", "");
                }
                Map<String, String> flat = flattenObject(item, "");
                sheetData.computeIfAbsent(contentType, k -> new ArrayList<>()).add(flat);
            }
        }

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);

            for (Map.Entry<String, List<Map<String, String>>> entry : sheetData.entrySet()) {
                String sheetName = safeSheetName(entry.getKey());
                List<Map<String, String>> rows = entry.getValue();
                List<String> columns = buildColumnOrder(rows);
                writeSheet(wb, st, sheetName, columns, rows);
            }

            Files.createDirectories(Paths.get(outputPath).getParent() != null
                ? Paths.get(outputPath).getParent() : Paths.get("."));
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
        }
        System.out.println("\nXuất xong: " + new File(outputPath).getAbsolutePath());
    }

    // ── JSON parsing ──────────────────────────────────────────────────────────

    /** Hỗ trợ: Orchard format {steps:[{name:"Content",data:[...]}]}, array, hoặc object đơn lẻ */
    private List<JsonObject> parseOrchardJson(String filePath) {
        List<JsonObject> result = new ArrayList<>();
        try {
            String raw = new String(Files.readAllBytes(Paths.get(filePath)));
            JsonElement root = JsonParser.parseString(raw);

            if (root.isJsonObject()) {
                JsonObject rootObj = root.getAsJsonObject();
                if (rootObj.has("steps")) {
                    // Orchard Core CMS export format
                    for (JsonElement step : rootObj.getAsJsonArray("steps")) {
                        JsonObject stepObj = step.getAsJsonObject();
                        if ("Content".equals(getStr(stepObj, "name")) && stepObj.has("data")) {
                            for (JsonElement item : stepObj.getAsJsonArray("data")) {
                                if (item.isJsonObject()) result.add(item.getAsJsonObject());
                            }
                        }
                    }
                } else {
                    result.add(rootObj);
                }
            } else if (root.isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray()) {
                    if (el.isJsonObject()) result.add(el.getAsJsonObject());
                }
            }
        } catch (Exception e) {
            System.err.println("  [LỖI] Không đọc được file: " + e.getMessage());
        }
        return result;
    }

    // ── JSON flattening ───────────────────────────────────────────────────────

    /** Flatten đệ quy: {TitlePart:{Title:"ABC"}} → {"TitlePart.Title": "ABC"} */
    private Map<String, String> flattenObject(JsonObject obj, String prefix) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonElement val = entry.getValue();

            if (val.isJsonNull()) {
                result.put(key, "");
            } else if (val.isJsonPrimitive()) {
                result.put(key, truncate(val.getAsString()));
            } else if (val.isJsonObject()) {
                result.putAll(flattenObject(val.getAsJsonObject(), key));
            } else if (val.isJsonArray()) {
                result.put(key, flattenArray(val.getAsJsonArray()));
            }
        }
        return result;
    }

    private String flattenArray(JsonArray arr) {
        if (arr.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (JsonElement el : arr) {
            if (el.isJsonNull()) parts.add("");
            else if (el.isJsonPrimitive()) parts.add(el.getAsString());
            else if (el.isJsonObject()) {
                // Lấy value đơn giản nhất từ object (ưu tiên "Value", "Text", "Title", "Url")
                JsonObject o = el.getAsJsonObject();
                for (String k : Arrays.asList("Value", "Text", "Title", "Url", "Path")) {
                    if (o.has(k) && o.get(k).isJsonPrimitive()) {
                        parts.add(o.get(k).getAsString());
                        break;
                    }
                }
                // Nếu không có key ưu tiên, dùng toString rút gọn
                if (parts.size() < arr.size()) parts.add(truncate(el.toString()));
            } else {
                parts.add(truncate(el.toString()));
            }
        }
        String joined = String.join(" | ", parts);
        return truncate(joined);
    }

    // ── Column order ──────────────────────────────────────────────────────────

    private List<String> buildColumnOrder(List<Map<String, String>> rows) {
        // Thu thập tất cả key từ tất cả rows
        LinkedHashSet<String> allKeys = new LinkedHashSet<>();
        for (Map<String, String> row : rows) allKeys.addAll(row.keySet());

        // Priority keys trước, còn lại theo thứ tự xuất hiện
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String k : PRIORITY_KEYS) {
            if (allKeys.contains(k)) ordered.add(k);
        }
        ordered.addAll(allKeys);
        return new ArrayList<>(ordered);
    }

    // ── Excel writing ─────────────────────────────────────────────────────────

    private void writeSheet(XSSFWorkbook wb, Styles st, String sheetName,
                            List<String> columns, List<Map<String, String>> rows) {
        XSSFSheet sh = wb.createSheet(sheetName);
        sh.createFreezePane(0, 2); // Cố định 2 dòng đầu khi scroll

        // Dòng 0: Tiêu đề sheet
        Row titleRow = sh.createRow(0);
        titleRow.setHeightInPoints(26);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(sheetName);
        titleCell.setCellStyle(st.title);
        if (columns.size() > 1) {
            sh.addMergedRegion(new CellRangeAddress(0, 0, 0, columns.size() - 1));
        }

        // Dòng 1: Header (tên trường)
        Row hdrRow = sh.createRow(1);
        hdrRow.setHeightInPoints(18);
        for (int c = 0; c < columns.size(); c++) {
            Cell cell = hdrRow.createCell(c);
            cell.setCellValue(columns.get(c));
            cell.setCellStyle(st.header);
            sh.setColumnWidth(c, columnWidth(columns.get(c)));
        }

        // Dòng 2+: Dữ liệu
        for (int r = 0; r < rows.size(); r++) {
            Row row = sh.createRow(r + 2);
            row.setHeightInPoints(16);
            Map<String, String> rowData = rows.get(r);
            CellStyle cs = (r % 2 == 0) ? st.dataEven : st.dataOdd;
            for (int c = 0; c < columns.size(); c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(rowData.getOrDefault(columns.get(c), ""));
                cell.setCellStyle(cs);
            }
        }

        System.out.printf("  Sheet %-30s: %3d dòng, %d cột%n", "\"" + sheetName + "\"", rows.size(), columns.size());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getStr(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : "";
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > MAX_CELL_LEN ? s.substring(0, MAX_CELL_LEN) + "…" : s;
    }

    private String safeSheetName(String name) {
        String safe = name.replaceAll("[\\[\\]:*?/\\\\]", "_");
        return safe.length() > 31 ? safe.substring(0, 31) : safe;
    }

    private int columnWidth(String col) {
        String lc = col.toLowerCase();
        if (lc.contains("body") || lc.contains("html") || lc.contains("noidung")) return 60 * 256;
        if (lc.contains("title") || lc.contains("displaytext") || lc.contains("tieude")) return 50 * 256;
        if (lc.contains("id") && lc.length() < 20) return 32 * 256;
        if (lc.contains("date") || lc.contains("ngay") || lc.contains("utc")) return 26 * 256;
        if (lc.contains("path") || lc.contains("url") || lc.contains("source")) return 40 * 256;
        return 22 * 256;
    }

    // ── Styles ────────────────────────────────────────────────────────────────

    private static class Styles {
        final CellStyle title, header, dataEven, dataOdd;

        Styles(XSSFWorkbook wb) {
            title    = build(wb, new int[]{31,  73, 125}, true,  13, true);
            header   = build(wb, new int[]{68, 114, 196}, true,  10, true);
            dataEven = build(wb, new int[]{255, 255, 255}, false, 10, false);
            dataOdd  = build(wb, new int[]{242, 242, 242}, false, 10, false);
        }

        private CellStyle build(XSSFWorkbook wb, int[] bg, boolean whiteFg, int sz, boolean bold) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setFillForegroundColor(
                new XSSFColor(new byte[]{(byte) bg[0], (byte) bg[1], (byte) bg[2]}, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont f = wb.createFont();
            f.setFontHeightInPoints((short) sz);
            f.setBold(bold);
            if (whiteFg) f.setColor(IndexedColors.WHITE.getIndex());
            s.setFont(f);
            s.setAlignment(HorizontalAlignment.LEFT);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
            s.setWrapText(false);
            return s;
        }
    }
}
