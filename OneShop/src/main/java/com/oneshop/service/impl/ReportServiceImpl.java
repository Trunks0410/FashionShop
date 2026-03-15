// src/main/java/com/oneshop/service/impl/ReportServiceImpl.java
package com.oneshop.service.impl;

import com.oneshop.entity.Order;
import com.oneshop.entity.OrderDetail;
import com.oneshop.entity.OrderStatus; // <<< Đảm bảo import này
import com.oneshop.repository.OrderRepository;
import com.oneshop.service.ReportService;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);

    @Autowired
    private OrderRepository orderRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public Workbook generateSalesReport(Long shopId) throws IOException {
        logger.info("Generating sales report for shopId: {}", shopId);
        List<Order> orders;
        try {
            // Lấy đơn hàng đã giao hoặc đang giao
            orders = orderRepository.findByShopIdAndOrderStatusIn(shopId,
                    List.of(OrderStatus.DELIVERED, OrderStatus.DELIVERING), 
                    Sort.by("createdAt").descending()); 
        } catch (Exception e) {
             logger.error("Error fetching orders for report (shopId={}): {}", shopId, e.getMessage());
             throw new IOException("Không thể lấy dữ liệu đơn hàng để tạo báo cáo.", e);
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("BaoCaoDonHang");
        logger.debug("Workbook and sheet created.");

        // --- Tạo Header Row ---
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Mã ĐH", "Ngày Đặt", "Người Nhận", "SĐT Giao", "Địa Chỉ Giao", "Sản Phẩm", "Số Lượng", "Đơn Giá", "Thành Tiền Item", "Trạng Thái ĐH"};
        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        logger.debug("Header row created.");

        // --- Điền Dữ liệu Đơn hàng ---
        int rowNum = 1;
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle centerDataStyle = createCenterDataStyle(workbook);

        for (Order order : orders) {
            // Dùng OrderDetails (đã sửa)
            if (order.getOrderDetails() != null) {
                for (OrderDetail item : order.getOrderDetails()) { // Dùng OrderDetail
                    Row row = sheet.createRow(rowNum++);

                    createCell(row, 0, "#" + order.getId(), dataStyle);
                    // Dùng getCreatedAt() (đã sửa)
                    createCell(row, 1, order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FORMATTER) : "N/A", dataStyle);
                    // Dùng getter đúng (đã sửa)
                    createCell(row, 2, order.getRecipientName(), dataStyle);
                    createCell(row, 3, order.getShippingPhone(), dataStyle);
                    createCell(row, 4, order.getShippingAddress(), dataStyle);

                    // Lấy tên từ ProductVariant (đã sửa)
                    String productName = "N/A";
                    String variantName = "";
                    if (item.getProductVariant() != null) {
                        variantName = item.getProductVariant().getName() != null ? " - " + item.getProductVariant().getName() : "";
                        if (item.getProductVariant().getProduct() != null) {
                            productName = item.getProductVariant().getProduct().getName() + variantName;
                        } else {
                             productName = "Sản phẩm lỗi" + variantName;
                        }
                    } else {
                        logger.warn("OrderDetail {} in Order {} has null ProductVariant.", item.getId(), order.getId());
                    }
                    createCell(row, 5, productName, dataStyle);

                    createCell(row, 6, item.getQuantity(), centerDataStyle);
                    // Lấy giá từ OrderDetail (đã sửa)
                    createCell(row, 7, item.getPrice() != null ? item.getPrice().doubleValue() : 0.0, currencyStyle);
                    // Tính thành tiền từ OrderDetail (đã sửa)
                    double itemTotal = (item.getPrice() != null) ? item.getQuantity() * item.getPrice().doubleValue() : 0.0;
                    createCell(row, 8, itemTotal, currencyStyle);
                    // Dùng getOrderStatus() (đã sửa)
                    createCell(row, 9, order.getOrderStatus() != null ? order.getOrderStatus().name() : "N/A", centerDataStyle);
                }
            } else {
                 logger.warn("Order {} has null OrderDetails.", order.getId());
            }
        }
        logger.debug("Data rows populated. Total rows: {}", rowNum);

        // --- Tự động điều chỉnh độ rộng cột ---
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        logger.debug("Columns auto-sized.");

        return workbook;
    }

    // --- Helper Methods ---
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle(); Font font = workbook.createFont(); font.setBold(true); font.setFontHeightInPoints((short) 12); style.setFont(font); style.setAlignment(HorizontalAlignment.CENTER); style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); style.setFillPattern(FillPatternType.SOLID_FOREGROUND); style.setBorderBottom(BorderStyle.THIN); style.setBorderTop(BorderStyle.THIN); style.setBorderLeft(BorderStyle.THIN); style.setBorderRight(BorderStyle.THIN); return style;
    }
     private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle(); style.setWrapText(true); style.setVerticalAlignment(VerticalAlignment.TOP); style.setBorderBottom(BorderStyle.THIN); style.setBorderTop(BorderStyle.THIN); style.setBorderLeft(BorderStyle.THIN); style.setBorderRight(BorderStyle.THIN); return style;
    }
     private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook); DataFormat format = workbook.createDataFormat(); style.setDataFormat(format.getFormat("#,##0")); return style;
    }
    private CellStyle createCenterDataStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook); style.setAlignment(HorizontalAlignment.CENTER); return style;
    }
    private void createCell(Row row, int column, String value, CellStyle style) { Cell cell = row.createCell(column); cell.setCellValue(value); cell.setCellStyle(style); }
    private void createCell(Row row, int column, Integer value, CellStyle style) { Cell cell = row.createCell(column); if (value != null) cell.setCellValue(value); cell.setCellStyle(style); }
    private void createCell(Row row, int column, Double value, CellStyle style) { Cell cell = row.createCell(column); if (value != null) cell.setCellValue(value); cell.setCellStyle(style); }
}