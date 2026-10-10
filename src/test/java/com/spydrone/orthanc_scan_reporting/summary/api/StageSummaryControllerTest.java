package com.spydrone.orthanc_scan_reporting.summary.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.spydrone.orthanc_scan_reporting.summary.StageSummary;
import com.spydrone.orthanc_scan_reporting.summary.StageSummaryRepository;

/** Pins the report's path and JSON shape. */
@WebMvcTest(StageSummaryController.class)
class StageSummaryControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private StageSummaryRepository summaries;

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

	private static StageSummary summary(String stage, long activeLots) {
		StageSummary summary = mock(StageSummary.class);
		given(summary.getStage()).willReturn(stage);
		given(summary.getActiveLots()).willReturn(activeLots);
		return summary;
	}
}
