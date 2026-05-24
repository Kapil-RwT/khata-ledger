package com.khataledger.reminder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findAllByMerchantIdOrderByCreatedAtDesc(Long merchantId);
}
