package com.swissquote.caa.analysis.llm;

import java.util.List;
import com.swissquote.caa.analysis.ActivityDigest;
import com.swissquote.caa.rag.RetrievedChunk;

/**
 * Port to the analysis model. Adapters receive both the rendered prompts (used by real LLM
 * providers) and the structured context (used by the deterministic stub), and return the raw
 * model output, which is expected to be a single JSON object matching
 * {@link LlmAnalysisResponse}.
 */
public interface LlmClient {

    String modelId();

    String complete(LlmRequest request);

    record LlmRequest(String systemPrompt, String userPrompt, ActivityDigest digest,
                      List<RetrievedChunk> retrievedChunks) {
    }
}
