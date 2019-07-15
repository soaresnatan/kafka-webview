package org.sourcelab.kafka.webview.ui.manager.kafka.dto;

import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorDefinition;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorStatus;
import org.sourcelab.kafka.webview.ui.model.Cluster;

public class ConnectorInfo {

    private Cluster cluster;
    private ConnectorDefinition connectorDefinition;
    private ConnectorStatus connectorStatus;

    public ConnectorInfo(Cluster cluster, ConnectorDefinition connectorDefinition, ConnectorStatus connectorStatus) {
        this.cluster = cluster;
        this.connectorDefinition = connectorDefinition;
        this.connectorStatus = connectorStatus;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public ConnectorDefinition getConnectorDefinition() {
        return connectorDefinition;
    }

    public void setConnectorDefinition(ConnectorDefinition connectorDefinition) {
        this.connectorDefinition = connectorDefinition;
    }

    public ConnectorStatus getConnectorStatus() {
        return connectorStatus;
    }

    public void setConnectorStatus(ConnectorStatus connectorStatus) {
        this.connectorStatus = connectorStatus;
    }

}
