package com.swissquote.caa.rag;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ingests the unstructured policy corpus (classpath:policies/*.md) into the vector store at
 * startup: one chunk per markdown "## " section, embedded with the configured model.
 * Idempotent — a document is re-ingested only when its content hash changed.
 */
@Service
public class PolicyIngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicyIngestionService.class);
    private static final int MAX_CHUNK_CHARS = 2500;

    private final PolicyChunkStore store;
    private final EmbeddingModel embeddingModel;
    private final TransactionTemplate transactionTemplate;

    public PolicyIngestionService(PolicyChunkStore store, EmbeddingModel embeddingModel,
                                  TransactionTemplate transactionTemplate) {
        this.store = store;
        this.embeddingModel = embeddingModel;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:policies/*.md");
        for (Resource resource : resources) {
            // delete + reinsert of one document is atomic
            transactionTemplate.executeWithoutResult(tx -> ingest(resource));
        }
        log.info("Policy corpus ready: {} documents, {} chunks", resources.length, store.chunkCount());
    }

    private void ingest(Resource resource) {
        String filename = resource.getFilename();
        String slug = filename == null ? "unknown" : filename.replaceFirst("\\.md$", "");
        String content = readContent(resource);
        String sha = sha256(content);

        if (store.documentSha(slug).filter(sha::equals).isPresent()) {
            return;
        }
        store.deleteDocument(slug);

        String title = content.lines()
            .filter(l -> l.startsWith("# "))
            .findFirst()
            .map(l -> l.substring(2).trim())
            .orElse(slug);
        UUID documentId = store.insertDocument(slug, title, sha);

        int index = 0;
        for (Section section : splitSections(content)) {
            for (String piece : splitOversized(section.body())) {
                String chunkText = (section.title() == null ? title : section.title()) + "\n\n" + piece;
                store.insertChunk(documentId, index++, section.title(), chunkText,
                    embeddingModel.embed(chunkText));
            }
        }
        log.info("Ingested policy document '{}' ({} chunks)", title, index);
    }

    private record Section(String title, String body) {
    }

    /** Splits markdown into sections on "## " headings; the preamble becomes its own section. */
    private static List<Section> splitSections(String content) {
        List<Section> sections = new ArrayList<>();
        String currentTitle = null;
        StringBuilder currentBody = new StringBuilder();
        for (String line : content.split("\n", -1)) {
            if (line.startsWith("## ")) {
                addSection(sections, currentTitle, currentBody);
                currentTitle = line.substring(3).trim();
                currentBody = new StringBuilder();
            } else if (!line.startsWith("# ")) {
                currentBody.append(line).append('\n');
            }
        }
        addSection(sections, currentTitle, currentBody);
        return sections;
    }

    private static void addSection(List<Section> sections, String title, StringBuilder body) {
        String text = body.toString().strip();
        if (!text.isEmpty()) {
            sections.add(new Section(title, text));
        }
    }

    /** Splits a very long section body on paragraph boundaries. */
    private static List<String> splitOversized(String body) {
        if (body.length() <= MAX_CHUNK_CHARS) {
            return List.of(body);
        }
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : body.split("\n\n")) {
            if (current.length() + paragraph.length() > MAX_CHUNK_CHARS && current.length() > 0) {
                pieces.add(current.toString().strip());
                current = new StringBuilder();
            }
            current.append(paragraph).append("\n\n");
        }
        if (!current.toString().isBlank()) {
            pieces.add(current.toString().strip());
        }
        return pieces;
    }

    private static String readContent(Resource resource) {
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
