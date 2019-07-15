package org.sourcelab.kafka.webview.ui.manager.kafka.dto;

import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigValidationResults;

public class PluginDetails {
    private String nameClass;
    private String namePlugin;
    private ConnectorPluginConfigValidationResults configValidationResults;

    public PluginDetails(String namePlugin, String nameClass, ConnectorPluginConfigValidationResults configValidationResults) {
        this.nameClass = nameClass;
        this.namePlugin = namePlugin;
        this.configValidationResults = configValidationResults;
    }

    public PluginDetails(String nameClass, ConnectorPluginConfigValidationResults configValidationResults) {
        this.nameClass = nameClass;
        this.configValidationResults = configValidationResults;
    }

    public String getNameClass() {
        return nameClass;
    }

    public void setNameClass(String nameClass) {
        this.nameClass = nameClass;
    }

    public String getNamePlugin() {
        return namePlugin;
    }

    public void setNamePlugin(String namePlugin) {
        this.namePlugin = namePlugin;
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
                + "+ name='" + nameClass + '\''
                + '}';
    }
}
