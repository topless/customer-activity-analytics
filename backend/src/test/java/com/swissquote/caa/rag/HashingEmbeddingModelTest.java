package com.swissquote.caa.rag;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import com.swissquote.caa.config.AppProperties;
import static org.assertj.core.api.Assertions.assertThat;

class HashingEmbeddingModelTest {

    private final HashingEmbeddingModel model = new HashingEmbeddingModel(
        new AppProperties(new AppProperties.Jwt("secret", Duration.ofHours(1)), null,
            new AppProperties.Rag(4, 384), new AppProperties.Analysis(40)));

    @Test
    void isDeterministicAndUnitLength() {
        float[] a = model.embed("high-value cross-border payment to a foreign bank");
        float[] b = model.embed("high-value cross-border payment to a foreign bank");
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSize(384);
        assertThat(norm(a)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-4));
    }

    @Test
    void topicallySimilarTextScoresHigherThanUnrelatedText() {
        float[] query = model.embed("structuring repeated sub-threshold payments 7-day window");
        float[] onTopic = model.embed(
            "Structuring means splitting a transfer into smaller payments below a threshold; "
                + "three or more payments within a rolling 7-day window are indicative.");
        float[] offTopic = model.embed(
            "Card-not-present transactions carry higher fraud risk because no chip or PIN is verified.");
        assertThat(cosine(query, onTopic)).isGreaterThan(cosine(query, offTopic));
    }

    @Test
    void emptyTextYieldsZeroVector() {
        assertThat(norm(model.embed(""))).isZero();
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    private static double norm(float[] v) {
        return Math.sqrt(cosine(v, v));
    }
}
