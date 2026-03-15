package com.oneshop.repository;

import com.oneshop.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    // Lấy lịch sử chat hai chiều giữa userA và userB
    @Query("""
        SELECT m FROM ChatMessageEntity m
        WHERE (m.sender = :userA AND m.receiver = :userB)
           OR (m.sender = :userB AND m.receiver = :userA)
        ORDER BY m.timestamp ASC
    """)
    List<ChatMessageEntity> findChatHistory(@Param("userA") String userA, @Param("userB") String userB);

    // Lấy danh sách username của người dùng đã nhắn tin với vendor
    @Query("""
        SELECT DISTINCT CASE
            WHEN m.sender = :vendor THEN m.receiver
            WHEN m.receiver = :vendor THEN m.sender
            ELSE null
        END
        FROM ChatMessageEntity m
        WHERE m.sender = :vendor OR m.receiver = :vendor
    """)
    List<String> findChatParticipants(@Param("vendor") String vendor);
}