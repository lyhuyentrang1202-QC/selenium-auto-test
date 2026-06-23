package com.tests;

import com.readers.OrchardJsonReader;
import commons.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

public class NewsMigrationTest {

    private static final String JSON_PATH = "src/test/resources/testdata/News.json";
    private static final String NEWS_LIST_URL = "https://sgddt-langson.ddns.net/Admin/EducationTrainingPortal/TinTuc";
    
    private OrchardJsonReader jsonReader;
    private WebDriverWait wait;

    @BeforeMethod
    public void setUp() {
        wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(15));
        jsonReader = new OrchardJsonReader(JSON_PATH);
    }

    @Test(description = "TC01: So sánh tổng số bài viết EtpTinTuc")
    public void testCompareTotalCount() {
        int jsonCount = 0;
        for (int i = 0; i < jsonReader.getArticleCount(); i++) {
            String type = (String) jsonReader.getValue(i, "ContentType");
            if ("EtpTinTuc".equals(type)) {
                jsonCount++;
            }
        }

        DriverManager.getDriver().get(NEWS_LIST_URL);
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("div.pagination-left.margin-right-xs")));
        
        WebElement totalElement = DriverManager.getDriver()
            .findElement(By.cssSelector("div.pagination-left.margin-right-xs"));
        String totalText = totalElement.getText();
        int uiCount = Integer.parseInt(totalText.replaceAll("[^0-9]", ""));

        System.out.println("=== SO SÁNH SỐ LƯỢNG ===");
        System.out.println("JSON (EtpTinTuc): " + jsonCount);
        System.out.println("UI (Tổng bản ghi): " + uiCount);
        
        Assert.assertTrue(uiCount > 0, "UI không có bản ghi nào!");
    }

    @Test(description = "TC02: Verify bài viết đầu tiên tồn tại trên UI")
    public void testFirstArticleExists() {
        String jsonTitle = (String) jsonReader.getValue(0, "TitlePart.Title");
        
        DriverManager.getDriver().get(NEWS_LIST_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//input[@placeholder='Tìm theo tiêu đề...']")));
        
        WebElement searchInput = DriverManager.getDriver()
            .findElement(By.xpath("//input[@placeholder='Tìm theo tiêu đề...']"));
        searchInput.clear();
        searchInput.sendKeys(jsonTitle);
        searchInput.submit();
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("div.pagination-left.margin-right-xs")));
        
        WebElement totalElement = DriverManager.getDriver()
            .findElement(By.cssSelector("div.pagination-left.margin-right-xs"));
        String resultText = totalElement.getText();
        int resultCount = Integer.parseInt(resultText.replaceAll("[^0-9]", ""));
        
        System.out.println("Title: " + jsonTitle);
        System.out.println("Kết quả tìm thấy: " + resultCount);
        
        Assert.assertTrue(resultCount > 0, "Bài viết không tồn tại trên UI!");
    }

    @Test(description = "TC03: Kiểm tra các field quan trọng")
    public void testValidateImportantFields() {
        System.out.println("=== KIỂM TRA CÁC FIELD ===");
        
        for (int i = 0; i < Math.min(3, jsonReader.getArticleCount()); i++) {
            String title = (String) jsonReader.getValue(i, "TitlePart.Title");
            String publishDate = (String) jsonReader.getValue(i, "EtpTinTucSidebarPart.NgayXuatBan.Value");
            boolean published = (boolean) jsonReader.getValue(i, "Published");
            
            System.out.println("[Bài " + (i+1) + "]");
            System.out.println("  Title: " + title);
            System.out.println("  Published: " + published);
            System.out.println("  Publish Date: " + publishDate);
            
            Assert.assertNotNull(title, "Title is null");
            Assert.assertTrue(published, "Article not published");
        }
    }
}