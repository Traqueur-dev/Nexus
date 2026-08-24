# Nexus — Developer Guide

Personal event hub for developers: ingest events from external sources (Discord,
GitHub, schedulers…), persist them, and run user-defined workflows against them.

This file is the entry point for anyone — human or tooling — working on the
codebase. It describes how the project is built, how it is structured, and which
rules must hold. For the full architectural rationale, see
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Stack

| Concern | Choice |
|---|---|
| Language | Java 25 (Gradle toolchain) |
| Framework | Spring Boot 4.1.1 (Spring Framework 7) |
| Build | Gradle 9.7, Kotlin DSL, multi-module |
| Database | PostgreSQL 17 + Flyway migrations |
| JSON | Jackson 3 (`tools.jackson.*`) |
| Messaging | RabbitMQ 4 (topic exchange `nexus.events`) |
| Cache | Redis 7 (provisioned, not wired yet) |
| Tests | JUnit 6, Testcontainers 2, Awaitility |

Virtual threads are enabled (`spring.threads.virtual.enabled=true`).

---

## Prerequisites

- **JDK 25** — the Gradle toolchain requires it; the build fails on older JDKs.
- **Docker** — required both to run the app locally and to run the integration
  tests (Testcontainers starts real PostgreSQL and RabbitMQ containers).

---

## Common commands

```bash
# Start backing services (PostgreSQL, RabbitMQ, Redis)
docker compose up -d

# Run the application — activates the `dev` profile automatically
./gradlew bootRun

# Full build (compile + test)
./gradlew build

# Tests only
./gradlew test

# A single test class
./gradlew test --tests '*ConditionSerializationTest'

# One module
./gradlew :nexus-domain:test
```

`bootRun` is preconfigured with `--spring.profiles.active=dev`, which points the
app at the services declared in `compose.yml` (all credentials are `nexus`
/ `nexus` locally).

---

## Module layout

```
nexus-domain          Pure Java. Zero dependencies. Also the public plugin SDK.
nexus-application     Use cases + port interfaces (in and out).
nexus-infrastructure  Adapters, driving and driven: REST, JPA, RabbitMQ, mail,
                      serialization. One package per protocol.
nexus-plugin-loader   Discovery and lifecycle of external adapters (empty).
nexus-bootstrap       @SpringBootApplication, wiring, produces the runnable jar.
```

Packages mirror the modules: `fr.traqueur.nexus.<layer>`, and inside
`infrastructure` one package per technology: `rest`, `messaging`, `persistence`,
`serialization`, `mail`. The dependency rules below are enforced by the module
graph — a violation fails the build, it is not a review comment.
`./gradlew :nexus-domain:dependencies` prints `No dependencies`, which is the
contract in one line. What the graph cannot express — one adapter package
reaching into another, a missing type identifier — is checked by the ArchUnit
rules in `nexus-bootstrap/src/test/java/fr/traqueur/nexus/architecture`.

Tests live with the module they exercise. Anything that needs a Spring context
or Testcontainers lives in `nexus-bootstrap`, because exercising the assembly is
what that module is for.

---

## Architecture rules

These are invariants, not preferences. A change that breaks one of them should
not be merged.

1. **Dependencies point inward.** `domain` depends on nothing.
   `application` depends only on `domain`. `infrastructure` depends on
   `application` and `domain`, never the reverse.
2. **`nexus-domain` has zero third-party dependencies.** No Spring, no Jakarta,
   no Jackson. It ships to third-party plugin authors as-is; anything added
   there is imposed on every plugin.
3. **`nexus-application` carries no framework annotation.** No `@Service`, no
   `@Component`. Application beans are declared from `nexus-bootstrap`, so the
   layer can be driven by something other than Spring — a plugin embedding the
   workflow engine, a CLI, another framework. Spring belongs to the adapters and
   to bootstrap.
4. **The domain decides, adapters act.** Domain code is pure: same inputs, same
   outputs, no I/O. An `Action` is a *description* of an intent, never its
   execution.
5. **Crossing a boundary goes through a port.** `application` defines interfaces
   (`ports/in`, `ports/out`); `infrastructure` implements them. Application code
   must never import a JPA entity, a Spring Data repository, or a REST DTO.
6. **Adapters do not share types.** The RabbitMQ consumer and the REST
   controller must not depend on each other's DTOs. Shared ingestion contracts
   belong to `application` as commands. Since adapters now live in packages of
   one module rather than in separate ones, the compiler no longer enforces
   this — `AdapterIsolationTest` does.
