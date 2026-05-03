# OAuth Provider Setup

## Goal

Document the external Google OAuth setup required to test the current backend OAuth skeleton.

## Current Scope

Included:

1. Google OAuth client registration placeholder
2. Google user info adapter
3. OAuth2 login success redirect skeleton
4. Security configuration skeleton

Excluded:

1. Automatic user sign-up
2. JWT access token issuance
3. Refresh token cookie storage
4. Auth controller APIs such as `/api/v1/auth/me`

## Required Values

You need the following values from Google Cloud Console:

| Provider | Required Value | Environment Variable |
|---|---|---|
| Google | OAuth Client ID | `GOOGLE_CLIENT_ID` |
| Google | OAuth Client Secret | `GOOGLE_CLIENT_SECRET` |

## Local Redirect URI

Register this URI in Google Cloud Console:

```text
http://localhost:8080/login/oauth2/code/google
```

Frontend success redirect used by the current skeleton:

```text
http://localhost:5173/oauth2/success
```

## Example Environment Variables

```text
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
OAUTH2_SUCCESS_REDIRECT_URI=http://localhost:5173/oauth2/success
```

## Production Redirect URI

Register the external HTTPS callback URI that matches the NGINX public domain.

Example:

```text
https://{service-domain}/login/oauth2/code/google
```

If the registered redirect URI and the backend external base URL do not match, OAuth login fails.

## Security Notes

1. Never commit the real client secret.
2. Do not commit a real `.env` file.
3. Register only the environments that you actually operate.
4. Production should use HTTPS.
5. Short-lived JWT access tokens will be added in the next auth step.
