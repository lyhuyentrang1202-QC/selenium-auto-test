package com.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * Model class cho một bài viết EtpTinTuc trong Orchard Core CMS
 * Map từ JSON export format
 */
public class EtpTinTucItem {
    
    // === TOP-LEVEL FIELDS ===
    @SerializedName("ContentItemId")
    private String contentItemId;
    
    @SerializedName("ContentItemVersionId")
    private String contentItemVersionId;
    
    @SerializedName("ContentType")
    private String contentType;
    
    @SerializedName("DisplayText")
    private String displayText;
    
    @SerializedName("Latest")
    private boolean latest;
    
    @SerializedName("Published")
    private boolean published;
    
    // === NESTED PARTS (dùng Map để linh hoạt) ===
    @SerializedName("TitlePart")
    private Map<String, Object> titlePart;
    
    @SerializedName("AutoroutePart")
    private Map<String, Object> autoroutePart;
    
    @SerializedName("EtpTinTucBodyPart")
    private Map<String, Object> bodyPart;
    
    @SerializedName("EtpTinTucSidebarPart")
    private Map<String, Object> sidebarPart;
    
    @SerializedName("EtpTinTucAttachmentPart")
    private Map<String, Object> attachmentPart;
    
    @SerializedName("EtpArticleSourcePart")
    private Map<String, Object> sourcePart;
    
    @SerializedName("EtpArticlePromotionPart")
    private Map<String, Object> promotionPart;
    
    @SerializedName("EtpTinTucRelatedPart")
    private Map<String, Object> relatedPart;
    
    // === GETTERS & SETTERS ===
    
    public String getContentItemId() { return contentItemId; }
    public void setContentItemId(String contentItemId) { this.contentItemId = contentItemId; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public String getDisplayText() { return displayText; }
    public void setDisplayText(String displayText) { this.displayText = displayText; }
    
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    
    public boolean isLatest() { return latest; }
    public void setLatest(boolean latest) { this.latest = latest; }
    
    public Map<String, Object> getTitlePart() { return titlePart; }
    public void setTitlePart(Map<String, Object> titlePart) { this.titlePart = titlePart; }
    
    public Map<String, Object> getAutoroutePart() { return autoroutePart; }
    public void setAutoroutePart(Map<String, Object> autoroutePart) { this.autoroutePart = autoroutePart; }
    
    public Map<String, Object> getBodyPart() { return bodyPart; }
    public void setBodyPart(Map<String, Object> bodyPart) { this.bodyPart = bodyPart; }
    
    public Map<String, Object> getSidebarPart() { return sidebarPart; }
    public void setSidebarPart(Map<String, Object> sidebarPart) { this.sidebarPart = sidebarPart; }
    
    public Map<String, Object> getAttachmentPart() { return attachmentPart; }
    public void setAttachmentPart(Map<String, Object> attachmentPart) { this.attachmentPart = attachmentPart; }
    
    public Map<String, Object> getSourcePart() { return sourcePart; }
    public void setSourcePart(Map<String, Object> sourcePart) { this.sourcePart = sourcePart; }
    
    public Map<String, Object> getPromotionPart() { return promotionPart; }
    public void setPromotionPart(Map<String, Object> promotionPart) { this.promotionPart = promotionPart; }
    
    public Map<String, Object> getRelatedPart() { return relatedPart; }
    public void setRelatedPart(Map<String, Object> relatedPart) { this.relatedPart = relatedPart; }
}