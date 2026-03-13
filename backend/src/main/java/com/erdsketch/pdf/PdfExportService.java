package com.erdsketch.pdf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PdfExportService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] generatePdf(String projectName, String documentName, String schemaJson) {
        try (PDDocument document = new PDDocument()) {
            // 1페이지: 타이틀
            addTitlePage(document, projectName, documentName);

            // 스키마 테이블 페이지
            List<Map<String, Object>> tables = extractTables(schemaJson);
            if (tables.isEmpty()) {
                addNoSchemaPage(document);
            } else {
                for (Map<String, Object> table : tables) {
                    addTablePage(document, table);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void addTitlePage(PDDocument document, String projectName, String documentName) throws Exception {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // 타이틀
            stream.beginText();
            stream.setFont(boldFont, 24);
            stream.newLineAtOffset(50, 750);
            stream.showText(sanitize(projectName));
            stream.endText();

            // 문서명
            stream.beginText();
            stream.setFont(normalFont, 18);
            stream.newLineAtOffset(50, 710);
            stream.showText(sanitize(documentName));
            stream.endText();

            // 날짜
            String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            stream.beginText();
            stream.setFont(normalFont, 12);
            stream.newLineAtOffset(50, 680);
            stream.showText("Generated: " + dateStr);
            stream.endText();
        }
    }

    private void addNoSchemaPage(PDDocument document) throws Exception {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            stream.beginText();
            stream.setFont(normalFont, 14);
            stream.newLineAtOffset(50, 750);
            stream.showText("No schema available.");
            stream.endText();
        }
    }

    @SuppressWarnings("unchecked")
    private void addTablePage(PDDocument document, Map<String, Object> table) throws Exception {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        String tableName = sanitize(String.valueOf(table.getOrDefault("name", "Unknown Table")));
        List<Object> columns = table.get("columns") instanceof List ? (List<Object>) table.get("columns") : List.of();

        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // 테이블 이름
            stream.beginText();
            stream.setFont(boldFont, 16);
            stream.newLineAtOffset(50, 750);
            stream.showText("Table: " + tableName);
            stream.endText();

            // 컬럼 헤더
            float y = 720;
            stream.beginText();
            stream.setFont(boldFont, 12);
            stream.newLineAtOffset(50, y);
            stream.showText("Column Name");
            stream.endText();

            stream.beginText();
            stream.setFont(boldFont, 12);
            stream.newLineAtOffset(200, y);
            stream.showText("Data Type");
            stream.endText();

            stream.beginText();
            stream.setFont(boldFont, 12);
            stream.newLineAtOffset(350, y);
            stream.showText("Nullable");
            stream.endText();

            // 구분선
            stream.setLineWidth(0.5f);
            stream.moveTo(50, y - 5);
            stream.lineTo(545, y - 5);
            stream.stroke();

            // 컬럼 목록
            y -= 20;
            for (Object colObj : columns) {
                if (y < 50) break;
                if (colObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> col = (Map<String, Object>) colObj;
                    String colName = sanitize(String.valueOf(col.getOrDefault("name", "")));
                    String dataType = sanitize(String.valueOf(col.getOrDefault("dataType", "")));
                    String nullable = String.valueOf(col.getOrDefault("nullable", "true"));

                    stream.beginText();
                    stream.setFont(normalFont, 11);
                    stream.newLineAtOffset(50, y);
                    stream.showText(colName);
                    stream.endText();

                    stream.beginText();
                    stream.setFont(normalFont, 11);
                    stream.newLineAtOffset(200, y);
                    stream.showText(dataType);
                    stream.endText();

                    stream.beginText();
                    stream.setFont(normalFont, 11);
                    stream.newLineAtOffset(350, y);
                    stream.showText(nullable);
                    stream.endText();

                    y -= 18;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractTables(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> schema = objectMapper.readValue(schemaJson, new TypeReference<>() {});
            Object tablesObj = schema.get("tables");
            if (!(tablesObj instanceof Map)) {
                return List.of();
            }
            Map<String, Object> tables = (Map<String, Object>) tablesObj;
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object val : tables.values()) {
                if (val instanceof Map) {
                    result.add((Map<String, Object>) val);
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * PDType1Font은 ASCII 범위만 지원하므로 비ASCII 문자를 '?' 로 대체.
     */
    private String sanitize(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c < 128) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }
}
