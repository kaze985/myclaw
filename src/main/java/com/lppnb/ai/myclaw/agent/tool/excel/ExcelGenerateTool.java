package com.lppnb.ai.myclaw.agent.tool.excel;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Excel 电子表格生成工具：使用 Apache POI 创建 .xlsx 文件，表头行加粗。
 */
@Slf4j
@Component
public class ExcelGenerateTool {

    @Value("${tools.file.download-dir}")
    private String downloadDir;

    @PostConstruct
    public void init() {
        new File(downloadDir).mkdirs();
    }

    @Tool(description = "Generate an Excel (.xlsx) spreadsheet.")
    public String generateExcel(
            @ToolParam(description = "Output file name without extension") String filename,
            @ToolParam(description = "List of column header names") List<String> headers,
            @ToolParam(description = "List of data rows, each row is a list of cell values") List<List<String>> rows) {
        try {
            File outFile = new File(downloadDir, filename + ".xlsx");
            outFile.getParentFile().mkdirs();

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Sheet1");

                // 表头加粗样式
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                // 写入表头行
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                    cell.setCellStyle(headerStyle);
                }

                // 按传入顺序写入数据行
                for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                    Row row = sheet.createRow(rowIdx + 1);
                    List<String> rowData = rows.get(rowIdx);
                    for (int colIdx = 0; colIdx < rowData.size(); colIdx++) {
                        row.createCell(colIdx).setCellValue(rowData.get(colIdx));
                    }
                }

                // 自动调整列宽
                for (int i = 0; i < headers.size(); i++) {
                    sheet.autoSizeColumn(i);
                }

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    workbook.write(fos);
                }
            }

            log.info("Excel 文件生成成功：{}", outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            log.error("Excel 文件生成失败：{}", filename, e);
            return "Excel文件生成失败：" + e.getMessage();
        }
    }
}
