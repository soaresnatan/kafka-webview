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
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.ConnectorInfo;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.PluginDetails;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.PluginList;
import org.sourcelab.kafka.webview.ui.manager.ui.BreadCrumbManager;
import org.sourcelab.kafka.webview.ui.manager.ui.FlashMessage;
import org.sourcelab.kafka.webview.ui.model.Cluster;
import org.sourcelab.kafka.webview.ui.model.Connector;
import org.sourcelab.kafka.webview.ui.repository.ClusterRepository;
import org.sourcelab.kafka.webview.ui.repository.ConnectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.*;

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
        KafkaOperations operations = new KafkaOperations();

        List<ConnectorInfo> connectorInfos = new ArrayList<>();
        for (Iterator it = connectorList.iterator(); it.hasNext(); ) {
            Connector connector = (Connector) it.next();

            connectorInfos.add(operations.getConnector(connector.getCluster(), connector.getName()));
        }


        model.addAttribute("connectorsInfos", connectorInfos);

        return "configuration/connector/index";
    }

    /**
     * GET Displays create view form.
     */
    @RequestMapping(path = "/create", method = RequestMethod.GET)
    public String createConnectorForm(final ConnectorForm connectorForm, final Model model) {
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

            // Retrieve cluster
            clusterRepository.findById(connectorForm.getClusterId()).ifPresent((cluster) -> {
                try (final KafkaOperations operations = kafkaOperationsFactory.create(cluster, getLoggedInUserId())) {

                    // ALTERAR PARA EDIT :)

                    // Get plugins
                    final PluginList plugins = operations.getAvailablePlugins(cluster.getConnectorHosts());
                    model.addAttribute("plugins", plugins.getPlugins());

                    // If we have a selected topic
                    if (connectorForm.getConnector() != null && !"!".equals(connectorForm.getConnector())) {
                        final PluginDetails pluginDetails = operations.getPluginDetails(plugins, connectorForm.getConnector());
//                        model.addAttribute("pluginsDetails", connectorForm.getPlugin());
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


        final boolean updateExisting;
        Connector connector = connectorRepository.findByName(connectorForm.getName());
        Cluster cluster;

        if (connector == null) {
            connector = new Connector();

            long clusterId = connectorForm.getClusterId();
            cluster = clusterRepository.findById(clusterId).get();

            connector.setName(connectorForm.getName());
            connector.setCluster(cluster);

            updateExisting = false;
        } else {
            if (!connectorForm.getConnector().isEmpty()) {
                redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newDanger("Connector already exists!"));

                return "redirect:/configuration/connector/create";
            }

            cluster = connector.getCluster();
            updateExisting = true;
        }

        ConnectorForm resultForm;
        String successMessage;
        try {
            final KafkaOperations operations = kafkaOperationsFactory.create(cluster, getLoggedInUserId());
            resultForm = updateExisting ? operations.updateConnector(cluster.getId(), cluster.getConnectorHosts(), allRequestParams) : operations.addConnector(cluster.getId(), cluster.getConnectorHosts(), allRequestParams);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newDanger(e.getMessage()));

            return "redirect:/configuration/connector";
        }

        if (!resultForm.getErros().haveErrors()) {

            successMessage = "Connector Updated!";
            if (!updateExisting) {
                connectorRepository.save(connector);
                successMessage = "Created new Connector!";
            }

            // Set flash message
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newSuccess(successMessage));

            return "redirect:/configuration/connector";

        } else {
            // Set flash message
            for (String error : resultForm.getErros().getListErros()) {
                for (String descptionError : resultForm.getErros().getErrors(error)) {
                    FieldError errorField = new FieldError(error, error, descptionError);

                    redirectAttributes.addFlashAttribute(FlashMessage.newDanger(descptionError));

                }
            }

            return updateExisting ? "redirect:/configuration/connector/" : "redirect:/configuration/connector/create";
        }
    }

    /**
     * GET Displays edit cluster form.
     */
    @RequestMapping(path = "/{connectorId}/edit", method = RequestMethod.GET)
    public String editClusterForm(
            @PathVariable final String connectorId,
            final ConnectorForm connectorForm,
            final RedirectAttributes redirectAttributes,
            final Model model) {

        Optional<Connector> ConnectorOptional = Optional.ofNullable(connectorRepository.findByName(connectorId));
        if (!ConnectorOptional.isPresent()) {
            // redirect
            // Set flash message
            final FlashMessage flashMessage = FlashMessage.newWarning("Unable to find Connector!");
            redirectAttributes.addFlashAttribute("FlashMessage", flashMessage);

            // redirect to cluster index
            return "redirect:/configuration/connector";
        }
        final Connector connector = ConnectorOptional.get();

        // Setup breadcrumbs
        setupBreadCrumbs(model, "Edit: " + connector.getName(), null);


        KafkaOperations operations = new KafkaOperations();
        ConnectorInfo connectorInfo = operations.getConnector(connector.getCluster(), connector.getName());
        PluginDetails pluginDetails = operations.getPluginDetails(connector.getCluster(), connectorInfo.getConnectorDefinition().getConfig().get("connector.class"));

        connectorForm.setName(connector.getName());
        connectorForm.setConfiguration(connectorInfo.getConnectorDefinition().getConfig());


        model.addAttribute("connectorForm", connectorForm);
        model.addAttribute("pluginDetails", pluginDetails);

        // Display template
        return "configuration/connector/edit";
    }


    @RequestMapping(path = "/{connectorId}/remove", method = RequestMethod.POST)
    public String removeConnector(@PathVariable final String connectorId,
                                  final RedirectAttributes redirectAttributes) {

        final Optional<Connector> connectorOptional = Optional.ofNullable(connectorRepository.findByName(connectorId));

        if (!connectorOptional.isPresent()) {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newWarning("Unable to find connector!"));

            return "redirect:/";
        }

        Connector connector = connectorOptional.get();
        Cluster cluster = connector.getCluster();
        boolean result = true;
        try (final KafkaOperations operations = kafkaOperationsFactory.create(cluster, getLoggedInUserId())) {
            result = operations.removeConnector(cluster.getConnectorHosts(), connector.getName());
        }

        if (result) {
            connectorRepository.delete(connector);

            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newSuccess("Connector " + connectorId + " removed"));
        } else {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newDanger("Connector " + connectorId + " not removed"));
        }

        return "redirect:/configuration/connector";
    }

    @RequestMapping(path = "/{connectorId}/task/{taskId}/restart", method = RequestMethod.POST)
    public String restartTask(@PathVariable final int taskId,
                              @PathVariable final String connectorId,
                              final RedirectAttributes redirectAttributes) {

        final Optional<Connector> connectorOptional = Optional.ofNullable(connectorRepository.findByName(connectorId));

        if (!connectorOptional.isPresent()) {
            // Set flash message
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newWarning("Unable to find connector!"));

            // redirect to home
            return "redirect:/";
        }

        Connector connector = connectorOptional.get();
        Cluster cluster = connector.getCluster();
        boolean result = false;
        try (final KafkaOperations operations = kafkaOperationsFactory.create(cluster, getLoggedInUserId())) {
            result = operations.restartConnectorTask(cluster.getConnectorHosts(), connector.getName(), taskId);
        }

        if (result) {
            // Set flash message
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newSuccess("Task " + taskId + " restarted"));
        } else {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newDanger("Task " + taskId + " not restarted"));
        }

        return "redirect:/connector/" + connectorId;
    }

    @RequestMapping(path = "/{connectorId}/restart", method = RequestMethod.POST)
    public String restartConnector(@PathVariable final String connectorId,
                                   final RedirectAttributes redirectAttributes) {

        final Optional<Connector> connectorOptional = Optional.ofNullable(connectorRepository.findByName(connectorId));

        if (!connectorOptional.isPresent()) {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newWarning("Unable to find connector!"));

            return "redirect:/";
        }

        Connector connector = connectorOptional.get();
        Cluster cluster = connector.getCluster();
        boolean result = false;
        try (final KafkaOperations operations = kafkaOperationsFactory.create(cluster, getLoggedInUserId())) {
            result = operations.restartConnector(cluster.getConnectorHosts(), connector.getName());
        }

        if (result) {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newSuccess("Connector " + connectorId + " restarted"));
        } else {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newDanger("Connector " + connectorId + " not restarted"));
        }

        return "redirect:/connector/" + connectorId;
    }

    @RequestMapping(path = "/{connectorId}/pause", method = RequestMethod.POST)
    public String pauseConnector(@PathVariable final String connectorId,
                                 final RedirectAttributes redirectAttributes) {

        final Optional<Connector> connectorOptional = Optional.ofNullable(connectorRepository.findByName(connectorId));

        if (!connectorOptional.isPresent()) {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newWarning("Unable to find connector!"));

            return "redirect:/";
        }

        Connector connector = connectorOptional.get();
        Cluster cluster = connector.getCluster();
        boolean result = false;
        try (final KafkaOperations operations = kafkaOperationsFactory.create(cluster, getLoggedInUserId())) {
            result = operations.pauseConnector(cluster.getConnectorHosts(), connector.getName());
        }

        if (result) {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newSuccess("Connector " + connectorId + " paused"));
        } else {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newDanger("Connector " + connectorId + " not paused"));
        }

        return "redirect:/connector/" + connectorId;
    }

    @RequestMapping(path = "/{connectorId}/resume", method = RequestMethod.POST)
    public String resumeConnector(@PathVariable final String connectorId,
                                  final RedirectAttributes redirectAttributes) {

        final Optional<Connector> connectorOptional = Optional.ofNullable(connectorRepository.findByName(connectorId));

        if (!connectorOptional.isPresent()) {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newWarning("Unable to find connector!"));

            return "redirect:/";
        }

        Connector connector = connectorOptional.get();
        Cluster cluster = connector.getCluster();
        boolean result = false;
        try (final KafkaOperations operations = kafkaOperationsFactory.create(cluster, getLoggedInUserId())) {
            result = operations.resumeConnector(cluster.getConnectorHosts(), connector.getName());
        }

        if (result) {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newSuccess("Connector " + connectorId + " back"));
        } else {
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newDanger("Connector " + connectorId + " not back"));
        }

        return "redirect:/connector/" + connectorId;
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
