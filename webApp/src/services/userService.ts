import { fetchApi } from './api';
import { UserDTO, UpdateUserProfileRequest, ChangePasswordRequest, ImageUploadResponse } from '../types';

export const userService = {
    getById: (id: string) => fetchApi<UserDTO>(`/users/${id}`),

    update: (id: string, request: UpdateUserProfileRequest) => {
        const body = request as unknown as UpdateUserProfileRequest;
        return fetchApi<UserDTO>(`/users/${id}`, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    },

    changePassword: (id: string, currentPassword: string, newPassword: string) =>
        fetchApi<void>(`/users/${id}/password`, {
            method: 'PATCH',
            body: JSON.stringify(new ChangePasswordRequest(currentPassword, newPassword))
        }),

    uploadProfileImage: async (id: string, file: File): Promise<ImageUploadResponse> => {
        const formData = new FormData();
        formData.append('file', file);

        const token = localStorage.getItem('drawbridge_token') || sessionStorage.getItem('drawbridge_token');
        const response = await fetch(`/api/users/${id}/profile-image`, {
            method: 'POST',
            headers: {
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            },
            body: formData
        });

        if (!response.ok) {
            throw new Error(`Upload failed: ${response.status}`);
        }

        return response.json();
    }
};
