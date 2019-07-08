package org.sourcelab.kafka.webview.ui.controller.connector;

import org.sourcelab.kafka.webview.ui.manager.ui.BreadCrumbManager;
import org.sourcelab.kafka.webview.ui.model.Cluster;
import org.sourcelab.kafka.webview.ui.model.Connector;
import org.sourcelab.kafka.webview.ui.repository.ClusterRepository;
import org.sourcelab.kafka.webview.ui.repository.ConnectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/connector")
public class ConnectorController {

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

        final Iterable<Connector> connectors;
        if (clusterId == null) {
            // Retrieve all views order by name asc.
            connectors = connectorRepository.findAllByOrderByNameAsc();
        } else {
            // Retrieve only views for the cluster
            connectors = connectorRepository.findAllByClusterIdOrderByNameAsc(clusterId);
        }

        // Set model Attributes
        model.addAttribute("connectorList", connectors);
        model.addAttribute("clustersById", clustersById);

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
//
//    /**
//     * GET Displays view for specified view.
//     */
//    @RequestMapping(path = "/{id}", method = RequestMethod.GET)
//    public String index(
//            @PathVariable final Long id,
//            final RedirectAttributes redirectAttributes,
//            final Model model) {
//
//        // Retrieve the view
//        final Optional<View> viewOptional = viewRepository.findById(id);
//        if (!viewOptional.isPresent()) {
//            // Set flash message
//            redirectAttributes.addFlashAttribute("FlashMessage", FlashMessage.newWarning("Unable to find view!"));
//
//            // redirect to home
//            return "redirect:/";
//        }
//        final View view = viewOptional.get();
//
//        // Setup breadcrumbs
//        new BreadCrumbManager(model)
//                .addCrumb("View", "/view")
//                .addCrumb(view.getName());
//
//        // Set model Attributes
//        model.addAttribute("view", view);
//        model.addAttribute("cluster", view.getCluster());
//
//        return "view/consume";
//    }


}
