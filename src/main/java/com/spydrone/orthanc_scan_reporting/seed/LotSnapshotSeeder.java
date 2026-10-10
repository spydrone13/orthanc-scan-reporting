package com.spydrone.orthanc_scan_reporting.seed;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.spydrone.orthanc_scan_reporting.lot.events.LotEventListener;
import com.spydrone.orthanc_scan_reporting.lot.events.LotEventMessage.LotState;
import com.spydrone.orthanc_scan_reporting.seed.ConsumerLotsClient.ConsumerLot;
import com.spydrone.orthanc_scan_reporting.summary.LotSnapshotRepository;
import com.spydrone.orthanc_scan_reporting.summary.StageSummaryProjector;

/**
 * A new queue only receives events published after it was bound, so on first start the lots that already
 * exist are loaded from the consumer. This runs after the context has started, so the queue is already
 * bound and buffering; the listener starts only afterwards, and the events it then applies replace the
 * seeded state, so nothing published during the seed is lost. If the consumer can't be reached, the
 * summary is built from events alone, and the seed is retried on the next start while nothing is stored.
 */
@Component
public class LotSnapshotSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(LotSnapshotSeeder.class);

	private final ConsumerLotsClient consumer;
	private final LotSnapshotRepository snapshots;
	private final StageSummaryProjector projector;
	private final RabbitListenerEndpointRegistry listeners;
	private final boolean seedOnStartup;

	public LotSnapshotSeeder(ConsumerLotsClient consumer, LotSnapshotRepository snapshots,
			StageSummaryProjector projector, RabbitListenerEndpointRegistry listeners,
			@Value("${app.reporting.seed-on-startup}") boolean seedOnStartup) {
		this.consumer = consumer;
		this.snapshots = snapshots;
		this.projector = projector;
		this.listeners = listeners;
		this.seedOnStartup = seedOnStartup;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			if (seedOnStartup && snapshots.count() == 0) {
				seed();
			}
		}
		catch (RestClientException e) {
			log.warn("Couldn't load the existing lots from the consumer; the summary will hold only lots seen in events", e);
		}
		finally {
			listeners.getListenerContainer(LotEventListener.ID).start();
		}
	}

	private void seed() {
		List<ConsumerLot> lots = consumer.fetchLots();
		Map<String, LotState> states = new LinkedHashMap<>();
		for (ConsumerLot lot : lots) {
			states.put(lot.lotId(), new LotState(lot.currentStage(), lot.wipLocation(), lot.status(), lot.onHold()));
		}
		projector.applyAll(states, Instant.now());
		log.info("Seeded {} lots from the consumer", states.size());
	}
}
