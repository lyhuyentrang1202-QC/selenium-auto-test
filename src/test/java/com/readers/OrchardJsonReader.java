package com.readers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.models.EtpTinTucItem;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Reader cho file JSON Orchard Core CMS export
 * Hỗ trợ get data value theo key path (dot notation)
 */
public class OrchardJsonReader {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private List<EtpTinTucItem> articles;
    private List<Map<String, Object>> rawArticles; // Lưu raw data để get by key linh hoạt
    
    /**
     * Constructor - Đọc file JSON và parse
     */
    public OrchardJsonReader(String filePath) {
        this.articles = new ArrayList<>();
        this.rawArticles = new ArrayList<>();
        loadJson(filePath);
    }
    
    /**
     * Load JSON file
     */
    private void loadJson(String filePath) {
        try (Reader reader = new FileReader(filePath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            
            // Lấy steps array
            JsonArray steps = root.getAsJsonArray("steps");
            if (steps == null || steps.size() == 0) {
                throw new RuntimeException("File JSON không có 'steps' array");
            }
            
            // Tìm step có name = "Content"
            JsonObject contentStep = null;
            for (JsonElement step : steps) {
                JsonObject stepObj = step.getAsJsonObject();
                if ("Content".equals(stepObj.get("name").getAsString())) {
                    contentStep = stepObj;
                    break;
                }
            }
            
            if (contentStep == null) {
                throw new RuntimeException("Không tìm thấy step 'Content'");
            }
            
            // Lấy data array
            JsonArray dataArray = contentStep.getAsJsonArray("data");
            if (dataArray == null) {
                throw new RuntimeException("Step Content không có 'data' array");
            }
            
            // Parse từng item
            for (JsonElement element : dataArray) {
                JsonObject itemObj = element.getAsJsonObject();
                
                // Convert sang Map để get by key linh hoạt
                @SuppressWarnings("unchecked")
                Map<String, Object> rawMap = gson.fromJson(itemObj, Map.class);
                rawArticles.add(rawMap);
                
                // Convert sang model class
                EtpTinTucItem article = gson.fromJson(itemObj, EtpTinTucItem.class);
                articles.add(article);
            }
            
            System.out.println(">>> Đã load " + articles.size() + " bài viết từ JSON");
            
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc file JSON: " + filePath, e);
        }
    }
    
    // ============================================
    // HÀM GET DATA VALUE THEO KEY
    // ============================================
    
    /**
     * Get value từ bài viết theo index và key path
     * Hỗ trợ dot notation: "TitlePart.Title", "EtpTinTucSidebarPart.NgayXuatBan.Value"
     * 
     * @param articleIndex Index của bài viết (0-based)
     * @param keyPath Key path với dot notation (vd: "TitlePart.Title")
     * @return Giá trị tìm được, null nếu không tìm thấy
     */
    public Object getValue(int articleIndex, String keyPath) {
        if (articleIndex < 0 || articleIndex >= rawArticles.size()) {
            throw new IndexOutOfBoundsException("Index " + articleIndex + " out of bounds. Total: " + rawArticles.size());
        }
        
        Map<String, Object> article = rawArticles.get(articleIndex);
        return getNestedValue(article, keyPath);
    }
    
    /**
     * Get value từ TẤT CẢ bài viết theo key path
     * 
     * @param keyPath Key path với dot notation
     * @return List các giá trị tìm được
     */
    public List<Object> getAllValues(String keyPath) {
        List<Object> values = new ArrayList<>();
        for (Map<String, Object> article : rawArticles) {
            Object value = getNestedValue(article, keyPath);
            values.add(value);
        }
        return values;
    }
    
    /**
     * Get value từ bài viết theo ContentItemId và key path
     * 
     * @param contentItemId ContentItemId của bài viết
     * @param keyPath Key path với dot notation
     * @return Giá trị tìm được, null nếu không tìm thấy
     */
    public Object getValueByContentItemId(String contentItemId, String keyPath) {
        for (Map<String, Object> article : rawArticles) {
            String id = (String) article.get("ContentItemId");
            if (contentItemId.equals(id)) {
                return getNestedValue(article, keyPath);
            }
        }
        return null;
    }
    
    /**
     * Hàm đệ quy để lấy value từ nested Map theo key path
     * Ví dụ: "EtpTinTucSidebarPart.NgayXuatBan.Value" 
     * -> article["EtpTinTucSidebarPart"]["NgayXuatBan"]["Value"]
     */
    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String keyPath) {
        if (map == null || keyPath == null || keyPath.isEmpty()) {
            return null;
        }
        
        // Tách key path bằng dấu chấm
        String[] keys = keyPath.split("\\.");
        Object current = map;
        
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else {
                return null; // Không thể traverse tiếp
            }
            
            if (current == null) {
                return null;
            }
        }
        
        return current;
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Get số lượng bài viết
     */
    public int getArticleCount() {
        return articles.size();
    }
    
    /**
     * Get tất cả bài viết
     */
    public List<EtpTinTucItem> getArticles() {
        return articles;
    }
    
    /**
     * Get bài viết theo index
     */
    public EtpTinTucItem getArticle(int index) {
        if (index < 0 || index >= articles.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds");
        }
        return articles.get(index);
    }
    
    /**
     * Filter bài viết theo ContentType
     */
    public List<EtpTinTucItem> filterByContentType(String contentType) {
        List<EtpTinTucItem> filtered = new ArrayList<>();
        for (EtpTinTucItem article : articles) {
            if (contentType.equals(article.getContentType())) {
                filtered.add(article);
            }
        }
        return filtered;
    }
    
    /**
     * Filter bài viết đã Published
     */
    public List<EtpTinTucItem> filterPublished() {
        List<EtpTinTucItem> filtered = new ArrayList<>();
        for (EtpTinTucItem article : articles) {
            if (article.isPublished()) {
                filtered.add(article);
            }
        }
        return filtered;
    }
    
    /**
     * In thông tin summary của articles (để debug)
     */
    public void printSummary() {
        System.out.println("\n=== ORCHARD ARTICLES SUMMARY ===");
        System.out.println("Total articles: " + articles.size());
        
        for (int i = 0; i < Math.min(5, articles.size()); i++) {
            EtpTinTucItem article = articles.get(i);
            System.out.println("\n[" + (i+1) + "] " + article.getDisplayText());
            System.out.println("    ID: " + article.getContentItemId());
            System.out.println("    Type: " + article.getContentType());
            System.out.println("    Published: " + article.isPublished());
            
            // Demo get by key
            String title = (String) getValue(i, "TitlePart.Title");
            String publishDate = (String) getValue(i, "EtpTinTucSidebarPart.NgayXuatBan.Value");
            String source = (String) getValue(i, "EtpArticleSourcePart.Source.Text");
            
            System.out.println("    Title (by key): " + title);
            System.out.println("    Publish Date (by key): " + publishDate);
            System.out.println("    Source (by key): " + source);
        }
    }
}