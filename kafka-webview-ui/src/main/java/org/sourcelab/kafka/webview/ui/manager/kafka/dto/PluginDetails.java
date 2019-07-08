package org.sourcelab.kafka.webview.ui.manager.kafka.dto;

import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigValidationResults;

public class PluginDetails {
    private String id;
    private String name;
    private ConnectorPluginConfigValidationResults configValidationResults;

    public PluginDetails(String id, String name, ConnectorPluginConfigValidationResults configValidationResults) {
        this.id = id;
        this.name = name;
        this.configValidationResults = configValidationResults;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConnectorPluginConfigValidationResults getConfigValidationResults() {
        return configValidationResults;
    }

    public void setConfigValidationResults(ConnectorPluginConfigValidationResults configValidationResults) {
        this.configValidationResults = configValidationResults;
    }

    @Override
    public String toString() {
        return "PluginDetails{"
                + "+ name='" + name + '\''
                + '}';
    }
}
