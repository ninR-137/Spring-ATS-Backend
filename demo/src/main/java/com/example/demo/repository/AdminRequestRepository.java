package com.example.demo.repository;

import com.example.demo.model.AdminRequest;
import com.example.demo.model.AdminRequestStatus;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminRequestRepository extends JpaRepository<AdminRequest, Long> {
    boolean existsByUserAndStatus(User user, AdminRequestStatus status);
    List<AdminRequest> findAllByUserOrderByCreatedAtDesc(User user);
    List<AdminRequest> findAllByStatusOrderByCreatedAtAsc(AdminRequestStatus status);
}