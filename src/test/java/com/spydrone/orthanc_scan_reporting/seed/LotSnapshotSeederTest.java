package com.spydrone.orthanc_scan_reporting.seed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.web.client.ResourceAccessException;

import com.spydrone.orthanc_scan_reporting.lot.events.LotEventListener;
import com.spydrone.orthanc_scan_reporting.lot.events.LotEventMessage.LotState;
import com.spydrone.orthanc_scan_reporting.lot.events.LotStatus;
import com.spydrone.orthanc_scan_reporting.seed.ConsumerLotsClient.ConsumerLot;
import com.spydrone.orthanc_scan_reporting.summary.LotSnapshotRepository;
import com.spydrone.orthanc_scan_reporting.summary.StageSummaryProjector;

class LotSnapshotSeederTest {

	private final ConsumerLotsClient consumer = mock(ConsumerLotsClient.class);
	private final LotSnapshotRepository snapshots = mock(LotSnapshotRepository.class);
	private final StageSummaryProjector projector = mock(StageSummaryProjector.class);
	private final RabbitListenerEndpointRegistry listeners = mock(RabbitListenerEndpointRegistry.class);
	private final MessageListenerContainer listener = mock(MessageListenerContainer.class);

	@BeforeEach
	void listenerExists() {
		given(listeners.getListenerContainer(LotEventListener.ID)).willReturn(listener);
	}

	@Test
	void seedsAnEmptySummaryBeforeStartingTheListener() {
		given(snapshots.count()).willReturn(0L);
		given(consumer.fetchLots()).willReturn(List.of(
				new ConsumerLot("L1", "intake", "INTAKE-001", LotStatus.ACTIVE, false)));

		run(true);

		InOrder order = inOrder(projector, listener);
		order.verify(projector).applyAll(
				eq(Map.of("L1", new LotState("intake", "INTAKE-001", LotStatus.ACTIVE, false))), any());
		order.verify(listener).start();
	}

	@Test
	void doesNotSeedASummaryThatAlreadyHasLots() {
		given(snapshots.count()).willReturn(3L);

		run(true);

		verifyNoInteractions(consumer);
		verify(listener).start();
	}

	@Test
	void doesNotSeedWhenSeedingIsOff() {
		run(false);

		verifyNoInteractions(consumer, snapshots);
		verify(listener).start();
	}

	@Test
	void startsTheListenerWhenTheConsumerIsDown() {
		given(snapshots.count()).willReturn(0L);
		given(consumer.fetchLots()).willThrow(new ResourceAccessException("Connection refused"));

		run(true);

		verify(projector, never()).applyAll(any(), any());
		verify(listener).start();
	}

	private void run(boolean seedOnStartup) {
		new LotSnapshotSeeder(consumer, snapshots, projector, listeners, seedOnStartup)
				.run(new DefaultApplicationArguments());
	}
}
