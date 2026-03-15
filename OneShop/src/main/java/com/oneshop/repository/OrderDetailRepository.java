package com.oneshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.OrderDetail;


@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    // Spring Data JPA sẽ tự động cung cấp các phương thức CRUD (Create, Read, Update, Delete)
    // Bạn có thể thêm các phương thức truy vấn tùy chỉnh ở đây nếu cần.
}
