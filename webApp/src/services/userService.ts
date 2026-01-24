import { fetchApi } from './api';
import { UserDTO, UpdateUserProfileRequest } from '../types';

export const userService = {
    getById: (id: string) => fetchApi<UserDTO>(`/users/${id}`),

    update: (id: string, request: UpdateUserProfileRequest) => {
        // cast request to ensure it matches the expected JSON structure if needed, 
        // though UpdateUserProfileRequest is a class in shared logic, passing plain object works with fetchApi's JSON.stringify
        const body = request as unknown as UpdateUserProfileRequest;
        return fetchApi<UserDTO>(`/users/${id}`, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    }
};
