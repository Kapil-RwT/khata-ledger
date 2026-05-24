package com.khataledger.customer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * We deliberately use a plain Long FK instead of @ManyToOne to Merchant.
     * Reason: we never need to navigate Customer -> Merchant in this app,
     * and skipping the association keeps queries simple and avoids accidental N+1 loads.
     * Easy interview talking point: when @ManyToOne *is* the right call vs when it isn't.
     */
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 20)
    private String phone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
