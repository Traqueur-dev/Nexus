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
                    ┌────────────┴────────────┐
                    │                         │
       ┌────────────┴─────────┐     ┌─────────┴──────┐
       │  nexus-infrastructure│     │nexus-plugin-   │
       │                      │     │loader          │
       │  adapters, one       │     │ discovery and  │
       │  package per         │     │ lifecycle of   │
       │  protocol:           │     │ external jars  │
       │  rest, messaging,    │     └───────▲────────┘
       │  persistence, mail,  │             │
       │  serialization       │             │
       └────────────▲─────────┘             │
                    │                       │
                    └───────────┬───────────┘
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
| `nexus-infrastructure` | REST controllers and DTOs, JPA entities, messaging, serialization | `application`, `domain` |
| `nexus-plugin-loader` | Plugin discovery, classloading, lifecycle | `application`, `domain` |
| `nexus-bootstrap` | Main class, Spring configuration, profiles | everything |

**`nexus-bootstrap` is where assembly decisions live.** Something has to know
every implementation in order to wire it to its port, and which types are
registered. Neither is an adapter's business: an adapter that decides what the
application layer is made of stops being replaceable. Keeping that in a module
nobody depends on also isolates the boot jar, which matters because a module
producing one cannot be consumed as a dependency (see ADR-007).

---

## 4. Where does this class go?

| Question | Answer |
|---|---|
| Does it describe a business concept, with no I/O? | `domain` |
| Does it orchestrate a use case, or define a contract the outside must satisfy? | `application` |
| Does it speak a protocol or a technology (HTTP, SQL, AMQP, SMTP, JSON)? | `infrastructure`, in that protocol's package |
| Does it exist only to connect the two? | `bootstrap` |

Rules of thumb:

- Anything annotated `@Entity`, `@RestController`, `@RabbitListener`,
  `@Component` belongs to an adapter — never to `domain` or `application`.
- A DTO belongs to the package that speaks its protocol. If two adapters need
  the same payload, it is not a DTO: it is an application-level **command**.
- Mappers live with the technology they map to, not in a shared `mapper`
  package: `EventEntityMapper` in `persistence`, `EventDtoMapper` in `rest`.

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

### ADR-011 — Write endpoints validate by construction, and address by PUT

**Status:** accepted.

