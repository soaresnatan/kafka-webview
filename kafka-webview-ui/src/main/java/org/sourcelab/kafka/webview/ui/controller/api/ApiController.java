/**
 * MIT License
 *
 * Copyright (c) 2017, 2018, 2019 SourceLab.org (https://github.com/SourceLabOrg/kafka-webview/)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.sourcelab.kafka.webview.ui.controller.api;

import org.sourcelab.kafka.webview.ui.controller.BaseController;
import org.sourcelab.kafka.webview.ui.controller.api.exceptions.ApiException;
import org.sourcelab.kafka.webview.ui.controller.api.exceptions.NotFoundApiException;
import org.sourcelab.kafka.webview.ui.controller.api.requests.*;
import org.sourcelab.kafka.webview.ui.controller.api.responses.ResultResponse;
import org.sourcelab.kafka.webview.ui.manager.kafka.*;
import org.sourcelab.kafka.webview.ui.manager.kafka.config.FilterDefinition;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.*;
import org.sourcelab.kafka.webview.ui.model.Cluster;
import org.sourcelab.kafka.webview.ui.model.Filter;
import org.sourcelab.kafka.webview.ui.model.View;
import org.sourcelab.kafka.webview.ui.repository.ClusterRepository;
import org.sourcelab.kafka.webview.ui.repository.FilterRepository;
import org.sourcelab.kafka.webview.ui.repository.ViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Handles API requests.
 */
@Controller
@RequestMapping("/api")
public class ApiController extends BaseController {
    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private FilterRepository filterRepository;

    @Autowired
    private WebKafkaConsumerFactory webKafkaConsumerFactory;

    @Autowired
    private KafkaOperationsFactory kafkaOperationsFactory;

    /**
     * POST kafka results.
     */
    @ResponseBody
    @RequestMapping(path = "/consumer/view/{id}", method = RequestMethod.POST, produces = "application/json")
    public KafkaResults consume(
        @PathVariable final Long id,
        @RequestBody final ConsumeRequest consumeRequest) {

        // Action describes what to consume 'next', 'prev', 'head', 'tail'
        final String action = consumeRequest.getAction();

        // Retrieve the view definition
        final View view = retrieveViewById(id);

        // Override settings
        final ViewCustomizer viewCustomizer = new ViewCustomizer(view, consumeRequest);
        viewCustomizer.overrideViewSettings();
        final List<FilterDefinition> configuredFilters = viewCustomizer.getFilterDefinitions();

        // Create consumer
        try (final WebKafkaConsumer webKafkaConsumer = setup(view, configuredFilters)) {
            // move directions if needed
            if ("next".equals(action)) {
                // Do nothing!
                //webKafkaConsumer.next();
            } else if ("previous".equals(action)) {
                webKafkaConsumer.previous();
            } else if ("head".equals(action)) {
                webKafkaConsumer.toHead();
            } else if ("tail".equals(action)) {
                webKafkaConsumer.toTail();
            }

            // Poll
            return webKafkaConsumer.consumePerPartition();
        } catch (final Exception e) {
            throw new ApiException("Consume", e);
        }
    }

    /**
     * POST manually set a consumer's offsets.
     */
    @ResponseBody
    @RequestMapping(
        path = "/consumer/view/{id}/offsets",
        method = RequestMethod.POST,
        produces = "application/json"
    )
    public ConsumerState setConsumerOffsets(
        @PathVariable final Long id,
        @RequestBody final Map<Integer, Long> partitionOffsetMap
    ) {
        // Retrieve View
        final View view = retrieveViewById(id);

        // Create consumer
        try (final WebKafkaConsumer webKafkaConsumer = setup(view, new HashSet<>())) {
            return webKafkaConsumer.seek(partitionOffsetMap);
        } catch (final Exception e) {
            throw new ApiException("Offsets", e);
        }
    }

