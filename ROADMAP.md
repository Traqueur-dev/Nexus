# Nexus Roadmap

## Legend

- ✅ Complete
- 🔄 In Progress
- ⏳ Planned

---

## Phase 0: Project Setup 🔄

### Repository
- [X] Gradle multi-module setup
- [X] `nexus-core` module
- [X] GitHub Actions CI

### First Code
- [X] Spring Boot 4 app with Virtual Threads
- [X] `/health` endpoint
- [X] First test

---

## Phase 1: Nexus Core ⏳

### Domain
- [X] Event model (sealed interfaces, records)
- [ ] Workflow model (Trigger, Condition, Action)

### Infrastructure
- [ ] PostgreSQL + Flyway
- [ ] RabbitMQ integration
- [ ] Redis cache

### API
- [ ] REST endpoints (events, workflows)
- [ ] WebSocket real-time stream

---

## Phase 2: Adapters ⏳

- [ ] Adapter SDK
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

---

## Phase 6: Plugin Architecture ⏳

- [ ] Open Event/Context interfaces (non-sealed level 1)
- [ ] Plugin JAR loading system
- [ ] Hot-reload via endpoint
- [ ] Plugin lifecycle management
- [ ] Sealed interfaces at level 2 (per plugin)