# Nexus Architecture

Reference document for the hexagonal restructuring tracked in the refactor epic.
It records the target shape, the rules that enforce it, and the reasoning behind
the decisions that are hard to reverse later.

---

## 1. Why hexagonal here

Nexus is an event hub whose whole purpose is to sit between systems it does not
control: Discord, GitHub, Minecraft, SMTP, webhooks. The set of those systems is
expected to grow, and part of it is expected to be written by other people as
plugins.

That makes the cost of coupling business rules to any one of those systems
unusually high. A ports-and-adapters architecture keeps a single answer to the
question *"what does Nexus actually do?"* independent of *"what is it plugged
into today?"*.

The secondary benefit matters just as much day to day: the workflow engine —
matching events against conditions and producing actions — is pure computation.
Kept free of infrastructure, it is testable in milliseconds without Docker.

---

## 2. Principles

1. **Dependencies point inward.** Outer layers know inner layers. Never the
   reverse.
2. **The domain decides, adapters act.** Domain code computes *what should
   happen*. Performing it — network, disk, SMTP — is an adapter's job.
3. **Every boundary crossing goes through a port.** An interface owned by the
   inside, implemented by the outside.
4. **The compiler enforces the architecture.** Once split into Gradle modules,
   a violation is a build failure rather than a review comment.

---

## 3. Target module structure

```
                        ┌─────────────────┐
                        │  nexus-domain   │   pure Java, zero dependencies
                        │  = plugin SDK   │   Event, Context, Workflow,
                        └────────▲────────┘   Condition, Action, annotations
                                 │
                        ┌────────┴────────┐
                        │nexus-application│   use cases + ports (in / out)
                        └────────▲────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
     ┌────────┴───────┐ ┌────────┴────────┐ ┌───────┴────────┐
     │   nexus-api    │ │nexus-infra-      │ │nexus-plugin-   │
     │                │ │structure         │ │loader          │
     │ driving        │ │ driven adapters  │ │ discovery and  │
     │ adapters:      │ │ JPA, RabbitMQ,   │ │ lifecycle of   │
     │ REST, WebSocket│ │ Redis, SMTP      │ │ external jars  │
     └────────▲───────┘ └────────▲─────────┘ └───────▲────────┘
              │                  │                   │
              └──────────────────┼───────────────────┘
                                 │
                       ┌─────────┴─────────┐
                       │  nexus-bootstrap  │  @SpringBootApplication,
                       │                   │  wiring, boot jar
                       └───────────────────┘
```

| Module | Contains | May depend on |
|---|---|---|
| `nexus-domain` | Entities, value objects, domain services, annotations | *nothing* |
| `nexus-application` | Use cases, port interfaces, commands | `domain` |
| `nexus-api` | REST controllers, WebSocket handlers, DTOs | `application`, `domain` |
| `nexus-infrastructure` | JPA entities, repositories, messaging, serialization | `application`, `domain` |
| `nexus-plugin-loader` | Plugin discovery, classloading, lifecycle | `application`, `domain` |
| `nexus-bootstrap` | Main class, Spring configuration, profiles | everything |

**`nexus-bootstrap` exists to break a cycle.** Something has to know every
implementation in order to wire it to its port. If that responsibility lives in
`infrastructure`, then `infrastructure` must see `api` (or the reverse) just to
host the main class, and the dependency graph closes on itself. An assembly
module that everyone else ignores keeps the graph acyclic. It is also the only
module that produces a boot jar.

---

## 4. Where does this class go?

| Question | Answer |
|---|---|
| Does it describe a business concept, with no I/O? | `domain` |
| Does it orchestrate a use case, or define a contract the outside must satisfy? | `application` |
| Does it speak a protocol or a technology (HTTP, SQL, AMQP, SMTP, JSON)? | an adapter |
| Does it exist only to connect the two? | `bootstrap` |

Rules of thumb:

- Anything annotated `@Entity`, `@RestController`, `@RabbitListener`,
  `@Component` belongs to an adapter — never to `domain` or `application`.
- A DTO belongs to the adapter that speaks its protocol. If two adapters need
  the same payload, it is not a DTO: it is an application-level **command**.
- Mappers live with the technology they map to, not in a shared `mapper`
  package. Entity mapping is infrastructure; DTO mapping is API.

---

## 5. Ports

Ports are interfaces owned by `application`. Inbound ports describe what Nexus
can be asked to do; outbound ports describe what Nexus needs from the world.

