package pageUI;

import org.openqa.selenium.By;

public class NewsListPageUI {

    public static final String NEWS_LIST_URL = "https://sgddt-langson.ddns.net/Admin/EducationTrainingPortal/TinTuc";

    // Tổng bản ghi (vd: "Tổng số bản ghi: 1.872")
    public static final By TOTAL_RECORDS_LABEL = By.cssSelector("div.pagination-left.margin-right-xs");

    // Ô tìm kiếm tiêu đề
    public static final By SEARCH_TITLE_INPUT = By.xpath("//input[@placeholder='Tìm theo tiêu đề...']");

    // Dòng trong bảng kết quả
    public static final By TABLE_ROWS = By.cssSelector("table tbody tr");

    // Link tiêu đề dòng đầu tiên (thường là <a> đầu tiên trong row)
    public static final By FIRST_ROW_TITLE_LINK = By.cssSelector("table tbody tr:first-child td a:first-of-type");

    // Nút Edit dòng đầu tiên
    public static final By FIRST_ROW_EDIT_BUTTON = By.cssSelector("table tbody tr:first-child a[href*='/Edit']");

    // Badge trạng thái dòng đầu tiên
    public static final By FIRST_ROW_STATUS_BADGE = By.cssSelector("table tbody tr:first-child .badge, table tbody tr:first-child .label");
}
