import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getApp, createApp, updateApp } from "../api/appApi";
import { AppRequest, AppImage } from "../types/app";
import {
    generateLocalId,
    parseApk,
    uploadImages,
    buildAppPayload,
} from "../utils/appFormHelpers";

import {
    DndContext,
    closestCenter,
    DragEndEvent,
} from "@dnd-kit/core";

import {
    SortableContext,
    verticalListSortingStrategy,
    arrayMove,
} from "@dnd-kit/sortable";

import SortableImageItem from "../components/SortableImageItem";

import "./AppForm.css";

const AppForm: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const [formData, setFormData] = useState<AppRequest>({
        name: "",
        packageName: "",
        version: "",
        description: "",
        iconUrl: "",
        apkUrl: "",
        category: "",
        status: "ACTIVE",
        featured: false,
        images: [],
    });

    const [images, setImages] = useState<(AppImage & { localId: string })[]>([]);
    const [apkFile, setApkFile] = useState<File | null>(null);
    const [loadingApk, setLoadingApk] = useState(false);
    const [loadingImages, setLoadingImages] = useState(false);

    /* ================= load app ================= */

    useEffect(() => {
        if (!id) return;

        getApp(id).then((res) => {
            const app = res.data;

            setFormData({
                name: app.name,
                packageName: app.packageName,
                version: app.version ?? "",
                description: app.description ?? "",
                iconUrl: app.iconUrl ?? "",
                apkUrl: app.apkUrl ?? "",
                category: app.category ?? "",
                status: app.status,
                featured: app.featured,
                images: [],
            });

            setImages(
                (app.images ?? []).map((img: AppImage) => ({
                    ...img,
                    localId: generateLocalId(),
                }))
            );
        });
    }, [id]);

    /* ================= form ================= */

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
            const payload = buildAppPayload(formData, images);

            if (id) {
                await updateApp(id, payload);
            } else {
                await createApp(payload);
            }

            navigate("/");
        } catch (err) {
            console.error("Save error:", err);
            alert("Ошибка при сохранении приложения");
        }
    };

    /* ================= APK ================= */

    const handleApkSelect = async (
        e: React.ChangeEvent<HTMLInputElement>
    ) => {
        if (!e.target.files?.length) return;

        const file = e.target.files[0];
        setApkFile(file);

        try {
            setLoadingApk(true);

            const data = await parseApk(file);

            setFormData((prev) => ({
                ...prev,
                name: data.name,
                packageName: data.packageName,
                version: data.versionName,
                apkUrl: data.apkUrl,
                iconUrl: data.iconUrl ?? "",
            }));
        } catch {
            alert("Не удалось распарсить APK");
        } finally {
            setLoadingApk(false);
        }
    };

    /* ================= images ================= */

    const handleImageSelect = async (
        e: React.ChangeEvent<HTMLInputElement>
    ) => {
        if (!e.target.files) return;

        setLoadingImages(true);

        try {
            const uploaded = await uploadImages(Array.from(e.target.files));
            setImages((prev) => [...prev, ...uploaded]);
        } catch {
            alert("Ошибка загрузки изображений");
        } finally {
            setLoadingImages(false);
        }
    };

    const handleRemoveImage = async (localId: string, imageUrl: string) => {
        try {
            const token = localStorage.getItem("token");

            // Отправляем запрос на удаление изображения на сервере
            await fetch(`${import.meta.env.VITE_API_URL}/apps/delete-image`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({ imageUrl }),
            });

            // Удаляем изображение с фронта
            setImages((prev) => prev.filter((i) => i.localId !== localId));
        } catch (err) {
            console.error("Failed to delete image:", err);
            alert("Ошибка при удалении изображения");
        }
    };

    const handleDragEnd = (event: DragEndEvent) => {
        const { active, over } = event;

        if (!over || active.id === over.id) return;

        setImages((prev) => {
            const oldIndex = prev.findIndex(
                (i) => i.localId === active.id
            );
            const newIndex = prev.findIndex(
                (i) => i.localId === over.id
            );

            return arrayMove(prev, oldIndex, newIndex);
        });
    };

    /* ================= render ================= */

    return (
        <div className="form-container">
            <div className="form-box">
                <h2 className="form-title">
                    {id ? "Редактировать приложение" : "Добавить приложение"}
                </h2>

                <div className="file-upload">
                    <span className="file-title">APK файл</span>
                    <label htmlFor="apk-input" className="file-upload-label">
                        Загрузить APK
                    </label>
                    <input
                        id="apk-input"
                        type="file"
                        accept=".apk"
                        onChange={handleApkSelect}
                        hidden
                    />
                    {apkFile && <div className="file-name">{apkFile.name}</div>}
                    {loadingApk && (
                        <div className="file-loading">Парсинг APK...</div>
                    )}
                </div>

                <form onSubmit={handleSubmit} className="form">
                    <label>
                        Название
                        <input
                            type="text"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            required
                        />
                    </label>

                    <label>
                        Package
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
                        URL иконки
                        <input
                            type="text"
                            name="iconUrl"
                            value={formData.iconUrl}
                            onChange={handleChange}
                        />
                    </label>

                    {formData.iconUrl && (
                        <img
                            src={formData.iconUrl}
                            className="icon-preview-image"
                        />
                    )}

                    {/* screenshots */}
                    <div className="file-upload">
                        <span className="file-title">Изображения</span>
                        <label
                            htmlFor="images-input"
                            className="file-upload-label"
                        >
                            Загрузить изображение
                        </label>
                        <input
                            id="images-input"
                            type="file"
                            accept="image/*"
                            multiple
                            onChange={handleImageSelect}
                            hidden
                        />

                        {loadingImages && <div>Загрузка...</div>}

                        <DndContext
                            collisionDetection={closestCenter}
                            onDragEnd={handleDragEnd}
                        >
                            <SortableContext
                                items={images.map((i) => i.localId)}
                                strategy={verticalListSortingStrategy}
                            >
                                <div className="images-preview-container">
                                    {images.map((img) => (
                                        <SortableImageItem
                                            key={img.localId}
                                            image={img}
                                            onRemove={(localId) => handleRemoveImage(localId, img.imageUrl)}
                                        />
                                    ))}
                                </div>
                            </SortableContext>
                        </DndContext>
                    </div>

                    <label>
                        Категория
                        <select
                            name="category"
                            value={formData.category}
                            onChange={handleChange}
                        >
                            <option value="">Выбрать</option>
                            <option value="Фильмы и ТВ">Фильмы и ТВ</option>
                            <option value="Программы">Программы</option>
                            <option value="Прочее">Прочее</option>
                        </select>
                    </label>

                    <label>
                        Описание
                        <textarea
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                        />
                    </label>

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
                            <option value="HIDDEN">Скрыто</option>
                        </select>
                    </label>

                    <button type="submit" className="submit-button">
                        {id ? "Обновить" : "Создать"}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default AppForm;