    /**
     * POST manually set a consumer's offsets using a timestamp.
     */
    @ResponseBody
    @RequestMapping(
        path = "/consumer/view/{id}/timestamp/{timestamp}",
        method = RequestMethod.POST,
        produces = "application/json"
    )
    public ConsumerState setConsumerOffsetsByTimestamp(
        @PathVariable final Long id,
        @PathVariable final Long timestamp
    ) {
        // Retrieve View
        final View view = retrieveViewById(id);

        // Create consumer
        try (final WebKafkaConsumer webKafkaConsumer = setup(view, new HashSet<>())) {
            return webKafkaConsumer.seek(timestamp);
        } catch (final Exception e) {
            throw new ApiException("OffsetsByTimestamp", e);
        }
    }

    /**
     * GET all available partitions for a given view.
     */
    @ResponseBody
    @RequestMapping(path = "/view/{id}/partitions", method = RequestMethod.GET, produces = "application/json")
    public Collection<Integer> getPartitionsForView(@PathVariable final Long id) {
        // Retrieve View
        final View view = retrieveViewById(id);

        // If the view has defined partitions, we'll return them
        if (!view.getPartitionsAsSet().isEmpty()) {
            return view.getPartitionsAsSet();
        }

        // Otherwise ask the cluster for what partitions.
        // Create new Operational Client
        final Set<Integer> partitionIds = new HashSet<>();
        try (final KafkaOperations operations = createOperationsClient(view.getCluster())) {
            final TopicDetails topicDetails = operations.getTopicDetails(view.getTopic());
            for (final PartitionDetails partitionDetail : topicDetails.getPartitions()) {
                partitionIds.add(partitionDetail.getPartition());
            }
        } catch (final Exception e) {
            throw new ApiException("Topics", e);
        }
        return partitionIds;
    }

