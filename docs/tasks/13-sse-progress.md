# 13. SSE Progress

## Goal

Expose a Server-Sent Events endpoint so the frontend can subscribe to Job progress without polling.

This task creates the SSE API skeleton. Actual Worker status callbacks and automatic publish on every state transition
are connected after the Internal Worker API task.

## Documents To Read First

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/12-job.md`
4. `docs/api/job-api.md`
5. `docs/architecture/nginx-routing.md`

## Scope

1. Job event controller
2. SSE emitter registry
3. Job progress event DTO
4. Heartbeat/progress/completed/failed event methods
5. NGINX buffering-off alignment
6. Tests for controller, service, registry, and event DTO

## Work Order

1. Create `JobEventController`.
2. Create `JobEventService`.
3. Create `JobProgressEvent` and `JobEventType`.
4. Create `SseEmitterRegistry`.
5. Configure connection timeout.
6. Clean up emitters on completion, timeout, and error.
7. Send initial heartbeat event.
8. Send initial `JOB_PROGRESS` snapshot event.
9. Add publish methods for `JOB_PROGRESS`, `JOB_COMPLETED`, and `JOB_FAILED`.
10. Confirm NGINX SSE route keeps buffering disabled.
11. Update Job API documentation.
12. Add tests.

## Deliverables

1. `GET /api/v1/jobs/{jobId}/events`
2. Job progress event DTO
3. SSE emitter registry skeleton
4. Documentation updates

## Completion Criteria

1. The frontend can connect to a Job event stream path.
2. Server resources are cleaned up when connections finish.
3. NGINX does not buffer SSE responses.
4. Unauthorized users cannot subscribe without passing normal Job readability validation.
5. API server still does not execute image preprocessing logic.

## Forbidden

1. Do not replace SSE with polling-only progress.
2. Do not leave unmanaged emitters for every connection.
3. Do not allow a user to subscribe to another user's Job without permission validation.
4. Do not implement Worker preprocessing or Worker callbacks in this task.
