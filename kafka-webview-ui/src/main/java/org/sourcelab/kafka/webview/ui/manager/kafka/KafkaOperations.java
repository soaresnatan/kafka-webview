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

package org.sourcelab.kafka.webview.ui.manager.kafka;

import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.sourcelab.kafka.connect.apiclient.request.dto.Task;
import org.sourcelab.kafka.webview.ui.controller.configuration.connector.forms.ConnectorForm;
import org.sourcelab.kafka.webview.ui.manager.kafka.dto.*;
import org.sourcelab.kafka.webview.ui.model.Cluster;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Wrapper for AdminClient.  For doing various 'operational' type commands
 * against a cluster, or retrieving metadata about a cluster.
 */
public class KafkaOperations implements AutoCloseable {
    private AdminClient adminClient;
    private KafkaConsumer<String, String> consumerClient;
    private KafkaConnect kafkaConnect;

    /**
     * Constructor.
     * @param adminClient The AdminClient to wrap.
     * @param consumerClient The KafkaConsumer instance to wrap.
     */
    public KafkaOperations(final AdminClient adminClient, final KafkaConsumer<String, String> consumerClient) {
        this.adminClient = adminClient;
        this.consumerClient = consumerClient;
        this.kafkaConnect = new KafkaConnect();
    }

    public KafkaOperations() {
        this.kafkaConnect = new KafkaConnect();
    }

    public PluginList getAvailablePlugins(String host) {
        kafkaConnect.connect(host);

        return new PluginList(kafkaConnect.getConnectorPlugins());
    }

    public PluginDetails getPluginDetails(PluginList pluginList, String plugin) {
        PluginDetails pluginDetails = pluginList.getPlugin(plugin);
        return pluginDetails;
    }

    public PluginDetails getPluginDetails(Cluster cluster, String className) {
        kafkaConnect.connect(cluster.getConnectorHosts());

        return kafkaConnect.getPlugin(className);
    }

    public Collection<Task> getTasksInfo(Cluster cluster, String connectorName) {
        kafkaConnect.connect(cluster.getConnectorHosts());

        return kafkaConnect.getConnectorTasks(connectorName);
    }

    public Boolean restartConnectorTask(String host, String connectorName, int taskId) {
        kafkaConnect.connect(host);

        return kafkaConnect.restartConnectorTask(connectorName, taskId);
    }

    public ConnectorForm updateConnector(long clusterId, String host, Map<String, String> map) {
        kafkaConnect.connect(host);

        return kafkaConnect.updateConnector(clusterId, map);
    }

    public ConnectorForm addConnector(long clusterId, String host, Map<String, String> map) {
        kafkaConnect.connect(host);

        return kafkaConnect.addConnector(clusterId, map);
    }

    public Boolean removeConnector(String host, String connectorName) {
        kafkaConnect.connect(host);
        return kafkaConnect.deleteConnector(connectorName);
    }

    public ConnectorInfo getConnector(Cluster cluster, String name) {
        kafkaConnect.connect(cluster.getConnectorHosts());

        return kafkaConnect.getConnector(cluster, name);
    }

    public boolean pauseConnector(String host, String connectorName) {
        kafkaConnect.connect(host);

        return kafkaConnect.pauseConnector(connectorName);
    }

    public boolean resumeConnector(String host, String connectorName) {
        kafkaConnect.connect(host);

        return kafkaConnect.resumeConnector(connectorName);
    }

    public boolean restartConnector(String host, String connectorName) {
        kafkaConnect.connect(host);

        return kafkaConnect.restartConnector(connectorName);
    }

