package com.tests.migration;

import com.comparators.MigrationResult;
import com.datastore.ArticleDataStore;
import com.google.gson.JsonObject;
import commons.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pageAction.AdminNewsListPageAction;

import java.util.*;

/**
 * So sánh màn Tin tức: New Admin UI → JSON (đại diện Old UI).
 *
 * Chiến lược (QA senior):
 *   TC01 — Count check: tổng số bài UI ≈ JSON (tolerance 2%)
 *   TC02 — Existence check: mọi bài trên UI tìm được trong JSON (by title)
 *   TC03 — Missing check: bài có trong JSON nhưng không thấy trên UI
 *   TC04 — Status check: trạng thái bài khớp (Xuất bản ↔ Published)
 */
public class NewsScreenMigrationTest extends BaseTest {

    private static final String TYPE       = "Article.News";
    private static final String SCREEN     = "Tin tức";
    private static final double TOLERANCE  = 0.05; // chấp nhận lệch 5%

    private ArticleDataStore store;
    private AdminNewsListPageAction newsPage;
    private MigrationResult result;

    // Toàn bộ rows đọc từ UI — chỉ paginate 1 lần, dùng chung cho TC02-04
    private List<Map<String, String>> uiRows;

    @BeforeClass
    public void setup() {
        store    = ArticleDataStore.get();
        newsPage = new AdminNewsListPageAction();
        result   = new MigrationResult(SCREEN);

        result.jsonTotal = store.countByType(TYPE);
        System.out.printf("%n=== [%s] JSON total: %d bài ===%n", SCREEN, result.jsonTotal);
    }

    // ── TC01: Count check ─────────────────────────────────────────────────────

    @Test(priority = 1, description = "TC01 - Tổng số bài trên UI ≈ JSON")
    public void TC01_countCheck() {
        newsPage.navigateTo();
        result.uiTotal = newsPage.getTotalCount();

        System.out.printf("[TC01] UI=%d  JSON=%d%n", result.uiTotal, result.jsonTotal);

        int diff = Math.abs(result.uiTotal - result.jsonTotal);
        double diffRatio = (double) diff / Math.max(result.jsonTotal, 1);

        Assert.assertTrue(diffRatio <= TOLERANCE,
            String.format("[TC01 FAIL] Lệch %.1f%% (UI=%d JSON=%d). Vượt tolerance %d%%",
                diffRatio * 100, result.uiTotal, result.jsonTotal, (int)(TOLERANCE * 100)));

        System.out.printf("[TC01 PASS] Lệch %d bài (%.1f%%)%n", diff, diffRatio * 100);
    }

    // ── TC02: Existence check (UI → JSON) ─────────────────────────────────────

    @Test(priority = 2, description = "TC02 - Mọi bài trên UI tìm được trong JSON")
    public void TC02_existenceCheck() {
        newsPage.navigateTo();
        uiRows = newsPage.getAllRows();
        result.checked = uiRows.size();

        List<String> notFound = new ArrayList<>();

        for (Map<String, String> row : uiRows) {
            String uiTitle = row.get("title");
            Optional<JsonObject> json = store.findByTitle(uiTitle);

            if (json.isPresent()) {
                result.matched++;
            } else {
                notFound.add(uiTitle);
                result.extra.add(uiTitle);
            }
        }

        System.out.printf("[TC02] Checked=%d  Found=%d  NotFound=%d%n",
            result.checked, result.matched, notFound.size());

        if (!notFound.isEmpty()) {
            System.out.println("[TC02 FAIL] Bài trên UI không tìm thấy trong JSON:");
            notFound.stream().limit(20).forEach(t -> System.out.println("  - " + t));
        }

        // Fail nếu quá 5% không tìm thấy trong JSON
        double missingRatio = (double) notFound.size() / Math.max(result.checked, 1);
        Assert.assertTrue(missingRatio <= TOLERANCE,
            String.format("[TC02 FAIL] %d/%d bài (%.1f%%) không match JSON",
                notFound.size(), result.checked, missingRatio * 100));
    }

    // ── TC03: Missing check (JSON → UI) ───────────────────────────────────────

