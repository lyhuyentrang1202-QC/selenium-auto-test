package com.utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Đọc file article.json (từ crawler LSN-SOGD) và xuất tài liệu Excel
 * mô tả các màn hình, trường dữ liệu và button theo schema trong README.
 *
 * Cách dùng:
 *   JsonToExcelExporter.main(new String[]{"path/to/article.json"})
 * hoặc:
 *   new JsonToExcelExporter().export("path/to/article.json", "output.xlsx")
 */
public class JsonToExcelExporter {

    private static final String[] COL_HEADERS = {
        "STT", "Loại", "Tên trường / Button", "JSON Key",
        "Kiểu dữ liệu", "Bắt buộc", "Locator (CSS / XPath)", "Mô tả", "Giá trị mẫu (từ JSON)"
    };

    public static void main(String[] args) throws Exception {
        String jsonPath   = args.length > 0 ? args[0] : "src/test/resources/testdata/article.json";
        String outputPath = args.length > 1 ? args[1] : "target/test-documentation.xlsx";
        new JsonToExcelExporter().export(jsonPath, outputPath);
        System.out.println("Excel exported: " + outputPath);
    }

    public void export(String jsonFilePath, String outputPath) throws Exception {
        JsonObject article = loadFirstArticle(jsonFilePath);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);

            buildIndexSheet(wb, st);
            buildLoginSheet(wb, st, article);
            buildNewsListSheet(wb, st, article);
            buildNewsDetailSheet(wb, st, article);

