package ru.itmo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "network_policy_cidrs")
@Getter
@Setter
public class NetworkPolicyCidr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private NetworkPolicy policy;

    @Column(nullable = false, length = 43)
    private String cidr;

    @Column(columnDefinition = "text")
    private String description;
}
