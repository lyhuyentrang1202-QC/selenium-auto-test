package pageAction;

import commons.DriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pageUI.NewsListPageUI;

import java.time.Duration;
import java.util.List;

public class NewsListPageAction {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public NewsListPageAction() {
        this.driver = DriverManager.getDriver();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void navigateToNewsList() {
        driver.get(NewsListPageUI.NEWS_LIST_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(NewsListPageUI.TOTAL_RECORDS_LABEL));
    }

    // Parse "Tổng số bản ghi: 1.872" → 1872
    public int getTotalRecordCount() {
        WebElement label = wait.until(ExpectedConditions.visibilityOfElementLocated(NewsListPageUI.TOTAL_RECORDS_LABEL));
        return Integer.parseInt(label.getText().replaceAll("[^0-9]", ""));
    }

    public void searchByTitle(String title) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(NewsListPageUI.SEARCH_TITLE_INPUT));
        input.clear();
        input.sendKeys(title);
        input.submit();
        wait.until(ExpectedConditions.visibilityOfElementLocated(NewsListPageUI.TOTAL_RECORDS_LABEL));
    }

    public int getSearchResultCount() {
        String text = driver.findElement(NewsListPageUI.TOTAL_RECORDS_LABEL).getText();
        return Integer.parseInt(text.replaceAll("[^0-9]", ""));
    }

    public String getFirstRowTitleText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(NewsListPageUI.FIRST_ROW_TITLE_LINK))
                   .getText().trim();
    }

    public void clickFirstRowEdit() {
        wait.until(ExpectedConditions.elementToBeClickable(NewsListPageUI.FIRST_ROW_EDIT_BUTTON)).click();
    }

    public int getTableRowCount() {
        List<WebElement> rows = driver.findElements(NewsListPageUI.TABLE_ROWS);
        return rows.size();
    }

    public String getFirstRowStatusBadgeText() {
        try {
            return driver.findElement(NewsListPageUI.FIRST_ROW_STATUS_BADGE).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }
}
