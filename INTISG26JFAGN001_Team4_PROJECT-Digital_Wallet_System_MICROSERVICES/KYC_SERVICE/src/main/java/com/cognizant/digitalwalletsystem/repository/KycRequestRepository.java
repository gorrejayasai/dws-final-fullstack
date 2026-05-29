package com.cognizant.digitalwalletsystem.repository;

import com.cognizant.digitalwalletsystem.entity.KycRequest;
import com.cognizant.digitalwalletsystem.entity.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycRequestRepository extends JpaRepository<KycRequest, Long> {

    Optional<KycRequest> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<KycRequest> findAllByStatus(KycStatus status);
}
