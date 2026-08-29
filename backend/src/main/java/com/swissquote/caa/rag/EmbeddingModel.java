package com.swissquote.caa.rag;

/**
 * Maps text to a fixed-dimension vector. The default implementation is a deterministic,
 * offline feature-hashing model; a hosted embedding provider (e.g. Voyage) could be dropped
 * in behind this interface without touching the rest of the RAG pipeline.
 */
public interface EmbeddingModel {

    float[] embed(String text);

    int dimension();
}
