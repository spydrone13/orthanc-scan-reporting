package com.spydrone.orthanc_scan_reporting;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// In-memory DB so tests don't write to the app's ./data file database; no seed, so the consumer needn't run.
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:context-test",
		"app.reporting.seed-on-startup=false" })
class OrthancScanReportingApplicationTests {

	@Test
	void contextLoads() {
	}

}
