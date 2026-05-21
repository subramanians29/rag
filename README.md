# RAG REST API

A Retrieval-Augmented Generation (RAG) REST API you can run entirely on your local machine — no cloud, no paid API keys.

## Stack

| Layer | What                                 | Why |
|---|--------------------------------------|---|
| Framework | Spring Boot 3.3 + LangChain4j 1.41.1 | Industry-standard Java AI stack |
| Chat LLM | GPT-4o-mini via LangChain4j demo key | Free, no signup needed |
| Embeddings | all-MiniLM-L6-v2 (local ONNX)        | Runs in JVM, zero API calls |
| Vector store | PostgreSQL + pgvector                | Real production-grade vector DB |
| Document parsing | LangChain4j Easy RAG                 | PDF, TXT, DOCX, HTML, MD support |

---

## Prerequisites

- Java 21+
- Maven
- Docker Desktop (for pgvector)

---

## Quick Start

### Step 1 — Start the database

```bash
docker-compose up -d
```

Verify it's running:

```bash
docker-compose ps
# postgres should show: healthy
```

### Step 2 — Run the application

```bash
./mvnw spring-boot:run
```

**First startup takes ~60 seconds** — the all-MiniLM-L6-v2 model (~90MB) is
downloaded once to your local Maven cache (`~/.m2`). Every startup after that
is instant.

You should see:

```
Started RagApiApplication in X.XXX seconds
```

### Step 3 — Upload a document

```bash
curl -X POST http://localhost:8080/api/v1/ingest \
     -F "file=@/path/to/any-document.pdf"
```

Works with: `.pdf`, `.txt`, `.docx`, `.html`, `.md`

Response:
```json
{
  "success": true,
  "message": "Ingested 'any-document.pdf' → 42 chunks stored in pgvector"
}
```

### Step 4 — Ask a question

```bash
curl -X POST http://localhost:8080/api/v1/query \
     -H "Content-Type: application/json" \
     -d '{"question": "What is the main topic of this document?"}'
```

Response:
```json
{
  "success": true,
  "message": "The document covers ..."
}
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/ingest` | Upload a document (multipart/form-data) |
| `POST` | `/api/v1/query` | Ask a question (JSON body) |
| `GET` | `/actuator/health` | Service health check |

---

## How It Works

```
INGEST FLOW
  File upload → Parse text (Easy RAG) → Split into 400-char chunks
  → Embed locally (MiniLM, no API) → Store vectors in pgvector

QUERY FLOW
  User question → Embed locally (MiniLM, no API)
  → Cosine similarity search in pgvector (top 5 chunks)
  → Build prompt: system + context + question
  → GPT-4o-mini via demo key → Return answer
```

---

## Troubleshooting

**Demo key quota exceeded**
> The demo key is rate-limited. If you see a 500 error mentioning quota,
> wait a few minutes and try again. For unlimited usage, get a free OpenAI
> key from platform.openai.com and update `application.yml`:
> ```yaml
> langchain4j:
>   open-ai:
>     chat-model:
>       base-url: https://api.openai.com/v1   # remove the demo base-url
>       api-key: sk-your-real-key-here
>       model-name: gpt-4o-mini
> ```

**pgvector connection refused**
> Make sure Docker is running and the container is healthy:
> ```bash
> docker-compose ps
> docker-compose up -d   # restart if stopped
> ```

**ONNX model download fails**
> The MiniLM model downloads from Maven Central on first run.
> If you're offline, try again when connected.

---

## Stopping

```bash
# Stop the app: Ctrl+C in the terminal running ./mvnw spring-boot:run

# Stop and remove the Docker container (keeps data)
docker-compose down

# Stop AND delete all stored embeddings (full reset)
docker-compose down -v
```

---

## Project Structure

```
src/main/java/com/subramanian/ragapi/
├── RagApiApplication.java          # Entry point
├── config/
│   ├── RagConfig.java              # Embedding model + pgvector + retriever beans
│   └── WebConfig.java              # CORS for local dev
├── assistant/
│   └── RagAssistant.java           # @AiService — LangChain4j AI interface
├── service/
│   └── IngestionService.java       # Parse → Chunk → Embed → Store
├── controller/
│   └── RagController.java          # REST endpoints
└── model/
    ├── QueryRequest.java
    └── ApiResponse.java
```
