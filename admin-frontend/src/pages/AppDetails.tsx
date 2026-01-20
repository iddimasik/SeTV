import React, { useEffect, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { getApp, deleteApp } from "../api/appApi";
import "./AppDetails.css";

interface AppDetailsProps {}

const AppDetails: React.FC<AppDetailsProps> = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [app, setApp] = useState<any>(null);

    useEffect(() => {
        if (id) {
            getApp(id).then((data) => setApp(data));
        }
    }, [id]);

    const handleDelete = async () => {
        if (id && window.confirm("Вы уверены, что хотите удалить это приложение?")) {
            await deleteApp(id);
            navigate("/");
        }
    };

    if (!app) return <div className="loading">Загрузка...</div>;

    return (
        <div className="app-details-container">
            <h2 className="app-details-title">{app.name}</h2>
            <p><strong>Пакет:</strong> {app.packageName}</p>
            <p><strong>Версия:</strong> {app.version || "-"}</p>
            <p><strong>Описание:</strong> {app.description}</p>
            <p><strong>Категория:</strong> {app.category}</p>
            <p><strong>Статус:</strong> {app.status === "ACTIVE" ? <span className="status-active">Активно</span> : <span className="status-inactive">Неактивно</span>}</p>
            <p><strong>Рекомендуемое:</strong> {app.featured ? "Да" : "Нет"}</p>

            <div className="app-details-buttons">
                <Link to={`/edit/${app.id}`} className="edit-button">Редактировать</Link>
                <button onClick={handleDelete} className="delete-button">Удалить</button>
                <Link to="/" className="back-button">Назад</Link>
            </div>
        </div>
    );
};

export default AppDetails;
