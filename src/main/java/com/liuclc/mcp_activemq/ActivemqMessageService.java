package com.liuclc.mcp_activemq;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class ActivemqMessageService {
    private final ConnectionFactoryCreator factoryCreator;

    @Autowired
    public ActivemqMessageService(ConnectionFactoryCreator factoryCreator) {
        this.factoryCreator = factoryCreator;
    }

    @Tool(name = "inspectQueue", description = "Inspect the status of an ActiveMQ queue using Jolokia REST API, user needs to disable the CORS of Jolokia to make this work.")
    public String inspectQueue(String jolokiaEndpoint, String username, String password, String queueName) {
        if (!isValidJolokiaEndpoint(jolokiaEndpoint)) {
            throw new IllegalArgumentException("Invalid Jolokia endpoint URL");
        }

        if (!isValidName(queueName)) {
            throw new IllegalArgumentException("Invalid queue name");
        }

        RestTemplate restTemplate = createRestTemplate();

        HttpHeaders headers = new HttpHeaders();

        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder()
                                   .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;
        headers.set("Authorization", authHeader);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String sanitizedEndpoint = sanitizeEndpoint(jolokiaEndpoint);
        String sanitizedQueueName = sanitizeName(queueName);

        String url = String.format("%s/read/org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=%s",
                                   sanitizedEndpoint, sanitizedQueueName);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return response.getBody();
        } catch (Exception e) {
            return "Error inspecting queue: " + e.getMessage();
        }
    }

    @Tool(name = "inspectTopic", description = "Inspect the status of an ActiveMQ topic using Jolokia REST API, user needs to disable the CORS of Jolokia to make this work.")
    public String inspectTopic(String jolokiaEndpoint, String username, String password, String topicName) {
        if (!isValidJolokiaEndpoint(jolokiaEndpoint)) {
            throw new IllegalArgumentException("Invalid Jolokia endpoint URL");
        }

        if (!isValidName(topicName)) {
            throw new IllegalArgumentException("Invalid queue name");
        }

        RestTemplate restTemplate = createRestTemplate();

        HttpHeaders headers = new HttpHeaders();

        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder()
                                   .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;
        headers.set("Authorization", authHeader);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String sanitizedEndpoint = sanitizeEndpoint(jolokiaEndpoint);
        String sanitizedTopicName = sanitizeName(topicName);

        String url = String.format("%s/read/org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Topic,destinationName=%s",
                                   sanitizedEndpoint, sanitizedTopicName);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return response.getBody();
        } catch (Exception e) {
            return "Error inspecting topic: " + e.getMessage();
        }
    }

    @Tool(name="sendMessageToQueue", description="Send a message to a queue of an ActiveMQ message broker")
    public void sendMessageToQueue(String activemqEndpoint,String activemqUsername, String activemqPassword, String queueName, String messageBody) throws JMSException {
        ActiveMQConnectionFactory connectionFactory = factoryCreator.createActiveMQConnectionFactory(activemqEndpoint, activemqUsername, activemqPassword);

        try (Connection producerConnection = connectionFactory.createConnection();
             Session producerSession = producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             MessageProducer producer = producerSession.createProducer(producerSession.createQueue(queueName))) {

            producerConnection.start();
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            TextMessage producerMessage = producerSession.createTextMessage(messageBody);
            producer.send(producerMessage);
        }
    }

    @Tool(name="sendMessageToTopic", description="Send a message to a topic of an ActiveMQ message broker")
    public void sendMessageToTopic(String activemqEndpoint,String activemqUsername, String activemqPassword, String topicName, String messageBody) throws JMSException {
        ActiveMQConnectionFactory connectionFactory = factoryCreator.createActiveMQConnectionFactory(activemqEndpoint, activemqUsername, activemqPassword);

        try (Connection producerConnection = connectionFactory.createConnection();
             Session producerSession = producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             MessageProducer producer = producerSession.createProducer(producerSession.createTopic(topicName));){
            producerConnection.start();
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            TextMessage producerMessage = producerSession.createTextMessage(messageBody);
            producer.send(producerMessage);
        }
    }

    protected RestTemplate createRestTemplate(){
        return new RestTemplate();
    }

    private boolean isValidJolokiaEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            return false;
        }

        try {
            URL url = new URL(endpoint);

            if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
                return false;
            }

            String host = url.getHost();

            if (!isAllowedHost(host)) {
                return false;
            }

            int port = url.getPort();
            if (port != -1 && (port < 1024 || port > 65535)) {
                return false;
            }

            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private boolean isAllowedHost(String host) {
        List<String> allowedHosts = Arrays.asList(
                "localhost",
                "127.0.0.1"
        );

        return allowedHosts.contains(host.toLowerCase());
    }

    private boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        return name.matches("^[a-zA-Z0-9._-]+$");
    }

    private String sanitizeEndpoint(String endpoint) {
        return endpoint.replaceAll("/+$", "");
    }

    private String sanitizeName(String name) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8);
    }
}