| Port | Direction | Implemented by |
|---|---|---|
| `IngestEvent` | in | called by REST and by the RabbitMQ consumer |
| `QueryEvents` | in | called by REST |
| `EventRepository` | out | JPA adapter over `EventEntity` |
| `WorkflowRepository` | out | JPA adapter |
| `ActionHandler<A>` | out | one implementation per action type |

Outbound ports speak the domain's language: `EventRepository.findById` takes an
`Event.Id`, not a `String`. A value object that is unwrapped at the boundary
provides no safety.

---

## 6. Plugin model

Adapters come in two forms, and **both use the same API**:

- **Bundled** — modules in this repository (Discord, GitHub).
- **External** — jars supplied by third parties.

A bundled adapter gets no privileged access. The moment one reaches past the
public SDK — into `EventRepository`, say — the plugin API becomes second-class
and stops being maintained honestly. Bundled adapters are the SDK's first
consumers and its continuous proof of usability.

This is why **the domain and the SDK are the same module**. What a plugin author
needs — `Event`, `Context`, `Action`, `Condition`, the metadata annotations, the
adapter lifecycle interface — is exactly the domain vocabulary. Splitting them
would mean either duplicating types or making the SDK depend on the domain,
which publishes the domain anyway.

The practical consequence is rule 2 in `CLAUDE.md`, stated more sharply: every
dependency added to `nexus-domain` is imposed on every plugin author, forever.
That is what "zero dependencies" is protecting.

---

## 7. Decisions

### ADR-006 — Every open hierarchy carries a metadata annotation and a registry

**Status:** accepted.

`Event`, `Context` and `Condition` each had a metadata annotation, a `Registry`
and a declared list of core types. `Action` had none, despite being equally open
and equally persisted inside a `Workflow`.

Left alone, the adapter persisting workflows would have had to invent an
identity: either a hardcoded `@JsonSubTypes` — the closed-at-the-boundary bug
ADR-001 exists to prevent — or fully-qualified class names, which turns a package
move into a broken migration.

**Decision:** `@ActionMetadata` + `Registry<Action, ActionMetadata>` +
`CoreActions`, matching the other three. `ActionDispatcher` keys handlers on the
registered identifier rather than the Java class.

**Consequences:** action identity has one source of truth, shared by dispatch,
reporting and (later) storage. The rule generalises: **an open hierarchy without
a metadata annotation and a registry is incomplete** — whatever crosses a
boundary needs a stable name that is not its class name.

### ADR-001 — Domain type hierarchies are open, not `sealed`

**Status:** accepted.

Events, contexts, actions and conditions were modelled as `sealed` interfaces
with explicit `permits` clauses.

A `sealed` hierarchy cannot be extended outside its own compilation module. That
makes a third-party adapter structurally impossible, which contradicts the
plugin model above. It also breaks as soon as the domain moves to its own Gradle
module, since the subtypes would then live outside it.

The bill was already being paid without the benefit: an audit of the codebase
found **zero** `switch` and **zero** `instanceof` over any sealed domain type.
All dispatch goes through `Registry` and string type identifiers. Sealing bought
exhaustiveness checking that nothing used.

**Decision:** drop `sealed`/`permits` from `Event`, `Context`, `Action` and
`Condition`. Keep the annotation + `Registry` mechanism as the dispatch
strategy, and change how it is fed: instead of walking `getPermittedSubclasses()`
at construction time, `Registry` accepts explicit registration so plugins can
contribute types as they load.

**Consequences:** exhaustiveness is no longer compiler-checked — an unknown type
identifier must fail loudly at runtime. Every place that enumerates subtypes by
hand becomes a bug: `ContextMixin`'s `@JsonSubTypes`, `RabbitMQConfig`'s source
list, `EventConsumer`'s queue list.

### ADR-002 — An `Action` is data; execution lives behind a port

**Status:** accepted.

An action must be able to send an email, which needs a mail client, which is
infrastructure. Putting `execute()` on the domain interface forces the
dependency into the domain by one route or another — a field, a parameter, or a
context object that hides it.

**Decision:** `Action` implementations stay pure records describing an intent.
Execution goes through the `ActionHandler<A>` outbound port, implemented per
action type in an adapter.

**Consequences:** the domain stays pure and `Workflow.matches()` is unit
testable. Because the intent is a value distinct from its execution, dry runs,
auditing what a workflow *would* do, and retrying a failed action all become
natural rather than requiring special support.

### ADR-003 — No JPMS module descriptor

**Status:** accepted.

