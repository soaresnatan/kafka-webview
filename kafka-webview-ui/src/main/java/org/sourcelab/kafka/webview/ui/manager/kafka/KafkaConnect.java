package org.sourcelab.kafka.webview.ui.manager.kafka;

import org.codehaus.jackson.map.ObjectMapper;
import org.sourcelab.kafka.connect.apiclient.Configuration;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;
import org.sourcelab.kafka.connect.apiclient.request.dto.*;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.PluginDetails;

import java.util.*;
import java.util.regex.Pattern;

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

    public Boolean addConnector(String name, Map<String, String> config) {
        NewConnectorDefinition newConnectorDefinition = NewConnectorDefinition.newBuilder().withName(name).withConfig(config).build();

        ConnectorPluginConfigValidationResults result = validatePlugin(name, config);

        if (result.getErrorCount() > 0)
            return false;

        kafkaConnectClient.addConnector(newConnectorDefinition);
        return true;
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

            ConnectorPluginConfigValidationResults options = validatePlugin(connectorPlugin.getClassName(), map);

            String namePlugin = connectorPlugin.getClassName();
            String splitPLugin[] = namePlugin.split(Pattern.quote("."));
            namePlugin = splitPLugin[splitPLugin.length - 1];

            pluginDetails.add(new PluginDetails(namePlugin, connectorPlugin.getClassName(), options));
        }

        return pluginDetails;
    }

    public ConnectorPluginConfigValidationResults validatePlugin(String name, Map<String, String> map) {
        ConnectorPluginConfigDefinition definition = new ConnectorPluginConfigDefinition(name, map);

        return kafkaConnectClient.validateConnectorPluginConfig(definition);
    }

}
