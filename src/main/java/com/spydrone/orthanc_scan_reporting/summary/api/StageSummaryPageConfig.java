package com.spydrone.orthanc_scan_reporting.summary.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the lots-by-stage report (static/stage-summary.html) at /reports/stage-summary; it's the app's
 * only page, so / goes there too.
 */
@Configuration
public class StageSummaryPageConfig implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/reports/stage-summary").setViewName("forward:/stage-summary.html");
		registry.addRedirectViewController("/", "/reports/stage-summary");
	}
}
