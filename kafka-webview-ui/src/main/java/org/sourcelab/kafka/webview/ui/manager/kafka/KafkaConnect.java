package org.sourcelab.kafka.webview.ui.manager.kafka;

import org.codehaus.jackson.map.ObjectMapper;
import org.sourcelab.kafka.connect.apiclient.Configuration;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;
import org.sourcelab.kafka.connect.apiclient.request.dto.*;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.PluginDetails;

import java.util.*;

public class KafkaConnect {

    private KafkaConnectClient kafkaConnectClient;
    private String connectorHost;

    public KafkaConnect(String connectorHost) {
        this.connectorHost = connectorHost;

        Configuration configuration = new Configuration(connectorHost);
        kafkaConnectClient = new KafkaConnectClient(configuration);
    }

    public Collection<String> getConnectors() {
        return kafkaConnectClient.getConnectors();
    }


    public ConnectorDefinition getConnector(String connectorName) {
        return kafkaConnectClient.getConnector(connectorName);
    }

    public Map<String, String> getConnectorConfig(final String connectorName) {
        return kafkaConnectClient.getConnectorConfig(connectorName);
    }

    public ConnectorStatus getConnectorStatus(final String connectorName) {
        return kafkaConnectClient.getConnectorStatus(connectorName);
    }

    public ConnectorDefinition addConnector(String name, Map<String, String> config) {
        NewConnectorDefinition connectorDefinition = NewConnectorDefinition.newBuilder().withName(name).withConfig(config).build();
        return kafkaConnectClient.addConnector(connectorDefinition);
    }

    public ConnectorDefinition updateConnectorConfig(final String connectorName, final Map<String, String> config) {
        return kafkaConnectClient.updateConnectorConfig(connectorName, config);
    }

    public Boolean restartConnector(final String connectorName) {
        return kafkaConnectClient.restartConnector(connectorName);
    }

    public Boolean pauseConnector(final String connectorName) {
        return kafkaConnectClient.pauseConnector(connectorName);
    }

    public Boolean resumeConnector(final String connectorName) {
        return kafkaConnectClient.resumeConnector(connectorName);
    }

    public Boolean deleteConnector(final String connectorName) {
        return kafkaConnectClient.deleteConnector(connectorName);
    }

    public Collection<Task> getConnectorTasks(final String connectorName) {
        return kafkaConnectClient.getConnectorTasks(connectorName);
    }

    public TaskStatus getConnectorTaskStatus(final String connectorName, final int taskId) {
        return kafkaConnectClient.getConnectorTaskStatus(connectorName, taskId);
    }

    public Boolean restartConnectorTask(final String connectorName, final int taskId) {
        return kafkaConnectClient.restartConnectorTask(connectorName, taskId);
    }

    public List<PluginDetails> getConnectorPlugins() {
        ObjectMapper objectMapper = new ObjectMapper();

        Collection<ConnectorPlugin> connectorPlugins = kafkaConnectClient.getConnectorPlugins();
        Iterator iterator = connectorPlugins.iterator();

        List<PluginDetails> pluginDetails = new ArrayList<>();
        while (iterator.hasNext()) {
            Map<String, String> map = new HashMap<>();

            ConnectorPlugin connectorPlugin = objectMapper.convertValue(iterator.next(), ConnectorPlugin.class);
            map.put("connector.class", connectorPlugin.getClassName());
            map.put("topics", "getPlugins");

            ConnectorPluginConfigDefinition definition = new ConnectorPluginConfigDefinition(connectorPlugin.getClassName(), map);
            ConnectorPluginConfigValidationResults options = kafkaConnectClient.validateConnectorPluginConfig(definition);

            pluginDetails.add(new PluginDetails(connectorPlugin.getClassName(), options));
        }

        return pluginDetails;
    }

}
