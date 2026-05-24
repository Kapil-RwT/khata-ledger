package com.khataledger.merchant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA gives us CRUD for free via JpaRepository<T, ID>.
 * The findByPhone method works by query-method derivation — Spring parses the method
 * name at runtime and generates "SELECT m FROM Merchant m WHERE m.phone = ?1".
 */
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByPhone(String phone);
    boolean existsByPhone(String phone);
}
