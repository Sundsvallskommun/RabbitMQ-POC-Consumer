package se.sundsvall.rabbitmqconsumer.service;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.rabbitmqconsumer.service.ProcessService.CANCEL_ORDER_DEAD_LETTER_ROUTING_KEY;
import static se.sundsvall.rabbitmqconsumer.service.ProcessService.CANCEL_ORDER_RETRY_ROUTING_KEY;
import static se.sundsvall.rabbitmqconsumer.service.ProcessService.NEW_ORDER_DEAD_LETTER_ROUTING_KEY;
import static se.sundsvall.rabbitmqconsumer.service.ProcessService.NEW_ORDER_RETRY_ROUTING_KEY;
import static se.sundsvall.rabbitmqconsumer.service.ProcessService.POC_DLX;
import static se.sundsvall.rabbitmqconsumer.service.ProcessService.POC_EXCHANGE;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import se.sundsvall.rabbitmqconsumer.integration.publisher.PublisherIntegration;
import se.sundsvall.rabbitmqconsumer.integration.publisher.model.Order;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTests {

	@Mock
	private RabbitTemplate rabbitTemplateMock;

	@Mock
	private PublisherIntegration publisherIntegrationMock;

	@Mock
	private final ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	private ProcessService processService;

	@BeforeEach
	void setup() {
		ReflectionTestUtils.setField(processService, "retryCount", 3);
	}

	@Test
	void testProcessNewOrder_WhenOKTest() throws IOException {
		var message = createMessage();
		doNothing().when(publisherIntegrationMock).updateOrder(anyString(), any());
		when(objectMapper.readValue(message.getBody(), Order.class)).thenReturn(createOrder());

		processService.processNewOrder(message);

		verify(objectMapper).readValue(message.getBody(), Order.class);
		verify(publisherIntegrationMock).updateOrder(anyString(), any());
		verifyNoInteractions(rabbitTemplateMock);
	}

	@Test
	void testProcessNewOrder_WhenError_SendToRetryQueueTest() throws IOException {
		var message = createMessage();
		doNothing().when(rabbitTemplateMock).send(POC_EXCHANGE, NEW_ORDER_RETRY_ROUTING_KEY, message);
		when(objectMapper.readValue(message.getBody(), Order.class)).thenThrow(new RuntimeException());

		processService.processNewOrder(message);

		verify(objectMapper).readValue(message.getBody(), Order.class);
		verify(rabbitTemplateMock).send(POC_EXCHANGE, NEW_ORDER_RETRY_ROUTING_KEY, message);
		verifyNoInteractions(publisherIntegrationMock);
	}

	@Test
	void testProcessNewOrder_WhenError_SendToDeadLetterQueueTest() throws IOException {
		var message = createMessage();
		message.getMessageProperties().getHeaders().put("x-retires-count", 3);
		doNothing().when(rabbitTemplateMock).send(POC_DLX, NEW_ORDER_DEAD_LETTER_ROUTING_KEY, message);
		when(objectMapper.readValue(message.getBody(), Order.class)).thenThrow(new RuntimeException());

		processService.processNewOrder(message);

		verify(objectMapper).readValue(message.getBody(), Order.class);
		verify(rabbitTemplateMock).send(POC_DLX, NEW_ORDER_DEAD_LETTER_ROUTING_KEY, message);
		verifyNoInteractions(publisherIntegrationMock);
	}

	@Test
	void testProcessCancelOrder_WhenOKTest() throws IOException {
		var message = createMessage();
		doNothing().when(publisherIntegrationMock).updateOrder(anyString(), any());
		when(objectMapper.readValue(message.getBody(), Order.class)).thenReturn(createOrder());

		processService.processCancelOrder(message);

		verify(objectMapper).readValue(message.getBody(), Order.class);
		verify(publisherIntegrationMock).updateOrder(anyString(), any());
		verifyNoInteractions(rabbitTemplateMock);
	}

	@Test
	void testProcessCancelOrder_WhenError_SendToRetryQueueTest() throws IOException {
		var message = createMessage();
		doNothing().when(rabbitTemplateMock).send(POC_EXCHANGE, CANCEL_ORDER_RETRY_ROUTING_KEY, message);
		when(objectMapper.readValue(message.getBody(), Order.class)).thenThrow(new RuntimeException());

		processService.processCancelOrder(message);

		verify(objectMapper).readValue(message.getBody(), Order.class);
		verify(rabbitTemplateMock).send(POC_EXCHANGE, CANCEL_ORDER_RETRY_ROUTING_KEY, message);
	}

	@Test
	void testProcessCancelOrder_WhenError_SendToDeadLetterQueueTest() throws IOException {
		var message = createMessage();
		message.getMessageProperties().getHeaders().put("x-retires-count", 3);
		doNothing().when(rabbitTemplateMock).send(POC_DLX, CANCEL_ORDER_DEAD_LETTER_ROUTING_KEY, message);
		when(objectMapper.readValue(message.getBody(), Order.class)).thenThrow(new RuntimeException());

		processService.processCancelOrder(message);

		verify(objectMapper).readValue(message.getBody(), Order.class);
		verify(rabbitTemplateMock).send(POC_DLX, CANCEL_ORDER_DEAD_LETTER_ROUTING_KEY, message);
		verifyNoInteractions(publisherIntegrationMock);
	}


	private Message createMessage() throws JsonProcessingException {
		return new Message(createMessageBody(), createMessageProperties());
	}

	private byte[] createMessageBody() throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();

		return objectMapper.writeValueAsBytes(createOrder());
	}

	private Order createOrder() {
		return Order.builder()
			.withId("testId")
			.withTest("testName")
			.withTest2("testName2")
			.withStatus("testStatus")
			.build();
	}

	private MessageProperties createMessageProperties() {
		var properties = new MessageProperties();
		properties.setReceivedExchange("testExchange");
		properties.setConsumerQueue("testQueue");
		properties.setReceivedRoutingKey("testRoutingKey");
		properties.setConsumerTag("amq_ctag-test");
		properties.setDeliveryTag(1);
		properties.setContentLength(0);
		properties.setContentType("application/octet-stream");
		properties.setHeaders(emptyMap());
		properties.setRedelivered(false);
		return properties;
	}
}
