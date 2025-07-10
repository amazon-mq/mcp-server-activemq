package com.liuclc.mcp_activemq;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ActivemqMessageServiceTest {

    @Mock
    private ConnectionFactoryCreator factoryCreator;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ActiveMQConnectionFactory connectionFactory;

    @Mock
    private PooledConnectionFactory pooledConnectionFactory;

    @Mock
    private Connection connection;

    @Mock
    private Session session;

    @Mock
    private Queue queue;

    @Mock
    private Topic topic;

    @Mock
    private MessageProducer producer;

    @Mock
    private TextMessage textMessage;

    @InjectMocks
    private ActivemqMessageService service;

    @BeforeEach
    void setUp() throws JMSException {
        // Common setup for connection factory
        when(factoryCreator.createActiveMQConnectionFactory(anyString(), anyString(), anyString()))
                .thenReturn(connectionFactory);
        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        when(session.createProducer(any(Destination.class))).thenReturn(producer);
        when(session.createTextMessage(anyString())).thenReturn(textMessage);
    }

    @Test
    void testInspectQueue_Success() {
        String jolokiaEndpoint = "http://localhost:8161/api/jolokia";
        String username = "admin";
        String password = "admin";
        String queueName = "testQueue";
        String expectedResponse = "{\"status\":200,\"value\":{\"QueueSize\":5}}";

        ActivemqMessageService serviceSpy = spy(service);
        doReturn(restTemplate).when(serviceSpy).createRestTemplate();

        ResponseEntity<String> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        String result = serviceSpy.inspectQueue(jolokiaEndpoint, username, password, queueName);

        assertEquals(expectedResponse, result);
        verify(restTemplate).exchange(
                contains(jolokiaEndpoint + "/read/org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=" + queueName),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void whenJolokiaEndpointIsNull_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectQueue(null, "user", "pass", "queue")
        );
        assertEquals("Invalid Jolokia endpoint URL", exception.getMessage());
    }

    @Test
    void whenJolokiaEndpointIsEmpty_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectQueue("", "user", "pass", "queue")
        );
        assertEquals("Invalid Jolokia endpoint URL", exception.getMessage());
    }

    @Test
    void whenJolokiaEndpointHasInvalidProtocol_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectQueue("ftp://localhost:8161", "user", "pass", "queue")
        );
        assertEquals("Invalid Jolokia endpoint URL", exception.getMessage());
    }

    @Test
    void whenJolokiaEndpointHasInvalidHost_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectQueue("http://unauthorized.host:8161", "user", "pass", "queue")
        );
        assertEquals("Invalid Jolokia endpoint URL", exception.getMessage());
    }

    @Test
    void whenJolokiaEndpointHasInvalidPort_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectQueue("http://localhost:80000", "user", "pass", "queue")
        );
        assertEquals("Invalid Jolokia endpoint URL", exception.getMessage());
    }

    @Test
    void whenQueueNameIsNull_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectQueue("http://localhost:8161", "user", "pass", null)
        );
        assertEquals("Invalid queue name", exception.getMessage());
    }

    @Test
    void whenQueueNameIsEmpty_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectQueue("http://localhost:8161", "user", "pass", "")
        );
        assertEquals("Invalid queue name", exception.getMessage());
    }

    @Test
    void whenQueueNameHasInvalidCharacters_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectQueue("http://localhost:8161", "user", "pass", "queue/with/slashes")
        );
        assertEquals("Invalid queue name", exception.getMessage());
    }

    @Test
    void testInspectQueue_Exception() {
        String jolokiaEndpoint = "http://localhost:8161/api/jolokia";
        String username = "admin";
        String password = "admin";
        String queueName = "testQueue";
        String errorMessage = "Connection refused";

        ActivemqMessageService serviceSpy = spy(service);
        doReturn(restTemplate).when(serviceSpy).createRestTemplate();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException(errorMessage));

        String result = serviceSpy.inspectQueue(jolokiaEndpoint, username, password, queueName);


        assertEquals("Error inspecting queue: " + errorMessage, result);
    }

    @Test
    void testInspectTopic_Success() {
        String jolokiaEndpoint = "http://localhost:8161/api/jolokia";
        String username = "admin";
        String password = "admin";
        String topicName = "testTopic";
        String expectedResponse = "{\"status\":200,\"value\":{\"TopicSize\":5}}";

        ActivemqMessageService serviceSpy = spy(service);
        doReturn(restTemplate).when(serviceSpy).createRestTemplate();

        ResponseEntity<String> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        String result = serviceSpy.inspectTopic(jolokiaEndpoint, username, password, topicName);

        assertEquals(expectedResponse, result);
        verify(restTemplate).exchange(
                contains(jolokiaEndpoint + "/read/org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Topic,destinationName=" + topicName),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void whenJolokiaEndpointIsNullForTopic_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectTopic(null, "user", "pass", "topic")
        );
        assertEquals("Invalid Jolokia endpoint URL", exception.getMessage());
    }

    @Test
    void whenJolokiaEndpointIsEmptyForTopic_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectTopic("", "user", "pass", "topic")
        );
        assertEquals("Invalid Jolokia endpoint URL", exception.getMessage());
    }

    @Test
    void whenJolokiaEndpointHasInvalidProtocolForTopic_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectTopic("ftp://localhost:8161", "user", "pass", "topic")
        );
        assertEquals("Invalid Jolokia endpoint URL", exception.getMessage());
    }

    @Test
    void whenJolokiaEndpointHasInvalidHostForTopic_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectTopic("http://unauthorized.host:8161", "user", "pass", "topic")
        );
        assertEquals("Invalid Jolokia endpoint URL", exception.getMessage());
    }

    @Test
    void whenJolokiaEndpointHasInvalidPortForTopic_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectTopic("http://localhost:80000", "user", "pass", "topic")
        );
        assertEquals("Invalid Jolokia endpoint URL", exception.getMessage());
    }

    @Test
    void whenTopicNameIsNull_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectTopic("http://localhost:8161", "user", "pass", null)
        );
        assertEquals("Invalid queue name", exception.getMessage());
    }

    @Test
    void whenTopicNameIsEmpty_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectTopic("http://localhost:8161", "user", "pass", "")
        );
        assertEquals("Invalid queue name", exception.getMessage());
    }

    @Test
    void whenTopicNameHasInvalidCharacters_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.inspectTopic("http://localhost:8161", "user", "pass", "topic/with/slashes")
        );
        assertEquals("Invalid queue name", exception.getMessage());
    }

    @Test
    void testInspectTopic_Exception() {
        String jolokiaEndpoint = "http://localhost:8161/api/jolokia";
        String username = "admin";
        String password = "admin";
        String topicName = "testTopic";
        String errorMessage = "Connection refused";

        ActivemqMessageService serviceSpy = spy(service);
        doReturn(restTemplate).when(serviceSpy).createRestTemplate();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException(errorMessage));

        String result = serviceSpy.inspectTopic(jolokiaEndpoint, username, password, topicName);


        assertEquals("Error inspecting topic: " + errorMessage, result);
    }

    @Test
    void testSendMessageToQueue() throws JMSException {
        // Arrange
        String activemqEndpoint = "tcp://localhost:61616";
        String activemqUsername = "admin";
        String activemqPassword = "admin";
        String queueName = "testQueue";
        String messageBody = "Test message";

        when(session.createQueue(queueName)).thenReturn(queue);

        service.sendMessageToQueue(activemqEndpoint, activemqUsername, activemqPassword, queueName, messageBody);

        verify(factoryCreator).createActiveMQConnectionFactory(activemqEndpoint, activemqUsername, activemqPassword);
        verify(connectionFactory).createConnection();
        verify(connection).start();
        verify(session).createQueue(queueName);
        verify(session).createProducer(queue);
        verify(producer).setDeliveryMode(DeliveryMode.PERSISTENT);
        verify(session).createTextMessage(messageBody);
        verify(producer).send(textMessage);
        verify(producer).close();
        verify(session).close();
        verify(connection).close();
    }

    @Test
    void testSendMessageToQueue_Exception() throws JMSException {
        String activemqEndpoint = "tcp://localhost:61616";
        String activemqUsername = "admin";
        String activemqPassword = "admin";
        String queueName = "testQueue";
        String messageBody = "Test message";

        when(session.createQueue(queueName)).thenReturn(queue);
        doThrow(new JMSException("Connection error")).when(connection).start();

        assertThrows(JMSException.class, () ->
                                                 service.sendMessageToQueue(activemqEndpoint, activemqUsername, activemqPassword, queueName, messageBody)
        );
    }

    @Test
    void testSendMessageToTopic() throws JMSException {
        String activemqEndpoint = "tcp://localhost:61616";
        String activemqUsername = "admin";
        String activemqPassword = "admin";
        String topicName = "testTopic";
        String messageBody = "Test message";

        when(session.createTopic(topicName)).thenReturn(topic);

        service.sendMessageToTopic(activemqEndpoint, activemqUsername, activemqPassword, topicName, messageBody);

        verify(factoryCreator).createActiveMQConnectionFactory(activemqEndpoint, activemqUsername, activemqPassword);
        verify(connectionFactory).createConnection();
        verify(connection).start();
        verify(session).createTopic(topicName);
        verify(session).createProducer(topic);
        verify(producer).setDeliveryMode(DeliveryMode.PERSISTENT);
        verify(session).createTextMessage(messageBody);
        verify(producer).send(textMessage);
        verify(producer).close();
        verify(session).close();
        verify(connection).close();
    }

    @Test
    void testSendMessageToTopic_Exception() throws JMSException {
        String activemqEndpoint = "tcp://localhost:61616";
        String activemqUsername = "admin";
        String activemqPassword = "admin";
        String topicName = "testTopic";
        String messageBody = "Test message";

        when(session.createTopic(topicName)).thenReturn(topic);
        doThrow(new JMSException("Connection error")).when(producer).send(any(TextMessage.class));

        assertThrows(JMSException.class, () ->
                                                 service.sendMessageToTopic(activemqEndpoint, activemqUsername, activemqPassword, topicName, messageBody)
        );
    }

    @Test
    void whenCreateTextMessageThrows_thenResourcesAreClosed() throws JMSException {
        String endpoint = "endpoint";
        String username = "username";
        String password = "password";
        String queueName = "testQueue";
        String messageBody = "test message";

        when(session.createQueue(queueName)).thenReturn(queue);
        when(session.createProducer(queue)).thenReturn(producer);

        when(session.createTextMessage(messageBody))
                .thenThrow(new JMSException("Test exception"));

        try {
            service.sendMessageToQueue(endpoint, username, password, queueName, messageBody);
        } catch (JMSException e) {
        }

        verify(producer).close();
        verify(session).close();
        verify(connection).close();
    }

    @Test
    void whenCreateTextMessageSucceeds_thenResourcesAreClosed() throws JMSException {
        String endpoint = "endpoint";
        String username = "username";
        String password = "password";
        String queueName = "testQueue";
        String messageBody = "test message";

        when(session.createQueue(queueName)).thenReturn(queue);
        when(session.createProducer(queue)).thenReturn(producer);

        TextMessage textMessage = mock(TextMessage.class);
        when(session.createTextMessage(messageBody)).thenReturn(textMessage);

        service.sendMessageToQueue(endpoint, username, password, queueName, messageBody);

        verify(producer).close();
        verify(session).close();
        verify(connection).close();
        verify(producer).send(textMessage);
    }

    @Test
    void whenCreateTextMessageThrowsForTopic_thenResourcesAreClosed() throws JMSException {
        String endpoint = "endpoint";
        String username = "username";
        String password = "password";
        String topicName = "testTopic";
        String messageBody = "test message";

        when(session.createTopic(topicName)).thenReturn(topic);
        when(session.createProducer(topic)).thenReturn(producer);

        when(session.createTextMessage(messageBody))
                .thenThrow(new JMSException("Test exception"));

        try {
            service.sendMessageToTopic(endpoint, username, password, topicName, messageBody);
        } catch (JMSException e) {
        }

        verify(producer).close();
        verify(session).close();
        verify(connection).close();
    }

    @Test
    void whenCreateTextMessageSucceedsForTopic_thenResourcesAreClosed() throws JMSException {
        String endpoint = "endpoint";
        String username = "username";
        String password = "password";
        String topicName = "testTopic";
        String messageBody = "test message";

        when(session.createTopic(topicName)).thenReturn(topic);
        when(session.createProducer(topic)).thenReturn(producer);

        TextMessage textMessage = mock(TextMessage.class);
        when(session.createTextMessage(messageBody)).thenReturn(textMessage);

        service.sendMessageToTopic(endpoint, username, password, topicName, messageBody);

        verify(producer).close();
        verify(session).close();
        verify(connection).close();
        verify(producer).send(textMessage);
    }
}