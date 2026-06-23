package com.kepco.complaint.repository;

import com.kepco.complaint.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    
    // 관제 화면에서 상태별(PENDING, IN_PROGRESS 등) 민원을 모아볼 때 사용할 기본 메서드
    List<Complaint> findByStatus(String status);
}
