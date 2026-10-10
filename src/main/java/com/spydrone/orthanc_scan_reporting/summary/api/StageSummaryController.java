package com.spydrone.orthanc_scan_reporting.summary.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spydrone.orthanc_scan_reporting.lot.events.LotStatus;
import com.spydrone.orthanc_scan_reporting.summary.LotSnapshotRepository;
import com.spydrone.orthanc_scan_reporting.summary.StageSummaryRepository;
import com.spydrone.orthanc_scan_reporting.summary.api.StageLots.StageLot;
import com.spydrone.orthanc_scan_reporting.summary.api.StageSummaryReport.StageCount;

@RestController
@RequestMapping("/api/reports")
public class StageSummaryController {

	private final StageSummaryRepository summaries;
	private final LotSnapshotRepository snapshots;

	public StageSummaryController(StageSummaryRepository summaries, LotSnapshotRepository snapshots) {
		this.summaries = summaries;
		this.snapshots = snapshots;
	}

	@GetMapping("/stage-summary")
	public StageSummaryReport stageSummary() {
		List<StageCount> stages = summaries.findAllByOrderByStageAsc().stream()
				.map(summary -> new StageCount(summary.getStage(), summary.getActiveLots()))
				.toList();
		return new StageSummaryReport(stages, stages.stream().mapToLong(StageCount::activeLots).sum());
	}

	/**
	 * Lists the lots counted at a stage, so only active ones. A stage with none gives an empty list rather
	 * than 404: its last lot may have moved on between the page showing it and this request.
	 */
	@GetMapping("/stage-summary/{stage}/lots")
	public StageLots stageLots(@PathVariable String stage) {
		List<StageLot> lots = snapshots.findByCurrentStageAndStatusOrderByLotIdAsc(stage, LotStatus.ACTIVE).stream()
				.map(lot -> new StageLot(lot.getLotId(), lot.getWipLocation(), lot.isOnHold(), lot.getUpdatedAt()))
				.toList();
		return new StageLots(stage, lots);
	}
}
