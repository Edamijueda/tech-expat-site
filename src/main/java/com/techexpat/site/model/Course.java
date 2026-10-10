package com.techexpat.site.model;

import java.math.BigDecimal;

public record Course(String slug, String title, BigDecimal priceUsd) {
}
