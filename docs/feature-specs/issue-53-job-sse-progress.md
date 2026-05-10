# Issue 53 Job SSE Progress

## Goal

Add a backend SSE skeleton for Job progress streaming.

The endpoint lets the frontend subscribe to `/api/v1/jobs/{jobId}/events`. This PR does not connect Worker callbacks yet;
it sends an initial heartbeat and current Job summary snapshot after validating the user's read access through the
existing Job query flow.

## API

```text
GET /api/v1/jobs/{jobId}/events
Accept: text/event-stream
Authorization: Bearer <access-token>
```

Initial events:

```text
event: HEARTBEAT
data: {"eventType":"HEARTBEAT","jobId":1,...}

event: JOB_PROGRESS
data: {"eventType":"JOB_PROGRESS","jobId":1,"total":100,"queued":10,...}
```

## Added Components

- `JobEventController`
- `JobEventService`
- `SseEmitterRegistry`
- `JobEventType`
- `JobProgressEvent`

## Event Types

- `HEARTBEAT`
- `JOB_PROGRESS`
- `JOB_COMPLETED`
- `JOB_FAILED`

## Non-Goals

- No Worker callback API.
- No direct Worker-to-frontend connection.
- No OpenCV preprocessing in the API server.
- No WebSocket implementation.
- No frontend UI completion.

## Follow-Up

1. Internal Worker API reports item start/success/failure.
2. Job state transitions publish progress events through `JobEventService`.
3. Frontend Job detail page consumes the SSE stream and renders counters.
