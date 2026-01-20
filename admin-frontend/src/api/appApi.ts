import axios, { AxiosRequestConfig, AxiosResponse } from "axios";
import { App } from "../types/app";
import { getToken, logout } from "../store/auth";

// базовый URL backend
const BASE_URL = `${import.meta.env.VITE_API_URL}/api/apps`;

// ===== Вспомогательная функция для запросов с JWT =====
const axiosWithAuth = async <T = any>(
    config: AxiosRequestConfig = {}
): Promise<AxiosResponse<T>> => {

    const token = getToken();

    if (!token) {
        logout();
        window.location.href = "/login";
        return Promise.reject(new Error("Не авторизован"));
    }

    try {
        return await axios({
            ...config,
            headers: {
                ...config.headers,
                Authorization: `Bearer ${token}`,
            },
        });
    } catch (err: any) {
        if (err.response?.status === 401) {
            logout();
            window.location.href = "/login";
        }
        throw err;
    }
};

// ===== CRUD =====

// получить все приложения
export const getApps = () =>
    axiosWithAuth<App[]>({ method: "GET", url: BASE_URL });

// получить одно приложение
export const getApp = (id: string) =>
    axiosWithAuth<App>({ method: "GET", url: `${BASE_URL}/${id}` });

// создать приложение
export const createApp = (data: App) =>
    axiosWithAuth<App>({ method: "POST", url: BASE_URL, data });

// обновить приложение
export const updateApp = (id: string, data: App) =>
    axiosWithAuth<App>({ method: "PUT", url: `${BASE_URL}/${id}`, data });

// удалить приложение
export const deleteApp = (id: string) =>
    axiosWithAuth<void>({ method: "DELETE", url: `${BASE_URL}/${id}` });

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

    return axiosWithAuth<ApkParseResult>({
        method: "POST",
        url: `${BASE_URL}/upload-apk`,
        data: formData,
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
};
