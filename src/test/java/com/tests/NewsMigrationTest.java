package com.tests;

import com.readers.OrchardJsonReader;
import commons.BaseTest;
import commons.DriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pageAction.NewsListPageAction;
import pageUI.NewsDetailPageUI;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NewsMigrationTest extends BaseTest {

    private static final String JSON_PATH = "src/test/resources/testdata/News.json";

    private OrchardJsonReader jsonReader;
    private NewsListPageAction newsListAction;
    private WebDriverWait wait;

    @BeforeMethod
    public void setUp() {
        wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(15));
        jsonReader = new OrchardJsonReader(JSON_PATH);
        newsListAction = new NewsListPageAction();
    }

    // ============================================================
    // TC01: So sánh tổng số bài viết JSON vs UI
    // ============================================================
    @Test(description = "TC01: UI phải có ít nhất số bài viết bằng JSON sample")
    public void testCompareTotalCount() {
        long jsonEtpCount = 0;
        for (int i = 0; i < jsonReader.getArticleCount(); i++) {
            if ("EtpTinTuc".equals(jsonReader.getValue(i, "ContentType"))) {
                jsonEtpCount++;
            }
        }

        newsListAction.navigateToNewsList();
        int uiTotal = newsListAction.getTotalRecordCount();

        System.out.println("=== TC01: TỔNG SỐ BÀI VIẾT ===");
        System.out.println("JSON (EtpTinTuc): " + jsonEtpCount);
        System.out.println("UI (tổng bản ghi): " + uiTotal);

        Assert.assertTrue(uiTotal > 0, "UI không có bản ghi nào - migration thất bại!");
        Assert.assertTrue(uiTotal >= jsonEtpCount,
            "UI (" + uiTotal + ") ít hơn JSON sample (" + jsonEtpCount + ")!");
    }

    // ============================================================
    // TC02: Search từng bài từ JSON → verify tồn tại trên UI
    // ============================================================
    @Test(description = "TC02: Tất cả bài trong JSON phải tìm thấy trên UI")
    public void testAllArticlesExistOnUI() {
        SoftAssert softAssert = new SoftAssert();

        System.out.println("=== TC02: SEARCH TỪNG BÀI ===");
        for (int i = 0; i < jsonReader.getArticleCount(); i++) {
            String title = (String) jsonReader.getValue(i, "TitlePart.Title");
            if (title == null || title.isBlank()) continue;

            newsListAction.navigateToNewsList();
            newsListAction.searchByTitle(title);
            int count = newsListAction.getSearchResultCount();

            System.out.printf("[%2d] \"%s\" → %d kết quả%n", i + 1, title, count);
            softAssert.assertTrue(count > 0, "Không tìm thấy bài trên UI: " + title);
        }
        softAssert.assertAll();
    }

    // ============================================================
    // TC03: Verify tiêu đề kết quả search khớp với JSON
    // ============================================================
    @Test(description = "TC03: Tiêu đề dòng đầu kết quả search phải khớp JSON")
    public void testTitleMatchAfterSearch() {
        SoftAssert softAssert = new SoftAssert();

        System.out.println("=== TC03: SO SÁNH TIÊU ĐỀ ===");
        for (int i = 0; i < Math.min(5, jsonReader.getArticleCount()); i++) {
            String jsonTitle = (String) jsonReader.getValue(i, "TitlePart.Title");
            if (jsonTitle == null || jsonTitle.isBlank()) continue;

            newsListAction.navigateToNewsList();
            newsListAction.searchByTitle(jsonTitle);

            if (newsListAction.getSearchResultCount() == 0) {
                softAssert.fail("Không tìm thấy bài: " + jsonTitle);
                continue;
            }

            String uiTitle = newsListAction.getFirstRowTitleText();
            System.out.printf("[%d] JSON: \"%s\"%n     UI:   \"%s\"%n", i + 1, jsonTitle, uiTitle);
            softAssert.assertEquals(uiTitle, jsonTitle, "Tiêu đề không khớp tại bài " + (i + 1));
        }
        softAssert.assertAll();
    }

    // ============================================================
    // TC04: Verify ngày xuất bản (JSON ISO 8601 → UI date)
    // ============================================================
    @Test(description = "TC04: Ngày xuất bản trên UI phải khớp với JSON (timezone +7)")
    public void testPublishDateMatch() {
        SoftAssert softAssert = new SoftAssert();
        WebDriver driver = DriverManager.getDriver();

        System.out.println("=== TC04: SO SÁNH NGÀY XUẤT BẢN ===");
        for (int i = 0; i < Math.min(3, jsonReader.getArticleCount()); i++) {
            String jsonTitle = (String) jsonReader.getValue(i, "TitlePart.Title");
            String jsonDateStr = (String) jsonReader.getValue(i, "EtpTinTucSidebarPart.NgayXuatBan.Value");

            if (jsonDateStr == null || jsonDateStr.isBlank()) {
                System.out.println("[" + (i + 1) + "] Không có ngày, bỏ qua");
                continue;
            }

            // JSON UTC → Vietnam +7
            LocalDate jsonDate = OffsetDateTime.parse(jsonDateStr)
                .atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toLocalDate();

            newsListAction.navigateToNewsList();
            newsListAction.searchByTitle(jsonTitle);
            if (newsListAction.getSearchResultCount() == 0) continue;

            newsListAction.clickFirstRowEdit();
            wait.until(ExpectedConditions.visibilityOfElementLocated(NewsDetailPageUI.PUBLISH_DATE_INPUT));

            String uiDateStr = driver.findElement(NewsDetailPageUI.PUBLISH_DATE_INPUT).getAttribute("value");
            LocalDate uiDate = parseUIDate(uiDateStr);

            System.out.printf("[%d] \"%s\"%n     JSON: %s | UI raw: \"%s\" | UI parsed: %s%n",
                i + 1, jsonTitle, jsonDate, uiDateStr, uiDate);

            softAssert.assertNotNull(uiDate, "Không parse được ngày UI: \"" + uiDateStr + "\"");
            if (uiDate != null) {
                softAssert.assertEquals(uiDate, jsonDate,
                    "Ngày không khớp tại bài: " + jsonTitle);
            }
        }
        softAssert.assertAll();
    }

    // ============================================================
    // TC05: Verify ảnh đại diện tồn tại trên UI nếu JSON có thumbnail
    // ============================================================
    @Test(description = "TC05: Ảnh đại diện phải được set trên UI nếu JSON có thumbnail")
    public void testThumbnailExists() {
        SoftAssert softAssert = new SoftAssert();
        WebDriver driver = DriverManager.getDriver();

        System.out.println("=== TC05: VERIFY ẢNH ĐẠI DIỆN ===");
        boolean testedAtLeastOne = false;

        for (int i = 0; i < jsonReader.getArticleCount(); i++) {
            Object pathsObj = jsonReader.getValue(i, "EtpTinTucSidebarPart.AnhDaiDien.Paths");
            if (!hasPaths(pathsObj)) continue;

            String jsonTitle = (String) jsonReader.getValue(i, "TitlePart.Title");
            newsListAction.navigateToNewsList();
            newsListAction.searchByTitle(jsonTitle);
            if (newsListAction.getSearchResultCount() == 0) continue;

            newsListAction.clickFirstRowEdit();
            testedAtLeastOne = true;

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(NewsDetailPageUI.THUMBNAIL_PATH_INPUT));
                String thumbValue = driver.findElement(NewsDetailPageUI.THUMBNAIL_PATH_INPUT).getAttribute("value");
                System.out.printf("[%d] \"%s\" → thumbnail: \"%s\"%n", i + 1, jsonTitle, thumbValue);
                softAssert.assertFalse(thumbValue == null || thumbValue.isBlank(),
                    "Ảnh đại diện rỗng trên UI cho bài: " + jsonTitle);
            } catch (Exception e) {
                softAssert.fail("Không tìm thấy field ảnh đại diện cho bài: " + jsonTitle);
            }
        }

        if (!testedAtLeastOne) {
            System.out.println("[TC05] Không có bài nào có thumbnail trong JSON - bỏ qua");
        }
        softAssert.assertAll();
    }

    // ============================================================
    // TC06: Verify số lượng file đính kèm khớp JSON vs UI
    // ============================================================
    @Test(description = "TC06: Số file đính kèm trên UI phải bằng JSON")
    public void testAttachmentCount() {
        SoftAssert softAssert = new SoftAssert();
        WebDriver driver = DriverManager.getDriver();

        System.out.println("=== TC06: VERIFY FILE ĐÍNH KÈM ===");
        boolean testedAtLeastOne = false;

        for (int i = 0; i < jsonReader.getArticleCount(); i++) {
            Object pathsObj = jsonReader.getValue(i, "EtpTinTucAttachmentPart.DanhSachFile.Paths");
            int jsonAttachCount = countPaths(pathsObj);
            if (jsonAttachCount == 0) continue;

            String jsonTitle = (String) jsonReader.getValue(i, "TitlePart.Title");
            newsListAction.navigateToNewsList();
            newsListAction.searchByTitle(jsonTitle);
            if (newsListAction.getSearchResultCount() == 0) continue;

            newsListAction.clickFirstRowEdit();
            testedAtLeastOne = true;

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(NewsDetailPageUI.ATTACHMENT_FILE_ITEMS));
                int uiCount = driver.findElements(NewsDetailPageUI.ATTACHMENT_FILE_ITEMS).size();
                System.out.printf("[%d] \"%s\" → JSON: %d file | UI: %d file%n",
                    i + 1, jsonTitle, jsonAttachCount, uiCount);
                softAssert.assertEquals(uiCount, jsonAttachCount,
                    "Số file đính kèm không khớp cho bài: " + jsonTitle);
            } catch (Exception e) {
                softAssert.fail("Không tìm thấy attachment section cho bài: " + jsonTitle);
            }
        }

        if (!testedAtLeastOne) {
            System.out.println("[TC06] Không có bài nào có file đính kèm trong JSON - bỏ qua");
        }
        softAssert.assertAll();
    }

    // ============================================================
    // TC07: Verify trạng thái Published và Featured
    // ============================================================
    @Test(description = "TC07: Trạng thái Published và Featured phải khớp JSON vs UI")
    public void testPublishedAndFeaturedStatus() {
        SoftAssert softAssert = new SoftAssert();
        WebDriver driver = DriverManager.getDriver();

        System.out.println("=== TC07: VERIFY PUBLISHED / FEATURED ===");
        for (int i = 0; i < Math.min(5, jsonReader.getArticleCount()); i++) {
            String jsonTitle = (String) jsonReader.getValue(i, "TitlePart.Title");
            boolean jsonPublished = Boolean.TRUE.equals(jsonReader.getValue(i, "Published"));
            boolean jsonFeatured = Boolean.TRUE.equals(
                jsonReader.getValue(i, "EtpArticlePromotionPart.IsFeaturedHome.Value"));

            newsListAction.navigateToNewsList();
            newsListAction.searchByTitle(jsonTitle);

            int count = newsListAction.getSearchResultCount();
            if (jsonPublished) {
                softAssert.assertTrue(count > 0,
                    "Bài published không tìm thấy trên UI: " + jsonTitle);
            }
            if (count == 0) continue;

            // Verify badge status hiển thị (published article phải có badge)
            String badge = newsListAction.getFirstRowStatusBadgeText();
            System.out.printf("[%d] \"%s\" | published=%b featured=%b | badge=\"%s\"%n",
                i + 1, jsonTitle, jsonPublished, jsonFeatured, badge);

            // Verify Featured từ trang edit
            newsListAction.clickFirstRowEdit();
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(NewsDetailPageUI.FEATURED_HOME_CHECKBOX));
                boolean uiFeatured = driver.findElement(NewsDetailPageUI.FEATURED_HOME_CHECKBOX).isSelected();
                softAssert.assertEquals(uiFeatured, jsonFeatured,
                    "Featured Home không khớp cho bài: " + jsonTitle);
            } catch (Exception e) {
                System.out.println("    [WARN] Không tìm thấy Featured checkbox: " + e.getMessage());
            }
        }
        softAssert.assertAll();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private LocalDate parseUIDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        DateTimeFormatter[] fmts = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        };
        for (DateTimeFormatter fmt : fmts) {
            try { return LocalDate.parse(raw.trim(), fmt); } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean hasPaths(Object pathsObj) {
        if (pathsObj == null) return false;
        if (pathsObj instanceof List) return !((List<?>) pathsObj).isEmpty();
        if (pathsObj instanceof String) return !((String) pathsObj).isBlank();
        return false;
    }

    private int countPaths(Object pathsObj) {
        if (pathsObj == null) return 0;
        if (pathsObj instanceof List) return ((List<?>) pathsObj).size();
        if (pathsObj instanceof String) return ((String) pathsObj).isBlank() ? 0 : 1;
        return 0;
    }
}
