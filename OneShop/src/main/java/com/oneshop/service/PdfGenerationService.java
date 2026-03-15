package com.oneshop.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.oneshop.entity.Order;
import com.oneshop.entity.OrderDetail;
import com.oneshop.entity.Shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
public class PdfGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(PdfGenerationService.class);

    /**
     * Tải font mỗi lần tạo PDF → an toàn đa luồng
     */
    private PdfFont loadFont(String fontPath) throws IOException {
        try {
            ClassPathResource fontResource = new ClassPathResource(fontPath);
            try (InputStream fontStream = fontResource.getInputStream()) {
                byte[] fontBytes = fontStream.readAllBytes();
                return PdfFontFactory.createFont(
                        fontBytes,
                        PdfEncodings.IDENTITY_H,
                        EmbeddingStrategy.PREFER_EMBEDDED
                );
            }
        } catch (Exception e) {
            logger.warn("Không tải được font: {}. Dùng font mặc định.", fontPath);
            return PdfFontFactory.createFont("Helvetica", PdfEncodings.WINANSI, EmbeddingStrategy.PREFER_NOT_EMBEDDED);
        }
    }

    // ==============================
    // 1. PHIẾU XUẤT HÀNG (A4)
    // ==============================
    public byte[] generatePackingSlip(Order order) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfFont regularFont = loadFont("fonts/Roboto-Regular.ttf");
        PdfFont boldFont = loadFont("fonts/Roboto-Bold.ttf");

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc, PageSize.A4)) {

            document.setMargins(40, 40, 40, 40);
            document.setFont(regularFont);

            // Tiêu đề
            document.add(new Paragraph("PHIẾU XUẤT HÀNG")
                    .setFont(boldFont).setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Mã đơn hàng: #" + order.getId())
                    .setFont(boldFont).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10));

            String orderDateStr = "Không rõ ngày đặt";
            if (order.getCreatedAt() != null) {
                orderDateStr = order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
            }
            document.add(new Paragraph("Ngày đặt: " + orderDateStr)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12));

            document.add(new Paragraph("\n"));

            // Thông tin giao hàng
            document.add(new Paragraph("THÔNG TIN GIAO HÀNG").setFont(boldFont).setFontSize(14));
            document.add(new Paragraph("Người nhận: " + Optional.ofNullable(order.getRecipientName()).orElse("N/A")));
            document.add(new Paragraph("Điện thoại: " + Optional.ofNullable(order.getShippingPhone()).orElse("N/A")));
            document.add(new Paragraph("Địa chỉ: " + Optional.ofNullable(order.getShippingAddress()).orElse("N/A")));
            document.add(new Paragraph("Thanh toán: " + Optional.ofNullable(order.getPaymentMethod()).orElse("N/A").toUpperCase()));
            document.add(new Paragraph("\n"));

            // Bảng sản phẩm
            document.add(new Paragraph("CHI TIẾT SẢN PHẨM").setFont(boldFont).setFontSize(14));

            Table table = new Table(UnitValue.createPercentArray(new float[]{4, 2, 1, 2}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginTop(10);

            // Header
            table.addHeaderCell(new Cell().add(new Paragraph("Sản phẩm").setFont(boldFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Phân loại").setFont(boldFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("SL").setFont(boldFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER));
            table.addHeaderCell(new Cell().add(new Paragraph("Giá").setFont(boldFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.RIGHT));

            Locale localeVN = new Locale("vi", "VN");
            NumberFormat currency = NumberFormat.getCurrencyInstance(localeVN);

            if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
                for (OrderDetail item : order.getOrderDetails()) {
                    String productName = "Sản phẩm lỗi";
                    String variantName = "N/A";

                    if (item.getProductVariant() != null) {
                        variantName = Optional.ofNullable(item.getProductVariant().getName()).orElse("N/A");
                        if (item.getProductVariant().getProduct() != null) {
                            productName = Optional.ofNullable(item.getProductVariant().getProduct().getName()).orElse(productName);
                        }
                    }

                    table.addCell(new Cell().add(new Paragraph(productName)));
                    table.addCell(new Cell().add(new Paragraph(variantName)));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(item.getQuantity()))
                            .setTextAlignment(TextAlignment.CENTER)));
                    table.addCell(new Cell().add(new Paragraph(currency.format(item.getPrice()))
                            .setTextAlignment(TextAlignment.RIGHT)));
                }
            } else {
                table.addCell(new Cell(1, 4).add(new Paragraph("Không có sản phẩm"))
                        .setTextAlignment(TextAlignment.CENTER));
            }

            document.add(table);

            // Tổng tiền
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("TỔNG TIỀN: " + currency.format(order.getTotal()))
                    .setFont(boldFont).setFontSize(16)
                    .setTextAlignment(TextAlignment.RIGHT));

       
            // Chữ ký
            document.add(new Paragraph("\n\n\n"));
            Table signTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
            signTable.setWidth(UnitValue.createPercentValue(100));

            signTable.addCell(new Cell().add(new Paragraph("Người lập phiếu").setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER));
            signTable.addCell(new Cell().add(new Paragraph("Người nhận hàng").setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER));

            signTable.addCell(new Cell().add(new Paragraph("\n\n\n(Ký, ghi rõ họ tên)").setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER));
            signTable.addCell(new Cell().add(new Paragraph("\n\n\n(Ký, ghi rõ họ tên)").setTextAlignment(TextAlignment.CENTER)).setBorder(Border.NO_BORDER));

            document.add(signTable);

        } catch (Exception e) {
            logger.error("Lỗi tạo phiếu xuất hàng: {}", e.getMessage(), e);
            throw new IOException("Không thể tạo PDF phiếu xuất hàng", e);
        }

        return baos.toByteArray();
    }

    // ==============================
    // 2. PHIẾU GỬI HÀNG (A5) – GIỐNG MẪU ẢNH
    // ==============================
    public byte[] generateShippingLabel(Order order) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfFont regularFont = loadFont("fonts/Roboto-Regular.ttf");
        PdfFont boldFont = loadFont("fonts/Roboto-Bold.ttf");

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc, PageSize.A5)) {

            document.setMargins(30, 30, 30, 30);
            document.setFont(regularFont);

            
            document.add(new Paragraph("PHIẾU GIAO HÀNG")
                    .setFont(boldFont).setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));

            // 2. Mã đơn
            document.add(new Paragraph("Mã đơn hàng: #" + order.getId())
                    .setFont(boldFont).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5));

            document.add(new Paragraph("\n"));

            // 3. TỪ: CỬA HÀNG – TỰ ĐỘNG LẤY TỪ SHOP
            Shop shop = order.getShop();
            String shopName = Optional.ofNullable(shop).map(Shop::getName).orElse("CỬA HÀNG ABC");
            
            String shopPhone = Optional.ofNullable(shop).map(Shop::getContactPhone).orElse("0900123456");

            document.add(new Paragraph("Từ: " + shopName.toUpperCase()).setFont(boldFont).setFontSize(13));
            
            document.add(new Paragraph("SĐT: " + shopPhone).setFontSize(11));

            document.add(new Paragraph("-------------------------------------------------")
                    .setTextAlignment(TextAlignment.CENTER).setFontSize(10));

            // 4. ĐẾN: NGƯỜI NHẬN
            String recipientName = Optional.ofNullable(order.getRecipientName()).orElse("N/A").toUpperCase();
            String shippingPhone = Optional.ofNullable(order.getShippingPhone()).orElse("N/A");
            String shippingAddress = Optional.ofNullable(order.getShippingAddress()).orElse("N/A");

            document.add(new Paragraph("ĐẾN: " + recipientName).setFont(boldFont).setFontSize(14));
            document.add(new Paragraph("SĐT: " + shippingPhone).setFont(boldFont).setFontSize(14));
            document.add(new Paragraph("Địa chỉ: " + shippingAddress).setFontSize(11));

            document.add(new Paragraph("-------------------------------------------------")
                    .setTextAlignment(TextAlignment.CENTER).setFontSize(10));

            // 5. THU HỘ (COD)
            String codAmount = "0 đ";
            if ("cod".equalsIgnoreCase(order.getPaymentMethod())) {
                Locale localeVN = new Locale("vi", "VN");
                NumberFormat currency = NumberFormat.getCurrencyInstance(localeVN);
                codAmount = currency.format(order.getTotal());
            }

            document.add(new Paragraph("THU HỘ (COD): " + codAmount)
                    .setFont(boldFont).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10));

            document.add(new Paragraph("\n"));

            Table footerTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}));
            footerTable.setWidth(UnitValue.createPercentValue(100));
            footerTable.setMarginTop(10);

           

            document.add(footerTable);  // ĐÃ THÊM FOOTER

            document.add(new Paragraph("\n"));

            // 6. Chú ý người nhận
            document.add(new Paragraph("Chú ý người nhận").setFont(boldFont).setFontSize(10));
            document.add(new Paragraph("Xác nhận hàng nguyên vẹn, không móp méo, bể vỡ")
                    .setFontSize(9));

            // 7. Chi dẫn giao hàng
            document.add(new Paragraph("Chi dẫn giao hàng:").setFont(boldFont).setFontSize(10));
            document.add(new Paragraph("- Không đóng kiện;").setFontSize(9));
            document.add(new Paragraph("- Chuyển hoàn sau 3 lần phát;").setFontSize(9));
            document.add(new Paragraph("- Lưu kho tối đa 5 ngày.").setFontSize(9));

        }

        return baos.toByteArray();
    }
}