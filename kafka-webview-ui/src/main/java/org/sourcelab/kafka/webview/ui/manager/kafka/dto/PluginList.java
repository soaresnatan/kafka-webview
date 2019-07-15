package org.sourcelab.kafka.webview.ui.manager.kafka.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PluginList {
    private List<PluginDetails> plugins;

    /**
     * Constructor.
     */
    public PluginList(List<PluginDetails> plugins) {
        final List<PluginDetails> sortedList = new ArrayList<>();
        sortedList.addAll(plugins);
        Collections.sort(sortedList, Comparator.comparing(PluginDetails::getNamePlugin));

        this.plugins = Collections.unmodifiableList(sortedList);
    }

    /**
     * @return a List of topics.
     */
    public List<PluginDetails> getPlugins() {
        return plugins;
    }

    /**
     * @return a List of the topic names.
     */
    public List<String> getPluginsNames() {
        final List<String> pluginNames = new ArrayList<>();
        for (final PluginDetails pluginDetail : getPlugins()) {
            pluginNames.add(pluginDetail.getNamePlugin());
        }
        return Collections.unmodifiableList(pluginNames);
    }

    public PluginDetails getPlugin(String plugin) {
        return this.plugins.stream().filter(pluginT -> pluginT.getNameClass().equals(plugin)).findAny().orElse(null);
    }

    @Override
    public String toString() {
        return "PluginDetail{"
                + "+ plugins=" + plugins
                + '}';
    }
}
