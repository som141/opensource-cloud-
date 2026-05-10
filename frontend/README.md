# frontend

React + Vite frontend skeleton for the document image preprocessing platform.

## Responsibility

1. Start Google OAuth login through `/oauth2/authorization/google`.
2. Render project, upload, job, image, preprocessing quality, and admin placeholders.
3. Call backend APIs through the root NGINX `/api` reverse proxy.
4. Connect to Job progress SSE through `/api/v1/jobs/{jobId}/events`.
5. Display original/processed/report data later, after the API and Worker artifact flows are implemented.

## Local Commands

```bash
npm install
npm run build
npm run dev
```

## Boundaries

1. Do not add Bootstrap, jQuery, or AdminLTE.
2. Do not put image preprocessing business logic in the frontend.
3. Do not expose Object Storage secrets.
4. Do not implement OCR text extraction as a frontend product workflow.
