package com.monagent.collection;

import com.monagent.collection.model.SourceSignal;

public interface SignalNormalizationFacade<T extends SourceSignal> {

    boolean supports(T signal);
}
