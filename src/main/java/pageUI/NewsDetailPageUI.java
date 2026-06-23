package pageUI;

import org.openqa.selenium.By;

// Locators trên trang Edit bài viết (Orchard CMS admin)
// NOTE: Cần verify lại sau khi inspect trang thực tế
public class NewsDetailPageUI {

    // Tiêu đề
    public static final By TITLE_INPUT = By.cssSelector("input[id*='TitlePart']");

    // Ngày xuất bản
    public static final By PUBLISH_DATE_INPUT = By.cssSelector("input[id*='NgayXuatBan'], input[name*='NgayXuatBan']");

    // Ảnh đại diện
    public static final By THUMBNAIL_PATH_INPUT = By.cssSelector("input[id*='AnhDaiDien'], input[name*='AnhDaiDien']");

    // Danh sách file đính kèm
    public static final By ATTACHMENT_FILE_ITEMS = By.cssSelector("[id*='DanhSachFile'] li, .attachment-list li, [class*='attachment'] li");

    // Checkbox Featured Home
    public static final By FEATURED_HOME_CHECKBOX = By.cssSelector("input[type='checkbox'][id*='IsFeaturedHome']");

    // Indicator "Published" trên edit page
    public static final By PUBLISHED_INDICATOR = By.cssSelector(".badge-success, .status-published, span.badge");
}
