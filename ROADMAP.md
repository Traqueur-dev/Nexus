# Nexus Roadmap

## Legend

- ✅ Complete
- 🔄 In Progress
- ⏳ Planned

---

## Phase 0: Project Setup ✅

### Repository
- [X] Gradle multi-module setup — version catalog and convention plugins in
      `build-logic/`
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
- [X] Polymorphic JSON serialization — one registry-driven mechanism for
      contexts, conditions and actions (ADR-009)
- [ ] Workflow persistence — the port exists and is answered by an in-memory
      adapter; nothing survives a restart yet
- [ ] Redis cache — container provisioned, not wired

### API
- [X] REST: read an event by id
- [ ] REST: list and filter events (pagination)
- [ ] REST endpoints for workflows
- [ ] WebSocket real-time stream

---

## Phase 1.5: Architecture Refactor ✅

Prerequisite for Phase 2: the previous design could not host third-party
adapters. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

- [X] Open the domain type hierarchies (remove `sealed`)
- [X] Introduce ports and adapters
- [X] Workflow engine behind an `ActionHandler` port
- [X] Build foundation: version catalog + convention plugins
- [X] Split into Gradle modules (domain / application / infrastructure /
      plugin-loader / bootstrap) — driving adapters live in `infrastructure`,
      there is no `api` module (ADR-007)
- [X] ArchUnit rules to prevent regression — layering, adapter isolation, and
      open hierarchies (ADR-006, ADR-009)

---

## Phase 2: Adapters ⏳

- [ ] Adapter SDK (= `nexus-domain` + lifecycle interface)
- [ ] Plugin loader (discovery, classloading, versioning)
- [ ] Dynamic queue provisioning (adapters auto-register at runtime)
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