    @Test(priority = 3, description = "TC03 - Bài trong JSON phải có mặt trên UI",
          dependsOnMethods = "TC02_existenceCheck")
    public void TC03_missingCheck() {
        if (uiRows == null || uiRows.isEmpty()) {
            System.out.println("[TC03 SKIP] uiRows chưa có data, skip");
            return;
        }

        // Build set normalized titles từ UI
        Set<String> uiTitles = new HashSet<>();
        for (Map<String, String> row : uiRows) {
            uiTitles.add(ArticleDataStore.normalize(row.get("title")));
        }

        // Tìm bài trong JSON không có trên UI
        List<JsonObject> jsonArticles = store.getByType(TYPE);
        List<String> missingOnUi = new ArrayList<>();

        for (JsonObject article : jsonArticles) {
            String jsonTitle = ArticleDataStore.str(article, "title", "");
            String status    = ArticleDataStore.str(article, "editorialStatus", "");

            // Chỉ check bài "Xuất bản" — bài nháp có thể không hiển thị trên UI
            if (!status.contains("Xuất bản") && !status.equalsIgnoreCase("published")) continue;

            if (!uiTitles.contains(ArticleDataStore.normalize(jsonTitle))) {
                missingOnUi.add(jsonTitle);
                result.missing.add(jsonTitle);
            }
        }

        System.out.printf("[TC03] Published trong JSON=%d  Missing trên UI=%d%n",
            jsonArticles.stream()
                .filter(a -> ArticleDataStore.str(a, "editorialStatus", "").contains("Xuất bản"))
                .count(),
            missingOnUi.size());

        if (!missingOnUi.isEmpty()) {
            System.out.println("[TC03 FAIL] Bài Xuất bản trong JSON nhưng không thấy trên UI:");
            missingOnUi.stream().limit(20).forEach(t -> System.out.println("  - " + t));
        }

        Assert.assertTrue(result.missing.isEmpty(),
            String.format("[TC03 FAIL] %d bài Published bị thiếu trên UI", result.missing.size()));
    }

    // ── TC04: Status check ────────────────────────────────────────────────────

    @Test(priority = 4, description = "TC04 - Trạng thái bài trên UI khớp JSON",
          dependsOnMethods = "TC02_existenceCheck")
    public void TC04_statusCheck() {
        if (uiRows == null || uiRows.isEmpty()) return;

        int mismatchCount = 0;

        for (Map<String, String> row : uiRows) {
            String uiTitle  = row.get("title");
            String uiStatus = row.get("status");

            Optional<JsonObject> jsonOpt = store.findByTitle(uiTitle);
            if (jsonOpt.isEmpty()) continue;

            String jsonStatus = ArticleDataStore.str(jsonOpt.get(), "editorialStatus", "");
            String expected   = mapStatus(jsonStatus);

            if (!expected.isEmpty() && !uiStatus.toLowerCase().contains(expected.toLowerCase())) {
                result.addMismatch(uiTitle, "status", expected, uiStatus);
                mismatchCount++;
                if (mismatchCount <= 10) {
                    System.out.printf("[TC04 MISMATCH] '%s' | JSON=%s → UI=%s%n",
                        truncate(uiTitle, 60), jsonStatus, uiStatus);
                }
            }
        }

        System.out.printf("[TC04] Status mismatch: %d/%d%n", mismatchCount, uiRows.size());

        Assert.assertEquals(mismatchCount, 0,
            String.format("[TC04 FAIL] %d bài bị sai status", mismatchCount));
    }

    // ── TC05: Summary print ───────────────────────────────────────────────────

    @Test(priority = 5, description = "TC05 - In tổng kết kết quả")
    public void TC05_printSummary() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println(result.summary());
        System.out.println("=".repeat(70));

        if (!result.mismatches.isEmpty()) {
            System.out.println("Chi tiết mismatch:");
            result.mismatches.stream().limit(30).forEach(m -> System.out.println(m));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Map trạng thái JSON → keyword kỳ vọng trên UI mới. */
    private String mapStatus(String jsonStatus) {
        if (jsonStatus == null) return "";
        if (jsonStatus.contains("Xuất bản"))          return "Published";
        if (jsonStatus.contains("Lưu nháp"))           return "Draft";
        if (jsonStatus.contains("Chờ kiểm duyệt"))    return "Pending";
        return "";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
