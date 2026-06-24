package pageAction;

import commons.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pageUI.OldAdminNewsListPageUI;

import java.time.Duration;
import java.util.*;

public class OldAdminNewsListPageAction {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public OldAdminNewsListPageAction() {
        this.driver = DriverManager.getDriver();
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    /**
     * Navigate đến màn list của 1 loại article bằng URL.
     * Lấy groupId từ URL hiện tại (đã đúng sau login), chỉ thay type/id/menuId.
     *
     * @param articleType  type value (ví dụ: "Article.News", "Article.LegalDocument")
     */
    public void navigateToScreen(String articleType) {
        String currentUrl = driver.getCurrentUrl();

        // Lấy groupId từ URL hiện tại
        String groupId = extractParam(currentUrl, "groupId");
        if (groupId == null || groupId.isEmpty()) {
            System.err.println("[WARN] Không tìm được groupId trong URL: " + currentUrl);
        }

        // Xây dựng URL mới với type mới, giữ groupId
        String base = "https://langson.edu.vn/page/CMS/Admin/Article/list";
        String url = base + "?type=" + articleType
            + (groupId != null ? "&groupId=" + groupId : "")
            + "&id=" + articleType
            + "&menuId=" + articleType;

        driver.get(url);
        waitForTable();
        System.out.println("  [Navigate] → " + articleType + " | URL: " + driver.getCurrentUrl());
    }

    /** Đọc tổng số bản ghi ("Tổng số bản ghi: 1.873") */
    public int getTotalCount() {
        try {
            WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(
                OldAdminNewsListPageUI.TOTAL_COUNT));
            String digits = el.getText().replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            System.err.println("[WARN] Không đọc được total count: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Đọc TOÀN BỘ rows qua pagination.
     * Chiến lược:
     *   1. Lấy URL hiện tại (đã có groupId đúng từ menu navigation)
     *   2. Thêm pageSize=50 vào URL để giảm số trang
     *   3. Paginate qua từng trang bằng URL (page=1, page=2...)
     *   4. Dừng khi không còn row hoặc số row < pageSize
     */
    public List<Map<String, String>> getAllRows() {
        List<Map<String, String>> all = new ArrayList<>();

        // Lấy base URL hiện tại (đúng groupId, đúng type)
        String baseUrl = driver.getCurrentUrl();

        // Thêm pageSize=50 — giảm từ ~93 trang xuống ~37 trang
        int pageSize = 50;
        String pagedBase = setUrlParam(baseUrl, "pageSize", String.valueOf(pageSize));

        int page = 1;
        while (true) {
            String pageUrl = setUrlParam(pagedBase, "page", String.valueOf(page));
            driver.get(pageUrl);
            waitForTable();

            List<WebElement> rows = driver.findElements(OldAdminNewsListPageUI.TABLE_ROWS);
            if (rows.isEmpty()) {
                System.out.printf("  [Old UI] Page %d — 0 rows → dừng%n", page);
                break;
            }

            int added = 0;
            for (WebElement row : rows) {
                String title = safeText(row, OldAdminNewsListPageUI.ROW_TITLE);
                if (title.isEmpty()) continue;

                Map<String, String> data = new LinkedHashMap<>();
                data.put("title",     title);
                data.put("status",    safeText(row, OldAdminNewsListPageUI.ROW_STATUS));
                data.put("date",      safeText(row, OldAdminNewsListPageUI.ROW_DATE));
                data.put("category",  safeText(row, OldAdminNewsListPageUI.ROW_CATEGORY));
                data.put("author",    safeText(row, OldAdminNewsListPageUI.ROW_AUTHOR));
                data.put("viewCount", safeText(row, OldAdminNewsListPageUI.ROW_VIEW_COUNT));
                all.add(data);
                added++;
            }

            System.out.printf("  [Old UI] Page %d — %d rows (tổng: %d)%n", page, added, all.size());

            // Trang cuối khi số row < pageSize
            if (rows.size() < pageSize) break;
            page++;
        }

        return all;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Lấy giá trị 1 query param từ URL. */
    private String extractParam(String url, String key) {
        String[] parts = url.split("[?&]");
        for (String part : parts) {
            if (part.startsWith(key + "=")) {
                return part.substring(key.length() + 1);
            }
        }
        return null;
    }

    /** Thêm hoặc thay thế 1 query param trong URL. */
    private String setUrlParam(String url, String key, String value) {
        String encoded = key + "=" + value;
        if (url.contains(key + "=")) {
            return url.replaceAll(key + "=[^&]*", encoded);
        }
        return url + (url.contains("?") ? "&" : "?") + encoded;
    }

    private void waitForTable() {
        // Bước 1: đợi table xuất hiện (cấu trúc DOM)
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                OldAdminNewsListPageUI.TABLE_LOADED));
        } catch (Exception e) {
            System.err.println("[WARN] Table không load được: " + e.getMessage());
            return;
        }
        // Bước 2: đợi title <b> load xong (image anchor không có <b>, chỉ text anchor có)
        WebDriverWait titleWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        try {
            titleWait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("table.inlineEditTable tbody tr td:nth-child(4) b")));
        } catch (Exception e) {
            // Có thể trang không có bài nào — tiếp tục
        }
    }

    private String safeText(WebElement parent, By by) {
        try { return parent.findElement(by).getText().trim(); }
        catch (NoSuchElementException e) { return ""; }
    }
}
