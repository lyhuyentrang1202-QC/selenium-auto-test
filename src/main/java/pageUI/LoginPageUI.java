package pageUI;

import org.openqa.selenium.By;

/**
 * Chứa tất cả locators của trang Login
 * Theo pattern: Chỉ định nghĩa By, không có logic
 */
public class LoginPageUI {
    
    private LoginPageUI() {
    }
    
    // === URL ===
    public static final String LOGIN_URL = "https://id.nentanggiaoduc.edu.vn/";
    
    // === LOCATORS ===
    
    // Input Username
    public static final By USERNAME_TEXTBOX = By.xpath("//input[@name='username']");
    
    // Input Password
    public static final By PASSWORD_TEXTBOX = By.xpath("//input[@name='password']");
    
    // Button Login
    public static final By LOGIN_BUTTON = By.xpath("//button[@type='submit']");
    
    // Error message (nếu login sai)
    public static final By ERROR_MESSAGE = By.cssSelector(".alert-danger, .validation-summary-errors");
    
    // Element chỉ có sau khi login thành công (để verify)
    // TODO: Inspect trang dashboard để lấy đúng locator
    public static final By DASHBOARD_MENU = By.xpath("//span[contains(text(),'Quản lý bài viết')]");
    
    // Link "Quên mật khẩu" (nếu có)
    public static final By FORGOT_PASSWORD_LINK = By.linkText("Quên mật khẩu");
}