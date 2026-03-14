package ru.itmo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "card_info")
public class CardInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cardId;

    @Column(nullable = false)
    private String expireDate;

    @Column(nullable = false)
    private String cvc;

    @Column
    private Long balanceKopecks;

    public Integer getMonth(){
        return Integer.valueOf(expireDate.split("/")[0]);
    }

    public Integer getYear(){
        return Integer.valueOf(expireDate.split("/")[1]);
    }
}
