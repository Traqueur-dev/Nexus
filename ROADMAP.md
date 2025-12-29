# Nexus Roadmap

## Legend

- ✅ Complete
- 🔄 In Progress
- ⏳ Planned

---

## Phase 0: Project Setup 🔄

### Repository
- [ ] Gradle multi-module setup
- [ ] `nexus-core` module
- [ ] GitHub Actions CI

### First Code
- [ ] Spring Boot 4 app with Virtual Threads
- [ ] `/health` endpoint
- [ ] First test

---

## Phase 1: Nexus Core ⏳

### Domain
- [ ] Event model (sealed interfaces, records)
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
