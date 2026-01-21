import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getApp, createApp, updateApp } from "../api/appApi";
import { App } from "../types/app";
import "./AppForm.css";

const AppForm: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const [formData, setFormData] = useState<App>({
        name: "",
        packageName: "",
        version: "",
        description: "",
        iconUrl: "",
        bannerUrl: "",
        apkUrl: "",
        category: "",
        status: "ACTIVE",
        featured: false,
    });

    const [apkFile, setApkFile] = useState<File | null>(null);
    const [loadingApk, setLoadingApk] = useState(false);

    useEffect(() => {
        if (id) {
            getApp(id).then((res) => setFormData(res.data));
        }
    }, [id]);

    const handleChange = (
        e: React.ChangeEvent<
            HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
        >
    ) => {
        const { name, value, type } = e.target;
        const checked =
            type === "checkbox" && (e.target as HTMLInputElement).checked;

        setFormData((prev) => ({
            ...prev,
            [name]: type === "checkbox" ? checked : value,
        }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        try {
            if (id) {
                await updateApp(id, formData);
            } else {
                await createApp(formData);
            }

            navigate("/");
        } catch (err) {
            console.error("Save error:", err);
            alert("Ошибка при сохранении приложения");
        }
    };

    const handleApkSelect = async (
        e: React.ChangeEvent<HTMLInputElement>
    ) => {
        if (!e.target.files || e.target.files.length === 0) return;

        const file = e.target.files[0];
        setApkFile(file);

        const formDataUpload = new FormData();
        formDataUpload.append("file", file);

        const token = localStorage.getItem("token");

        try {
            setLoadingApk(true);

            const res = await fetch(
                `${import.meta.env.VITE_API_URL}/apps/parse-apk`,
                {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                    body: formDataUpload,
                }
            );

            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || "Ошибка парсинга APK");
            }

            const data = await res.json();

            setFormData((prev) => ({
                ...prev,
                name: data.name,
                packageName: data.packageName,
                version: data.versionName,
                apkUrl: data.apkUrl,
            }));
        } catch (err) {
            console.error("APK parse error:", err);
            alert("Не удалось распарсить APK");
        } finally {
            setLoadingApk(false);
        }
    };

    return (
        <div className="form-container">
            <div className="form-box">
                <h2 className="form-title">
                    {id
                        ? "Редактировать приложение"
                        : "Добавить новое приложение"}
                </h2>

                <form onSubmit={handleSubmit} className="form">
                    <label>
                        Название приложения
                        <input
                            type="text"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            required
                        />
                    </label>

                    <label>
                        Пакет (Package Name)
                        <input
                            type="text"
                            name="packageName"
                            value={formData.packageName}
                            onChange={handleChange}
                            required
                        />
                    </label>

                    <label>
                        Версия
                        <input
                            type="text"
                            name="version"
                            value={formData.version}
                            onChange={handleChange}
                        />
                    </label>

                    <label>
                        Категория
                        <input
                            type="text"
                            name="category"
                            value={formData.category}
                            onChange={handleChange}
                        />
                    </label>

                    <label>
                        Описание
                        <textarea
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                        />
                    </label>

                    <label>
                        URL иконки
                        <input
                            type="text"
                            name="iconUrl"
                            value={formData.iconUrl}
                            onChange={handleChange}
                        />
                    </label>

                    <label>
                        URL баннера
                        <input
                            type="text"
                            name="bannerUrl"
                            value={formData.bannerUrl}
                            onChange={handleChange}
                        />
                    </label>

                    <div className="file-upload">
                        <span className="file-title">APK файл</span>

                        <label
                            htmlFor="apk-input"
                            className="file-upload-label"
                        >
                            Загрузить APK
                        </label>

                        <input
                            id="apk-input"
                            type="file"
                            accept=".apk"
                            onChange={handleApkSelect}
                            hidden
                        />

                        {apkFile && (
                            <div className="file-name">
                                {apkFile.name}
                            </div>
                        )}

                        {loadingApk && (
                            <div className="file-loading">
                                Парсинг APK...
                            </div>
                        )}
                    </div>

                    <label className="checkbox-label">
                        Рекомендуемое приложение
                        <input
                            type="checkbox"
                            name="featured"
                            checked={formData.featured}
                            onChange={handleChange}
                        />
                    </label>

                    <label>
                        Статус
                        <select
                            name="status"
                            value={formData.status}
                            onChange={handleChange}
                        >
                            <option value="ACTIVE">Активно</option>
                            <option value="HIDDEN">Неактивно</option>
                        </select>
                    </label>

                    <button type="submit" className="submit-button">
                        {id ? "Обновить приложение" : "Добавить приложение"}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default AppForm;
