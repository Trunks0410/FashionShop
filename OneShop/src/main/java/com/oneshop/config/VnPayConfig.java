package com.oneshop.config; // Đảm bảo package chính xác

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VnPayConfig {

    // Các thuộc tính lấy từ application.properties
    private String url;
    private String apiUrl;
    private String returnUrl;
    private String tmnCode;
    private String hashSecret;
    private String version;
    
    // Hằng số
    public static final String VNP_VERSION = "vnp_Version";
    public static final String VNP_COMMAND = "vnp_Command";
    public static final String VNP_TMNCODE = "vnp_TmnCode";
    public static final String VNP_AMOUNT = "vnp_Amount";
    public static final String VNP_CREATE_DATE = "vnp_CreateDate";
    public static final String VNP_CURR_CODE = "vnp_CurrCode";
    public static final String VNP_IPADDR = "vnp_IpAddr";
    public static final String VNP_LOCALE = "vnp_Locale";
    public static final String VNP_ORDER_INFO = "vnp_OrderInfo";
    public static final String VNP_ORDER_TYPE = "vnp_OrderType";
    public static final String VNP_RETURN_URL = "vnp_ReturnUrl";
    public static final String VNP_TXNREF = "vnp_TxnRef";
    public static final String VNP_SECURE_HASH = "vnp_SecureHash";
    public static final String VNP_EXPIRE_DATE = "vnp_ExpireDate";

    /**
     * Phương thức tạo URL thanh toán.
     * Đây là phương thức (instance method) mà Controller của bạn sẽ gọi.
     */
    public String createPaymentUrl(String vnp_TxnRef, long amount, String vnp_OrderInfo, String vnp_IpAddr) 
            throws UnsupportedEncodingException {
        
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put(VNP_VERSION, this.version); // Sử dụng 'this.version'
        vnp_Params.put(VNP_COMMAND, "pay");
        vnp_Params.put(VNP_TMNCODE, this.tmnCode); // Sử dụng 'this.tmnCode'
        vnp_Params.put(VNP_AMOUNT, String.valueOf(amount));
        vnp_Params.put(VNP_CURR_CODE, "VND");
        vnp_Params.put(VNP_TXNREF, vnp_TxnRef);
        vnp_Params.put(VNP_ORDER_INFO, vnp_OrderInfo);
        vnp_Params.put(VNP_ORDER_TYPE, "other"); // Loại hàng hóa (có thể cấu hình)
        vnp_Params.put(VNP_LOCALE, "vn"); // Ngôn ngữ
        vnp_Params.put(VNP_RETURN_URL, this.returnUrl); // Sử dụng 'this.returnUrl'
        vnp_Params.put(VNP_IPADDR, vnp_IpAddr);

        // Đặt thời gian tạo và hết hạn
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put(VNP_CREATE_DATE, vnp_CreateDate);
        
        cld.add(Calendar.MINUTE, 15); // Thời gian hết hạn (ví dụ 15 phút)
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put(VNP_EXPIRE_DATE, vnp_ExpireDate);

        // Tạo chữ ký
        // Sử dụng 'this.hashSecret'
        String vnp_SecureHash = hashAllFields(vnp_Params, this.hashSecret); 
        vnp_Params.put(VNP_SECURE_HASH, vnp_SecureHash);

        // Xây dựng URL cuối cùng
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                query.append(java.net.URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(java.net.URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                }
            }
        }
        return this.url + "?" + query.toString(); // Sử dụng 'this.url'
    }

    /**
     * Phương thức lấy IP.
     * Đây là phương thức (instance method) mà Controller của bạn sẽ gọi.
     */
    public String getIpAddress(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            // Xử lý trường hợp có nhiều IP (chỉ lấy IP đầu tiên)
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
        } catch (Exception e) {
            ipAddress = "Invalid IP:" + e.getMessage();
        }
        // Trường hợp test local
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "127.0.0.1".equals(ipAddress)) {
            ipAddress = "127.0.0.1"; // Hoặc một IP public đã whitelist để test
        }
        return ipAddress;
    }
    public boolean validateVnPayHash(Map<String, String> vnp_Params, String vnp_SecureHash) {
        try {
            // Lấy hashSecret từ thuộc tính của class
            String secret = this.hashSecret; 
            
            // Tái tạo chuỗi hash data từ vnp_Params
            // Phương thức hashAllFields (private static) đã có sẵn logic
            // sắp xếp, nối chuỗi và URL-encode giá trị.
            String calculatedHash = hashAllFields(vnp_Params, secret);
            
            // So sánh chữ ký tính toán được với chữ ký VNPAY gửi về
            return calculatedHash.equals(vnp_SecureHash);
            
        } catch (UnsupportedEncodingException e) {
            // Log lỗi nếu cần
            System.err.println("Error validating VNPAY hash: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error validating VNPAY hash: " + e.getMessage());
            return false;
        }
    }

    // === CÁC PHƯƠNG THỨC HELPER (PRIVATE STATIC) ===
    
    /**
     * Hỗ trợ tạo chữ ký
     */
    private static String hashAllFields(Map<String, String> fields, String secret) 
            throws UnsupportedEncodingException {
        
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                // Quan trọng: Phải encode giá trị trước khi hash
                hashData.append(java.net.URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString())); 
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        return hmacSHA512(secret, hashData.toString());
    }

    /**
     * Thuật toán HmacSHA512
     */
    private static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("Key or data is null");
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] resultBytes = hmac512.doFinal(dataBytes);

            // Chuyển byte array sang hex string
            StringBuilder sb = new StringBuilder(2 * resultBytes.length);
            for (byte b : resultBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error hashing data with HmacSHA512", e);
        }
    }
}