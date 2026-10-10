package com.spydrone.orthanc_scan_reporting.seed;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.spydrone.orthanc_scan_reporting.lot.events.LotStatus;

/** Reads the current lots from orthanc-scan-consumer's GET /api/lots. */
@Component
public class ConsumerLotsClient {

	private final RestClient rest;

	public ConsumerLotsClient(@Value("${app.reporting.consumer-base-url}") String baseUrl) {
		this.rest = RestClient.create(baseUrl);
	}

	public List<ConsumerLot> fetchLots() {
		List<ConsumerLot> lots = rest.get().uri("/api/lots").retrieve().body(new ParameterizedTypeReference<>() {
		});
		return lots == null ? List.of() : lots;
	}

	/** The part of the consumer's LotView this app reads. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ConsumerLot(String lotId, String currentStage, String wipLocation, LotStatus status, boolean onHold) {
	}
}
