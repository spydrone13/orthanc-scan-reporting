package com.spydrone.orthanc_scan_reporting.summary.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spydrone.orthanc_scan_reporting.summary.StageSummaryRepository;
import com.spydrone.orthanc_scan_reporting.summary.api.StageSummaryReport.StageCount;

@RestController
@RequestMapping("/api/reports")
public class StageSummaryController {

	private final StageSummaryRepository summaries;

	public StageSummaryController(StageSummaryRepository summaries) {
		this.summaries = summaries;
	}

	@GetMapping("/stage-summary")
	public StageSummaryReport stageSummary() {
		List<StageCount> stages = summaries.findAllByOrderByStageAsc().stream()
				.map(summary -> new StageCount(summary.getStage(), summary.getActiveLots()))
				.toList();
		return new StageSummaryReport(stages, stages.stream().mapToLong(StageCount::activeLots).sum());
	}
}
