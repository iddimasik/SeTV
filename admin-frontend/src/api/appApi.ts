import axios, {
    AxiosInstance,
    AxiosResponse,
    AxiosHeaders,
} from "axios";
import { App } from "../types/app";
import { getToken, logout } from "../store/auth";

const api: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
});

api.interceptors.request.use(
    (config) => {
        const token = getToken();

        if (!token) {
            logout();
            window.location.href = "/login";
            return Promise.reject(new Error("Не авторизован"));
        }

        if (!config.headers) {
            config.headers = new AxiosHeaders();
        }

        config.headers.set(
            "Authorization",
            `Bearer ${token}`
        );

        return config;
    },
    (error) => Promise.reject(error)
);

api.interceptors.response.use(
    (response: AxiosResponse) => response,
    (error) => {
        const status = error.response?.status;

        if (status === 401 || status === 403) {
            logout();
            window.location.href = "/login";
        }

        return Promise.reject(error);
    }
);

export const getApps = () =>
    api.get<App[]>("/apps");

export const getApp = (id: string) =>
    api.get<App>(`/apps/${id}`);

export const createApp = (data: App) =>
    api.post<App>("/apps", data);

export const updateApp = (id: string, data: App) =>
    api.put<App>(`/apps/${id}`, data);

export const deleteApp = (id: string) =>
    api.delete<void>(`/apps/${id}`);
