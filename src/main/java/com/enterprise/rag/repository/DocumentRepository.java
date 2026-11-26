
package com.enterprise.rag.repository;

import com.enterprise.rag.dto.SemanticSearchResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;


import java.util.UUID;

@Repository
public class DocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveDocument(
            UUID documentId,
            String filename,
            String contentType
    ) {

        String sql = """
                INSERT INTO documents
                (id, filename, content_type, status)
                VALUES (?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                documentId,
                filename,
                contentType,
                "PENDING"
        );
    }


    public void saveChunk(
            UUID chunkId,
            UUID documentId,
            int chunkIndex,
            String content,
            String metadataJson
    ) {

        String sql = """
            INSERT INTO document_chunks
            (id, document_id, chunk_index, content,metadata)
            VALUES (?, ?, ?, ?,CAST(? AS jsonb))
            """;

        int insertedRows = jdbcTemplate.update(
                sql,
                chunkId,
                documentId,
                chunkIndex,
                content,
                metadataJson
        );
        if (insertedRows != 1) {
            throw new IllegalStateException(
                    "Expected to insert exactly one chunk: " + chunkId
            );
        }
    }

    public boolean existsByFilenameAndContentType(
            String filename,
            String contentType
    ) {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM documents
                WHERE filename = ?
                  AND content_type = ?
            )
            """;

        Boolean exists = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                filename,
                contentType
        );

        return Boolean.TRUE.equals(exists);
    }


    public void updateChunkEmbedding(
            UUID chunkId,
            float[] embedding
    ) {

        if (embedding == null || embedding.length != 768) {
            throw new IllegalArgumentException(
                    "Embedding must contain exactly 768 dimensions"
            );
        }

        StringBuilder vector = new StringBuilder("[");

        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                vector.append(",");
            }
            vector.append(embedding[i]);
        }

        vector.append("]");

        String sql = """
            UPDATE document_chunks
            SET embedding = CAST(? AS vector)
            WHERE id = ?
            """;

        int updatedRows = jdbcTemplate.update(
                sql,
                vector.toString(),
                chunkId
        );

        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Expected to update exactly one chunk: " + chunkId
            );
        }
    }


    public List<Map<String, Object>> findChunksWithoutEmbeddings(
            UUID documentId
    ) {

        String sql = """
            SELECT id, chunk_index, content
            FROM document_chunks
            WHERE document_id = ?
              AND embedding IS NULL
            ORDER BY chunk_index
            """;

        return jdbcTemplate.queryForList(sql, documentId);
    }

    public List<SemanticSearchResponse> searchSimilarChunks(
            float[] queryEmbedding,
            int topK
    ){
        if (queryEmbedding == null || queryEmbedding.length != 768) {
            throw new IllegalArgumentException("Query embedding must contain exactly 768 dimensions");
        }
        if(topK <=0){
            throw new IllegalArgumentException("Top K must be greater than 0");
        }
        StringBuilder vector = new StringBuilder("[");

        for (int i = 0; i < queryEmbedding.length; i++) {
            if (i > 0) {
                vector.append(",");
            }
            vector.append(queryEmbedding[i]);
        }
        vector.append("]");
        String sql =  """
            SELECT
                id,
                document_id,
                chunk_index,
                content,
                embedding <=> CAST(? AS vector) AS distance,
                metadata->>'filename' AS filename,
                metadata->>'documentTitle' AS document_title,
                metadata->>'sectionHeading' AS section_heading
            FROM document_chunks
            WHERE embedding IS NOT NULL
                            AND metadata->>'sectionHeading'
                          IS DISTINCT FROM 'Document Metadata'
            ORDER BY embedding <=> CAST(? AS vector)
            LIMIT ?
            """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SemanticSearchResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("document_id", UUID.class),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        rs.getDouble("distance"),
                        rs.getString("filename"),
                        rs.getString("document_title"),
                        rs.getString("section_heading")
                ),
                vector.toString(),
                vector.toString(),
                topK
        );
    }

    public int updateDocumentStatus(UUID documentId, String status) {
        String sql = """
            UPDATE documents
            SET status = ?,
                updated_at = NOW()
            WHERE id = ?
            """;

        return jdbcTemplate.update(sql, status, documentId);
    }


    public boolean areAllChunksEmbedded(UUID documentId) {
        String sql = """
            SELECT
                COUNT(*) > 0
                AND COUNT(*) FILTER (WHERE embedding IS NULL) = 0
            FROM document_chunks
            WHERE document_id = ?
            """;

        Boolean result = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                documentId
        );

        return Boolean.TRUE.equals(result);
    }

}

