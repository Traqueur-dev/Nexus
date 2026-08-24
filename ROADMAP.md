# Nexus Roadmap

## Legend

- ✅ Complete
- 🔄 In Progress
- ⏳ Planned

---

## Phase 0: Project Setup ✅

### Repository
- [X] Gradle multi-module setup
- [X] `nexus-core` module
- [X] GitHub Actions CI

### First Code
- [X] Spring Boot 4 app with Virtual Threads
- [ ] Health endpoint — the initial custom controller was removed; to be
      replaced by Spring Boot Actuator, which also provides PostgreSQL and
      RabbitMQ health indicators (needed for Phase 4 probes)
- [X] First test

---

## Phase 1: Nexus Core 🔄

### Domain
- [X] Event model (records, annotation-based type registry)
- [X] Condition model (`equals`, `contains`, `composite`, `group`, `always`)
- [X] Action model — descriptive only, no execution yet
- [X] Workflow behaviour (`matches()`, invariants)
- [X] Workflow execution engine (behind the `ActionHandler` port)

### Infrastructure
- [X] PostgreSQL + Flyway
- [X] RabbitMQ integration
- [X] Polymorphic JSON serialization (events, conditions)
- [ ] Redis cache — container provisioned, not wired

### API
- [X] REST: read an event by id
- [ ] REST: list and filter events (pagination)
- [ ] REST endpoints for workflows
- [ ] WebSocket real-time stream

---

## Phase 1.5: Architecture Refactor 🔄

Prerequisite for Phase 2: the current design cannot host third-party adapters.
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

- [X] Open the domain type hierarchies (remove `sealed`)
- [X] Introduce ports and adapters
- [X] Workflow engine behind an `ActionHandler` port
- [ ] Build foundation: version catalog + convention plugins
- [ ] Split into Gradle modules (domain / application / api / infrastructure /
      plugin-loader / bootstrap)
- [ ] ArchUnit rules to prevent regression

---

## Phase 2: Adapters ⏳

- [ ] Adapter SDK (= `nexus-domain` + lifecycle interface)
- [ ] Plugin loader (discovery, classloading, versioning)
- [ ] Discord adapter
- [ ] GitHub adapter
- [ ] Minecraft adapter

---

## Phase 3: Dashboard ⏳

- [ ] Next.js setup
- [ ] Event timeline
- [ ] Workflow editor

---

## Phase 4: Cloud Deployment ⏳

- [ ] Docker images
- [ ] Kubernetes manifests
- [ ] CI/CD pipeline

---

## Phase 5: Advanced ⏳

- [ ] Spring AI integration
- [ ] Event replay
- [ ] Monitoring (Prometheus/Grafana)
