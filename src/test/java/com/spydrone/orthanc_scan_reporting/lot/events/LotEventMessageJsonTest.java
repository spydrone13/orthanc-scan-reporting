package com.spydrone.orthanc_scan_reporting.lot.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.spydrone.orthanc_scan_reporting.lot.events.LotEventMessage.LotState;

import tools.jackson.databind.json.JsonMapper;

/** Pins the parts of the consumer's lot event JSON (its docs/jira.md example) that this app reads. */
class LotEventMessageJsonTest {

	private static final String MOVED = """
			{
			  "eventId": "6f1c2e0a-1d4b-4c55-9a7e-0b3f8f0c2d11", "type": "lot.moved", "schemaVersion": 1, "sequence": 1042,
			  "occurredAt": "2026-10-08T13:45:00Z", "lotId": "LOT-001",
			  "lot":  { "currentStage": "wafer-prep", "wipLocation": "WAFER-PREP-001", "status": "active", "onHold": false },
			  "from": { "stage": "intake", "wipLocation": null },
			  "to":   { "stage": "wafer-prep", "wipLocation": "WAFER-PREP-001" },
			  "scan": { "clientId": "c-7f3a", "userName": "op1", "scanStage": "intake", "note": "" },
			  "discrepancy": null, "rejectionReason": null, "previousStatus": null,
			  "someFutureField": "ignored"
			}
			""";

	private final JsonMapper json = JsonMapper.builder().build();

	@Test
	void readsTheConsumersEvent() {
		LotEventMessage event = json.readValue(MOVED, LotEventMessage.class);

		assertThat(event).isEqualTo(new LotEventMessage("6f1c2e0a-1d4b-4c55-9a7e-0b3f8f0c2d11", "lot.moved", 1, 1042L,
				Instant.parse("2026-10-08T13:45:00Z"), "LOT-001",
				new LotState("wafer-prep", "WAFER-PREP-001", LotStatus.ACTIVE, false)));
	}

	@Test
	void readsLowercaseStatuses() {
		assertThat(json.readValue("\"complete\"", LotStatus.class)).isEqualTo(LotStatus.COMPLETE);
		assertThat(json.readValue("\"canceled\"", LotStatus.class)).isEqualTo(LotStatus.CANCELED);
		assertThat(json.readValue("\"destroyed\"", LotStatus.class)).isEqualTo(LotStatus.DESTROYED);
	}
}
