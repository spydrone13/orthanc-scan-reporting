package com.spydrone.orthanc_scan_reporting.summary.api;

import java.util.List;

/** Active lots per stage, and across all stages. */
public record StageSummaryReport(List<StageCount> stages, long totalActiveLots) {

	public record StageCount(String stage, long activeLots) {
	}
}