7. **Domain types stay open.** Event, Context, Action and Condition hierarchies
   must remain extensible by external plugins — see ADR-001 in
   [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
8. **Internal adapters use the public SDK.** The bundled Discord and GitHub
   adapters get no privileged access a third-party plugin lacks.

---

## Domain vocabulary

- **Event** — something that happened, identified by an `Event.Id`
  (`<source>-<6 alphanumeric chars>`, e.g. `github-a3f9k2`). Immutable once
  ingested; the event store is append-only.
- **Context** — source-specific metadata attached to an event.
- **Workflow** — `events` + `condition` + `actions`: what to run, when.
- **Condition** — a composable predicate over an event (`equals`, `contains`,
  `composite`, `group`, `always`).
- **Action** — a *described intent* (send an email, call a webhook). It carries
  data only; execution happens in an adapter behind a port.
- **Adapter** — a source of events. Either bundled in this repo or supplied as
  an external plugin. Both use the same SDK.

Types are registered by annotation (`@EventMetadata`, `@ContextMetadata`,
`@ConditionMetadata`, `@ActionMetadata`) and resolved at runtime through
`Registry`, which maps a stable string type (`github.push_received`) to a class.
That string is a persisted contract: **renaming it breaks stored rows and
in-flight messages.**

Every open hierarchy has all four pieces: an annotation, a `Registry` bean in
`bootstrap/RegistriesConfig`, a `Core*` list declaring the built-in types, and
identifiers used wherever the type crosses a boundary. A hierarchy missing any
of them is incomplete — see ADR-006.

---

## Conventions

**Branches** — `<type>/<issue-number>-<slug>`, e.g.
`feature/6-workflow-persistence`, `refactor/19-hexagonal-architecture`.
Types: `feature`, `refactor`, `fix`, `chore`.

**Commits** — Conventional Commits with a scope matching the layer or module:
```
feat(domain): add condition serialization
fix(infrastructure): prevent silent event overwrite
```
Scopes in use: `domain`, `application`, `infrastructure`, `core`, `ci`, `docs`.

**Flow** — feature branches target `develop`; `develop` merges into `main` for
releases. Every branch is opened from an issue.

**Tests** — a test lives in the package of the code it exercises. Domain logic
is unit-tested without Spring; anything touching PostgreSQL or RabbitMQ uses
Testcontainers. The rules above are themselves tested, in
`fr.traqueur.nexus.architecture` — adding a layer, an adapter package or an open
hierarchy means adding a rule there.

---

## Known pitfalls

- **Gradle multi-module + Spring Boot**: the `org.springframework.boot` plugin is
  declared `apply false` at the root and applied only by the module producing the
  runnable application. A module that builds a boot jar cannot be consumed as a
  dependency by another.
- **The application's `main` must be `public`.** Java 25 lets the JVM launch a
  non-public one (JEP 512), so `bootRun` and the IDE work either way — but Spring
  Boot resolves the main class by scanning for a `public static main`, and
  `bootJar` fails without it. Test with `java -jar`, not only `bootRun`.
- **Build configuration lives in `build-logic/`**, applied as
  `nexus.java-conventions` / `nexus.spring-conventions`. Dependency versions live
  in `gradle/libs.versions.toml`. Neither belongs in a module's build file.
- **`Event.Id` collisions**: 6 base-36 characters is roughly 2.2 billion values,
  so collisions become likely well before a million events per source. Saving
  through `JpaRepository.save()` on an existing id issues an `UPDATE`, silently
  overwriting an event. Tracked in the refactor epic.
- **Reflection**: event and condition instances are rebuilt reflectively from
  their record components. Renaming a record component is a silent breaking
  change — nothing fails at compile time.
- **`spring.jpa.open-in-view=false`** is deliberate. Entities must be mapped to
  domain objects inside the transaction.
- **Jackson 3, not Jackson 2.** Spring Boot 4 wires Jackson 3
  (`tools.jackson.*`) into its message converters. Writing against Jackson 2
  (`com.fasterxml.jackson.databind.*`) compiles — the BOM manages both lines —
  but produces an `ObjectMapper` that is a *different object* from the one
  serializing HTTP responses, so mixins and custom serializers silently do not
  apply there. Annotations stay under `com.fasterxml.jackson.annotation`.
- **Testcontainers 2 renamed every module** with a `testcontainers-` prefix:
  `org.testcontainers:postgresql` is now
  `org.testcontainers:testcontainers-postgresql`.
- **Integration tests need Docker.** Without a running daemon they fail at
  startup rather than being skipped.
