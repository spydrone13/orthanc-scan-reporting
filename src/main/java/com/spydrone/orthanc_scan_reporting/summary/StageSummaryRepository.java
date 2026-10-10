package com.spydrone.orthanc_scan_reporting.summary;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StageSummaryRepository extends JpaRepository<StageSummary, String> {

	List<StageSummary> findAllByOrderByStageAsc();
}
