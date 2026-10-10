package com.spydrone.orthanc_scan_reporting.summary;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spydrone.orthanc_scan_reporting.lot.events.LotStatus;

public interface LotSnapshotRepository extends JpaRepository<LotSnapshot, String> {

	List<LotSnapshot> findByCurrentStageAndStatusOrderByLotIdAsc(String stage, LotStatus status);
}
