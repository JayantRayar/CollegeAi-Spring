
package com.collegeai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OAuthCodeExchangeRequest {

    @NotBlank(message = "OAuth authorization code is required.")
    private String code;
}

