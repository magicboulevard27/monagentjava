package com.monagent.ai;

class StubIncidentAnalysisClient implements IncidentAnalysisClient {

    private final String response;

    StubIncidentAnalysisClient(String response) {
        this.response = response;
    }

    @Override
    public String analyze(String prompt) {
        return response;
    }
}
