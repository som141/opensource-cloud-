const ACCESS_TOKEN_KEY = 'doc-pipeline.access-token';

export function readStoredAccessToken() {
  return window.localStorage.getItem(ACCESS_TOKEN_KEY) ?? undefined;
}

export function storeAccessToken(accessToken: string) {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
}

export function clearAccessToken() {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
}
