package com.spydrone.orthanc_scan_reporting.lot.events;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The consumer's lot status; lowercase on the wire in both lot events and its /api/lots. */
public enum LotStatus {
	@JsonProperty("active") ACTIVE,
	@JsonProperty("canceled") CANCELED,
	@JsonProperty("destroyed") DESTROYED,
	@JsonProperty("complete") COMPLETE
}
