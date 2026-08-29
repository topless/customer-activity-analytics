package com.swissquote.caa.rag;

import java.util.UUID;

public record RetrievedChunk(UUID chunkId, String documentTitle, String sectionTitle,
                             String content, double similarity) {
}