The workflows API (#10) is the first endpoint that accepts a write, so two
questions had to be answered once rather than per controller.

**Validation lives in the domain type, not in annotations on the DTO.** The
obvious move is Jakarta Bean Validation — `@NotEmpty` on `events`, `@NotNull` on
`condition`, `@Valid` on the body. It was rejected: `Workflow` already refuses an
empty event list, an empty action list and a missing condition in its compact
constructor. Restating those rules on the DTO produces two definitions of a valid
workflow, and only one of them is the one the engine trusts. They would drift the
first time an invariant changed, and the API would be the copy that stayed wrong.

**Decision:** the adapter builds the domain object and translates the failure.
`WorkflowDtoMapper.toDomain` catches `IllegalArgumentException` and
`NullPointerException` and raises `InvalidWorkflowException`, annotated
`@ResponseStatus(BAD_REQUEST)`. Construction *is* the validation.

The translation is not optional decoration. Without it the domain's refusal
surfaces as a 500 — the server reporting that it broke, when in fact it rejected
the request on purpose. That is the same reasoning, and the same placement, as
`EventController.parseId` raising `InvalidEventIdException`.

Unknown condition and action types need no handling at all: the registry-backed
deserializer (ADR-009) fails while reading the body, and Spring already maps an
unreadable body to 400.

**Writes are `PUT /workflows/{id}`, and there is no `POST`.** A workflow id is
chosen by the user and is meaningful — `notify-on-main-push`, not a generated key
— and the store is an upsert (ADR-010). That makes a write idempotent and
addressable, which is what PUT means. A `POST` to the collection would have to
invent an id the caller did not ask for, and would split into "create" and
"update" an operation the system performs once.

**Consequences:** a first write and a replacement are indistinguishable to the
client, so both answer 200 rather than 201-then-200. Telling them apart needs a
read before every write to learn something the caller already knows, or a `save`
on the inbound port that reports which happened — putting an HTTP status concern
into the application layer. Neither is worth a status code.

The cost of skipping Bean Validation is that a rejected request carries one
message about the first broken invariant, not a field-by-field report. If the
dashboard ever needs per-field errors, the answer is to enrich what the domain
throws, not to re-declare the rules next to it.

### ADR-010 — Workflows are stored in PostgreSQL, not in a document store

**Status:** accepted.

A `Workflow` is a document by shape: a fixed four-field record whose `condition`
is an arbitrarily nested tree and whose `actions` are a heterogeneous list, both
extensible by plugins. MongoDB was the obvious candidate, and was evaluated
before writing the schema.

**Decision:** PostgreSQL, with `TEXT[]` for the event types and `JSONB` for the
condition and action trees.

The deciding argument is type identity, not query ergonomics. Spring Data MongoDB
resolves polymorphism by writing `_class` — a fully-qualified Java class name —
which is the exact failure ADR-006 exists to prevent: a package move breaks every
stored row. The standard remedy, `@TypeAlias`, is unavailable here, because it
would put `org.springframework.data.annotation` into `nexus-domain`, which has no
third-party dependency and ships as the plugin SDK. Even if it were available it
would be a *second* identifier declared beside `@ConditionMetadata`, kept in sync
by hand — the drift ADR-006 names.

That leaves a custom `MongoTypeMapper` reading the `Registry`: rebuilding
`RegistryBackedSerialization` against BSON, one week after ADR-009 deleted the
duplicate mechanism. The remaining option — storing the workflow as an opaque
JSON string in Mongo — discards the only reason to have chosen Mongo.

Meanwhile the capability Mongo was wanted for is already present. `events.context`
and `events.payload` have been `jsonb` with GIN indexes since V1, and the one
query the port exposes, `findTriggeredBy`, is `events @> ARRAY[?]` served from a
GIN index.

**Consequences:** one datastore to run, back up and deploy, and one serialization
mechanism rather than two. The cost is that querying *inside* a condition tree
means `jsonb` path expressions rather than Mongo's query language. Acceptable at
this size — workflows number in the hundreds, and no use case queries into a tree;
if one appears, filtering in Java after `findTriggeredBy` is still cheap.

The domain invariants on `Workflow` are restated as `CHECK` constraints. The
duplication is deliberate: the record protects what it constructs, the schema
protects what a migration or a future adapter writes.

`save()` is a plain upsert here, deliberately unlike `JpaEventRepository`, which
goes out of its way to avoid it (#27, ADR-008). The difference is the data, not
the technology: an event is a fact, so an UPDATE is silent loss; a workflow is
configuration the user edits, so refusing an UPDATE would make it uneditable.

### ADR-009 — One registry-backed mechanism for every polymorphic hierarchy

**Status:** accepted.

There were two mechanisms for the same problem, and the weaker one was the one
whose limitation was written down in a comment.

`Condition` used a registry-driven serializer pair that looked the type up inside
`deserialize()`. `Context` used a `@JsonTypeInfo` mixin whose subtype list was
fed from the registry when the `ObjectMapper` was built. ADR-001 had already
removed the hardcoded `@JsonSubTypes` there; what replaced it was a *snapshot* of
a live registry, which is the same closure moved one level up. A plugin
registering `SlackContext` at T+5min is in the registry, satisfies
`Context.source()`, and still fails to deserialize.

The plugin loader could not have fixed that from the outside. Rebuilding the
mapper invalidates every bean already holding a reference to it —
`EventEntityMapper`, `EventConsumer`, the message converters — and the only other
route is reaching into Jackson's subtype resolver. The correct mechanism already
existed in the repository, forty lines away, hardcoded to one hierarchy.

**Decision:** `RegistryBackedSerialization`, generic over the base type, its
registry and its discriminator property. Contexts (`source`), conditions (`type`)
and actions (`type`) all use it. `ContextMixin` and `registerContextSubtypes` are
deleted; startup-time subtype registration disappears as a concept, and a fourth
hierarchy is one `register(...)` line rather than a fourth mechanism.

The wire format is unchanged — the discriminator is still written inline as a
property, which is what `@JsonTypeInfo(As.PROPERTY)` produced — so stored rows
and queued messages are unaffected.

**Consequences:** the constraint this trades into is that a polymorphic type must
be a **record**. Serialization walks record components rather than delegating to
the mapper, because delegating re-enters the same serializer and recurses. That
was already true of conditions and already assumed by `EventFactory`, which
rebuilds an event from its components; it is now stated rather than implied, and
`OpenHierarchyTest` fails the build on a non-record. A plugin's types are only
visible at runtime, so the serializer also refuses one with a message naming the
class — loud rather than lossy, since the previous mechanism would have written
such a type as nothing but its discriminator and failed on the read instead.

Actions are registered here although nothing serializes one yet. The alternative
was to decide their format while writing the workflow store (#6), which is the
worst moment to decide a persisted contract.

### ADR-008 — Event identifiers are UUID version 7, and writes only insert

**Status:** accepted.

An `Event.Id` was a source prefix and six base-36 characters. That is 2.2 billion
values per source, which reads as ample and is not: by the birthday bound a
collision becomes likely around 55 000 events for a single source, and is already
possible in the low thousands. On collision, `JpaRepository.save()` issued an
`UPDATE` — the stored event was replaced, with no error (#27).

Two failures, and they need separate answers. Fixing only the write turns silent
loss into an ingestion error under load; fixing only the identifier leaves the
store willing to overwrite whenever an id repeats for any other reason.

**Decision:** identifiers carry a UUID version 7, and the JPA adapter uses
`persist()` with an explicit `flush()` rather than `save()`.

Version 7 over version 4 because it is time-ordered: for an append-only store
queried by time, ids that sort by generation cost nothing and occasionally help.
Over a wider base-36 string because widening only moves the threshold, and a UUID
is a format every tool already understands.

**Consequences:** the id is longer and no longer memorable, which is why the
source prefix stays — `github-0192f3c4-…` is still readable in a log. The domain
generates the UUID itself, about twenty lines against RFC 9562, because the JDK
only produces version 4 and `nexus-domain` accepts no third-party dependency; a
UUID library here would be imposed on every plugin author.

`EventAlreadyStoredException` belongs to the port rather than the adapter: every
implementation owes the caller that guarantee. It should now be unreachable —
which is precisely what was believed about six base-36 characters.

The format is a persisted contract, so V2 rewrites existing rows and replaces the
`CHECK` constraint. Migrated rows take a version 4 UUID, since PostgreSQL 17 has
no `uuidv7()`; they lose the ordering property and keep their event, and nothing
reads order from the id anyway.

### ADR-007 — Driving adapters live in `infrastructure`; there is no `api` module

**Status:** accepted.

REST controllers, their DTOs and their exceptions lived in a `nexus-api` module,
separate from `nexus-infrastructure`, on the grounds that driving adapters and
driven adapters are different kinds of thing.

They are — but that distinction is about the direction a call travels, not about
what a module must protect. Both speak a protocol, both depend on `application`
and `domain`, and neither may depend on the other. A module boundary buys
enforcement of exactly one rule here: that `rest` and `messaging` cannot see each
other. Five classes is a high price for one rule, and the same rule is expressible
as an ArchUnit package rule (#26) that also covers `persistence` and `mail`,
which the old split never did.

**Decision:** one adapter module, one package per protocol —
`infrastructure.rest`, `infrastructure.messaging`, `infrastructure.persistence`,
`infrastructure.serialization`, `infrastructure.mail`.

**Consequences:** the graph loses a node and gains nothing it was actually using.
The cost is real and worth stating plainly: cross-adapter coupling is no longer a
compile error. Until #26 lands, nothing stops `EventController` from importing
`EventMessage` — it is the one guarantee this change trades away.

It also removes the original justification for `nexus-bootstrap`, which was to
break the cycle `api` and `infrastructure` would form if either hosted the main
class. With one adapter module there is no cycle to break. Bootstrap is kept for
two reasons that survive: it holds the assembly decisions (ADR-005 — which beans
the application layer is made of, which types are registered), and a module that
produces a boot jar cannot be consumed as a dependency by another, so hosting it
in `infrastructure` would make `infrastructure` unusable to any future module.

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

Only `nexus-bootstrap` applies the Spring Boot plugin and produces a boot jar;
every other module builds an ordinary consumable jar. Adapters no longer sit in
separate modules (ADR-007), so keeping them from seeing each other is ArchUnit's
job now, not the compiler's.

Three things this surfaced that a single module had hidden:

- **The double encoding in `EventDtoMapper`** (#32). It serialized the context
  into a `String` that the message converter then serialized again, and that was
  the only reason the REST adapter needed Jackson at all. `EventResponseDto`
  now carries a `Context`; the converter writes it.
- **The codebase was on Jackson 2 while Spring Boot 4 ships Jackson 3**
  (`tools.jackson.*`). It compiled only because `jackson-datatype-jsr310` pulled
  Jackson 2 in transitively, which meant the configured `ObjectMapper` and the
  one serializing HTTP responses were different objects — mixins and custom
  serializers applied to the first and not the second. Migrated to Jackson 3;
  the runtime classpath now carries exactly one `jackson-databind`.
- **The migration then reproduced the same split in a subtler form.** A
  `@Bean ObjectMapper` no longer replaces Boot's: `JacksonAutoConfiguration`
  backs off on a missing `JsonMapper` bean, and the mapper it builds is
  `@Primary`, so injection picked Boot's unconfigured one. Contexts were stored
  without their `source` discriminator and could not be read back. The
  serialization is now a `JsonMapperBuilderCustomizer`, which leaves one mapper
  in the application. This was invisible until the Testcontainers suite was run
  against a real database for the first time.

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

All seven steps are done. The rules ArchUnit adds are the ones a module boundary
cannot express: a framework annotation leaking into the domain, layering inside a
module, one adapter package reaching into another (ADR-007), an open hierarchy
whose type forgot its identifier (ADR-006) or is not a record (ADR-009) — failures
that otherwise surface at runtime, on ingestion, far from their cause.

Each rule carries a `because` clause stating what it protects, so a failing build
explains the constraint rather than only naming the violated line. One test
checks the checker: it runs a rule against a deliberately broken type and asserts
the report names it.

Steps 1–4 keep a single module and a green build throughout, so each is
independently reviewable and revertable.
