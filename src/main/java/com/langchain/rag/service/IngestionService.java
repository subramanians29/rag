package com.langchain.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * Chunk size in characters
     * 400 chars ~80 words - small enough for precise retrieval,
     * large enough to carry meaningful context
     */
    private static final int CHUNK_SIZE = 400;

    /**
     * Overlap between adjacent chunks, in characters.
     * Prevents answers from being split across chunk boundries.
     */
    private static final int CHUNK_OVERLAP = 50;

    /**
     * Ingest a file:
     *    1. Save Multipart File to a temp path (loaders need a Path not a stream)
     *    2. Load Document - auto-detects PDF, TXT, DOCX, HTML, MD
     *    3. Split intooverlapping chunks
     *    4. Embed all chunks (local MiniLM)
     *    5. Store vectors in pgvector
     */
    public String ingest(MultipartFile file) throws IOException {

        String filename = file.getOriginalFilename();
        log.info("Ingesting: {}", filename);

        // Write to a temp file - langchain4j's FileSystemDocumentLoader needs a Path
        Path tempFile = Files.createTempFile("rag-", "-" + filename);

        try {
            file.transferTo(tempFile.toFile());

            Document document = FileSystemDocumentLoader.loadDocument(tempFile);

            // Recursive splitter : tries paragraph -> sentence -> word -> char boundaries
            // before hard-splitting, so chunks are semantically cleaner
            DocumentSplitter splitter = DocumentSplitters.recursive(
                    CHUNK_SIZE, CHUNK_OVERLAP
            );
            List<TextSegment> segments = splitter.split(document);
            log.info(
                    "Split '{}' into chunks (size = {}, overlap = {})",
                    filename, segments.size(), CHUNK_SIZE, CHUNK_OVERLAP
            );

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

            embeddingStore.addAll(embeddings, segments);
            log.info(
                    "Stored {} vectors for '{}'",
                    segments.size(), filename
            );

            return String.format(
                    "Ingested '%s' -> %d chunks stored in pgvector", filename, segments.size()
            );

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
