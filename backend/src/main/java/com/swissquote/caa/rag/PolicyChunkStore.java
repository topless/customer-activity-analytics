package com.swissquote.caa.rag;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * pgvector-backed storage and similarity search for policy chunks. Plain JDBC because JPA
 * has no notion of the {@code vector} column type.
 */
@Repository
public class PolicyChunkStore {

    private final JdbcTemplate jdbc;

    public PolicyChunkStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> documentSha(String slug) {
        List<String> shas = jdbc.query(
            "select content_sha256 from policy_documents where slug = ?",
            (rs, i) -> rs.getString(1), slug);
        return shas.stream().findFirst();
    }

    public void deleteDocument(String slug) {
        jdbc.update("delete from policy_documents where slug = ?", slug);
    }

    public UUID insertDocument(String slug, String title, String sha256) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into policy_documents (document_id, slug, title, content_sha256, ingested_at)
                values (?, ?, ?, ?, ?)
                """,
            id, slug, title, sha256, java.sql.Timestamp.from(Instant.now()));
        return id;
    }

    public void insertChunk(UUID documentId, int chunkIndex, String sectionTitle, String content,
                            float[] embedding) {
        jdbc.update("""
                insert into policy_chunks (chunk_id, document_id, chunk_index, section_title, content, embedding)
                values (?, ?, ?, ?, ?, ?::vector)
                """,
            UUID.randomUUID(), documentId, chunkIndex, sectionTitle, content, toVectorLiteral(embedding));
    }

    /** Exact top-k nearest chunks by cosine distance ({@code <=>}). */
    public List<RetrievedChunk> topK(float[] queryEmbedding, int k) {
        String literal = toVectorLiteral(queryEmbedding);
        return jdbc.query("""
                select pc.chunk_id, pd.title, pc.section_title, pc.content,
                       1 - (pc.embedding <=> ?::vector) as similarity
                from policy_chunks pc
                join policy_documents pd on pd.document_id = pc.document_id
                order by pc.embedding <=> ?::vector
                limit ?
                """,
            (rs, i) -> new RetrievedChunk(
                rs.getObject("chunk_id", UUID.class),
                rs.getString("title"),
                rs.getString("section_title"),
                rs.getString("content"),
                rs.getDouble("similarity")),
            literal, literal, k);
    }

    public int chunkCount() {
        Integer count = jdbc.queryForObject("select count(*) from policy_chunks", Integer.class);
        return count == null ? 0 : count;
    }

    static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        return sb.append(']').toString();
    }
}