`nexus-core` carried a `module-info.java`. It declared `requires` but no `opens`,
while Spring, Hibernate and Jackson all need reflective access. The application
runs only because the Spring Boot fat jar puts everything on the classpath,
where the descriptor is ignored at runtime.

So it constrained compilation, protected nothing at runtime, and would need to
be maintained per module after the split.

**Decision:** remove it. Gradle module boundaries enforce the architecture, and
ArchUnit enforces package rules within a module.

**Consequences:** if plugins later need a standard discovery mechanism,
`ServiceLoader` works on the classpath without JPMS. Isolating plugins with a
dedicated `URLClassLoader` — the approach used by Jenkins and Bukkit — gives
more control over versioning and unloading than JPMS would.

### ADR-005 — The application layer carries no framework annotation

**Status:** accepted.

`EventService`, `EventFactory`, `ActionDispatcher` and `WorkflowEngine` were
annotated `@Service` / `@Component`. Nothing forced that: they are plain classes
with constructor injection.

**Decision:** remove the annotations and declare these beans from
`bootstrap/ApplicationConfig`.

**Consequences:** the application layer becomes ordinary Java, drivable by
something other than Spring — a plugin embedding the workflow engine, a CLI, a
different framework — without touching it. The cost is one configuration class,
and the discipline of declaring a bean when adding a service. In exchange the
application's dependency graph is readable in one file, which component scanning
hides.

`RegistriesConfig` moved to `bootstrap` for the same reason: deciding which
types are registered is assembly, not infrastructure.

Handlers are collected through an `ObjectProvider`, not an injected `List`.
Spring fails to start when a `List<T>` has no candidate, and having no action
handler is legitimate — `SendEmailActionHandler` exists only when SMTP is
configured.

### ADR-004 — Ports live in `application`

**Status:** accepted.

Both inbound and outbound port interfaces live in `nexus-application`, rather
than declaring repository interfaces in `domain` as classical DDD does.

One rule, no case-by-case arbitration: the domain holds business concepts, the
application holds contracts with the outside. It also keeps `domain` purely
descriptive, which matters because it doubles as the public plugin SDK.

---

## 8. Current state

The split has landed. The rules are no longer conventions checked by review —
they are the module graph, and a violation fails the build.

```
$ ./gradlew :nexus-domain:dependencies --configuration compileClasspath
compileClasspath - Compile classpath for source set 'main'.
No dependencies

$ ./gradlew :nexus-application:dependencies --configuration compileClasspath
compileClasspath - Compile classpath for source set 'main'.
\--- project :nexus-domain
```

No adapter sees another. Only `nexus-bootstrap` applies the Spring Boot plugin
and produces a boot jar; every other module builds an ordinary consumable jar.

Two things the split surfaced that a single module had hidden:

- **`nexus-api` depends on Jackson only because of the double encoding** in
  `EventDtoMapper` (#32). Typing `EventResponseDto.context` as `Context` removes
  the dependency outright.
- **The codebase was on Jackson 2 while Spring Boot 4 ships Jackson 3**
  (`tools.jackson.*`). It compiled only because `jackson-datatype-jsr310` pulled
  Jackson 2 in transitively, which meant the configured `ObjectMapper` and the
  one serializing HTTP responses were different objects — mixins and custom
  serializers applied to the first and not the second. Migrated to Jackson 3;
  the runtime classpath now carries exactly one `jackson-databind`.

## 9. Migration order

Deliberately sequenced so the module split comes last. Splitting first, with
seven violations and no ports, produces a fight with Gradle instead of an
architecture.

| Step | Work | Why here |
|---|---|---|
| 1 | Open the type hierarchies (ADR-001); adapt `Registry`, `ContextMixin` | Small diff, unblocks everything else |
| 2 | Introduce ports and adapters inside the existing module | The actual inversion; no build changes yet |
| 3 | Split `EventMapper`; move the ingestion contract to `application` | Removes the remaining cross-adapter coupling |
| 4 | Workflow engine: `Workflow.matches()`, `ActionHandler` (ADR-002) | First feature validating the design |
| 5 | Build foundation: version catalog, convention plugins | Prerequisite for a sane multi-module build |
| 6 | Split into Gradle modules | Now only locks in what is already correct |
| 7 | ArchUnit rules | Guards what a module boundary cannot express |

Steps 1 to 6 are done. Step 7 remains: module boundaries catch dependencies
*between* modules, but not annotations leaking into the domain or layering
inside a module.

Steps 1–4 keep a single module and a green build throughout, so each is
independently reviewable and revertable.
