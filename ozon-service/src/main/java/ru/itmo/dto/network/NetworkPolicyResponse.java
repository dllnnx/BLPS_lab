package ru.itmo.dto.network;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkPolicyResponse {

    private Long id;
    private String name;
    private String description;
    private List<NetworkPolicyAddressDto> addresses = new ArrayList<>();
    private Set<String> roleNames = new HashSet<>();
}
