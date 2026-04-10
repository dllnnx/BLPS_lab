package ru.itmo.dto.network;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NetworkPolicyAddressDto {

    @NotBlank
    private String addr;

    private String description;
}
