package com.spydrone.orthanc_scan_reporting.summary;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.spydrone.orthanc_scan_reporting.lot.events.LotEventMessage.LotState;
import com.spydrone.orthanc_scan_reporting.lot.events.LotStatus;

@DataJpaTest
@Import(StageSummaryProjector.class)
class StageSummaryProjectorTest {

	private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");

	@Autowired
	private StageSummaryProjector projector;

	@Autowired
	private StageSummaryRepository summaries;

	@Autowired
	private LotSnapshotRepository snapshots;

	@Test
	void countsANewLotAtItsStage() {
		projector.apply("L1", active("intake"), 1L, NOW);
		projector.apply("L2", active("intake"), 2L, NOW);

		assertThat(counts()).containsExactly(Map.entry("intake", 2L));
	}

	@Test
	void movesTheCountWithTheLot() {
		projector.apply("L1", active("intake"), 1L, NOW);
		projector.apply("L2", active("intake"), 2L, NOW);

		projector.apply("L1", active("wafer-prep"), 3L, NOW);

		assertThat(counts()).containsExactly(Map.entry("intake", 1L), Map.entry("wafer-prep", 1L));
	}

	@Test
	void dropsTheRowWhenTheLastLotLeavesAStage() {
		projector.apply("L1", active("intake"), 1L, NOW);

		projector.apply("L1", active("wafer-prep"), 2L, NOW);

		assertThat(counts()).containsExactly(Map.entry("wafer-prep", 1L));
	}

	@Test
	void stopsCountingALotThatIsNoLongerActive() {
		projector.apply("L1", active("testing"), 1L, NOW);

		projector.apply("L1", new LotState("testing", "TESTING-001", LotStatus.COMPLETE, false), 2L, NOW);

		assertThat(counts()).isEmpty();
		assertThat(snapshots.findById("L1")).get().extracting(LotSnapshot::getStatus).isEqualTo(LotStatus.COMPLETE);
	}

	@Test
	void countsALotAgainWhenItIsReactivated() {
		projector.apply("L1", new LotState("testing", "TESTING-001", LotStatus.CANCELED, false), 1L, NOW);

		projector.apply("L1", active("testing"), 2L, NOW);

		assertThat(counts()).containsExactly(Map.entry("testing", 1L));
	}

	@Test
	void keepsCountingHeldLots() {
		projector.apply("L1", active("intake"), 1L, NOW);

		projector.apply("L1", new LotState("intake", "INTAKE-001", LotStatus.ACTIVE, true), 2L, NOW);

		assertThat(counts()).containsExactly(Map.entry("intake", 1L));
	}

	@Test
	void skipsDuplicateAndStaleEvents() {
		projector.apply("L1", active("intake"), 5L, NOW);
		projector.apply("L1", active("wafer-prep"), 6L, NOW);

		assertThat(projector.apply("L1", active("wafer-prep"), 6L, NOW)).isFalse();
		assertThat(projector.apply("L1", active("intake"), 5L, NOW)).isFalse();

		assertThat(counts()).containsExactly(Map.entry("wafer-prep", 1L));
	}

	@Test
	void keepsTheLotsWipLocation() {
		projector.apply("L1", new LotState("intake", "INTAKE-002", LotStatus.ACTIVE, false), 1L, NOW);

		assertThat(snapshots.findById("L1")).get().extracting(LotSnapshot::getWipLocation).isEqualTo("INTAKE-002");
	}

	@Test
	void findsActiveLotsAtAStage() {
		projector.apply("L3", active("intake"), 1L, NOW);
		projector.apply("L1", active("intake"), 2L, NOW);
		projector.apply("L2", new LotState("intake", "INTAKE-001", LotStatus.CANCELED, false), 3L, NOW);
		projector.apply("L4", active("testing"), 4L, NOW);

		assertThat(snapshots.findByCurrentStageAndStatusOrderByLotIdAsc("intake", LotStatus.ACTIVE))
				.extracting(LotSnapshot::getLotId)
				.containsExactly("L1", "L3");
	}

	@Test
	void eventsAfterASeedReplaceTheSeededState() {
		projector.applyAll(Map.of("L1", active("intake")), NOW);

		projector.apply("L1", active("wafer-prep"), 1L, NOW);

		assertThat(counts()).containsExactly(Map.entry("wafer-prep", 1L));
	}

	private static LotState active(String stage) {
		return new LotState(stage, stage.toUpperCase() + "-001", LotStatus.ACTIVE, false);
	}

	private Map<String, Long> counts() {
		Map<String, Long> counts = new LinkedHashMap<>();
		summaries.findAllByOrderByStageAsc().forEach(summary -> counts.put(summary.getStage(), summary.getActiveLots()));
		return counts;
	}
}
