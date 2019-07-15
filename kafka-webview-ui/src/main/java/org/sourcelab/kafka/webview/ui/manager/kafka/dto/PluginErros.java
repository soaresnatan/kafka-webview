package org.sourcelab.kafka.webview.ui.manager.kafka.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginErros {
    private Map<String, List<String>> mapErrors;

    public PluginErros() {
        mapErrors = new HashMap<>();
    }

    public void addError(String error, String message) {
        if (!mapErrors.containsKey(error))
            mapErrors.put(error, new ArrayList<>());

        mapErrors.get(error).add(message);
    }

    public List<String> getErrors(String error) {
        return mapErrors.get(error);
    }

    public List<String> getListErros() {
        List<String> response = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : mapErrors.entrySet()) {
            response.add(entry.getKey());
        }
        return response;
    }

    public boolean haveErrors() {
        return mapErrors.size() > 0 ? true : false;
    }


}
