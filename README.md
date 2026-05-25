# What's and Why's
I have experience with building models with python during my master's, but since i wanted to focus more on using the models with existing enterprise solutions, 
i wanted to build a program which provides answers user queries based on the documents that were ingested in java, two popular framework options were spring ai and langchain4j, Both were interesting,
but i found langchain4j to be more easy to configure, spring ai has a lot of abstractions which is good if i just want to connect 
to a llm framework and was already working in a spring heavy platform, but since this is a simple learning project i decided to 
go with langchain4j. I chose pgvector over standalone vector databases like Pinecone because this project runs entirely locally — pgvector runs inside the existing PostgreSQL container with no external service or API key needed. For a production system handling millions of embeddings I would evaluate a dedicated vector database, but for a self-contained demo pgvector is the pragmatic choice.

Had the Help of Claude sonnet 4.6 in writing this, for exploring different options in langchain4j, as i am trying to explore more on
AI Assisted(or AI centric) Coding.

Initially my prompt was to draft a high level plan for this project which uses everything locally in a simple way, 
and i did no other changes till it gave me the full working code. 

I rewrote the code (Copied it manually line by line) to understand what the ai had given me, and i didn't feel the need to change anything.

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
