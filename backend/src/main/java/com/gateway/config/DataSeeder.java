package com.gateway.config;

import com.gateway.models.Merchant;
import com.gateway.repositories.MerchantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private final MerchantRepository merchantRepository;

    @Value("${gateway.test.merchant.email}")
    private String testEmail;

    @Value("${gateway.test.merchant.key}")
    private String testKey;

    @Value("${gateway.test.merchant.secret}")
    private String testSecret;

    public DataSeeder(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (merchantRepository.findByEmail(testEmail).isEmpty()) {
            Merchant merchant = new Merchant();
            merchant.setId(UUID.randomUUID().toString());
            merchant.setEmail(testEmail);
            merchant.setPassword("password");
            merchant.setApiKey(testKey);
            merchant.setApiSecret(testSecret);
            merchant.setName("Test Merchant");
            merchant.setWebhookSecret("whsec_test_abc123");

            merchantRepository.save(merchant);
            System.out.println("✅ Test Merchant Seeded: " + testEmail);
        } else {
            System.out.println("ℹ️ Test Merchant already exists.");
        }
    }
}