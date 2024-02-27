package se.sundsvall.rabbitmqconsumer.service;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import se.sundsvall.rabbitmqconsumer.integration.publisher.PublisherIntegration;
import se.sundsvall.rabbitmqconsumer.integration.publisher.model.Order;

@Service
public class ProcessService {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessService.class);
	public static final String POC_EXCHANGE = "poc-exchange";
	public static final String POC_DLX = "poc-dlx";
	public static final String NEW_ORDER_RETRY_ROUTING_KEY = "RK_newOrderQueue.retry";
	public static final String NEW_ORDER_DEAD_LETTER_ROUTING_KEY = "RK_newOrderQueue.dlq";
	public static final String CANCEL_ORDER_RETRY_ROUTING_KEY = "RK_cancelOrderQueue.retry";
	public static final String CANCEL_ORDER_DEAD_LETTER_ROUTING_KEY = "RK_cancelOrderQueue.dlq";

	private final PublisherIntegration publisherIntegration;
	private final ObjectMapper objectMapper;

	private final RabbitTemplate rabbitTemplate;

	@Value("${retry.count}")
	private Integer retryCount;

	public ProcessService(final PublisherIntegration publisherIntegration,
		final ObjectMapper objectMapper, final RabbitTemplate rabbitTemplate) {
		this.publisherIntegration = publisherIntegration;
		this.objectMapper = objectMapper;
		this.rabbitTemplate = rabbitTemplate;
	}

	@RabbitListener(queues = "${integration.rabbitmq.new-order-queue}")
	public void processNewOrder(final Message message) {
		var retries = Optional.ofNullable((Integer) message.getMessageProperties().getHeaders().get("x-retires-count")).orElse(1);
		LOG.info("Process started, this is the {} try to process this message", retries);

		try {
			var order = objectMapper.readValue(message.getBody(), Order.class);
			// Might call some external service to start the order process here
			order.setStatus("PROCESSED");
			publisherIntegration.updateOrder(order.getId(), order);
			LOG.info("Successfully processed order");
		} catch (Exception e) {
			LOG.error("Error while processing order", e);
			message.getMessageProperties().getHeaders().put("x-retires-count", ++retries);
			if (retries > retryCount) {
				LOG.info("Error occured for the {} time, sending message to dead letter queue", retries - 1);
				rabbitTemplate.send(POC_DLX, NEW_ORDER_DEAD_LETTER_ROUTING_KEY, message);
			} else {
				LOG.info("Error occured for the {} time, sending message to retry queue", retries - 1);
				rabbitTemplate.send(POC_EXCHANGE, NEW_ORDER_RETRY_ROUTING_KEY, message);
			}
		}
	}

	@RabbitListener(queues = "${integration.rabbitmq.cancel-order-queue}")
	public void processCancelOrder(final Message message) {
		var retries = Optional.ofNullable((Integer) message.getMessageProperties().getHeaders().get("x-retires-count")).orElse(1);
		LOG.info("Process started, this is the {} try to process this message", retries);

		try {
			var order = objectMapper.readValue(message.getBody(), Order.class);
			// Might call some external service to cancel the order here
			order.setStatus("CANCELLED");
			publisherIntegration.updateOrder(order.getId(), order);
			LOG.info("Successfully processed order");
		} catch (Exception e) {
			LOG.error("Error while processing order", e);
			message.getMessageProperties().getHeaders().put("x-retires-count", ++retries);
			if (retries > retryCount) {
				LOG.info("Error occured for the {} time, sending message to dead letter queue", retries - 1);
				rabbitTemplate.send(POC_DLX, CANCEL_ORDER_DEAD_LETTER_ROUTING_KEY, message);
			} else {
				LOG.info("Error occured for the {} time, sending message to retry queue", retries - 1);
				rabbitTemplate.send(POC_EXCHANGE, CANCEL_ORDER_RETRY_ROUTING_KEY, message);
			}
		}
	}
}
