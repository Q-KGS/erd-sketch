package com.erdsketch.pdf;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PdfExportServiceTest {

    private final PdfExportService service = new PdfExportService();

    private static final String SCHEMA_WITH_TABLES = """
            {
              "tables": {
                "t1": {
                  "name": "users",
                  "columns": [
                    {"name": "id", "dataType": "BIGINT", "nullable": false},
                    {"name": "email", "dataType": "VARCHAR(255)", "nullable": false},
                    {"name": "name", "dataType": "VARCHAR(100)", "nullable": true}
                  ]
                },
                "t2": {
                  "name": "orders",
                  "columns": [
                    {"name": "id", "dataType": "BIGINT", "nullable": false},
                    {"name": "total", "dataType": "DECIMAL(10,2)", "nullable": true}
                  ]
                }
              }
            }
            """;

    // ───── B-PDF-01: PDF 헤더 확인 ─────
    @Test
    void B_PDF_01_반환된_바이트_배열이_PDF_헤더로_시작() {
        // when
        byte[] pdf = service.generatePdf("Test Project", "Test Document", SCHEMA_WITH_TABLES);

        // then
        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        // PDF 파일 시그니처 "%PDF-" 확인
        String header = new String(pdf, 0, 5);
        assertThat(header).isEqualTo("%PDF-");
    }

    // ───── B-PDF-02: PDF 길이 > 0 ─────
    @Test
    void B_PDF_02_PDF_바이트_배열_길이_양수() {
        // when
        byte[] pdf = service.generatePdf("My Project", "My Document", null);

        // then
        assertThat(pdf.length).isGreaterThan(0);
    }

    // ───── B-PDF-03: 테이블 포함 스키마로 PDF 생성 ─────
    @Test
    void B_PDF_03_테이블_포함_스키마로_PDF_생성_예외_없음() {
        // when & then
        assertThatCode(() -> {
            byte[] pdf = service.generatePdf("ERD Project", "Schema v1", SCHEMA_WITH_TABLES);
            assertThat(pdf.length).isGreaterThan(0);
        }).doesNotThrowAnyException();
    }

    // ───── B-PDF-04: null/빈 스키마 → 예외 없이 PDF 반환 ─────
    @Test
    void B_PDF_04_null_스키마로_PDF_생성_예외_없음() {
        // when & then
        assertThatCode(() -> {
            // null 스키마
            byte[] pdf1 = service.generatePdf("Project", "Document", null);
            assertThat(pdf1.length).isGreaterThan(0);

            // 빈 스키마
            byte[] pdf2 = service.generatePdf("Project", "Document", "");
            assertThat(pdf2.length).isGreaterThan(0);
        }).doesNotThrowAnyException();
    }
}
