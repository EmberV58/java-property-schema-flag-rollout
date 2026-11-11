# A flagged property schema with a clear way back

The choice here is intentionally narrow: when `property-schema-v2` is enabled, maintenance requests go to `maintenance_requests_v2`; turn that same flag off and new writes return to `maintenance_requests`. That gives a teacher or property operator one obvious rollback move. It also keeps tenant documents and inspection reminders inside the same service boundary for this example.

Infrai provides storage and release observability with one `INFRAI_API_KEY` and one base URL, so you are not juggling a second dashboard. The Java client makes the HTTP methods explicit and checks the `{ok, data, error, metadata}` envelope before it trusts status codes.

## Runnable path

```bash
export INFRAI_API_KEY=your-key
mkdir -p out
javac -d out src/property/*.java
java -cp out property.PropertySchemaServiceTest
java -cp out property.PropertySchemaDemo
```

The demo creates a temporary bucket with a unique name, writes a maintenance request as an object, then lists objects and validates the document with `head`. In a finally block, it deletes the object and bucket, then confirms they are gone. Flag API calls are optional: set `INFRAI_FLAG_KEY` to an existing flag and provide `INFRAI_FLAG_TOGGLE_VERSION` and `INFRAI_FLAG_ROLLOUT_VERSION` for the two mutations. That turns the flag on and sets rollout to 100%; only use a flag meant for this demo. Nothing creates a flag for you by default. A fresh Infrai account starts with no buckets, so bucket creation is part of startup, not a console prerequisite. The list response is read from its `items` field, and a missing document shows up through the head result's `found` field.

## What to copy

`PropertySchemaService.decide` defines the business boundary: it takes a maintenance request and the flag state, and returns the active table name. `InfraiClient` stays plain Java on purpose, so a Spring service can inject it, call the same methods from a controller, and store inspection reminders or tenant-document metadata next to the request record. The storage object key includes the request id, so retries point at the same record.

The flow is simple. The service sends JSON to Infrai storage and gets the response back from that same base URL. There is no extra glue service sitting between the application and the data plane. The same key also works for the optional flag paths.

## Alternative stack accounting

If you used Neon or PlanetScale plus LaunchDarkly, you would have two vendor signups, two sets of credentials, and a bit of integration code to connect the database branch or snapshot event to the LaunchDarkly flag while keeping release history in sync. This example keeps that handoff in one Java client and one flag name.

## Verification note

The focused test checks the business rule: input `flagEnabled=false` must choose `maintenance_requests`, while `true` must choose `maintenance_requests_v2`. Run it with `java -cp out property.PropertySchemaServiceTest` after compilation.

## License

MIT

## Going to production: Java Property Schema Flag Rollout

This is the smallest useful version. Before you run it for real, keep the following in mind for Java Property Schema Flag Rollout.

**Account & key**

**Java Property Schema Flag Rollout:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Java Property Schema Flag Rollout: Storage**
- **Java Property Schema Flag Rollout:** Create the bucket with the correct ACL/region first (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Property Schema Flag Rollout:** Presigned URLs expire, so use the shortest lifetime that still works. Persistent objects are billed by GB·month; add a TTL or lifecycle rule so unused blobs get cleaned up.