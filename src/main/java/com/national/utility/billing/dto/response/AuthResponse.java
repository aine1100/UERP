package com.national.utility.billing.dto.response;

import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.model.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = """
        Authentication response. On login/setup-password/refresh:
        - accessToken: JWT (stateless, NOT stored in DB — client holds it)
        - refreshToken: opaque UUID stored in refresh_tokens table (DB) until used or expired
        """)
public class AuthResponse {

    @Schema(description = "Short-lived JWT access token (Bearer). Not persisted server-side.")
    private String accessToken;

    @Schema(description = "Long-lived refresh token. Persisted in refresh_tokens table linked to user.")
    private String refreshToken;

    @Schema(example = "Bearer")
    private String tokenType;

    @Schema(description = "Access token TTL in seconds", example = "900")
    private Long expiresIn;

    private Long userId;
    private String email;
    private String fullNames;
    private UserRole role;
    private UserStatus status;
}
