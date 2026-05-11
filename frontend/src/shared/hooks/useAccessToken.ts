import { useState } from 'react';
import { readStoredAccessToken, storeAccessToken } from '../auth/accessTokenStore';

export function useAccessToken() {
  const [accessToken, setAccessToken] = useState<string | undefined>(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('accessToken') ?? undefined;
    if (token) {
      storeAccessToken(token);
      return token;
    }
    return readStoredAccessToken();
  });

  return { accessToken, setAccessToken };
}
