package com.monagent.ai;

public class StubIncidentAnalysisClient implements IncidentAnalysisClient {

    private final String response;

    public StubIncidentAnalysisClient(String response) {
        this.response = response;
    }

    @Override
    public String analyze(String prompt) {
        return response;
    }
}
