import { clearAccessToken, readStoredAccessToken, storeAccessToken } from '../auth/accessTokenStore';

export type ApiResponse<T> = {
  isSuccess: boolean;
  code: string;
  message: string;
  result: T;
};

type TokenRefreshResponse = {
  accessToken: string;
  accessTokenExpiresAt: string;
};

export class ApiClient {
  constructor(private readonly baseUrl = '/api') {}

  async get<T>(path: string, accessToken?: string): Promise<ApiResponse<T>> {
    return this.request<T>('GET', path, undefined, accessToken);
  }

  async post<T>(path: string, body: unknown, accessToken?: string): Promise<ApiResponse<T>> {
    return this.request<T>('POST', path, body, accessToken);
  }

  async patch<T>(path: string, body: unknown, accessToken?: string): Promise<ApiResponse<T>> {
    return this.request<T>('PATCH', path, body, accessToken);
  }

  async delete<T>(path: string, accessToken?: string): Promise<ApiResponse<T>> {
    return this.request<T>('DELETE', path, undefined, accessToken);
  }

  private async request<T>(
    method: 'GET' | 'POST' | 'PATCH' | 'DELETE',
    path: string,
    body?: unknown,
    accessToken?: string
  ): Promise<ApiResponse<T>> {
    const response = await this.fetchJson(method, path, body, accessToken);
    if (response.status === 401 && this.canRefresh(path)) {
      const refreshedAccessToken = await this.refreshAccessToken();
      const retryResponse = await this.fetchJson(method, path, body, refreshedAccessToken);
      return this.parse<T>(retryResponse);
    }
    return this.parse<T>(response);
  }

  private async fetchJson(
    method: 'GET' | 'POST' | 'PATCH' | 'DELETE',
    path: string,
    body?: unknown,
    accessToken?: string
  ) {
    const headers: Record<string, string> = {
      ...this.headers(accessToken)
    };
    const init: RequestInit = {
      method,
      headers,
      credentials: 'include',
      redirect: 'manual'
    };
    if (method === 'POST' || method === 'PATCH') {
      headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(body ?? {});
    }
    return fetch(`${this.baseUrl}${path}`, init);
  }

  private async refreshAccessToken() {
    const response = await fetch(`${this.baseUrl}/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      redirect: 'manual',
      body: '{}'
    });
    const payload = await this.parse<TokenRefreshResponse>(response);
    storeAccessToken(payload.result.accessToken);
    return payload.result.accessToken;
  }

  private canRefresh(path: string) {
    return path !== '/v1/auth/refresh' && path !== '/v1/auth/logout';
  }

  private async parse<T>(response: Response): Promise<ApiResponse<T>> {
    const contentType = response.headers.get('content-type') ?? '';
    if (!contentType.includes('application/json')) {
      const text = await response.text();
      const preview = text.replace(/\s+/g, ' ').slice(0, 120);
      throw new Error(`API returned non-JSON response with HTTP ${response.status}: ${preview}`);
    }
    const payload = (await response.json()) as ApiResponse<T>;
    if (!response.ok || !payload.isSuccess) {
      if (response.status === 401) {
        clearAccessToken();
      }
      throw new Error(payload.message || `API request failed with ${response.status}`);
    }
    return payload;
  }

  private headers(accessToken?: string): Record<string, string> {
    const token = accessToken ?? readStoredAccessToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }
}

export const apiClient = new ApiClient();
