package pageUI;

import org.openqa.selenium.By;

public class AdminNewsListPageUI {

    public static final String URL = "https://sgddt-langson.ddns.net/Admin/EducationTrainingPortal/TinTuc";

    // Tổng số record (ví dụ "Tổng số bản ghi: 1.659")
    public static final By TOTAL_COUNT = By.cssSelector(".pagination-left, .total-records, [class*='total']");

    // Các row trong bảng
    public static final By TABLE_ROWS  = By.cssSelector("table tbody tr");

    // Trong mỗi row
    public static final By ROW_TITLE   = By.cssSelector("td a:first-of-type, td:nth-child(2) a");
    public static final By ROW_STATUS  = By.cssSelector("td .badge, td .label, td [class*='status']");
    public static final By ROW_DATE    = By.cssSelector("td:nth-child(3), td time, td[class*='date']");
    public static final By ROW_EDIT    = By.cssSelector("a[href*='/Edit'], a[href*='edit']");

    // Pagination
    public static final By PAGE_NEXT   = By.cssSelector("a[rel='next'], .pagination a:last-child, li.next a");
    public static final By PAGE_SIZE_SELECT = By.cssSelector("select[name='pageSize'], select[name='perPage']");
    public static final By CURRENT_PAGE = By.cssSelector(".pagination .active, [class*='page-current']");
}
