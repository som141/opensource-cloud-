export type ApiResponse<T> = {
  isSuccess: boolean;
  code: string;
  message: string;
  result: T;
};

export class ApiClient {
  constructor(private readonly baseUrl = '/api') {}

  async get<T>(path: string, accessToken?: string): Promise<ApiResponse<T>> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      headers: this.headers(accessToken),
      credentials: 'include'
    });
    return this.parse<T>(response);
  }

  async post<T>(path: string, body: unknown, accessToken?: string): Promise<ApiResponse<T>> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      method: 'POST',
      headers: {
        ...this.headers(accessToken),
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify(body)
    });
    return this.parse<T>(response);
  }

  private async parse<T>(response: Response): Promise<ApiResponse<T>> {
    const payload = (await response.json()) as ApiResponse<T>;
    if (!response.ok || !payload.isSuccess) {
      throw new Error(payload.message || `API request failed with ${response.status}`);
    }
    return payload;
  }

  private headers(accessToken?: string): HeadersInit {
    return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
  }
}

export const apiClient = new ApiClient();
