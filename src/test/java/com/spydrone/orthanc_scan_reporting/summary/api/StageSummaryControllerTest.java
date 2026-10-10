package com.spydrone.orthanc_scan_reporting.summary.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.spydrone.orthanc_scan_reporting.lot.events.LotStatus;
import com.spydrone.orthanc_scan_reporting.summary.LotSnapshot;
import com.spydrone.orthanc_scan_reporting.summary.LotSnapshotRepository;
import com.spydrone.orthanc_scan_reporting.summary.StageSummary;
import com.spydrone.orthanc_scan_reporting.summary.StageSummaryRepository;

/** Pins the report's paths and JSON shapes, which the page depends on. */
@WebMvcTest(StageSummaryController.class)
class StageSummaryControllerTest {

	private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private StageSummaryRepository summaries;

	@MockitoBean
	private LotSnapshotRepository snapshots;

	@Test
	void reportsActiveLotsPerStageAndInTotal() throws Exception {
		List<StageSummary> stages = List.of(summary("intake", 2), summary("testing", 3));
		given(summaries.findAllByOrderByStageAsc()).willReturn(stages);

		mvc.perform(get("/api/reports/stage-summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stages[0].stage").value("intake"))
				.andExpect(jsonPath("$.stages[0].activeLots").value(2))
				.andExpect(jsonPath("$.stages[1].stage").value("testing"))
				.andExpect(jsonPath("$.stages[1].activeLots").value(3))
				.andExpect(jsonPath("$.totalActiveLots").value(5));
	}

	@Test
	void listsTheActiveLotsAtAStage() throws Exception {
		List<LotSnapshot> lots = List.of(lot("L1", "INTAKE-001", false), lot("L2", null, true));
		given(snapshots.findByCurrentStageAndStatusOrderByLotIdAsc("intake", LotStatus.ACTIVE)).willReturn(lots);

		mvc.perform(get("/api/reports/stage-summary/intake/lots"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stage").value("intake"))
				.andExpect(jsonPath("$.lots[0].lotId").value("L1"))
				.andExpect(jsonPath("$.lots[0].wipLocation").value("INTAKE-001"))
				.andExpect(jsonPath("$.lots[0].onHold").value(false))
				.andExpect(jsonPath("$.lots[0].updatedAt").value("2026-10-10T12:00:00Z"))
				.andExpect(jsonPath("$.lots[1].lotId").value("L2"))
				.andExpect(jsonPath("$.lots[1].onHold").value(true));
	}

	@Test
	void listsNoLotsForAStageWithNone() throws Exception {
		mvc.perform(get("/api/reports/stage-summary/shipped/lots"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stage").value("shipped"))
				.andExpect(jsonPath("$.lots").isEmpty());
	}

	private static StageSummary summary(String stage, long activeLots) {
		StageSummary summary = mock(StageSummary.class);
		given(summary.getStage()).willReturn(stage);
		given(summary.getActiveLots()).willReturn(activeLots);
		return summary;
	}

	private static LotSnapshot lot(String lotId, String wipLocation, boolean onHold) {
		LotSnapshot lot = mock(LotSnapshot.class);
		given(lot.getLotId()).willReturn(lotId);
		given(lot.getWipLocation()).willReturn(wipLocation);
		given(lot.isOnHold()).willReturn(onHold);
		given(lot.getUpdatedAt()).willReturn(NOW);
		return lot;
	}
}
