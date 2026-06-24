package com.comparators;

import java.util.*;

/** Kết quả so sánh 1 màn hình: tổng hợp pass/fail theo từng hạng mục. */
public class MigrationResult {

    public final String screenName;

    public int uiTotal       = 0;   // tổng record UI đếm được
    public int jsonTotal     = 0;   // tổng record trong JSON
    public int checked       = 0;   // số bài đã check
    public int matched       = 0;   // số bài khớp hoàn toàn

    public final List<String> missing      = new ArrayList<>(); // có trong JSON, không thấy trên UI
    public final List<String> extra        = new ArrayList<>(); // trên UI nhưng không tìm thấy trong JSON
    public final List<FieldMismatch> mismatches = new ArrayList<>();

    public MigrationResult(String screenName) {
        this.screenName = screenName;
    }

    public void addMismatch(String title, String field, String expected, String actual) {
        mismatches.add(new FieldMismatch(title, field, expected, actual));
    }

    public boolean passed() {
        return missing.isEmpty() && extra.isEmpty() && mismatches.isEmpty()
            && Math.abs(uiTotal - jsonTotal) <= (int)(jsonTotal * 0.02); // tolerance 2%
    }

    public String summary() {
        return String.format(
            "[%s] UI=%d JSON=%d | checked=%d matched=%d | missing=%d extra=%d mismatch=%d | %s",
            screenName, uiTotal, jsonTotal, checked, matched,
            missing.size(), extra.size(), mismatches.size(),
            passed() ? "PASS ✓" : "FAIL ✗");
    }

    public static class FieldMismatch {
        public final String articleTitle;
        public final String field;
        public final String expected; // JSON value
        public final String actual;   // UI value

        public FieldMismatch(String articleTitle, String field, String expected, String actual) {
            this.articleTitle = articleTitle;
            this.field = field;
            this.expected = expected;
            this.actual = actual;
        }

        @Override
        public String toString() {
            return String.format("  [%s] %s: expected='%s' actual='%s'", articleTitle, field, expected, actual);
        }
    }
}
