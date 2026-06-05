package com.national.utility.billing.util;

import java.security.SecureRandom;

public final class OtpGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpGenerator() {
    }

    public static String generateSixDigitOtp() {
        int otp = RANDOM.nextInt(900_000) + 100_000;
        return String.valueOf(otp);
    }
}
