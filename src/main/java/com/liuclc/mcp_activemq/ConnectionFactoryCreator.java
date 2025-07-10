package com.liuclc.mcp_activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.stereotype.Service;

@Service
public class ConnectionFactoryCreator {
    public PooledConnectionFactory createPooledConnectionFactory(ActiveMQConnectionFactory connectionFactory) {
        try {
            final PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
            pooledConnectionFactory.setConnectionFactory(connectionFactory);
            pooledConnectionFactory.setMaxConnections(10);
            return pooledConnectionFactory;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PooledConnectionFactory", e);
        }
    }

    public ActiveMQConnectionFactory createActiveMQConnectionFactory(
            String activemqEndpoint,
            String activemqUsername,
            String activemqPassword) {
        try {
            final ActiveMQConnectionFactory connectionFactory =
                    new ActiveMQConnectionFactory(activemqEndpoint);
            connectionFactory.setUserName(activemqUsername);
            connectionFactory.setPassword(activemqPassword);
            return connectionFactory;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ActiveMQConnectionFactory", e);
        }
    }
}
