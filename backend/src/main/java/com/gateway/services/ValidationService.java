package com.gateway.services;

import org.springframework.stereotype.Service;
import java.time.YearMonth;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    private static final Pattern VPA_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$");

    // 1. VPA Validation
    public boolean isValidVpa(String vpa) {
        return vpa != null && VPA_PATTERN.matcher(vpa).matches();
    }

    // 2. Luhn Algorithm for Card Numbers
    public boolean isValidLuhn(String cardNumber) {
        if (cardNumber == null) return false;
        String sanitized = cardNumber.replaceAll("[\\s-]", "");
        if (!sanitized.matches("\\d{13,19}")) return false;

        int sum = 0;
        boolean alternate = false;
        for (int i = sanitized.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(sanitized.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    // 3. Card Network Detection
    public String getCardNetwork(String cardNumber) {
        if (cardNumber == null) return "unknown";
        String sanitized = cardNumber.replaceAll("[\\s-]", "");
        
        if (sanitized.startsWith("4")) return "visa";
        
        // Mastercard: 51-55
        if (sanitized.matches("^5[1-5].*")) return "mastercard";
        
        // Amex: 34, 37
        if (sanitized.matches("^3[47].*")) return "amex";
        
        // RuPay: 60, 65, 81-89
        if (sanitized.matches("^(60|65|8[1-9]).*")) return "rupay";
        
        return "unknown";
    }

    // 4. Expiry Date Validation
    public boolean isValidExpiry(String monthStr, String yearStr) {
        try {
            int month = Integer.parseInt(monthStr);
            int year = Integer.parseInt(yearStr);

            // Handle 2-digit years (e.g., "25" -> 2025)
            if (year < 100) year += 2000;

            if (month < 1 || month > 12) return false;

            YearMonth expiry = YearMonth.of(year, month);
            return !expiry.isBefore(YearMonth.now());
        } catch (NumberFormatException e) {
            return false;
        }
    }
}