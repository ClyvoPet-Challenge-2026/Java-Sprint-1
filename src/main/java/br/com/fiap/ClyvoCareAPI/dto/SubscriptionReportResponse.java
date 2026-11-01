package br.com.fiap.ClyvoCareAPI.dto;

import tools.jackson.databind.JsonNode;

public record SubscriptionReportResponse(JsonNode items, long totalCount) {
}
