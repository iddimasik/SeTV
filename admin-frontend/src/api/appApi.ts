import axios, { AxiosRequestConfig, AxiosResponse } from "axios";
import { App } from "../types/app";
import { getToken, logout } from "../store/auth";

const BASE_URL = `${import.meta.env.VITE_API_URL}/api/apps`;

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

export const getApps = () =>
    axiosWithAuth<App[]>({ method: "GET", url: BASE_URL });

export const getApp = (id: string) =>
    axiosWithAuth<App>({ method: "GET", url: `${BASE_URL}/${id}` });

export const createApp = (data: App) =>
    axiosWithAuth<App>({ method: "POST", url: BASE_URL, data });

export const updateApp = (id: string, data: App) =>
    axiosWithAuth<App>({ method: "PUT", url: `${BASE_URL}/${id}`, data });

export const deleteApp = (id: string) =>
    axiosWithAuth<void>({ method: "DELETE", url: `${BASE_URL}/${id}` });
