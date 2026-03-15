package com.oneshop.service.impl;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.oneshop.service.FileStorageService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    // Đường dẫn gốc của thư mục lưu file
    private final Path root = Paths.get("uploads");
    private final Path imageRoot = root.resolve("images");
    
    // === THƯ MỤC MỚI CHO REVIEW MEDIA ===
    private final Path reviewRoot = root.resolve("reviews"); 
    // ======================================


    @Override
    public void init() {
        try {
            if (!Files.exists(root)) {
                Files.createDirectory(root);
            }
            if (!Files.exists(imageRoot)) {
                Files.createDirectory(imageRoot);
            }
            // === KHỞI TẠO THƯ MỤC REVIEWS ===
            if (!Files.exists(reviewRoot)) {
                Files.createDirectory(reviewRoot);
            }
            // =================================
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!", e);
        }
    }

    @Override
    public String save(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            
            String originalFilename = file.getOriginalFilename();
            // Lấy phần mở rộng (extension), an toàn hơn khi kiểm tra lastIndexOf
            String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Lưu file vào thư mục /uploads/images
            Files.copy(file.getInputStream(), this.imageRoot.resolve(uniqueFilename));
            
            return uniqueFilename; // Trả về tên file duy nhất
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file. Error: " + e.getMessage(), e);
        }
    }
    
    // === TRIỂN KHAI PHƯƠNG THỨC MỚI CHO REVIEW MEDIA ===
    @Override
    public String storeReviewFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }
        
        // Tạo tên file duy nhất
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Lưu file vào thư mục /uploads/reviews
        Files.copy(file.getInputStream(), this.imageRoot.resolve(uniqueFilename));
        
        // Trả về đường dẫn tương đối (ví dụ: /uploads/reviews/unique-name.jpg)
        return uniqueFilename; 
    }
    // ======================================================

    @Override
    public Resource load(String filename) {
        try {
            // CÂN NHẮC: Hàm này chỉ load từ imageRoot. Nếu bạn muốn load cả reviews,
            // bạn sẽ cần truyền thêm tham số (vd: loại thư mục)
            Path file = imageRoot.resolve(filename); 
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            // CÂN NHẮC: Hàm này chỉ xóa từ imageRoot. Nếu file là review, nó sẽ không xóa được.
            Path file = imageRoot.resolve(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete the file. Error: " + e.getMessage(), e);
        }
    }
}