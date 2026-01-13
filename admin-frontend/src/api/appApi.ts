import axios from "axios";
import { App } from "../types/app";

// базовый URL backend
const BASE_URL = "http://localhost:8080/api/apps";

// ===== CRUD =====

// получить все приложения
export const getApps = () => {
    return axios.get<App[]>(BASE_URL);
};

// получить одно приложение
export const getApp = (id: string) => {
    return axios.get<App>(`${BASE_URL}/${id}`);
};

// создать приложение
export const createApp = (data: App) => {
    return axios.post<App>(BASE_URL, data);
};

// обновить приложение
export const updateApp = (id: string, data: App) => {
    return axios.put<App>(`${BASE_URL}/${id}`, data);
};

// удалить приложение
export const deleteApp = (id: string) => {
    return axios.delete<void>(`${BASE_URL}/${id}`);
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

    return axios.post<ApkParseResult>(
        `${BASE_URL}/upload-apk`,
        formData,
        {
            headers: {
                "Content-Type": "multipart/form-data",
            },
        }
    );
};
