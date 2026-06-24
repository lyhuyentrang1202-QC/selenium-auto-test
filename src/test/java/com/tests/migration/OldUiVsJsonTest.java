package com.tests.migration;

import com.comparators.MigrationResult;
import com.datastore.ArticleDataStore;
import com.google.gson.JsonObject;
import commons.BaseTest;
import commons.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pageAction.OldAdminNewsListPageAction;

import java.time.Duration;
import java.util.*;

/**
 * So sánh Old Admin UI (langson.edu.vn) với JSON crawled data.
 *
 * Flow login đúng:
 *   1. Mở langson.edu.vn/page/CMS/Admin/ → tự redirect sang SSO
 *   2. SSO login xong → redirect ngược về langson.edu.vn admin
 *   3. Navigate qua sidebar menu → lấy đúng URL với groupId
 */
public class OldUiVsJsonTest extends BaseTest {

    // type → text hiển thị trong sidebar
    private static final String[][] TYPES = {
        {"Article.News",                    "Tin tức"},
        {"Article.LegalDocument",           "Văn bản pháp quy"},
        {"Article.Video",                   "Video clip"},
        {"Article.PhotoAlbum",              "Thư viện ảnh"},
        {"Article.Download",                "Tải về"},
        {"Article.AdministrativeProcedure", "Thủ tục hành chính"},
        {"Article.Person",                  "Nhân sự"},
    };

    private static final double COUNT_TOLERANCE = 0.10;
    private static final double MATCH_TOLERANCE = 0.05;

    private ArticleDataStore store;
    private OldAdminNewsListPageAction oldUi;
    private final List<MigrationResult> allResults = new ArrayList<>();

    // ── Override login: đợi element thực tế trên langson.edu.vn admin ──────────

