package se.sundsvall.rabbitmqconsumer.integration.publisher;

import org.springframework.stereotype.Component;

import se.sundsvall.rabbitmqconsumer.integration.publisher.model.Order;

@Component
public class PublisherIntegration {

	private final PublisherClient publisherClient;

	public PublisherIntegration(final PublisherClient publisherClient) {
		this.publisherClient = publisherClient;
	}

	public void updateOrder(final String orderId, final Order order) {
		publisherClient.updateOrder(orderId, order);
	}

}
