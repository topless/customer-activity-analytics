package com.swissquote.caa.rag;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.swissquote.caa.config.AppProperties;

/**
 * Deterministic bag-of-words feature-hashing embedding ("hashing trick"): each token is
 * hashed to a bucket with a pseudo-random sign, weighted 1+log(tf), then L2-normalised.
 * Cosine similarity over these vectors approximates weighted token overlap, which is a
 * reasonable retrieval signal for a small, domain-specific policy corpus — and it works
 * offline with zero dependencies, keeping the whole RAG pipeline runnable in the demo.
 */
@Component
public class HashingEmbeddingModel implements EmbeddingModel {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private static final Set<String> STOPWORDS = Set.of(
        "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "with", "by", "is",
        "are", "be", "as", "at", "that", "this", "it", "from", "any", "all", "not", "no",
        "if", "when", "than", "then", "was", "were", "has", "have", "had", "must",
        "should", "may", "can", "will", "shall", "its", "their", "such", "these", "those");

    private final int dimension;

    public HashingEmbeddingModel(AppProperties properties) {
        this.dimension = properties.rag().embeddingDim();
    }

    @Override
    public float[] embed(String text) {
        Map<String, Integer> termFrequencies = new HashMap<>();
        for (String token : NON_ALNUM.split(text.toLowerCase(Locale.ROOT))) {
            if (token.length() < 2 || STOPWORDS.contains(token)) {
                continue;
            }
            termFrequencies.merge(token, 1, Integer::sum);
        }

        float[] vector = new float[dimension];
        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            int h = entry.getKey().hashCode(); // String.hashCode is spec-defined => stable
            int bucket = Math.floorMod(h, dimension);
            int sign = (Integer.rotateLeft(h * 0x9E3779B1, 13) & 1) == 0 ? 1 : -1;
            vector[bucket] += sign * (1.0f + (float) Math.log(entry.getValue()));
        }

        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        if (norm > 0) {
            float inv = (float) (1.0 / Math.sqrt(norm));
            for (int i = 0; i < dimension; i++) {
                vector[i] *= inv;
            }
        }
        return vector;
    }

    @Override
    public int dimension() {
        return dimension;
    }
}
