package com.spydrone.orthanc_scan_reporting.lot.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The consumer declares only the orthanc.lots exchange; each subscriber owns its queue. Ours is durable
 * so events published while this app is down wait for it. Events that can't be applied are dead-lettered
 * through the default exchange straight to the dead-letter queue.
 */
@Configuration
public class LotEventsRabbitConfig {

	@Bean
	TopicExchange lotsExchange(@Value("${app.lots.exchange}") String name) {
		return new TopicExchange(name);
	}

	/**
	 * RabbitMQ won't change an existing queue's arguments: a queue declared with different ones must be
	 * deleted so it can be redeclared.
	 */
	@Bean
	Queue lotEventsQueue(@Value("${app.reporting.queue}") String name,
			@Value("${app.reporting.dead-letter-queue}") String deadLetterQueue) {
		return QueueBuilder.durable(name)
				.deadLetterExchange("")
				.deadLetterRoutingKey(deadLetterQueue)
				.build();
	}

	@Bean
	Binding lotEventsBinding(Queue lotEventsQueue, TopicExchange lotsExchange) {
		return BindingBuilder.bind(lotEventsQueue).to(lotsExchange).with("lot.#");
	}

	@Bean
	Queue lotEventsDeadLetterQueue(@Value("${app.reporting.dead-letter-queue}") String name) {
		return QueueBuilder.durable(name).build();
	}

	/** Events carry no type header, so the target type is inferred from the listener's parameter. */
	@Bean
	MessageConverter jsonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}
}
