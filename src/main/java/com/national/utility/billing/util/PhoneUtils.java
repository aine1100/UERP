package com.national.utility.billing.util;

public final class PhoneUtils {

    private PhoneUtils() {
    }

    /**
     * Normalizes Rwanda phone numbers to local 10-digit format (e.g. 0781234567).
     */
    public static String normalizeRwandaPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return phoneNumber;
        }
        String trimmed = phoneNumber.trim();
        if (trimmed.startsWith("+250")) {
            return "0" + trimmed.substring(4);
        }
        return trimmed;
    }
}
