import axios, { AxiosRequestConfig } from "axios";
import { App } from "../types/app";
import { getToken, logout } from "../store/auth";

// базовый URL backend
const BASE_URL = "http://localhost:8080/api/apps";

// ===== Вспомогательная функция для запросов с JWT =====
const axiosWithAuth = (config: AxiosRequestConfig = {}) => {
    const token = getToken();
    if (!token) throw new Error("Нет токена, пользователь не авторизован");

    return axios({
        ...config,
        headers: {
            ...config.headers,
            Authorization: `Bearer ${token}`,
        },
    }).catch((err) => {
        if (err.response?.status === 401) {
            logout();
            window.location.href = "/login"; // редирект на страницу входа
        }
        throw err;
    });
};

// ===== CRUD =====

// получить все приложения
export const getApps = () => {
    return axiosWithAuth({ method: "GET", url: BASE_URL }) as Promise<{ data: App[] }>;
};

// получить одно приложение
export const getApp = (id: string) => {
    return axiosWithAuth({ method: "GET", url: `${BASE_URL}/${id}` }) as Promise<{ data: App }>;
};

// создать приложение
export const createApp = (data: App) => {
    return axiosWithAuth({ method: "POST", url: BASE_URL, data }) as Promise<{ data: App }>;
};

// обновить приложение
export const updateApp = (id: string, data: App) => {
    return axiosWithAuth({ method: "PUT", url: `${BASE_URL}/${id}`, data }) as Promise<{ data: App }>;
};

// удалить приложение
export const deleteApp = (id: string) => {
    return axiosWithAuth({ method: "DELETE", url: `${BASE_URL}/${id}` });
};

// ===== APK upload & parse =====
export interface ApkParseResult {
    name: string;
    packageName: string;
    versionName?: string;
    apkUrl: string;
}

export const uploadApk = (file: File) => {
    const formData = new FormData();
    formData.append("file", file);

    return axiosWithAuth({
        method: "POST",
        url: `${BASE_URL}/upload-apk`,
        data: formData,
        headers: {
            "Content-Type": "multipart/form-data",
        },
    }) as Promise<{ data: ApkParseResult }>;
};
