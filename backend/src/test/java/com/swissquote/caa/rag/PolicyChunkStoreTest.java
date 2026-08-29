package com.swissquote.caa.rag;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PolicyChunkStoreTest {

    @Test
    void vectorLiteralMatchesPgvectorSyntax() {
        assertThat(PolicyChunkStore.toVectorLiteral(new float[]{0.5f, -1.0f, 0.0f}))
            .isEqualTo("[0.5,-1.0,0.0]");
    }
}
