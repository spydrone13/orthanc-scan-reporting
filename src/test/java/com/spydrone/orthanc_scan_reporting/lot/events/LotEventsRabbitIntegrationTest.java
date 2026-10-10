package com.spydrone.orthanc_scan_reporting.lot.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.spydrone.orthanc_scan_reporting.seed.ConsumerLotsClient;
import com.spydrone.orthanc_scan_reporting.seed.ConsumerLotsClient.ConsumerLot;
import com.spydrone.orthanc_scan_reporting.summary.StageSummary;
import com.spydrone.orthanc_scan_reporting.summary.StageSummaryRepository;

/**
 * Seeding and lot events against a real RabbitMQ: the queue binding, JSON published the way the consumer's
 * outbox relay publishes it (no type header), and the resulting counts. Skipped without Docker.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:rabbit-it")
@Testcontainers(disabledWithoutDocker = true)
class LotEventsRabbitIntegrationTest {

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4-management");

	/** Stands in for the consumer's /api/lots, which the seeder reads while the context starts. */
	@TestConfiguration
	static class SeededLots {

		@Bean
		@Primary
		ConsumerLotsClient seededLots() {
			return new ConsumerLotsClient("http://unused") {
				@Override
				public List<ConsumerLot> fetchLots() {
					return List.of(
							new ConsumerLot("IT-1", "intake", "INTAKE-001", LotStatus.ACTIVE, false),
							new ConsumerLot("IT-2", "intake", "INTAKE-001", LotStatus.ACTIVE, false));
				}
			};
		}
	}

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private StageSummaryRepository summaries;

	@Test
	void appliesPublishedLotEventsOnTopOfTheSeed() {
		publish("lot.moved", """
				{"eventId":"e-1","type":"lot.moved","schemaVersion":1,"sequence":10,"occurredAt":"2026-10-10T12:00:00Z",
				 "lotId":"IT-1","lot":{"currentStage":"wafer-prep","wipLocation":"WAFER-PREP-001","status":"active","onHold":false},
				 "from":{"stage":"intake","wipLocation":"INTAKE-001"},"to":{"stage":"wafer-prep","wipLocation":"WAFER-PREP-001"}}
				""");
		publish("lot.status-changed", """
				{"eventId":"e-2","type":"lot.status-changed","schemaVersion":1,"sequence":11,"occurredAt":"2026-10-10T12:01:00Z",
				 "lotId":"IT-2","lot":{"currentStage":"intake","wipLocation":"INTAKE-001","status":"canceled","onHold":false},
				 "previousStatus":"active"}
				""");

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(summaries.findAllByOrderByStageAsc())
				.extracting(StageSummary::getStage, StageSummary::getActiveLots)
				.containsExactly(tuple("wafer-prep", 1L)));
	}

	private void publish(String type, String json) {
		MessageProperties properties = new MessageProperties();
		properties.setType(type);
		properties.setContentType("application/json");
		properties.setContentEncoding("UTF-8");
		rabbitTemplate.send("orthanc.lots", type, new Message(json.getBytes(StandardCharsets.UTF_8), properties));
	}
}
