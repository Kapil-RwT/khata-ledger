package com.khataledger.auth;

import com.khataledger.auth.dto.AuthResponse;
import com.khataledger.auth.dto.LoginRequest;
import com.khataledger.auth.dto.SignupRequest;
import com.khataledger.common.exception.BadRequestException;
import com.khataledger.common.exception.UnauthorizedException;
import com.khataledger.merchant.Merchant;
import com.khataledger.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MerchantRepository merchants;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    @Transactional
    public AuthResponse signup(SignupRequest req) {
        if (merchants.existsByPhone(req.phone())) {
            throw new BadRequestException("Phone already registered");
        }
        Merchant m = Merchant.builder()
                .businessName(req.businessName().trim())
                .phone(req.phone().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .build();
        m = merchants.save(m);
        return new AuthResponse(jwt.issue(m.getId(), m.getBusinessName()),
                                m.getId(), m.getBusinessName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Merchant m = merchants.findByPhone(req.phone().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), m.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return new AuthResponse(jwt.issue(m.getId(), m.getBusinessName()),
                                m.getId(), m.getBusinessName());
    }
}
