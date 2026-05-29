package com.wallet.walletservice.config;

import com.wallet.walletservice.entity.Wallet;
import com.wallet.walletservice.enums.WalletStatus;
import com.wallet.walletservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class TestWalletSeeder implements CommandLineRunner {

    private final WalletRepository walletRepository;

    // username → starting balance
    private static final List<Object[]> SEED_DATA = List.of(
            new Object[]{"alice",   new BigDecimal("850000.0000")},
            new Object[]{"bob",     new BigDecimal("620000.0000")},
            new Object[]{"charlie", new BigDecimal("430000.0000")},
            new Object[]{"diana",   new BigDecimal("990000.0000")},
            new Object[]{"evan",    new BigDecimal("175000.0000")},
            new Object[]{"fiona",   new BigDecimal("310000.0000")},
            new Object[]{"george",  new BigDecimal("540000.0000")},
            new Object[]{"hannah",  new BigDecimal("760000.0000")},
            new Object[]{"ivan",    new BigDecimal("220000.0000")},
            new Object[]{"julia",   new BigDecimal("480000.0000")}
    );

    @Override
    public void run(String... args) {
        RestTemplate restTemplate = new RestTemplate();

        for (Object[] row : SEED_DATA) {
            String username = (String) row[0];
            BigDecimal balance = (BigDecimal) row[1];

            try {
                // Look up userId from UserService
                String url = "http://localhost:8082/user/internal/by-username/" + username;
                Map<?, ?> response = restTemplate.getForObject(url, Map.class);
                if (response == null || response.get("userId") == null) {
                    log.warn("User not found: {}, skipping wallet seed", username);
                    continue;
                }
                Long userId = Long.valueOf(response.get("userId").toString());

                if (walletRepository.existsByUserId(userId)) {
                    log.info("Wallet already exists for user: {}, skipping", username);
                    continue;
                }

                Wallet wallet = new Wallet();
                wallet.setUserId(userId);
                wallet.setCurrency("INR");
                wallet.setAvailableBalance(balance);
                wallet.setHeldBalance(BigDecimal.ZERO);
                wallet.setStatus(WalletStatus.ACTIVE);

                walletRepository.save(wallet);
                log.info("Wallet seeded for user: {} (userId={}) balance={}", username, userId, balance);

            } catch (Exception e) {
                log.warn("Could not seed wallet for user {}: {}", username, e.getMessage());
            }
        }
    }
}
