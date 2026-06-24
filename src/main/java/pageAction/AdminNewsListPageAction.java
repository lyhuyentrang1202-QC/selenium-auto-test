package pageAction;

import commons.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pageUI.AdminNewsListPageUI;

import java.time.Duration;
import java.util.*;

public class AdminNewsListPageAction {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public AdminNewsListPageAction() {
        this.driver = DriverManager.getDriver();
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void navigateTo() {
        driver.get(AdminNewsListPageUI.URL);
        waitForTable();
    }

    /** Đọc tổng số record từ label phân trang. */
    public int getTotalCount() {
        try {
            WebElement el = wait.until(
                ExpectedConditions.presenceOfElementLocated(AdminNewsListPageUI.TOTAL_COUNT));
            String text = el.getText();
            String digits = text.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            System.err.println("[WARN] Không đọc được total count: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Lấy toàn bộ rows từ list (tự paginate).
     * Mỗi row = map gồm: title, status, date, editUrl.
     */
    public List<Map<String, String>> getAllRows() {
        List<Map<String, String>> all = new ArrayList<>();
        int page = 1;

        while (true) {
            waitForTable();
            List<WebElement> rows = driver.findElements(AdminNewsListPageUI.TABLE_ROWS);

            for (WebElement row : rows) {
                Map<String, String> data = new HashMap<>();
                data.put("title",   safeText(row, AdminNewsListPageUI.ROW_TITLE));
                data.put("status",  safeText(row, AdminNewsListPageUI.ROW_STATUS));
                data.put("date",    safeText(row, AdminNewsListPageUI.ROW_DATE));
                data.put("editUrl", safeAttr(row, AdminNewsListPageUI.ROW_EDIT, "href"));
                if (!data.get("title").isEmpty()) all.add(data);
            }

            System.out.printf("  [Page %d] Đọc %d rows (tổng: %d)%n", page, rows.size(), all.size());

            // Kiểm tra còn trang tiếp không
            List<WebElement> nextBtns = driver.findElements(AdminNewsListPageUI.PAGE_NEXT);
            if (nextBtns.isEmpty() || !nextBtns.get(0).isEnabled()
                    || nextBtns.get(0).getAttribute("class") != null
                       && nextBtns.get(0).getAttribute("class").contains("disabled")) {
                break;
            }

            try {
                nextBtns.get(0).click();
                page++;
                Thread.sleep(500);
            } catch (Exception e) {
                break;
            }
        }

        return all;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void waitForTable() {
        wait.until(ExpectedConditions.presenceOfElementLocated(AdminNewsListPageUI.TABLE_ROWS));
    }

    private String safeText(WebElement parent, By by) {
        try { return parent.findElement(by).getText().trim(); }
        catch (NoSuchElementException e) { return ""; }
    }

    private String safeAttr(WebElement parent, By by, String attr) {
        try { return parent.findElement(by).getAttribute(attr); }
        catch (NoSuchElementException e) { return ""; }
    }
}