    /**
     * Retrieve all available topics within cluster.  Includes internal topics.
     */
    public TopicList getAvailableTopics() {
        final List<org.sourcelab.kafka.webview.ui.manager.kafka.dto.TopicListing> topicListings = new ArrayList<>();

        try {
            // We want to show all topics, including internal topics.
            final ListTopicsOptions listTopicsOptions = new ListTopicsOptions().listInternal(true);

            final Collection<TopicListing> results = adminClient
                .listTopics(listTopicsOptions)
                .listings()
                .get();

            for (final TopicListing entry: results) {
                topicListings.add(
                    new org.sourcelab.kafka.webview.ui.manager.kafka.dto.TopicListing(entry.name(), entry.isInternal())
                );
            }
            return new TopicList(topicListings);
        } catch (InterruptedException | ExecutionException e) {
            // TODO Handle
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Get information about brokers within the cluster.
     */
    public NodeList getClusterNodes() {
        final List<NodeDetails> nodeDetails = new ArrayList<>();

        try {
            final Collection<Node> nodes = adminClient.describeCluster().nodes().get();
            for (final Node node: nodes) {
                nodeDetails.add(new NodeDetails(node.id(), node.host(), node.port(), node.rack()));
            }
            return new NodeList(nodeDetails);
        } catch (InterruptedException | ExecutionException e) {
            // TODO Handle
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Get information about several topics.
     */
    public Map<String, TopicDetails> getTopicDetails(final Collection<String> topics) {
        final Map<String, TopicDetails> results = new HashMap<>();

        try {
            final Map<String, TopicDescription> topicDescriptionMap = adminClient.describeTopics(topics).all().get();

            // Now parse results
            for (final Map.Entry<String, TopicDescription> entry: topicDescriptionMap.entrySet()) {
                final String topic = entry.getKey();
                final TopicDescription topicDescription = entry.getValue();

                final List<PartitionDetails> partitionDetails = new ArrayList<>();

                for (final TopicPartitionInfo partitionInfo: topicDescription.partitions()) {
                    final List<NodeDetails> isrNodes = new ArrayList<>();
                    final List<NodeDetails> replicaNodes = new ArrayList<>();

                    // Translate Leader
                    final Node partitionLeader = partitionInfo.leader();
                    final NodeDetails leaderNode = new NodeDetails(
                        partitionLeader.id(), partitionLeader.host(), partitionLeader.port(), partitionLeader.rack()
                    );

                    // Translate ISR nodes
                    for (final Node node: partitionInfo.isr()) {
                        isrNodes.add(
                            new NodeDetails(node.id(), node.host(), node.port(), node.rack())
                        );
                    }

                    // Translate Replicas nodes
                    for (final Node node: partitionInfo.replicas()) {
                        replicaNodes.add(
                            new NodeDetails(node.id(), node.host(), node.port(), node.rack())
                        );
                    }

                    // Create the details
                    final PartitionDetails partitionDetail = new PartitionDetails(
                        topicDescription.name(),
                        partitionInfo.partition(),
                        leaderNode,
                        replicaNodes,
                        isrNodes
                    );

                    // Add to the list.
                    partitionDetails.add(partitionDetail);

                    // Create new TopicDetails.
                    final TopicDetails topicDetails = new TopicDetails(
                        topicDescription.name(),
                        topicDescription.isInternal(),
                        partitionDetails
                    );
                    results.put(topic, topicDetails);
                }
            }
            // Return it
            return results;
        } catch (final InterruptedException | ExecutionException exception) {
            // TODO Handle this
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    /**
     * Get information about a specific topic.
     */
    public TopicDetails getTopicDetails(final String topic) {
        final Map<String, TopicDetails> results = getTopicDetails(Collections.singleton(topic));
        return results.get(topic);
    }

    /**
     * Get the configuration for topic.
     */
    public TopicConfig getTopicConfig(final String topic) {
        final ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        return new TopicConfig(describeResource(configResource));
    }

    /**
     * Get the configuration for topic.
     */
    public BrokerConfig getBrokerConfig(final String brokerId) {
        final ConfigResource configResource = new ConfigResource(ConfigResource.Type.BROKER, brokerId);
        return new BrokerConfig(describeResource(configResource));
    }

    /**
     * Create a new topic on the cluster.
     * @param createTopic defines the new topic
     * @return boolean, true if success.
     */
    public boolean createTopic(final CreateTopic createTopic) {
        final NewTopic newTopic = new NewTopic(
            createTopic.getName(),
            createTopic.getNumberOfPartitions(),
            createTopic.getReplicaFactor()
        );

        try {
            // Create the topic
            final CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));

            // Wait for the async request to process.
            result.all().get();

            // return true?
            return true;
        } catch (final InterruptedException | ExecutionException exception) {
            // TODO Handle this
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    /**
     * Modify configuration values for a specific topic.
     * @param topic The topic to modify.
     * @param configItems Map of Key to Value to modify.
     * @return boolean
     */
    public TopicConfig alterTopicConfig(final String topic, final Map<String, String> configItems) {
        try {
            // Define the resource we want to modify, the topic.
            final ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic);

            final List<ConfigEntry> configEntries = new ArrayList<>();
            for (final Map.Entry<String, String> entry : configItems.entrySet()) {
                configEntries.add(
                    new ConfigEntry(entry.getKey(), entry.getValue())
                );
            }

            // Define the configuration set
            final Config config = new Config(configEntries);

            // Create the topic
            final AlterConfigsResult result = adminClient.alterConfigs(Collections.singletonMap(configResource, config));

            // Wait for the async request to process.
            result.all().get();

            // Lets return updated topic details
            return getTopicConfig(topic);
        } catch (final InterruptedException | ExecutionException exception) {
            // TODO Handle this
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    /**
     * Remove a topic from a kafka cluster.  This is destructive!
     * @param topic name of the topic to remove.
     * @return true on success.
     */
    public boolean removeTopic(final String topic) {
        try {
            final DeleteTopicsResult result = adminClient.deleteTopics(Collections.singletonList(topic));

            // Wait for the async request to process.
            result.all().get();

            // return true?
            return true;
        } catch (final InterruptedException | ExecutionException exception) {
            // TODO Handle this
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    /**
     * List all consumer groups on server.
     * @return Immutable sorted list of Consumer Group Identifiers.
     */
    public List<ConsumerGroupIdentifier> listConsumers() {
        // Make request
        final ListConsumerGroupsResult results = adminClient.listConsumerGroups();

        // Generate return list.
        final List<ConsumerGroupIdentifier> consumerIds = new ArrayList<>();

        try {
            // Iterate over results
            results
                .all()
                .get()
                .forEach((result) ->  {
                    consumerIds.add(
                        new ConsumerGroupIdentifier(result.groupId(), result.isSimpleConsumerGroup())
                    );
                });

            // Sort them by consumer Id.
            consumerIds.sort(Comparator.comparing(ConsumerGroupIdentifier::getId));

            // return immutable list.
            return Collections.unmodifiableList(consumerIds);

        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Removes a consumer's state.
     * @param id id of consumer group to remove.
     * @return boolean true if success.
     * @throws RuntimeException on underlying errors.
     */
    public boolean removeConsumerGroup(final String id) {
        final DeleteConsumerGroupsResult request = adminClient.deleteConsumerGroups(Collections.singleton(id));

        try {
            request.all().get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            // TODO Handle this
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns details about every consumer within the consumer group id.
     * @param consumerGroupId id to retrieve details for.
     * @return ConsumerGroupDetails
     */
    public ConsumerGroupDetails getConsumerGroupDetails(final String consumerGroupId) {
        final List<ConsumerGroupDetails> consumerGroupDetails = getConsumerGroupDetails(
            Collections.singletonList(consumerGroupId)
        );

        // There should only be a single entry
        if (consumerGroupDetails.size() == 1) {
            return consumerGroupDetails.get(0);
        }
        // If we had zero or more than one.... not sure... toss exception
        throw new RuntimeException("Unable to locate consumer group id " + consumerGroupId);
    }

    /**
     * Returns details about every consumer within the consumer group id.
     * @param consumerGroupIds ids to retrieve details for.
     * @return immutable list of ConsumerGroupDetails
     */
    public List<ConsumerGroupDetails> getConsumerGroupDetails(final List<String> consumerGroupIds) {
        // Make request
        final DescribeConsumerGroupsResult results = adminClient.describeConsumerGroups(consumerGroupIds);

        // Generate return list.
        final List<ConsumerGroupDetails> consumerGroupDetails = new ArrayList<>();

        try {
            // Iterate over results
            results
                .all()
                .get()
                .forEach((key, value) ->  {
                    final List<ConsumerGroupDetails.Member> members = new ArrayList<>();

                    // Loop over each member
                    value.members().forEach((member) -> {
                        // Collect the members assigned partitions
                        final Set<TopicPartition> assignedPartitions = new HashSet<>();
                        member.assignment().topicPartitions().forEach((topicPartition -> {
                            assignedPartitions.add(new TopicPartition(
                                topicPartition.topic(),
                                topicPartition.partition()
                            ));
                        }));

                        // Create new ConsumerGroupDetails for this member
                        members.add(new ConsumerGroupDetails.Member(
                            member.consumerId(),
                            member.clientId(),
                            member.host(),
                            assignedPartitions
                        ));
                    });

                    final NodeDetails coordinator = new NodeDetails(
                        value.coordinator().id(),
                        value.coordinator().host(),
                        value.coordinator().port(),
                        value.coordinator().rack()
                    );

                    consumerGroupDetails.add(new ConsumerGroupDetails(
                        value.groupId(),
                        value.isSimpleConsumerGroup(),
                        value.partitionAssignor(),
                        value.state().toString(),
                        members,
                        coordinator
                    ));
                });

            // Sort list by consumer group id
            consumerGroupDetails.sort(Comparator.comparing(ConsumerGroupDetails::getGroupId));

            // Return immutable list.
            return Collections.unmodifiableList(consumerGroupDetails);
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns consumer offsets for requested consumer group id.
     * @param consumerGroupId id to retrieve offsets for.
     * @return ConsumerGroupOffsets
     */
    public ConsumerGroupOffsets getConsumerGroupOffsets(final String consumerGroupId) {
        try {
            // Make request
            final ListConsumerGroupOffsetsResult results = adminClient.listConsumerGroupOffsets(consumerGroupId);

            final List<PartitionOffset> offsetList = new ArrayList<>();
            String topic = null;

            // Iterate over results
            final Map<org.apache.kafka.common.TopicPartition, OffsetAndMetadata> partitionsToOffsets = results
                .partitionsToOffsetAndMetadata()
                .get();

            for (final Map.Entry<org.apache.kafka.common.TopicPartition, OffsetAndMetadata> entry : partitionsToOffsets.entrySet()) {
                offsetList.add(new PartitionOffset(
                    entry.getKey().partition(), entry.getValue().offset()
                ));
                if (topic == null) {
                    topic = entry.getKey().topic();
                }
            }

            return new ConsumerGroupOffsets(consumerGroupId, topic, offsetList);
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns consumer offsets for requested consumer group id along with the associated tail offset positions for
     * each of those partitions.
     *
     * @param consumerGroupId id to retrieve offsets for.
     * @return ConsumerGroupOffsets
     */
    public ConsumerGroupOffsetsWithTailPositions getConsumerGroupOffsetsWithTailOffsets(final String consumerGroupId) {
        final ConsumerGroupOffsets consumerGroupOffsets = getConsumerGroupOffsets(consumerGroupId);
        final TailOffsets tailOffsets = getTailOffsets(consumerGroupOffsets.getTopic(), consumerGroupOffsets.getPartitions());

        final List<PartitionOffsetWithTailPosition> offsetsWithPartitions = new ArrayList<>();

        for (final PartitionOffset entry : tailOffsets.getOffsets()) {
            offsetsWithPartitions.add(new PartitionOffsetWithTailPosition(
                entry.getPartition(),
                consumerGroupOffsets.getOffsetForPartition(entry.getPartition()),
                entry.getOffset()
            ));
        }

        return new ConsumerGroupOffsetsWithTailPositions(
            consumerGroupId,
            consumerGroupOffsets.getTopic(),
            offsetsWithPartitions,
            System.currentTimeMillis()
        );
    }

    /**
     * Given a topic name, return the tail offsets for all partitions on that topic.
     * @param topic Name of topic to get tail offsets for.
     * @return TailOffsets map.
     */
    public TailOffsets getTailOffsets(final String topic) {
        // Determine which partitions exist on the topic
        final TopicDetails topicDetails = getTopicDetails(topic);
        final List<Integer> partitions = topicDetails.getPartitions()
            .stream()
            .map(PartitionDetails::getPartition)
            .collect(Collectors.toList());

        // Call method for specific partitions.
        return getTailOffsets(topic, partitions);
    }

    /**
     * Given a topic name, return the tail offsets for the requested partitions.
     * @param topic Name of topic to get tail offsets for.
     * @param partitions Partitions to get tail offsets for.
     * @return TailOffsets map.
     */
    public TailOffsets getTailOffsets(final String topic, final Collection<Integer> partitions) {
        final Collection<org.apache.kafka.common.TopicPartition> topicPartitions = new ArrayList<>();
        partitions.forEach((partitionId) -> {
            topicPartitions.add(new org.apache.kafka.common.TopicPartition(topic, partitionId));
        });

        final Map<org.apache.kafka.common.TopicPartition, Long> offsets = consumerClient.endOffsets(topicPartitions);
        final Map<Integer, Long> offsetMap = new HashMap<>();
        offsets.forEach((key, value) -> {
            offsetMap.put(key.partition(), value);
        });

        return new TailOffsets(topic, offsetMap);
    }

    private List<ConfigItem> describeResource(final ConfigResource configResource) {
        final DescribeConfigsResult result = adminClient.describeConfigs(Collections.singleton(configResource));

        final List<ConfigItem> configItems = new ArrayList<>();

        try {
            final Map<ConfigResource, Config> configMap = result.all().get();

            final Config config = configMap.get(configResource);
            for (final ConfigEntry configEntry : config.entries()) {
                // Skip sensitive entries
                if (configEntry.isSensitive()) {
                    continue;
                }
                configItems.add(
                    new ConfigItem(configEntry.name(), configEntry.value(), configEntry.isDefault())
                );
            }
            return configItems;
        } catch (InterruptedException | ExecutionException e) {
            // TODO Handle this
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Close out the Client.
     */
    public void close() {
        adminClient.close();
        consumerClient.close();
    }
}
