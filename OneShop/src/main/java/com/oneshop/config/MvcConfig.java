package com.oneshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối đến thư mục 'uploads/images'
        // Thư mục 'uploads' này nằm ở gốc dự án, ngang hàng với 'src'
        Path imageUploadDir = Paths.get("./uploads/images");
        String imageUploadPath = imageUploadDir.toFile().getAbsolutePath();

        // Cấu hình: Khi ai đó truy cập /uploads/images/**
        // Hãy tìm file trong thư mục 'file:/đường/dẫn/tuyệt/đối/uploads/images/'
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations("file:/" + imageUploadPath + "/");
    }
}