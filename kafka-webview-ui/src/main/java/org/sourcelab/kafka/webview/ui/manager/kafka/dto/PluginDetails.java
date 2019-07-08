package org.sourcelab.kafka.webview.ui.manager.kafka.dto;

import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigValidationResults;

public class PluginDetails {
    private String name;
    private ConnectorPluginConfigValidationResults configValidationResults;

    public PluginDetails(String name, ConnectorPluginConfigValidationResults configValidationResults) {
        this.name = name;
        this.configValidationResults = configValidationResults;
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
