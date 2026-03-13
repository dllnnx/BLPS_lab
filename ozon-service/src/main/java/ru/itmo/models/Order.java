package ru.itmo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column
    private OrderStatus orderStatus;

    @Column
    private String username;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_point_id", nullable = false)
    private PickupPoint pickupPoint;

    @Column(name = "delivery_address")
    private String deliveryAddress;

}