package com.spydrone.orthanc_scan_reporting.lot.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.spydrone.orthanc_scan_reporting.summary.StageSummaryProjector;

/**
 * Applies lot events to the stage summary. Doesn't start with the context: the seeder starts it once the
 * existing lots are loaded, so the events queued meanwhile are applied on top of them.
 */
@Component
public class LotEventListener {

	public static final String ID = "lotEvents";

	private static final Logger log = LoggerFactory.getLogger(LotEventListener.class);

	private final StageSummaryProjector projector;

	public LotEventListener(StageSummaryProjector projector) {
		this.projector = projector;
	}

	@RabbitListener(id = ID, queues = "${app.reporting.queue}", autoStartup = "false")
	public void onLotEvent(LotEventMessage event) {
		if (event.schemaVersion() > LotEventMessage.SCHEMA_VERSION || event.lotId() == null || event.lot() == null) {
			throw new AmqpRejectAndDontRequeueException("Can't apply lot event " + event.eventId()
					+ " (type " + event.type() + ", schema version " + event.schemaVersion() + ")");
		}
		if (!projector.apply(event.lotId(), event.lot(), event.sequence(), event.occurredAt())) {
			log.debug("Skipped lot event {} (sequence {}) for {}: already applied or stale",
					event.eventId(), event.sequence(), event.lotId());
		}
	}
}