    /**
     * GET listing of all available kafka topics for a requested cluster.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/topics/list", method = RequestMethod.GET, produces = "application/json")
    public List<TopicListing> getTopics(@PathVariable final Long id, @RequestParam(required = false) final String search) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        // Create new Operational Client
        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            // Get all topics available on cluster.
            TopicList topics = operations.getAvailableTopics();

            // If search value supplied
            if (search != null && !search.trim().isEmpty()) {
                // filter
                topics = topics.filterByTopicName(search);
            }

            // return matched topics.
            return topics.getTopics();
        } catch (final Exception e) {
            throw new ApiException("Topics", e);
        }
    }

    /**
     * GET Details for a specific Topic.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/topic/{topic}/details", method = RequestMethod.GET, produces = "application/json")
    public TopicDetails getTopicDetails(@PathVariable final Long id, @PathVariable final String topic) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        // Create new Operational Client
        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            return operations.getTopicDetails(topic);
        } catch (final Exception e) {
            throw new ApiException("TopicDetails", e);
        }
    }

    /**
     * GET listing of all available kafka topics for a requested cluster.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/plugins/list", method = RequestMethod.GET, produces = "application/json")
    public List<PluginDetails> getPlugins(@PathVariable final Long id, @RequestParam(required = false) final String search) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        // Create new Operational Client
        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            // Get all topics available on cluster.
            PluginList pluginList = operations.getAvailablePlugins(cluster.getConnectorHosts());

            // return matched topics.
            return pluginList.getPlugins();
        } catch (final Exception e) {
            throw new ApiException("Connectors", e);
        }
    }

    /**
     * GET Details for a specific Topic.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/plugin/{plugin}/details", method = RequestMethod.GET, produces = "application/json")
    public PluginDetails getPluginDetails(@PathVariable final Long id, @PathVariable final String plugin) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        // Create new Operational Client
        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            PluginList pluginList = operations.getAvailablePlugins(cluster.getConnectorHosts());
            return operations.getPluginDetails(pluginList, plugin);
        } catch (final Exception e) {
            throw new ApiException("PluginDetails", e);
        }
    }


    /**
     * GET Config for a specific Topic.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/topic/{topic}/config", method = RequestMethod.GET, produces = "application/json")
    public List<ConfigItem> getTopicConfig(@PathVariable final Long id, @PathVariable final String topic) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        // Create new Operational Client
        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            return operations.getTopicConfig(topic).getConfigEntries();
        } catch (final Exception e) {
            throw new ApiException("TopicConfig", e);
        }
    }

    /**
     * GET Config for a specific broker.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/broker/{brokerId}/config", method = RequestMethod.GET, produces = "application/json")
    public List<ConfigItem> getBrokerConfig(@PathVariable final Long id, @PathVariable final String brokerId) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        // Create new Operational Client
        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            return operations.getBrokerConfig(brokerId).getConfigEntries();
        } catch (final Exception e) {
            throw new ApiException("BrokerConfig", e);
        }
    }

    /**
     * GET Details for all Topics on a cluster.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/topics/details", method = RequestMethod.GET, produces = "application/json")
    public Collection<TopicDetails> getAllTopicsDetails(@PathVariable final Long id) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        // Create new Operational Client
        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            // First get all of the topics
            final TopicList topicList = operations.getAvailableTopics();

            // Now get details about all the topics
            final Map<String, TopicDetails> results = operations.getTopicDetails(topicList.getTopicNames());

            // Sort the results by name
            final List<TopicDetails> sortedResults = new ArrayList<>(results.values());
            sortedResults.sort(Comparator.comparing(TopicDetails::getName));

            // Return values.
            return sortedResults;
        } catch (final Exception e) {
            throw new ApiException("TopicDetails", e);
        }
    }

    /**
     * POST Create new topic on cluster.
     * This should require ADMIN role.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/create/topic", method = RequestMethod.POST, produces = "application/json")
    public ResultResponse createTopic(@PathVariable final Long id, @RequestBody final CreateTopicRequest createTopicRequest) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        final String name = createTopicRequest.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new ApiException("CreateTopic", "Invalid topic name");
        }

        final Integer partitions = createTopicRequest.getPartitions();
        if (partitions == null || partitions < 1) {
            throw new ApiException("CreateTopic", "Invalid partitions value");
        }

        final Short replicas = createTopicRequest.getReplicas();
        if (replicas == null || replicas < 1) {
            throw new ApiException("CreateTopic", "Invalid replicas value");
        }

        final CreateTopic createTopic = new CreateTopic(name, partitions, replicas);

        // Create new Operational Client
        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            final boolean result = operations.createTopic(createTopic);

            // Quick n dirty json response
            return new ResultResponse("CreateTopic", result, "Created topic '" + createTopicRequest.getName() + "'");
        } catch (final Exception e) {
            throw new ApiException("CreateTopic", e);
        }
    }

    /**
     * POST Modify a topic's configuration on cluster.
     * This should require ADMIN role.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/modify/topic", method = RequestMethod.POST, produces = "application/json")
    public List<ConfigItem> modifyTopicConfig(
        @PathVariable final Long id,
        @RequestBody final ModifyTopicConfigRequest modifyTopicConfigRequest
    ) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        final String name = modifyTopicConfigRequest.getTopic();
        if (name == null || name.trim().isEmpty()) {
            throw new ApiException("ModifyTopic", "Invalid topic name");
        }

        final Map<String, String> configEntries = modifyTopicConfigRequest.getConfig();
        if (configEntries == null || configEntries.isEmpty()) {
            throw new ApiException("ModifyTopic", "Invalid configuration defined");
        }

        // Create new Operational Client
        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            return operations.alterTopicConfig(name, configEntries).getConfigEntries();
        } catch (final Exception e) {
            throw new ApiException("ModifyTopic", e);
        }
    }

    /**
     * POST Delete existing topic on cluster.
     * This should require ADMIN role.
     *
     * TODO explicitly disabled until custom user roles. https://github.com/SourceLabOrg/kafka-webview/issues/157
     */
//    @ResponseBody
//    @RequestMapping(path = "/cluster/{id}/delete/topic", method = RequestMethod.POST, produces = "application/json")
    public ResultResponse deleteTopic(@PathVariable final Long id, @RequestBody final DeleteTopicRequest deleteTopicRequest) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        final String name = deleteTopicRequest.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new ApiException("DeleteTopic", "Invalid topic name");
        }

        // Create new Operational Client
        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            final boolean result = operations.removeTopic(deleteTopicRequest.getName());

            // Quick n dirty json response
            return new ResultResponse("DeleteTopic", result, "Removed topic '" + deleteTopicRequest.getName() + "'");
        } catch (final Exception e) {
            throw new ApiException("DeleteTopic", e);
        }
    }

    /**
     * GET Nodes within a cluster.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/nodes", method = RequestMethod.GET, produces = "application/json")
    public List<NodeDetails> getClusterNodes(@PathVariable final Long id) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            final NodeList nodes = operations.getClusterNodes();
            return nodes.getNodes();
        } catch (final Exception exception) {
            throw new ApiException("ClusterNodes", exception);
        }
    }

    /**
     * GET Options for a specific filter.
     */
    @ResponseBody
    @RequestMapping(path = "/filter/{id}/options", method = RequestMethod.GET, produces = "application/json")
    public String[] getFilterOptions(@PathVariable final Long id) {
        // Retrieve Filter
        final Filter filter = retrieveFilterById(id);
        final String[] options = filter.getOptions().split(",");

        return options;
    }

    /**
     * GET list all consumer groups for a specific cluster.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/consumers", method = RequestMethod.GET, produces = "application/json")
    public List<ConsumerGroupIdentifier> listConsumers(@PathVariable final Long id) {

        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            return operations.listConsumers();
        } catch (final Exception exception) {
            throw new ApiException("ClusterNodes", exception);
        }
    }

    /**
     * GET list all consumer groups for a specific cluster with details about each one.
     */
    @ResponseBody
    @RequestMapping(
        path = "/cluster/{id}/consumersAndDetails",
        method = RequestMethod.GET,
        produces = "application/json"
    )
    public List<ConsumerGroupDetails> listConsumersAndDetails(@PathVariable final Long id) {

        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            // First get list of all consumerGroups.
            final List<ConsumerGroupIdentifier> consumerGroupIdentifiers = operations.listConsumers();
            if (consumerGroupIdentifiers.isEmpty()) {
                return new ArrayList<>();
            }

            final List<String> stringIds = new ArrayList<>();
            consumerGroupIdentifiers.forEach(groupId -> {
                stringIds.add(groupId.getId());
            });

            // Now get details about all of em.
            return operations.getConsumerGroupDetails(stringIds);
        } catch (final Exception exception) {
            throw new ApiException("ClusterNodes", exception);
        }
    }

    /**
     * GET Retrieve details about a single specific consumer.
     */
    @ResponseBody
    @RequestMapping(
        path = "/cluster/{id}/consumer/{consumerGroupId}/details",
        method = RequestMethod.GET,
        produces = "application/json"
    )
    public ConsumerGroupDetails getConsumerDetails(
        @PathVariable final Long id,
        @PathVariable final String consumerGroupId
    ) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            final List<String> stringIds = new ArrayList<>();
            stringIds.add(consumerGroupId);

            // Now get details about all of em.
            final List<ConsumerGroupDetails> detailsList = operations.getConsumerGroupDetails(stringIds);
            if (detailsList.isEmpty()) {
                throw new RuntimeException("Unable to find consumer group id " + consumerGroupId);
            }

            return detailsList.get(0);
        } catch (final Exception exception) {
            throw new ApiException("ClusterNodes", exception);
        }
    }

    /**
     * GET Retrieve offsets for a specific consumer group id.
     */
    @ResponseBody
    @RequestMapping(
        path = "/cluster/{id}/consumer/{consumerGroupId}/offsets",
        method = RequestMethod.GET,
        produces = "application/json"
    )
    public ConsumerGroupOffsets getConsumerOffsets(
        @PathVariable final Long id,
        @PathVariable final String consumerGroupId
    ) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            return operations.getConsumerGroupOffsets(consumerGroupId);
        } catch (final Exception exception) {
            throw new ApiException("ClusterNodes", exception);
        }
    }

    /**
     * GET Retrieve offsets for a specific consumer group id with tail positions.
     */
    @ResponseBody
    @RequestMapping(
        path = "/cluster/{id}/consumer/{consumerGroupId}/offsetsAndTailPositions",
        method = RequestMethod.GET, produces = "application/json"
    )
    public ConsumerGroupOffsetsWithTailPositions getConsumerOffsetsWithTailPositions(
        @PathVariable final Long id,
        @PathVariable final String consumerGroupId
    ) {
        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(id);

        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            return operations.getConsumerGroupOffsetsWithTailOffsets(consumerGroupId);
        } catch (final Exception exception) {
            throw new ApiException("ClusterNodes", exception);
        }
    }

    /**
     * POST list all consumer groups for a specific cluster.
     * This should require ADMIN role.
     */
    @ResponseBody
    @RequestMapping(path = "/cluster/{id}/consumer/remove", method = RequestMethod.POST, produces = "application/json")
    public boolean removeConsumer(
        @PathVariable final Long id,
        @RequestBody final ConsumerRemoveRequest consumerRemoveRequest) {

        // Retrieve cluster
        final Cluster cluster = retrieveClusterById(consumerRemoveRequest.getClusterId());

        try (final KafkaOperations operations = createOperationsClient(cluster)) {
            return operations.removeConsumerGroup(consumerRemoveRequest.getConsumerId());
        } catch (final Exception exception) {
            throw new ApiException("ClusterNodes", exception);
        }
    }

    /**
     * Error handler for ApiExceptions.
     */
    @ResponseBody
    @ExceptionHandler(ApiException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleApiException(final ApiException exception) {
        return new ApiErrorResponse(exception.getType(), exception.getMessage(), ApiErrorResponse.buildCauseList(exception));
    }

    /**
     * Create an operations client.
     */
    private KafkaOperations createOperationsClient(final Cluster cluster) {
        return kafkaOperationsFactory.create(cluster, getLoggedInUserId());
    }

    /**
     * Creates a WebKafkaConsumer instance.
     */
    private WebKafkaConsumer setup(final View view, final Collection<FilterDefinition> filterDefinitions) {
        final SessionIdentifier sessionIdentifier = SessionIdentifier.newWebIdentifier(
            getLoggedInUserId(),
            getLoggedInUserSessionId()
        );
        return webKafkaConsumerFactory.createWebClient(view, filterDefinitions, sessionIdentifier);
    }

    /**
     * Override parent method.
     */
    @Override
    @ModelAttribute
    public void addAttributes(final Model model) {
        // Do nothing.
    }

    /**
     * Helper method to retrieve a cluster by its Id.  If its not found it will throw the appropriate
     * NotFoundApiException exception.
     *
     * @param id id of cluster to retrieve
     * @return the cluster entity.
     * @throws NotFoundApiException if not found.
     */
    private Cluster retrieveClusterById(final Long id) throws NotFoundApiException {
        final Optional<Cluster> clusterOptional = clusterRepository.findById(id);
        if (!clusterOptional.isPresent()) {
            throw new NotFoundApiException("TopicConfig", "Unable to find cluster");
        }
        return clusterOptional.get();
    }

    /**
     * Helper method to retrieve a view by its Id.  If its not found it will throw the appropriate
     * NotFoundApiException exception.
     *
     * @param id id of view to retrieve
     * @return the view entity.
     * @throws NotFoundApiException if not found.
     */
    private View retrieveViewById(final Long id) throws NotFoundApiException {
        // Retrieve View
        final Optional<View> viewOptional = viewRepository.findById(id);
        if (!viewOptional.isPresent()) {
            throw new NotFoundApiException("Partitions", "Unable to find view");
        }
        return viewOptional.get();
    }

    /**
     * Helper method to retrieve a filter by its Id.  If its not found it will throw the appropriate
     * NotFoundApiException exception.
     *
     * @param id id of filter to retrieve
     * @return the filter entity.
     * @throws NotFoundApiException if not found.
     */
    private Filter retrieveFilterById(final Long id) throws NotFoundApiException {
        // Retrieve Filter
        final Optional<Filter> filterOptional = filterRepository.findById(id);
        if (!filterOptional.isPresent()) {
            throw new NotFoundApiException("FilterOptions", "Unable to find filter");
        }
        return filterOptional.get();
    }
}