    @Override
    protected void loginToAdmin() {
        WebDriver driver = DriverManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            System.out.println(">>> Start URL: " + driver.getCurrentUrl());

            // langson.edu.vn/page/login — click link "Đăng nhập bằng tài khoản SSO"
            wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a[href*='redirectLogin'], a[href*='EduSSO'], a[href*='SSO']")));
            WebElement ssoLink = driver.findElement(
                By.cssSelector("a[href*='redirectLogin'], a[href*='EduSSO'], a[href*='SSO']"));
            System.out.println(">>> Clicking SSO link: " + ssoLink.getAttribute("href"));
            ssoLink.click();

            // Đợi redirect sang id.nentanggiaoduc.edu.vn
            wait.until(ExpectedConditions.urlContains("id.nentanggiaoduc"));
            System.out.println(">>> SSO page: " + driver.getCurrentUrl());

            // Điền credentials SSO
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")));
            driver.findElement(By.name("username")).sendKeys("tuannd25@viettel.com.vn");
            driver.findElement(By.name("password")).sendKeys("Smas@2025");
            driver.findElement(By.cssSelector("button[type='submit']")).click();
            System.out.println(">>> SSO form submitted");

            // Đợi redirect về langson.edu.vn
            wait.until(ExpectedConditions.urlContains("langson.edu.vn"));
            Thread.sleep(2000);

            // Navigate đến article list — không dùng groupId vì groupId có thể không phù hợp với account
            driver.get("https://langson.edu.vn/page/CMS/Admin/Article/list?type=Article.News&id=Article.News&menuId=Article.News");
            Thread.sleep(1500);
            System.out.println(">>> Login OK | URL: " + driver.getCurrentUrl());

        } catch (Exception e) {
            System.out.println(">>> Login ERROR: " + e.getMessage());
            try { System.out.println(">>> Current URL: " + driver.getCurrentUrl()); }
            catch (Exception ignored) {}
        }
    }

    @BeforeClass
    public void setup() {
        store = ArticleDataStore.get();
        oldUi = new OldAdminNewsListPageAction();
        System.out.println("\n=== BẮT ĐẦU KIỂM TRA: Old UI vs JSON ===");
        System.out.println("JSON loaded: " + store.total() + " articles\n");
    }

    // ── Test duy nhất — loop qua tất cả types, dùng SoftAssert ───────────────

    @Test(description = "So sánh toàn bộ loại article: Old UI vs JSON")
    public void compareAllTypes() {
        SoftAssert soft = new SoftAssert();

        for (String[] typeEntry : TYPES) {
            String type       = typeEntry[0];
            String screenName = typeEntry[1];

            MigrationResult result = new MigrationResult(screenName);
            result.jsonTotal = store.countByType(type);

            System.out.printf("%n--- [%s] JSON=%d bài ---%n", screenName, result.jsonTotal);

            try {
                // 1. Navigate đến màn article list theo type
                oldUi.navigateToScreen(type);
                result.uiTotal = oldUi.getTotalCount();
                System.out.printf("  Old UI total  : %d%n", result.uiTotal);

                // 2. Đọc toàn bộ rows
                List<Map<String, String>> uiRows = oldUi.getAllRows();
                result.checked = uiRows.size();

                // Chỉ giữ bài Xuất bản từ UI
                Map<String, Map<String, String>> uiPublished = new LinkedHashMap<>();
                for (Map<String, String> row : uiRows) {
                    if (isPublished(row.get("status"))) {
                        uiPublished.put(ArticleDataStore.normalize(row.get("title")), row);
                    }
                }
                System.out.printf("  Old UI published: %d / %d%n", uiPublished.size(), uiRows.size());

                // Chỉ giữ bài Xuất bản từ JSON
                Map<String, JsonObject> jsonPublished = new LinkedHashMap<>();
                for (JsonObject a : store.getByType(type)) {
                    String status = ArticleDataStore.str(a, "editorialStatus", "");
                    if (status.contains("Xuất bản")) {
                        String title = ArticleDataStore.str(a, "title", "");
                        jsonPublished.put(ArticleDataStore.normalize(title), a);
                    }
                }
                System.out.printf("  JSON published  : %d%n", jsonPublished.size());

                // 3. UI → JSON
                int matchCount = 0;
                for (String norm : uiPublished.keySet()) {
                    if (jsonPublished.containsKey(norm)) {
                        matchCount++;
                    } else {
                        result.extra.add(uiPublished.get(norm).get("title"));
                    }
                }

                // 4. JSON → UI (bài bị thiếu)
                for (String norm : jsonPublished.keySet()) {
                    if (!uiPublished.containsKey(norm)) {
                        result.missing.add(ArticleDataStore.str(jsonPublished.get(norm), "title", ""));
                    }
                }
                result.matched = matchCount;

                // In warnings
                if (!result.extra.isEmpty()) {
                    System.out.printf("  [WARN] %d bài trên Old UI KHÔNG có trong JSON:%n", result.extra.size());
                    result.extra.stream().limit(5).forEach(t -> System.out.println("    - " + t));
                }
                if (!result.missing.isEmpty()) {
                    System.out.printf("  [WARN] %d bài trong JSON KHÔNG thấy trên Old UI:%n", result.missing.size());
                    result.missing.stream().limit(5).forEach(t -> System.out.println("    - " + t));
                }

                double matchPct = uiPublished.isEmpty() ? 1.0
                    : (double) matchCount / uiPublished.size();
                System.out.printf("  Match: %d/%d (%.1f%%)  %s%n",
                    matchCount, uiPublished.size(), matchPct * 100,
                    matchPct >= (1 - MATCH_TOLERANCE) ? "✓" : "✗");

                // SoftAssert — không throw ngay, ghi nhận tiếp type khác
                if (result.uiTotal > 0 && result.jsonTotal > 0) {
                    double countDiff = Math.abs(result.uiTotal - result.jsonTotal) * 1.0 / result.uiTotal;
                    soft.assertTrue(countDiff <= COUNT_TOLERANCE,
                        String.format("[%s] Count lệch %.1f%% (UI=%d JSON=%d)",
                            screenName, countDiff * 100, result.uiTotal, result.jsonTotal));
                }
                if (!uiPublished.isEmpty()) {
                    soft.assertTrue(matchPct >= (1 - MATCH_TOLERANCE),
                        String.format("[%s] Match chỉ %.1f%%", screenName, matchPct * 100));
                }

            } catch (org.openqa.selenium.WebDriverException e) {
                // Chrome crashed hoặc session mất — không tiếp tục được
                System.err.printf("  [SKIP] %s: browser session lost: %s%n", screenName, e.getMessage().split("\n")[0]);
                soft.fail("[" + screenName + "] Browser crashed: " + e.getMessage().split("\n")[0]);
                break;
            } catch (Exception e) {
                System.err.printf("  [ERROR] %s: %s%n", screenName, e.getMessage());
                soft.fail("[" + screenName + "] Exception: " + e.getMessage());
            }

            allResults.add(result);
        }

        // In báo cáo tổng hợp
        printReport();

        // Throw tất cả assertion failures cùng 1 lúc ở cuối
        soft.assertAll();
    }

    // ── Báo cáo ───────────────────────────────────────────────────────────────

    private void printReport() {
        System.out.println("\n" + "=".repeat(78));
        System.out.println("BÁO CÁO: OLD UI vs JSON");
        System.out.println("=".repeat(78));
        System.out.printf("%-25s %7s %7s %7s %7s %7s  %s%n",
            "Màn hình", "UI", "JSON", "Match", "Thiếu", "Thừa", "");
        System.out.println("-".repeat(78));

        for (MigrationResult r : allResults) {
            System.out.printf("%-25s %7d %7d %7d %7d %7d  %s%n",
                r.screenName, r.uiTotal, r.jsonTotal, r.matched,
                r.missing.size(), r.extra.size(),
                r.passed() ? "✓ PASS" : "✗ FAIL");
        }
        System.out.println("=".repeat(78));
    }

    private boolean isPublished(String status) {
        if (status == null) return false;
        return status.contains("Xuất bản") || status.toLowerCase().contains("published");
    }
}
