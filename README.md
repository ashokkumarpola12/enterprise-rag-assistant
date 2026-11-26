# Intelligent Incident and Knowledge Management System

Enterprise Retrieval-Augmented Generation (RAG) assistant that answers
incident questions from runbooks and knowledge base articles, with answers
grounded in the organization's own documents and cited to their sources.

## What it does

- Document ingestion and chunking from local files — each runbook is
  ingested from the configured directory via `POST /api/v1/documents/markdown/ingest`
  and stored in PostgreSQL. Chunking is section- and size-aware: the markdown
  pipeline splits on `## ` section headings and caps sections at 1000
  characters (150-character overlap). `MarkdownIngestionCoordinator`
  groups chunks by filename and ingests each document via
  `ingestMarkdownChunks`, which refuses a re-ingest of a file that is
  already in the database and stores chunk metadata (filename, title,
  section, chunk index) as JSONB.
- Embedding generation with nomic-embed-text (768-dim) via local Ollama — a separate step per document after ingest (`generateEmbeddingsForDocument`), since chunks are stored without embeddings and retrieval filters out chunks with no embedding
- Vector storage in PostgreSQL + pgvector
- Top-k semantic retrieval with configurable cosine-distance filtering,
  so weak chunks never reach the model
- Grounded answer generation with llama3.2:3b, returned as structured
  JSON with source references
- Honest refusal: when nothing passes the distance filter, it answers
  "no relevant information found" with an empty sources array — no LLM
  call, no invented fix

## Stack

Java 21, Spring Boot, Spring AI, PostgreSQL + pgvector, Ollama
(nomic-embed-text, llama3.2:3b). Fully local, $0 in API calls. Model
choices are configurable — add the related model dependency jar and
change configuration; hosted models can slot in later without rewiring.

## Run it

Prerequisites: Java 21, Docker, Ollama with `nomic-embed-text` and
`llama3.2:3b` pulled locally.

```bash
# 1. Database only — pgvector/pgvector:pg16, ragdb / raguser, host port 5433.
#    Set DB_PASSWORD first (env, or a gitignored .env in the project root);
#    Compose fails fast without it and no password is written in the files.
docker compose up -d
docker compose ps   # wait for healthy

# 2. App on the host (not containerised — it talks to Postgres at
#    localhost:5433 and Ollama at localhost:11434):
#    ./mvnw spring-boot:run   # app on http://localhost:8082

# 3. Ingest the runbooks, then generate embeddings (separate step per document):
#    curl -X POST http://localhost:8082/api/v1/documents/markdown/ingest
#    curl -X POST http://localhost:8082/api/v1/documents/markdown/<documentId>/embeddings
#    Save the filename -> UUID map the ingest call returns — search results cite UUIDs.
```

## Endpoints

- `GET /api/v1/search?query=...&topK=...` — retrieval-only: ranked chunks
  with content + cosine distance, no LLM call (topK default 3, max 20)
- `GET /api/v1/rag/ask?question=...` — grounded answer + structured sources
  (fixed top-3 retrieval, max-distance filter 0.40)
- `POST /api/v1/documents/markdown/ingest` — ingest all `*.md` runbooks
  from the configured directory via the Pipeline + Coordinator;
  returns filename → document UUID. Re-ingesting an existing file
  fails (dedup).
- `POST /api/v1/documents/markdown/{documentId}/embeddings` — generate
  embeddings for one ingested document; returns the count generated.
  A separate step after ingest — chunks are stored without
  embeddings until this runs.

Not in this push: `POST /api/v1/documents` (multipart file upload) is
planned but not built yet. Ingestion in this push runs from files
already on disk in the configured directory, triggered by the
endpoints above.

## Measured results — smoke test, Tue Nov 24, 2026, after the metadata filter (10 seed questions, 4 runbooks)

- Smoke test, 10 seed questions over 4 fictional runbooks:
  correct citations 10/10, grounded answers 10/10,
  median latency 3150 ms.
- Expected chunk in top 5: 10/10.
- Baseline (Sun Nov 22, before the filter) was also 10/10 citations
  and 10/10 grounded, median 4445 ms. The filter changed ranks 2–3
  only: the Document Metadata chunk no longer occupies a top-3 slot
  (it did in 7/10 baseline questions), and the freed slots went to
  Resolution (Q4, Q7) and Prevention (Q10).

## Demo data

- `seed-content/runbooks/` — 4 fictional runbooks (payments API latency,
  auth login failures, Postgres replication lag, AKS CrashLoopBackOff).
- `seed-content/servicenow-incidents/` — 10 fictional resolved incidents
  (INC0001001–INC0001010), ServiceNow-shaped, as a second corpus.
  Ingest only after the runbook smoke test.
- All demo data is fictional. No real employer records, no real
  ServiceNow/Confluence connection — ingestion is a document pipeline
  today, and the incident fields are document metadata, not a live
  integration.

## What it does not do

- No live ServiceNow or Confluence connection.
- It retrieves past resolutions; it does not learn from history beyond
  retrieval, and there is no auto-triage yet.
- No hybrid search or conversation history yet — both are Phase 2,
  conditional on eval evidence.

## Roadmap

- Phase 2: reranking + evals (30–50 question golden set), threshold
  tuning from measured scores. Section-aware reranking is a Phase 2
  option if evals show ranking failures after the corpus grows; the
  metadata filter ships first so the before/after comparison stays clean.
- Phase 3 (planned): Python/LangGraph multi-agent incident response
  layer over this Java core — supervisor + triage + diagnosis +
  resolution + verification agents with human-in-the-loop approval,
  MCP servers for runbook search, incident store, and service status,
  LangSmith tracing + evals. Java stays the source of truth for
  ingestion, chunking, embeddings and retrieval.
