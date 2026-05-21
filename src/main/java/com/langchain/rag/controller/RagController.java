package com.langchain.rag.controller;

import com.langchain.rag.assistant.RagAssistant;
import com.langchain.rag.model.ApiResponse;
import com.langchain.rag.model.QueryRequest;
import com.langchain.rag.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RagController {

    private final RagAssistant ragAssistant;
    private final IngestionService ingestionService;

    /**
     * POST
     *
     * Upload a document. Supported formats PDF, TXT, DOCX, HTML, MD.
     * The File is parsed, chunked, embedded locally, and stored in pgvector.
     */
    @PostMapping(
            value = "/ingest",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse> ingest(@RequestParam("file")MultipartFile file){
        if(file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No File provided or file is empty."));
        }

        log.info("POST /ingest - file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        try{
            String result = ingestionService.ingest(file);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IOException e) {
            log.error("Ingestion failed for {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Ingestion failed: " + e.getMessage()));
        }

    }

    /**
     * Ask a question. The API:
     * 1. Embeds your question (local MiniLM - no API Call)
     * 2. Retrieves top-5 relevant chunks from pgvector
     * 3. Sends chunks + question to GPT-40-minix
     * 4. Returns the grounded answer
     *
     * If the demo quota is exceeded, you'll get a 500 with an error message.
     * Wait a few minutes and try again
     */
    @PostMapping("/query")
    public ResponseEntity<ApiResponse> query(@RequestBody QueryRequest request){

        if(request.question() == null || request.question().isBlank()){
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Question cannot be empty."));
        }

        log.info("POST /query - question: {}", request.question());

        try {
            String answer = ragAssistant.answer(request.question());
            return ResponseEntity.ok(ApiResponse.ok(answer));
        } catch (Exception e){
            log.error("Query failed : {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Query Failed : " + e.getMessage()));
        }

    }


}
