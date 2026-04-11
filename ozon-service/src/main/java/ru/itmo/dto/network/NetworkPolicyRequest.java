package ru.itmo.dto.network;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class NetworkPolicyRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @Valid
    private List<NetworkPolicyAddressDto> addresses = new ArrayList<>();

    @NotEmpty
    private Set<String> roleNames = new HashSet<>();
}
