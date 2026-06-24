package com.tests;

import com.readers.OrchardJsonReader;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.List;

/**
 * Test demo hàm get data value theo key
 */
public class ReadJsonByKeyTest {
    
    private OrchardJsonReader reader;
    private static final String JSON_PATH = "src/test/resources/testdata/News.json";
    
    @BeforeClass
    public void setUp() {
        reader = new OrchardJsonReader(JSON_PATH);
    }
    
    @Test(description = "TC01: Get title theo key path")
    public void testGetTitleByKey() {
        // Get title của bài viết đầu tiên
        String title = (String) reader.getValue(0, "TitlePart.Title");
        
        System.out.println("Title bài 1: " + title);
        Assert.assertNotNull(title, "Title không được null");
        Assert.assertFalse(title.isEmpty(), "Title không được rỗng");
    }
    
    @Test(description = "TC02: Get publish date theo key path")
    public void testGetPublishDateByKey() {
        // Get ngày xuất bản
        String publishDate = (String) reader.getValue(0, "EtpTinTucSidebarPart.NgayXuatBan.Value");
        
        System.out.println("Publish Date: " + publishDate);
        Assert.assertNotNull(publishDate, "Publish date không được null");
    }
    
    @Test(description = "TC03: Get source text theo key path")
    public void testGetSourceByKey() {
        // Get nguồn tin
        String source = (String) reader.getValue(0, "EtpArticleSourcePart.Source.Text");
        
        System.out.println("Source: " + source);
    }
    
    @Test(description = "TC04: Get attachment paths theo key path")
    @SuppressWarnings("unchecked")
    public void testGetAttachmentsByKey() {
        // Get danh sách file đính kèm
        Object attachmentObj = reader.getValue(0, "EtpTinTucAttachmentPart.DanhSachFile.Paths");
        
        System.out.println("Attachments: " + attachmentObj);
        
        if (attachmentObj instanceof List) {
            List<String> paths = (List<String>) attachmentObj;
            System.out.println("Số file đính kèm: " + paths.size());
            for (String path : paths) {
                System.out.println("  - " + path);
            }
        }
    }
    
    @Test(description = "TC05: Get value từ TẤT CẢ bài viết")
    public void testGetAllValuesByKey() {
        // Get title của tất cả bài viết
        List<Object> allTitles = reader.getAllValues("TitlePart.Title");
        
        System.out.println("\n=== TẤT CẢ TITLES ===");
        for (int i = 0; i < allTitles.size(); i++) {
            System.out.println("[" + i + "] " + allTitles.get(i));
        }
        
        Assert.assertEquals(allTitles.size(), reader.getArticleCount(), 
            "Số title phải bằng số bài viết");
    }
    
    @Test(description = "TC06: Get value theo ContentItemId")
    public void testGetValueByContentItemId() {
        // Lấy ContentItemId của bài đầu tiên
        String contentItemId = (String) reader.getValue(0, "ContentItemId");
        
        // Get title theo ContentItemId
        String title = (String) reader.getValueByContentItemId(contentItemId, "TitlePart.Title");
        
        System.out.println("\nTitle by ContentItemId: " + title);
        Assert.assertNotNull(title, "Title không được null");
    }
    
    @Test(description = "TC07: Get nested value nhiều cấp")
    public void testGetDeepNestedValue() {
        // Get thumbnail path
        Object thumbnailPaths = reader.getValue(0, "EtpTinTucSidebarPart.AnhDaiDien.Paths");
        
        System.out.println("\nThumbnail Paths: " + thumbnailPaths);
        
        // Get featured flag
        Object isFeatured = reader.getValue(0, "EtpArticlePromotionPart.IsFeaturedHome.Value");
        System.out.println("Is Featured Home: " + isFeatured);
    }
    
    @Test(description = "TC08: Handle key không tồn tại")
    public void testGetNonExistentKey() {
        // Get key không tồn tại
        Object value = reader.getValue(0, "NonExistentPart.NonExistentField");
        
        System.out.println("\nValue for non-existent key: " + value);
        Assert.assertNull(value, "Value phải null nếu key không tồn tại");
    }
    
    @Test(description = "TC09: Print summary")
    public void testPrintSummary() {
        reader.printSummary();
    }
}