package com.spydrone.orthanc_scan_reporting.summary;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** How many active lots are at a stage. Stages with none have no row. */
@Entity
@Table(name = "stage_summaries")
public class StageSummary {

	@Id
	private String stage;
	@Column(nullable = false)
	private long activeLots;
	private Instant updatedAt;

	protected StageSummary() {
	}

	StageSummary(String stage) {
		this.stage = stage;
	}

	void increment(Instant at) {
		activeLots++;
		updatedAt = at;
	}

	void decrement(Instant at) {
		activeLots--;
		updatedAt = at;
	}

	public String getStage() {
		return stage;
	}

	public long getActiveLots() {
		return activeLots;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
