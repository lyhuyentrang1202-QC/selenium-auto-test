package pageUI;

import org.openqa.selenium.By;

public class OldAdminNewsListPageUI {

    // ── Table ─────────────────────────────────────────────────────────────────

    // Kiểm tra table đã load — đợi có ít nhất 1 td
    public static final By TABLE_LOADED = By.cssSelector("table tbody tr td");

    // Tất cả row data (bỏ header)
    public static final By TABLE_ROWS = By.cssSelector("table tbody tr");

    // ── Columns (từ DOM thực tế: checkbox|STT|Tính năng|Tiêu đề|Trạng thái|Danh mục|Đồng bộ|Lượt xem|Ngày XB|Hành động)

    // Col 4 — Tiêu đề: <b> bên trong text anchor (image anchor không có <b>)
    public static final By ROW_TITLE      = By.cssSelector("td:nth-child(4) b");

    // Col 4 — Author: dòng nhỏ bên dưới tiêu đề
    public static final By ROW_AUTHOR     = By.cssSelector("td:nth-child(4) small");

    // Col 5 — Trạng thái badge
    public static final By ROW_STATUS     = By.cssSelector("td:nth-child(5) span");

    // Col 6 — Danh mục
    public static final By ROW_CATEGORY   = By.cssSelector("td:nth-child(6)");

    // Col 8 — Lượt xem
    public static final By ROW_VIEW_COUNT = By.cssSelector("td:nth-child(8)");

    // Col 9 — Ngày xuất bản
    public static final By ROW_DATE       = By.cssSelector("td:nth-child(9)");

    // ── Status bar (đầu trang): "Tổng số: 1873  Xuất bản: 1496 ..."

    // Tổng số bài viết (status bar)
    public static final By TOTAL_COUNT = By.xpath(
        "//span[contains(@class,'status-item') and .//span[contains(text(),'Tổng số:')]]");

    // Số bài Xuất bản (dùng để cross-check nhanh)
    public static final By PUBLISHED_COUNT = By.xpath(
        "//span[contains(@class,'status-item') and .//span[contains(text(),'Xuất bản:')]]");
}
