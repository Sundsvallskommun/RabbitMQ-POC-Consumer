package se.sundsvall.rabbitmqconsumer.integration.publisher.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class Order {

	private String id;
	private String test;
	private String test2;
	private String status;
}
