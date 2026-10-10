package com.spydrone.orthanc_scan_reporting.summary;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LotSnapshotRepository extends JpaRepository<LotSnapshot, String> {
}
