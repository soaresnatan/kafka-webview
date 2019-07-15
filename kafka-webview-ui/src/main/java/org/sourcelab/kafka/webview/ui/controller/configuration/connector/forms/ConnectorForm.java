/**
 * MIT License
 * <p>
 * Copyright (c) 2017, 2018, 2019 SourceLab.org (https://github.com/SourceLabOrg/kafka-webview/)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.sourcelab.kafka.webview.ui.controller.configuration.connector.forms;

import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigValidationResults;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.PluginErros;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Map;

/**
 * Represents the Create/Update Connector Form.
 */
public class ConnectorForm {
    private Long id = null;

    @NotNull(message = "Enter a unique name")
    @Size(min = 2, max = 255)
    private String name;

    @NotNull(message = "Select a cluster")
    private Long clusterId;

    @NotNull(message = "Select a connector")
    private String connector;

    private ConnectorPluginConfigValidationResults options;

    private Map<String, String> configuration;

    private PluginErros erros;

    public ConnectorForm() {
    }

    public ConnectorForm(String name, Long clusterId, String connector, ConnectorPluginConfigValidationResults options, Map<String, String> configuration, PluginErros erros) {
        this.name = name;
        this.clusterId = clusterId;
        this.connector = connector;
        this.options = options;
        this.configuration = configuration;
        this.erros = erros;
    }

    public PluginErros getErros() {
        return erros;
    }

    public void setErros(PluginErros erros) {
        this.erros = erros;
    }

    public ConnectorPluginConfigValidationResults getOptions() {
        return options;
    }

    public void setOptions(ConnectorPluginConfigValidationResults options) {
        this.options = options;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(final Long clusterId) {
        this.clusterId = clusterId;
    }


    public String getConnector() {
        return connector;
    }

    public void setConnector(String connector) {
        this.connector = connector;
    }

    public boolean exists() {
        return getId() != null;
    }

    @Override
    public String toString() {
        return "ViewForm{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", clusterId=" + clusterId
                + ", connector=" + connector
                + '}';
    }
}
