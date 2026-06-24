package pageAction;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pageUI.LoginPageUI;
import java.time.Duration;

/**
 * Chứa các action thao tác trên trang Login
 * Theo pattern: Chỉ xử lý logic, không chứa locator
 */
public class LoginPageAction {
    
    private WebDriver driver;
    private WebDriverWait wait;
    
    public LoginPageAction(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }
    
    /**
     * Navigate đến trang login
     */
    public void navigateToLoginPage() {
        driver.get(LoginPageUI.LOGIN_URL);
    }
    
    /**
     * Nhập username
     */
    public void enterUsername(String username) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(LoginPageUI.USERNAME_TEXTBOX))
            .clear();
        driver.findElement(LoginPageUI.USERNAME_TEXTBOX).sendKeys(username);
    }
    
    /**
     * Nhập password
     */
    public void enterPassword(String password) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(LoginPageUI.PASSWORD_TEXTBOX))
            .clear();
        driver.findElement(LoginPageUI.PASSWORD_TEXTBOX).sendKeys(password);
    }
    
    /**
     * Click nút Login
     */
    public void clickLoginButton() {
        wait.until(ExpectedConditions.elementToBeClickable(LoginPageUI.LOGIN_BUTTON)).click();
    }
    
    /**
     * Thực hiện login đầy đủ
     */
    public void login(String username, String password) {
        navigateToLoginPage();
        enterUsername(username);
        enterPassword(password);
        clickLoginButton();
    }
    
    /**
     * Kiểm tra login có thành công không (chờ dashboard hiện ra)
     */
    public boolean isLoginSuccessful() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(LoginPageUI.DASHBOARD_MENU));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Kiểm tra có hiển thị lỗi không
     */
    public boolean isErrorMessageDisplayed() {
        try {
            return driver.findElement(LoginPageUI.ERROR_MESSAGE).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Lấy nội dung thông báo lỗi
     */
    public String getErrorMessageText() {
        return driver.findElement(LoginPageUI.ERROR_MESSAGE).getText();
    }
}