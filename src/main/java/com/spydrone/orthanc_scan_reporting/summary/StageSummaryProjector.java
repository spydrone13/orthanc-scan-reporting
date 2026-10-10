package com.spydrone.orthanc_scan_reporting.summary;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spydrone.orthanc_scan_reporting.lot.events.LotEventMessage.LotState;

/**
 * The only place the stage counts change. A lot's new state is applied as a whole, whatever the event
 * type, so seeding and every event go through the same rule: a lot counts toward its stage while active.
 */
@Service
public class StageSummaryProjector {

	private final LotSnapshotRepository snapshots;
	private final StageSummaryRepository summaries;

	public StageSummaryProjector(LotSnapshotRepository snapshots, StageSummaryRepository summaries) {
		this.snapshots = snapshots;
		this.summaries = summaries;
	}

	/**
	 * Sets the lot's state and moves its count if the stage it counts toward changed.
	 *
	 * @param sequence the event's sequence, or null when seeding
	 * @return false if the event was skipped as a duplicate or older than one already applied
	 */
	@Transactional
	public boolean apply(String lotId, LotState state, Long sequence, Instant at) {
		LotSnapshot snapshot = snapshots.findById(lotId).orElseGet(() -> new LotSnapshot(lotId));
		if (snapshot.hasApplied(sequence)) {
			return false;
		}
		String before = snapshot.countedStage();
		snapshot.update(state, sequence, at);
		String after = snapshot.countedStage();
		if (!Objects.equals(before, after)) {
			if (before != null) {
				decrement(before, at);
			}
			if (after != null) {
				increment(after, at);
			}
		}
		snapshots.save(snapshot);
		return true;
	}

	/** Applies all the lots in one transaction, so a failed seed leaves nothing behind to block a retry. */
	@Transactional
	public void applyAll(Map<String, LotState> states, Instant at) {
		states.forEach((lotId, state) -> apply(lotId, state, null, at));
	}

	private void increment(String stage, Instant at) {
		StageSummary summary = summaries.findById(stage).orElseGet(() -> new StageSummary(stage));
		summary.increment(at);
		summaries.save(summary);
	}

	private void decrement(String stage, Instant at) {
		summaries.findById(stage).ifPresent(summary -> {
			summary.decrement(at);
			if (summary.getActiveLots() <= 0) {
				summaries.delete(summary);
			}
			else {
				summaries.save(summary);
			}
		});
	}
}
