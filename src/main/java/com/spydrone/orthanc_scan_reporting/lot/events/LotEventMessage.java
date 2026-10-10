package com.spydrone.orthanc_scan_reporting.lot.events;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The part of the consumer's lot event (exchange orthanc.lots) this app reads. Every event type carries
 * the lot's full state after the change in {@code lot}, so the type-specific fields aren't needed;
 * unknown fields are ignored because the consumer adds fields without bumping the schema version.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LotEventMessage(
		String eventId,
		String type,
		int schemaVersion,
		Long sequence,
		Instant occurredAt,
		String lotId,
		LotState lot) {

	/** The schema this app understands; a higher version is a breaking change. */
	public static final int SCHEMA_VERSION = 1;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LotState(String currentStage, String wipLocation, LotStatus status, boolean onHold) {
	}
}
