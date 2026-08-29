package com.swissquote.caa.rag;

import java.util.List;
import org.springframework.stereotype.Service;
import com.swissquote.caa.config.AppProperties;

@Service
public class PolicyRetrievalService {

    private final PolicyChunkStore store;
    private final EmbeddingModel embeddingModel;
    private final AppProperties properties;

    public PolicyRetrievalService(PolicyChunkStore store, EmbeddingModel embeddingModel,
                                  AppProperties properties) {
        this.store = store;
        this.embeddingModel = embeddingModel;
        this.properties = properties;
    }

    public List<RetrievedChunk> retrieve(String query) {
        return store.topK(embeddingModel.embed(query), properties.rag().topK());
    }
}
