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
    private String lastKnownGroupId; // reuse across navigations when sidebar can't provide one

    public OldAdminNewsListPageAction() {
        this.driver = DriverManager.getDriver();
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    /**
     * Navigate đến màn list của 1 loại article.
     * Ưu tiên lấy groupId từ sidebar link trên trang hiện tại (chứa đúng groupId cho từng type).
     * Fallback về URL không có groupId nếu không tìm được.
     *
     * @param articleType  type value (ví dụ: "Article.News", "Article.LegalDocument")
     */
    public void navigateToScreen(String articleType) {
        String base = "https://langson.edu.vn/page/CMS/Admin/Article/list";

        // 1. Lấy groupId từ sidebar link
        String groupId = extractGroupIdFromSidebar(articleType);

        // 2. Fallback: dùng groupId đã biết từ lần navigate trước
        if (groupId == null || groupId.isEmpty()) {
            groupId = lastKnownGroupId;
        }

        if (groupId == null || groupId.isEmpty()) {
            System.err.println("[WARN] Không tìm được groupId cho: " + articleType);
        }

        String url = base + "?type=" + articleType
            + (groupId != null && !groupId.isEmpty() ? "&groupId=" + groupId : "")
            + "&id=" + articleType
            + "&menuId=" + articleType;

        driver.get(url);
        waitForTable();

        // 3. Nếu bị redirect sang login → session vẫn OK, thử lại qua sidebar click
        if (driver.getCurrentUrl().contains("login")) {
            System.err.println("[WARN] Bị redirect login khi navigate " + articleType + " — thử click sidebar");
            if (tryClickSidebarLink(articleType)) {
                System.out.println("  [Navigate] → " + articleType + " | URL: " + driver.getCurrentUrl());
                updateLastKnownGroupId();
                return;
            }
        }

        // 4. Nếu vẫn chưa có groupId, đọc lại từ pagination link trên trang vừa load
        if (groupId == null || groupId.isEmpty()) {
            groupId = extractGroupIdFromPagination();
            if (groupId != null && !groupId.isEmpty()) {
                System.out.println("  [Navigate] Lấy được groupId từ pagination: " + groupId);
                url = base + "?type=" + articleType + "&groupId=" + groupId
                    + "&id=" + articleType + "&menuId=" + articleType;
                driver.get(url);
                waitForTable();
            }
        }

        updateLastKnownGroupId();
        System.out.println("  [Navigate] → " + articleType + " | URL: " + driver.getCurrentUrl());
    }

    private void updateLastKnownGroupId() {
        String g = extractParam(driver.getCurrentUrl(), "groupId");
        if (g != null && !g.isEmpty()) lastKnownGroupId = g;
    }

    private boolean tryClickSidebarLink(String articleType) {
        try {
            String shortType = articleType.contains(".")
                ? articleType.substring(articleType.lastIndexOf('.') + 1)
                : articleType;
            List<WebElement> links = driver.findElements(By.xpath(
                "//a[contains(@href,'Article/list') and (contains(@href,'" + articleType + "') or contains(@href,'" + shortType + "'))]"));
            if (links.isEmpty()) return false;
            String href = links.get(0).getAttribute("href");
            if (href == null || href.isEmpty()) return false;
            driver.get(href);
            waitForTable();
            return !driver.getCurrentUrl().contains("login");
        } catch (Exception ignored) {
            return false;
        }
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
     * Dừng khi: 0 rows trên trang, bị redirect sang login, hoặc đã đủ totalCount.
     * Không thêm pageSize vào URL — để server dùng pageSize mặc định.
     */
    public List<Map<String, String>> getAllRows() {
        List<Map<String, String>> all = new ArrayList<>();

        int totalCount = getTotalCount();
        System.out.printf("  [Old UI] Total count: %d%n", totalCount);

        String baseUrl = driver.getCurrentUrl();
        int page = 1;

        while (true) {
            String pageUrl = setUrlParam(baseUrl, "page", String.valueOf(page));
            driver.get(pageUrl);

            // Dừng sớm nếu bị redirect sang login (session hết hoặc URL sai)
            if (driver.getCurrentUrl().contains("login")) {
                System.err.printf("  [Old UI] Page %d → redirect login, dừng%n", page);
                break;
            }

            waitForTable();

            List<WebElement> rows = driver.findElements(OldAdminNewsListPageUI.TABLE_ROWS);
            if (rows.isEmpty()) {
                System.out.printf("  [Old UI] Page %d → 0 rows → dừng%n", page);
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

            System.out.printf("  [Old UI] Page %d → %d rows (tổng: %d/%d)%n",
                page, added, all.size(), totalCount);

            if (totalCount > 0 && all.size() >= totalCount) break;
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

    /**
     * Tìm groupId từ sidebar link ứng với articleType, dùng XPath để tránh CSS encoding issues.
     */
    private String extractGroupIdFromSidebar(String articleType) {
        try {
            String shortType = articleType.contains(".")
                ? articleType.substring(articleType.lastIndexOf('.') + 1)
                : articleType;
            // XPath tìm link có groupId và chứa articleType hoặc shortType trong href
            for (String pattern : new String[]{articleType, shortType}) {
                List<WebElement> links = driver.findElements(By.xpath(
                    "//a[contains(@href,'groupId') and contains(@href,'" + pattern + "')]"));
                if (!links.isEmpty()) {
                    String href = links.get(0).getAttribute("href");
                    return extractParam(href, "groupId");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Lấy groupId từ link phân trang trên trang hiện tại (fallback). */
    private String extractGroupIdFromPagination() {
        try {
            List<WebElement> links = driver.findElements(
                OldAdminNewsListPageUI.PAGINATION_LINK_WITH_GROUPID);
            if (!links.isEmpty()) {
                String href = links.get(0).getAttribute("href");
                return extractParam(href, "groupId");
            }
        } catch (Exception ignored) {}
        return null;
    }
}
