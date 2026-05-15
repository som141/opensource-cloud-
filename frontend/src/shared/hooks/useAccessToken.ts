import { useState } from 'react';
import { readStoredAccessToken, storeAccessToken } from '../auth/accessTokenStore';

export function useAccessToken() {
  const [accessToken, setAccessToken] = useState<string | undefined>(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('accessToken') ?? undefined;
    if (token) {
      storeAccessToken(token);
      params.delete('accessToken');
      params.delete('accessTokenExpiresAt');
      const nextSearch = params.toString();
      const nextUrl = `${window.location.pathname}${nextSearch ? `?${nextSearch}` : ''}${window.location.hash}`;
      window.history.replaceState({}, document.title, nextUrl);
      return token;
    }
    return readStoredAccessToken();
  });

  return { accessToken, setAccessToken };
}
