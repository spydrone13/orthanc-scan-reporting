package com.spydrone.orthanc_scan_reporting.summary;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.spydrone.orthanc_scan_reporting.lot.events.LotEventMessage.LotState;
import com.spydrone.orthanc_scan_reporting.lot.events.LotStatus;

/**
 * A lot's last known state. Events say where a lot is now; this says where it was counted before, so
 * the right stage can be decremented, and which event was applied last, so duplicates are skipped.
 */
@Entity
@Table(name = "lot_snapshots")
public class LotSnapshot {

	@Id
	private String lotId;
	private String currentStage;
	@Enumerated(EnumType.STRING)
	private LotStatus status;
	@Column(nullable = false)
	private boolean onHold;
	/** The last applied event's sequence; null when seeded from the consumer's API. */
	private Long lastSequence;
	private Instant updatedAt;

	protected LotSnapshot() {
	}

	LotSnapshot(String lotId) {
		this.lotId = lotId;
	}

	/** Whether an event with this sequence was already applied, or is older than the last one applied. */
	boolean hasApplied(Long sequence) {
		return sequence != null && lastSequence != null && sequence <= lastSequence;
	}

	/** The stage this lot counts toward, or null if it isn't counted: only active lots with a stage are. */
	String countedStage() {
		return status == LotStatus.ACTIVE ? currentStage : null;
	}

	void update(LotState state, Long sequence, Instant at) {
		this.currentStage = state.currentStage();
		this.status = state.status();
		this.onHold = state.onHold();
		this.lastSequence = sequence;
		this.updatedAt = at;
	}

	public String getLotId() {
		return lotId;
	}

	public String getCurrentStage() {
		return currentStage;
	}

	public LotStatus getStatus() {
		return status;
	}

	public boolean isOnHold() {
		return onHold;
	}

	public Long getLastSequence() {
		return lastSequence;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
