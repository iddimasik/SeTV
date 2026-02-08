import axios, {
    AxiosInstance,
    AxiosResponse,
    AxiosHeaders,
} from "axios";
import { App, AppRequest } from "../types/app";
import { getToken, logout } from "../store/auth";

const api: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
});

/* ================= INTERCEPTORS ================= */

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

        config.headers.set("Authorization", `Bearer ${token}`);
        config.headers.set("Content-Type", "application/json");

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

/* ================= API ================= */

export const getApps = () =>
    api.get<App[]>("/apps");

export const getApp = (id: string) =>
    api.get<App>(`/apps/${id}`);

/* ================= CREATE ================= */

export const createApp = (data: AppRequest) =>
    api.post<App>("/apps", {
        ...data,
        images: data.images ?? [],
    });

/* ================= UPDATE ================= */

export const updateApp = (id: string, data: AppRequest) =>
    api.put<App>(`/apps/${id}`, {
        ...data,
        images: data.images ?? [],
    });

/* ================= DELETE ================= */

export const deleteApp = (id: string) =>
    api.delete<void>(`/apps/${id}`);