            new File(outputPath).getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
        }
        System.out.println("Done. File: " + new File(outputPath).getAbsolutePath());
    }

    // ── JSON helpers ─────────────────────────────────────────────────────────

    private JsonObject loadFirstArticle(String path) {
        try {
            String raw = new String(Files.readAllBytes(Paths.get(path)));
            JsonElement el = JsonParser.parseString(raw);
            if (el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                if (!arr.isEmpty() && arr.get(0).isJsonObject()) return arr.get(0).getAsJsonObject();
            }
            if (el.isJsonObject()) return el.getAsJsonObject();
        } catch (Exception e) {
            System.err.println("Cannot read JSON: " + e.getMessage());
        }
        return new JsonObject();
    }

    /** Lấy giá trị tại dot-notation key, vd "thumbnail.url", "flags.isFeatured". */
    private String val(JsonObject root, String dotKey) {
        if (root == null || dotKey == null || dotKey.isEmpty()) return "";
        try {
            String[] parts = dotKey.split("\\.");
            JsonObject cur = root;
            for (int i = 0; i < parts.length - 1; i++) {
                if (!cur.has(parts[i])) return "";
                JsonElement e = cur.get(parts[i]);
                if (!e.isJsonObject()) return "";
                cur = e.getAsJsonObject();
            }
            String last = parts[parts.length - 1];
            if (!cur.has(last)) return "";
            JsonElement v = cur.get(last);
            if (v.isJsonNull()) return "";
            if (v.isJsonArray() || v.isJsonObject()) {
                String s = v.toString();
                return s.length() > 120 ? s.substring(0, 120) + "…" : s;
            }
            return v.getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    private String clip(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ── Sheet builders ────────────────────────────────────────────────────────

    private void buildIndexSheet(XSSFWorkbook wb, Styles st) {
        XSSFSheet sh = wb.createSheet("Danh sach man hinh");
        sh.setColumnWidth(0, 6 * 256);
        sh.setColumnWidth(1, 32 * 256);
        sh.setColumnWidth(2, 60 * 256);
        sh.setColumnWidth(3, 25 * 256);

        int r = 0;
        r = mergedTitle(sh, st, r, "DANH SÁCH MÀN HÌNH TEST", 4);
        r++;

        Row hdr = sh.createRow(r++);
        cell(hdr, 0, "STT", st.colHdr);
        cell(hdr, 1, "Tên màn hình", st.colHdr);
        cell(hdr, 2, "URL / Ghi chú", st.colHdr);
        cell(hdr, 3, "Sheet tương ứng", st.colHdr);

        Object[][] rows = {
            {"1", "Đăng nhập",               "https://id.nentanggiaoduc.edu.vn/",                                          "Dang nhap"},
            {"2", "Danh sách Tin Tức",        "https://sgddt-langson.ddns.net/Admin/EducationTrainingPortal/TinTuc",         "Danh sach Tin Tuc"},
            {"3", "Chi tiết / Sửa Tin Tức",  "/Admin/EducationTrainingPortal/TinTuc/Edit/{id}",                             "Chi tiet Tin Tuc"},
        };
        for (Object[] d : rows) {
            Row row = sh.createRow(r++);
            for (int c = 0; c < d.length; c++) cell(row, c, (String) d[c], st.data);
        }
    }

    private void buildLoginSheet(XSSFWorkbook wb, Styles st, JsonObject article) {
        XSSFSheet sh = wb.createSheet("Dang nhap");
        applyWidths(sh);
        int r = 0;
        r = mergedTitle(sh, st, r, "MÀN ĐĂNG NHẬP", COL_HEADERS.length);
        r++;
        r = columnHeaders(sh, st, r);

        r = sectionRow(sh, st, r, "TRƯỜNG NHẬP LIỆU");
        r = fieldRow(sh, st, r, 1, "Field",   "Tài khoản (Username)", "",         "string (text)",     "Có",     "//input[@name='username']",                                  "Nhập tên đăng nhập", "");
        r = fieldRow(sh, st, r, 2, "Field",   "Mật khẩu (Password)",  "",         "string (password)", "Có",     "//input[@name='password']",                                  "Nhập mật khẩu", "");
        r = fieldRow(sh, st, r, 3, "Display", "Thông báo lỗi",        "",         "string",            "Không",  ".alert-danger, .validation-summary-errors",                  "Hiển thị khi đăng nhập sai", "");

        r = sectionRow(sh, st, r, "BUTTON");
        r = fieldRow(sh, st, r, 1, "Button",  "Đăng nhập",            "",         "—",                 "—",      "//button[@type='submit']",                                   "Gửi form đăng nhập", "");
        r = fieldRow(sh, st, r, 2, "Button",  "Quên mật khẩu",        "",         "—",                 "—",      "link text: Quên mật khẩu",                                   "Chuyển trang reset mật khẩu", "");
    }

    private void buildNewsListSheet(XSSFWorkbook wb, Styles st, JsonObject article) {
        XSSFSheet sh = wb.createSheet("Danh sach Tin Tuc");
        applyWidths(sh);
        int r = 0;
        r = mergedTitle(sh, st, r, "MÀN DANH SÁCH TIN TỨC", COL_HEADERS.length);
        r++;
        r = columnHeaders(sh, st, r);

        r = sectionRow(sh, st, r, "TRƯỜNG HIỂN THỊ / TÌM KIẾM");
        r = fieldRow(sh, st, r, 1, "Display", "Tổng số bản ghi",      "",               "number",  "Không", "div.pagination-left.margin-right-xs",                         "Tổng số bài trong hệ thống", "");
        r = fieldRow(sh, st, r, 2, "Field",   "Tìm theo tiêu đề",     "title",          "string",  "Không", "//input[@placeholder='Tìm theo tiêu đề...']",                 "Ô tìm kiếm bài theo tiêu đề", clip(val(article, "title"), 80));
        r = fieldRow(sh, st, r, 3, "Display", "Tiêu đề bài (cột)",    "title",          "string",  "—",     "table tbody tr td a:first-of-type",                           "Link tiêu đề trong bảng",     clip(val(article, "title"), 80));
        r = fieldRow(sh, st, r, 4, "Display", "Trạng thái (cột)",     "editorialStatus","string",  "—",     "table tbody tr .badge, table tbody tr .label",                "Badge trạng thái biên tập",   val(article, "editorialStatus"));

        r = sectionRow(sh, st, r, "BUTTON");
        r = fieldRow(sh, st, r, 1, "Button",  "Sửa / Edit",           "",               "—",       "—",     "table tbody tr a[href*='/Edit']",                             "Mở trang sửa bài viết", "");
        r = fieldRow(sh, st, r, 2, "Button",  "Thêm mới",             "",               "—",       "—",     "a[href*='/Create'], .btn-create",                            "Tạo bài viết mới", "");
    }

    private void buildNewsDetailSheet(XSSFWorkbook wb, Styles st, JsonObject article) {
        XSSFSheet sh = wb.createSheet("Chi tiet Tin Tuc");
        applyWidths(sh);
        int r = 0;
        r = mergedTitle(sh, st, r, "MÀN CHI TIẾT / SỬA TIN TỨC", COL_HEADERS.length);
        r++;
        r = columnHeaders(sh, st, r);

        // Thông tin cơ bản
        r = sectionRow(sh, st, r, "THÔNG TIN CƠ BẢN");
        r = fieldRow(sh, st, r, 1, "Field", "Tiêu đề",        "title",       "string",       "Có",    "input[id*='TitlePart']",                              "Tiêu đề bài viết",               clip(val(article, "title"), 100));
        r = fieldRow(sh, st, r, 2, "Field", "Tiêu đề phụ",    "subTitle",    "string",       "Không", "input[id*='SubTitle'], input[name*='subTitle']",       "Tiêu đề phụ của bài",            val(article, "subTitle"));
        r = fieldRow(sh, st, r, 3, "Field", "Tóm tắt",        "summary",     "string (text)","Không", "textarea[id*='brief'], textarea[name*='brief']",       "Tóm tắt ngắn (text thuần)",      clip(val(article, "summary"), 100));
        r = fieldRow(sh, st, r, 4, "Field", "Nội dung (HTML)", "contentHtml", "string (HTML)","Không", "iframe.cke_wysiwyg_frame, .ck-editor__editable",      "CKEditor — nội dung bài",        "(HTML content)");
        r = fieldRow(sh, st, r, 5, "Field", "Danh mục",       "category",    "string[]",     "Không", "select[id*='category'], input[id*='category']",        "Danh mục bài viết",              clip(val(article, "category"), 100));
        r = fieldRow(sh, st, r, 6, "Field", "Từ khóa",        "keywords",    "string[]",     "Không", "input[id*='tag'], input[name*='tag']",                 "Từ khóa (phân cách bằng dấu phẩy)", clip(val(article, "keywords"), 100));
        r = fieldRow(sh, st, r, 7, "Field", "Dòng sự kiện",   "eventFlow",   "string",       "Không", "input[id*='EventFlow'], input[name*='eventFlow']",     "Dòng sự kiện",                   val(article, "eventFlow"));
        r = fieldRow(sh, st, r, 8, "Field", "Slug / URL đẹp", "friendlyUrl", "string",       "Không", "input[id*='rewriteURL'], input[name*='rewriteURL']",   "Đường dẫn thân thiện",           val(article, "friendlyUrl"));

        // Ngày tháng
        r = sectionRow(sh, st, r, "NGÀY THÁNG  (định dạng dd/MM/yyyy [HH:mm:ss] — múi giờ VN)");
        r = fieldRow(sh, st, r, 1, "Field", "Ngày xuất bản",  "publishedAt", "date string",  "Không", "input[id*='NgayXuatBan'], input[name*='NgayXuatBan']", "VD: 05/09/2025 08:00:00",        val(article, "publishedAt"));
        r = fieldRow(sh, st, r, 2, "Field", "Ngày ghim",      "pinDate",     "date string",  "Không", "input[id*='PinDate'], input[name*='pinDate']",         "Ghim bài lên đầu đến ngày này",  val(article, "pinDate"));
        r = fieldRow(sh, st, r, 3, "Field", "Ngày hết hạn",   "expireDate",  "date string",  "Không", "input[id*='ExpireDate'], input[name*='expireDate']",   "Hết hạn hiển thị",               val(article, "expireDate"));

        // Tác giả & Nguồn
        r = sectionRow(sh, st, r, "TÁC GIẢ & NGUỒN");
        r = fieldRow(sh, st, r, 1, "Field", "Tác giả",        "author",      "string",       "Không", "input[id*='Author'], input[name*='author']",           "Tên tác giả",                    val(article, "author"));
        r = fieldRow(sh, st, r, 2, "Field", "Bút danh",       "penName",     "string",       "Không", "input[id*='PenName'], input[name*='penName']",         "Bút danh tác giả",               val(article, "penName"));
        r = fieldRow(sh, st, r, 3, "Field", "Nguồn tin",      "source",      "string",       "Không", "input[id*='Source'], input[name*='source']",           "Nguồn gốc bài viết",             val(article, "source"));
        r = fieldRow(sh, st, r, 4, "Field", "Người tạo",      "creator",     "string",       "Không", "(hiển thị — không chỉnh sửa được)",                   "Người tạo bài (từ list-scraper)", val(article, "creator"));

        // Media
        r = sectionRow(sh, st, r, "MEDIA");
        r = fieldRow(sh, st, r, 1, "Field",   "Ảnh đại diện (URL)",       "thumbnail.url",   "URL/file", "Không", "input[id*='AnhDaiDien'], input[name*='AnhDaiDien']",   "URL ảnh đại diện bài",           val(article, "thumbnail.url"));
        r = fieldRow(sh, st, r, 2, "Display", "Ảnh đại diện (alt)",       "thumbnail.alt",   "string",   "Không", "img[id*='AnhDaiDien'], .thumbnail-preview img",        "Alt text ảnh đại diện",          val(article, "thumbnail.alt"));
        r = fieldRow(sh, st, r, 3, "Field",   "File đính kèm (danh sách)","attachments",     "object[]", "Không", "[id*='DanhSachFile'] li, .attachment-list li",         "Danh sách PDF/DOC/ZIP đính kèm", clip(val(article, "attachments"), 100));
        r = fieldRow(sh, st, r, 4, "Field",   "Video (URL)",              "video.url",       "URL",      "Không", "input[id*='videoLink'], input[name*='videoLink']",      "Video nhúng (YouTube) hoặc upload", val(article, "video.url"));

        // Flags / Cờ
        r = sectionRow(sh, st, r, "CỜ / FLAGS (checkbox boolean)");
        r = fieldRow(sh, st, r, 1, "Field", "Tin nổi bật",       "flags.isFeatured",  "boolean", "Không", "input[type='checkbox'][id*='IsFeaturedHome']",   "Đánh dấu tin nổi bật trang chủ",  val(article, "flags.isFeatured"));
        r = fieldRow(sh, st, r, 2, "Field", "Tin tiêu điểm",     "flags.isFocus",     "boolean", "Không", "input[type='checkbox'][id*='IsFocus']",          "Đánh dấu tin tiêu điểm",          val(article, "flags.isFocus"));
        r = fieldRow(sh, st, r, 3, "Field", "Đồng bộ tin tức",   "flags.syncNews",    "boolean", "Không", "input[type='checkbox'][id*='SyncNews']",         "Bật đồng bộ tin tức",             val(article, "flags.syncNews"));
        r = fieldRow(sh, st, r, 4, "Field", "Yêu cầu đăng nhập", "flags.requireLogin","boolean", "Không", "input[type='checkbox'][id*='RequireLogin']",     "Bài cần đăng nhập mới xem",       val(article, "flags.requireLogin"));

        // SEO
        r = sectionRow(sh, st, r, "SEO");
        r = fieldRow(sh, st, r, 1, "Field", "SEO Tiêu đề",   "seo.title",       "string", "Không", "input[id*='seoTitle'], input[name*='seoTitle']",                   "Meta title",       val(article, "seo.title"));
        r = fieldRow(sh, st, r, 2, "Field", "SEO Mô tả",     "seo.description", "string", "Không", "textarea[id*='seoDescription'], textarea[name*='seoDescription']", "Meta description", clip(val(article, "seo.description"), 100));
        r = fieldRow(sh, st, r, 3, "Field", "SEO Từ khóa",   "seo.keywords",    "string", "Không", "input[id*='seoKeywords'], input[name*='seoKeywords']",             "Meta keywords",    val(article, "seo.keywords"));

        // Trạng thái (display)
        r = sectionRow(sh, st, r, "TRẠNG THÁI (chỉ đọc — hiển thị trên form)");
        r = fieldRow(sh, st, r, 1, "Display", "Trạng thái biên tập", "editorialStatus", "string", "—", ".badge-success, .status-published, span.badge",  "VD: Xuất bản / Lưu nháp / Chờ duyệt", val(article, "editorialStatus"));
        r = fieldRow(sh, st, r, 2, "Display", "Trạng thái hiển thị", "displayStatus",   "string", "—", ".display-status, [class*='display-status']",      "Hiển thị bài viết / Ẩn bài viết",     val(article, "displayStatus"));

        // Buttons
        r = sectionRow(sh, st, r, "BUTTON");
        r = fieldRow(sh, st, r, 1, "Button", "Lưu nháp",             "", "—", "—", "button[name='submit.Save'], button:contains('Lưu')",    "Lưu bài ở trạng thái nháp", "");
        r = fieldRow(sh, st, r, 2, "Button", "Xuất bản",             "", "—", "—", "button[name='submit.Publish'], .btn-publish",            "Đổi trạng thái → Xuất bản",  "");
        r = fieldRow(sh, st, r, 3, "Button", "Hủy xuất bản",         "", "—", "—", "button[name='submit.Unpublish'], .btn-unpublish",        "Gỡ bài đã xuất bản",         "");
        r = fieldRow(sh, st, r, 4, "Button", "Xem trước",            "", "—", "—", "a[href*='preview'], .btn-preview",                      "Xem bài trên giao diện công khai", "");
        r = fieldRow(sh, st, r, 5, "Button", "Quay lại danh sách",   "", "—", "—", "a[href*='TinTuc']:not([href*='Edit']), .btn-back",       "Quay về màn danh sách", "");
    }

    // ── Row writers ───────────────────────────────────────────────────────────

    private int mergedTitle(XSSFSheet sh, Styles st, int rowNum, String title, int colSpan) {
        Row row = sh.createRow(rowNum);
        row.setHeightInPoints(28);
        Cell c = row.createCell(0);
        c.setCellValue(title);
        c.setCellStyle(st.sheetTitle);
        if (colSpan > 1)
            sh.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, colSpan - 1));
        return rowNum + 1;
    }

    private int columnHeaders(XSSFSheet sh, Styles st, int rowNum) {
        Row row = sh.createRow(rowNum);
        row.setHeightInPoints(20);
        for (int i = 0; i < COL_HEADERS.length; i++) cell(row, i, COL_HEADERS[i], st.colHdr);
        return rowNum + 1;
    }

    private int sectionRow(XSSFSheet sh, Styles st, int rowNum, String label) {
        Row row = sh.createRow(rowNum);
        row.setHeightInPoints(18);
        Cell c = row.createCell(0);
        c.setCellValue(label);
        c.setCellStyle(st.section);
        sh.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, COL_HEADERS.length - 1));
        return rowNum + 1;
    }

    private int fieldRow(XSSFSheet sh, Styles st, int rowNum,
                         int stt, String type, String name, String jsonKey,
                         String dataType, String required, String locator,
                         String desc, String sample) {
        Row row = sh.createRow(rowNum);
        row.setHeightInPoints(16);
        CellStyle cs = type.equals("Button") ? st.buttonRow : st.dataRow;
        cell(row, 0, String.valueOf(stt), cs);
        cell(row, 1, type, cs);
        cell(row, 2, name, cs);
        cell(row, 3, jsonKey, cs);
        cell(row, 4, dataType, cs);
        cell(row, 5, required, cs);
        cell(row, 6, locator, cs);
        cell(row, 7, desc, cs);
        cell(row, 8, sample, cs);
        return rowNum + 1;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void applyWidths(XSSFSheet sh) {
        int[] widths = {5, 10, 28, 28, 22, 10, 52, 35, 38};
        for (int i = 0; i < widths.length; i++) sh.setColumnWidth(i, widths[i] * 256);
    }

    private void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        c.setCellStyle(style);
    }

    // ── Styles ────────────────────────────────────────────────────────────────

    private static class Styles {
        final CellStyle sheetTitle, colHdr, section, dataRow, buttonRow, data;

        Styles(XSSFWorkbook wb) {
            sheetTitle = build(wb, new int[]{31, 73, 125},  true,  13, true,  HorizontalAlignment.CENTER);
            colHdr     = build(wb, new int[]{68, 114, 196}, true,  10, true,  HorizontalAlignment.CENTER);
            section    = build(wb, new int[]{255, 242, 204},false, 10, true,  HorizontalAlignment.LEFT);
            dataRow    = build(wb, new int[]{255, 255, 255},false, 10, false, HorizontalAlignment.LEFT);
            buttonRow  = build(wb, new int[]{226, 239, 218},false, 10, false, HorizontalAlignment.LEFT);
            data       = build(wb, new int[]{255, 255, 255},false, 10, false, HorizontalAlignment.LEFT);
        }

        private CellStyle build(XSSFWorkbook wb, int[] bg, boolean whiteFg,
                                int sz, boolean bold, HorizontalAlignment align) {
            XSSFCellStyle s = wb.createCellStyle();
            XSSFColor bgColor = new XSSFColor(
                new byte[]{(byte) bg[0], (byte) bg[1], (byte) bg[2]}, null);
            s.setFillForegroundColor(bgColor);
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFFont font = wb.createFont();
            font.setFontHeightInPoints((short) sz);
            font.setBold(bold);
            if (whiteFg) font.setColor(IndexedColors.WHITE.getIndex());
            s.setFont(font);

            s.setAlignment(align);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
            s.setWrapText(true);
            return s;
        }
    }
}
