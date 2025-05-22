package de.meziane.redis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductDto(Long id, @NotBlank String name, @Positive BigDecimal price) implements Serializable {
}
