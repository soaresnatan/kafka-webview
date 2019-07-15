package org.sourcelab.kafka.webview.ui.controller.connector;

import org.sourcelab.kafka.webview.ui.controller.BaseController;
import org.sourcelab.kafka.webview.ui.manager.kafka.KafkaOperations;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.ConnectorInfo;
import org.sourcelab.kafka.webview.ui.manager.ui.BreadCrumbManager;
import org.sourcelab.kafka.webview.ui.manager.ui.FlashMessage;
import org.sourcelab.kafka.webview.ui.model.Cluster;
import org.sourcelab.kafka.webview.ui.model.Connector;
import org.sourcelab.kafka.webview.ui.repository.ClusterRepository;
import org.sourcelab.kafka.webview.ui.repository.ConnectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/connector")
public class ConnectorController extends BaseController {

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private ConnectorRepository connectorRepository;

    /**
     * GET views index.
     */
    @RequestMapping(path = "", method = RequestMethod.GET)
    public String index(
            final Model model,
            @RequestParam(name = "clusterId", required = false) final Long clusterId
    ) {
        // Setup breadcrumbs
        final BreadCrumbManager breadCrumbManager = new BreadCrumbManager(model);

        // Retrieve all clusters and index by id
        final Map<Long, Cluster> clustersById = new HashMap<>();
        clusterRepository
                .findAllByOrderByNameAsc()
                .forEach((cluster) -> clustersById.put(cluster.getId(), cluster));

        final Iterable<Connector> connectorList;
        if (clusterId == null) {
            // Retrieve all views order by name asc.
            connectorList = connectorRepository.findAllByOrderByNameAsc();
        } else {
            // Retrieve only views for the cluster
            connectorList = connectorRepository.findAllByClusterIdOrderByNameAsc(clusterId);
        }

        KafkaOperations operations = new KafkaOperations();

        List<ConnectorInfo> connectorsInfos = new ArrayList<>();
        for (Iterator it = connectorList.iterator(); it.hasNext(); ) {
            Connector connector = (Connector) it.next();

            connectorsInfos.add(operations.getConnector(connector.getCluster(), connector.getName()));
        }

        // Set model Attributes
        model.addAttribute("clustersById", clustersById);
        model.addAttribute("connectorsInfos", connectorsInfos);

        final String clusterName;
        if (clusterId != null && clustersById.containsKey(clusterId)) {
            // If filtered by a cluster
            clusterName = clustersById.get(clusterId).getName();

            // Add top level breadcrumb
            breadCrumbManager
                    .addCrumb("Connector", "/connector")
                    .addCrumb("Cluster: " + clusterName);
        } else {
            // If showing all connectors
            clusterName = null;

            // Add top level breadcrumb
            breadCrumbManager.addCrumb("Connector", null);
        }
        model.addAttribute("clusterName", clusterName);

        return "connector/index";
    }


    /**
     * GET Displays view for specified view.
     */
    @RequestMapping(path = "/{connectorName}", method = RequestMethod.GET)
    public String index(
            @PathVariable final String connectorName,
            final RedirectAttributes redirectAttributes,
            final Model model) {

        // Retrieve the connector
        final Optional<Connector> connectorOptional = Optional.ofNullable(connectorRepository.findByName(connectorName));
        if (!connectorOptional.isPresent()) {
            // Set flash message
            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newWarning("Unable to find connector!"));

            // redirect to home
            return "redirect:/";
        }

        Connector connector = connectorOptional.get();

        // Setup breadcrumbs
        new BreadCrumbManager(model)
                .addCrumb("Connector", "/connector")
                .addCrumb(connector.getName());


        Cluster cluster = connector.getCluster();
        KafkaOperations operations = new KafkaOperations();
        ConnectorInfo connectorInfo = operations.getConnector(cluster, connector.getName());

        // Set model Attributes
        model.addAttribute("connectorInfo", connectorInfo);
        model.addAttribute("cluster", cluster);

        return "connector/info";
    }

}
