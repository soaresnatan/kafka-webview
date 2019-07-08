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

package org.sourcelab.kafka.webview.ui.controller.configuration.connector;

import org.sourcelab.kafka.webview.ui.controller.BaseController;
import org.sourcelab.kafka.webview.ui.controller.configuration.connector.forms.ConnectorForm;
import org.sourcelab.kafka.webview.ui.manager.kafka.KafkaOperations;
import org.sourcelab.kafka.webview.ui.manager.kafka.KafkaOperationsFactory;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.PluginDetails;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.PluginList;
import org.sourcelab.kafka.webview.ui.manager.ui.BreadCrumbManager;
import org.sourcelab.kafka.webview.ui.model.Connector;
import org.sourcelab.kafka.webview.ui.repository.ClusterRepository;
import org.sourcelab.kafka.webview.ui.repository.ConnectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Map;

/**
 * Controller for CRUD over View entities.
 */
@Controller
@RequestMapping("/configuration/connector")
public class ConnectorConfigController extends BaseController {

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private ConnectorRepository connectorRepository;

    @Autowired
    private KafkaOperationsFactory kafkaOperationsFactory;

    /**
     * GET Displays main configuration index.
     */
    @RequestMapping(path = "", method = RequestMethod.GET)
    public String index(final Model model) {
        // Setup breadcrumbs
        setupBreadCrumbs(model, null, null);

        // Retrieve all message formats
        final Iterable<Connector> connectorList = connectorRepository.findAllByOrderByNameAsc();
        model.addAttribute("connectors", connectorList);

        return "configuration/connector/index";
    }

    /**
     * GET Displays create view form.
     */
    @RequestMapping(path = "/create", method = RequestMethod.GET)
    public String createViewForm(final ConnectorForm connectorForm, final Model model) {
        // Setup breadcrumbs
        if (!model.containsAttribute("BreadCrumbs")) {
            setupBreadCrumbs(model, "Create", null);
        }

        // Retrieve all clusters
        model.addAttribute("clusters", clusterRepository.findAllByOrderByNameAsc());

        // Retrieve all plugins
        model.addAttribute("plugins", new ArrayList<>());
        model.addAttribute("pluginsDetails", new ArrayList<>());

        if (connectorForm.getClusterId() != null) {
            // Lets load the topics now
            // Retrieve cluster
            clusterRepository.findById(connectorForm.getClusterId()).ifPresent((cluster) -> {
                try (final KafkaOperations operations = kafkaOperationsFactory.create(cluster, getLoggedInUserId())) {
                    // Get plugins
                    final PluginList plugins = operations.getAvailablePlugins(cluster.getConnectorHosts());
                    model.addAttribute("plugins", plugins.getPlugins());

                    // If we have a selected topic
                    if (connectorForm.getConnector() != null && !"!".equals(connectorForm.getConnector())) {
                        final PluginDetails pluginDetails = operations.getPluginDetails(plugins, connectorForm.getConnector());
                        model.addAttribute("pluginsDetails", pluginDetails);
                    }
                }
            });
        }

        return "configuration/connector/create";
    }

    /**
     * Handles both Update and Creating views.
     */
    @RequestMapping(path = "/update", method = RequestMethod.POST)
    public String updateView(
            @Valid final ConnectorForm connectorForm,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes,
            final Model model,
            @RequestParam final Map<String, String> allRequestParams) {

        // Determine if we're updating or creating
        final boolean updateExisting = connectorForm.exists();

        // redirect to cluster index
        return "redirect:/configuration/connector";
    }

    private void setupBreadCrumbs(final Model model, String name, String url) {
        // Setup breadcrumbs
        final BreadCrumbManager manager = new BreadCrumbManager(model)
                .addCrumb("Configuration", "/configuration");

        if (name != null) {
            manager.addCrumb("Connectors", "/configuration/connector");
            manager.addCrumb(name, url);
        } else {
            manager.addCrumb("Connectors", null);
        }
    }
}
