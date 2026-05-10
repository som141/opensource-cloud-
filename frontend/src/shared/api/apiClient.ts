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
    return response.json() as Promise<ApiResponse<T>>;
  }

  private headers(accessToken?: string): HeadersInit {
    return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
  }
}

export const apiClient = new ApiClient();
