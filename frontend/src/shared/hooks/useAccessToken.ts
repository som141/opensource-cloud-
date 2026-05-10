import { useState } from 'react';

export function useAccessToken() {
  const [accessToken, setAccessToken] = useState<string | undefined>(() => {
    const params = new URLSearchParams(window.location.search);
    return params.get('accessToken') ?? undefined;
  });

  return { accessToken, setAccessToken };
}
