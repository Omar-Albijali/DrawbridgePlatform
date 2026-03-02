import { fetchApi } from './api';
import { UserDTO, UpdateUserProfileRequest, ChangePasswordRequest } from '../types';

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
        })
};
