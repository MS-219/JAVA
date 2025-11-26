package com.yinlian.service;

import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class MqttService implements MqttCallback {
    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);

    @Value("${mqtt.host}")
    private String host;

    @Value("${mqtt.clientId}")
    private String clientId;

    private MqttClient client;

    @PostConstruct
    public void init() {
        if (host == null || host.isEmpty() || host.contains("your-mqtt-broker")) {
            logger.warn("MQTT host not configured, skipping connection.");
            return;
        }
        connect();
    }

    public void connect() {
        try {
            client = new MqttClient(host, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            // options.setUserName(...);
            // options.setPassword(...);
            options.setAutomaticReconnect(true);
            
            client.connect(options);
            client.setCallback(this);
            logger.info("MQTT connected");
            
            // client.subscribe("your/topic");
        } catch (MqttException e) {
            logger.error("MQTT connect failed", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("MQTT connection lost", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        logger.info("MQTT msg received: {}", new String(message.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    public boolean publish(String topic, String payload) {
        if (client == null || !client.isConnected()) {
            logger.warn("MQTT client not connected, skip publish");
            return false;
        }
        try {
            client.publish(topic, payload.getBytes(), 1, false);
            logger.info("MQTT publish topic={}, bytes={}", topic, payload.length());
            return true;
        } catch (MqttException e) {
            logger.error("MQTT publish failed", e);
            return false;
        }
    }
}
