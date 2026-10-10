package com.spydrone.orthanc_scan_reporting.summary.api;

import java.time.Instant;
import java.util.List;

/** The active lots at a stage: the lots behind its count in {@link StageSummaryReport}. */
public record StageLots(String stage, List<StageLot> lots) {

	/** {@code updatedAt} is when the lot's last event happened (or when it was seeded). */
	public record StageLot(String lotId, String wipLocation, boolean onHold, Instant updatedAt) {
	}
}
