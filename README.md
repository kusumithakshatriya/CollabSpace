# CollabSpace — AI-powered collaborative workspace

A runnable educational starter project for a unified team workspace. It includes a Spring Boot / Java 17 backend, responsive browser UI, REST APIs, WebSocket broadcast events, and CI/CD deployment examples.

## Quick start (recommended: Docker)

**Prerequisite:** Docker Desktop installed and running.

1. Extract the ZIP.
2. Open a terminal in the extracted `CollabSpace` folder.
3. Run:
   ```bash
   docker compose up --build
   ```
4. Open **http://localhost:8080**.
5. Sign in using one of the demo accounts below.

To stop it, press `Ctrl+C` and run `docker compose down`.

## Run without Docker

Install **JDK 17** and **Maven 3.8+**. From the project folder:

- Windows: double-click `run.bat` or run `mvn spring-boot:run`.
- macOS/Linux: run `chmod +x run.sh && ./run.sh` or `mvn spring-boot:run`.

Then open http://localhost:8080.

## Demo logins

All demo passwords are `collab123`.

| Username | Role | Display name |
|---|---|---|
| `admin` | Workspace admin | Alex Morgan |
| `maya` | Member | Maya Chen |
| `sam` | Member | Sam Patel |

This is demo authentication for local evaluation, not production-grade identity management. Do not expose this sample to the public internet.

## Included functionality

- **Overview dashboard:** task, completion, message and document counts; recent activity; quick links.
- **Sign-in and roles:** demo accounts with admin/member roles and token-based API requests.
- **Team chat:** shared general conversation; WebSocket broadcast updates across open clients.
- **Task management:** create tasks, assign a teammate, set priority, and change status.
- **Code room:** edit and save shared code snippets in several languages.
- **Whiteboard:** add colored sticky-note ideas to a shared board.
- **Documents:** create and read shared project documents.
- **AI assistant demo:** local assistant provides workspace summaries and practical code/debugging guidance without an external API key.
- **Container and deployment:** Dockerfile, Docker Compose, Kubernetes Deployment/Service and Jenkins pipeline.
- **Health check:** `GET /api/health`.

## Important implementation notes

This is a **working MVP / college-project baseline**, not a production SaaS. The current in-memory sample data resets when the application restarts; the H2 dependency is included for extension, but the demo controller currently uses in-memory collections. WebSocket messages broadcast refresh notifications/events; the code room is shared-snippet storage, not full multi-cursor IDE editing. The whiteboard currently provides shared sticky notes rather than arbitrary drawing tools. The AI assistant is rule-based and does not claim to run an LLM. These boundaries are documented so you can present the implementation honestly.

Before production, add persistent JPA storage and migrations, secure password hashing and session/JWT validation, workspace-level membership/authorization on every endpoint, rate limiting, audit logging, file upload scanning, HTTPS, backups, and automated tests. To use a hosted LLM, implement a server-side provider adapter and keep API keys in environment variables, never in browser JavaScript.

## CI/CD: Git, GitHub, Maven, Jenkins, Docker, Kubernetes

1. Create a GitHub repository and push this folder:
   ```bash
   git init
   git add .
   git commit -m "Initial CollabSpace implementation"
   git branch -M main
   git remote add origin https://github.com/YOUR-USERNAME/YOUR-REPOSITORY.git
   git push -u origin main
   ```
2. Install/configure Jenkins with JDK 17, Maven 3 and Docker available to the Jenkins agent.
3. Create a Jenkins Pipeline job pointing at the GitHub repository and select `Jenkinsfile`.
4. Build locally with `mvn clean package`; build the image with `docker build -t collabspace:latest .`.
5. For Kubernetes, make sure the cluster can access the image. If using a local cluster such as Minikube, build/load the image into that cluster first. Apply manifests with `kubectl apply -f k8s/` and check `kubectl get pods,svc`.
6. The Jenkins deploy stage assumes the Jenkins agent is already configured with cluster credentials and can access the image. For a remote cluster, push to a container registry and update the image name in the pipeline and `k8s/deployment.yaml`.

## Main API routes

- `POST /api/login` — JSON `{ "username": "admin", "password": "collab123" }`
- `GET /api/me`, `GET /api/dashboard`
- `GET/POST /api/tasks`, `PATCH /api/tasks/{id}`
- `GET/POST /api/messages`
- `GET/POST /api/documents`
- `GET/POST /api/whiteboard`
- `GET/POST /api/code`
- `POST /api/assistant` — JSON `{ "prompt": "Summarize our workspace" }`
- `GET /api/health`

Protected requests use `X-Auth-Token`, returned by login.

## Suggested report structure

1. Abstract and problem statement
2. Objectives and scope
3. Requirements and system architecture
4. Technology stack and implementation
5. Screenshots and module descriptions
6. API and WebSocket design
7. Testing and sample results
8. CI/CD pipeline (Git → GitHub → Maven → Jenkins → Docker → Kubernetes)
9. Security considerations and limitations
10. Conclusion and future enhancements
