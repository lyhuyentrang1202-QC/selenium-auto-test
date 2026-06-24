package commons;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;

import java.time.Duration;

public class BaseTest {

    @Parameters({"url", "browser"})
    @BeforeTest
    protected void setupDriver(String url, String browser) {
        WebDriver driver = null;
        System.out.println("Browser: " + browser);
        
        switch (browser.toLowerCase()) {
            case "edge" -> {
                EdgeOptions options = new EdgeOptions();
                driver = new EdgeDriver(options);
            }
            case "hedge" -> {
                EdgeOptions options = new EdgeOptions();
                options.addArguments("--headless");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-gpu");
                driver = new EdgeDriver(options);
            }
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--disable-gpu");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--autoplay-policy=user-gesture-required");
                options.addArguments("--disable-features=AutoplayIgnoreWebAudio,MediaRouter");
                driver = new ChromeDriver(options);
            }
            default -> throw new IllegalArgumentException("Browser không hỗ trợ: " + browser);
        }

        DriverManager.setDriver(driver);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.get(url);
        
        // Auto login
        loginToAdmin();
    }

    protected void loginToAdmin() {
        WebDriver driver = DriverManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        
        try {
            // Đợi trang login load
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.name("username")));
            
            // Nhập thông tin đăng nhập
            driver.findElement(By.name("username")).sendKeys("tuannd25@viettel.com.vn");
            driver.findElement(By.name("password")).sendKeys("Smas@2025"); // Thay password thật
            driver.findElement(By.cssSelector("button[type='submit']")).click();
            
            // Chờ login thành công - đợi element của admin dashboard
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//span[contains(text(),'Quản lý bài viết')]")));
            
            System.out.println(">>> Login thành công!");
            
        } catch (Exception e) {
            System.out.println("⚠️  Login thất bại hoặc đã login sẵn: " + e.getMessage());
        }
    }

    @AfterTest
    protected void tearDown() {
        try {
            if (DriverManager.getDriver() != null) {
                DriverManager.getDriver().quit();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}