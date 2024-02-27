package se.sundsvall.rabbitmqconsumer.integration.publisher;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import se.sundsvall.rabbitmqconsumer.integration.publisher.model.Order;

@FeignClient(
	name = "publisher",
	url = "${integration.publisher.url}"
)
public interface PublisherClient {

	@PatchMapping(value = "/order/update/{orderId}")
	void updateOrder(@PathVariable("orderId") final String orderId, @RequestBody final Order order);

}
