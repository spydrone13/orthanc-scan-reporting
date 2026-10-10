package com.spydrone.orthanc_scan_reporting.summary.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.spydrone.orthanc_scan_reporting.summary.LotSnapshotRepository;
import com.spydrone.orthanc_scan_reporting.summary.StageSummaryRepository;

@WebMvcTest(StageSummaryController.class)
@Import(StageSummaryPageConfig.class)
class StageSummaryPageTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private StageSummaryRepository summaries;

	@MockitoBean
	private LotSnapshotRepository snapshots;

	@Test
	void friendlyUrlForwardsToPage() throws Exception {
		mvc.perform(get("/reports/stage-summary"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/stage-summary.html"));
	}

	@Test
	void rootRedirectsToTheReport() throws Exception {
		mvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/reports/stage-summary"));
	}

	@Test
	void servesPageAndAssets() throws Exception {
		mvc.perform(get("/stage-summary.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("id=\"rows\"")))
				.andExpect(content().string(containsString("id=\"lots-dialog\"")));
		mvc.perform(get("/js/stage-summary.js")).andExpect(status().isOk());
		mvc.perform(get("/css/reports.css")).andExpect(status().isOk());
	}
}
