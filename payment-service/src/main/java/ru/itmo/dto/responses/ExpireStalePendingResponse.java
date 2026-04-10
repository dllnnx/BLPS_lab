package ru.itmo.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExpireStalePendingResponse {
    private int expiredCount;
}
