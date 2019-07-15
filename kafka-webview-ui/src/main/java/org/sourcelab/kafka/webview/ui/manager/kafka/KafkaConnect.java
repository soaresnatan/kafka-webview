package org.sourcelab.kafka.webview.ui.manager.kafka;

import org.codehaus.jackson.map.ObjectMapper;
import org.sourcelab.kafka.connect.apiclient.Configuration;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;
import org.sourcelab.kafka.connect.apiclient.request.dto.*;
import org.sourcelab.kafka.webview.ui.controller.configuration.connector.forms.ConnectorForm;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.ConnectorInfo;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.PluginDetails;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.PluginErros;
import org.sourcelab.kafka.webview.ui.model.Cluster;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class KafkaConnect {

    private KafkaConnectClient kafkaConnectClient;

    public void connect(String connectorHost) {

        Configuration configuration = new Configuration(connectorHost);
        kafkaConnectClient = new KafkaConnectClient(configuration);
    }

    public Collection<String> getConnectors() {
        return kafkaConnectClient.getConnectors();
    }


    public ConnectorInfo getConnector(Cluster cluster, String connectorName) {

        ConnectorDefinition connectorDefinition = kafkaConnectClient.getConnector(connectorName);
        ConnectorStatus connectorStatus = getConnectorStatus(connectorName);

        return new ConnectorInfo(cluster, connectorDefinition, connectorStatus);
    }

    public ConnectorStatus getConnectorStatus(final String connectorName) {
        return kafkaConnectClient.getConnectorStatus(connectorName);
    }

    public ConnectorForm updateConnector(long clusterId, Map<String, String> config) {
        ConnectorForm connectorForm = getConnectorForm(clusterId, config);

        if (!connectorForm.getErros().haveErrors()) {
            kafkaConnectClient.updateConnectorConfig(connectorForm.getName(), connectorForm.getConfiguration());
        }

        return connectorForm;

    }

    public ConnectorForm addConnector(long clusterId, Map<String, String> config) {

        ConnectorForm connectorForm = getConnectorForm(clusterId, config);

        if (!connectorForm.getErros().haveErrors()) {
            NewConnectorDefinition newConnectorDefinition = NewConnectorDefinition.newBuilder().withName(connectorForm.getName()).withConfig(connectorForm.getConfiguration()).build();
            kafkaConnectClient.addConnector(newConnectorDefinition);
        }

        return connectorForm;
    }

    public List<PluginDetails> getConnectorPlugins() {
        ObjectMapper objectMapper = new ObjectMapper();


        Collection<ConnectorPlugin> connectorPlugins = kafkaConnectClient.getConnectorPlugins();
        Iterator iterator = connectorPlugins.iterator();

        List<PluginDetails> pluginDetails = new ArrayList<>();
        while (iterator.hasNext()) {
            ConnectorPlugin connectorPlugin = objectMapper.convertValue(iterator.next(), ConnectorPlugin.class);

            Map<String, String> map = new HashMap<>();
            map.put("connector.class", connectorPlugin.getClassName());
            map.put("topics", "getPlugins");

            ConnectorPluginConfigValidationResults options = validationConfigPlugin(map);

            String namePlugin = connectorPlugin.getClassName();
            String splitPLugin[] = namePlugin.split(Pattern.quote("."));
            namePlugin = splitPLugin[splitPLugin.length - 1];

            pluginDetails.add(new PluginDetails(namePlugin, connectorPlugin.getClassName(), options));
        }

        return pluginDetails;
    }

    public PluginDetails getPlugin(String className) {
        Map<String, String> pluginConfigDefault = new HashMap<>();
        pluginConfigDefault.put("connector.class", className);

        ConnectorPluginConfigValidationResults options = validationConfigPlugin(pluginConfigDefault);
        return new PluginDetails(className, options);
    }

    private ConnectorForm getConnectorForm(long clusterId, Map<String, String> config) {
        ConnectorPluginConfigValidationResults options = validationConfigPlugin(config);

        Map<String, String> pluginConfig = new HashMap<>();
        for (ConnectorPluginConfigValidationResults.Config configuration : options.getConfigs()) {
            String field = configuration.getDefinition().getName();
            String valueDefault = configuration.getDefinition().getDefaultValue();
            boolean required = configuration.getDefinition().isRequired();

            String valueForm = config.get(field);

            if (!valueForm.isEmpty()) {
                pluginConfig.put(field, valueForm);
            } else {
                if (required)
                    pluginConfig.put(field, valueDefault);
            }
        }

        PluginErros pluginErros = new PluginErros();
        ConnectorPluginConfigValidationResults finalValidation = validationConfigPlugin(pluginConfig);
        for (ConnectorPluginConfigValidationResults.Config configuration : finalValidation.getConfigs()) {

            for (String erro : configuration.getValue().getErrors()) {
                pluginErros.addError(configuration.getDefinition().getDisplayName(), erro);
            }
        }

        ConnectorForm connectorForm = new ConnectorForm(pluginConfig.get("name"), clusterId, pluginConfig.get("connector.class"), options, pluginConfig, pluginErros);
        return connectorForm;
    }

    private ConnectorPluginConfigValidationResults validationConfigPlugin(Map<String, String> config) {
        String classConnector = config.get("connector.class");

        Map<String, String> pluginConfigDefault = new HashMap<>();
        pluginConfigDefault.put("connector.class", classConnector);

        ConnectorPluginConfigDefinition connectorPluginConfigDefinition = new ConnectorPluginConfigDefinition(classConnector, config);

        return kafkaConnectClient.validateConnectorPluginConfig(connectorPluginConfigDefinition);
    }

    public Collection<Task> getConnectorTasks(final String connectorName) {
        return kafkaConnectClient.getConnectorTasks(connectorName);
    }

    public Boolean restartConnectorTask(final String connectorName, final int taskId) {
        return kafkaConnectClient.restartConnectorTask(connectorName, taskId);
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


    public ConnectorDefinition updateConnectorConfig(final String connectorName, final Map<String, String> config) {
        return kafkaConnectClient.updateConnectorConfig(connectorName, config);
    }

    public TaskStatus getConnectorTaskStatus(final String connectorName, final int taskId) {
        return kafkaConnectClient.getConnectorTaskStatus(connectorName, taskId);
    }

    public Map<String, String> getConnectorConfig(final String connectorName) {
        return kafkaConnectClient.getConnectorConfig(connectorName);
    }

}
