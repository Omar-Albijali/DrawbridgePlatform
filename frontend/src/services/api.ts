const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

interface RequestOptions extends RequestInit {
    token?: string;
}

export async function fetchApi<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
    const { token, headers, ...customConfig } = options;

    const storedToken = localStorage.getItem('drawbridge_token') || sessionStorage.getItem('drawbridge_token');
    const authToken = token || storedToken;

    const config: RequestInit = {
        ...customConfig,
        headers: {
            'Content-Type': 'application/json',
            ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
            ...headers,
        },
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

    if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(`API Error: ${response.status} ${response.statusText} - ${errorBody}`);
    }

    // Handle empty responses (e.g. 204 No Content)
    if (response.status === 204) {
        return {} as T;
    }

    try {
        return await response.json();
    } catch (e) {
        // If response is not JSON, return text or empty object
        return {} as T;
    }
}
