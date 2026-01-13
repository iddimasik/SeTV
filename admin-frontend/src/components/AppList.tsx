import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getApps, deleteApp } from "../api/appApi";
import { App } from "../types/app"; // ✅ ВАЖНО
import "./AppList.css";

const AppList: React.FC = () => {
    const [apps, setApps] = useState<App[]>([]);
    const navigate = useNavigate();

    useEffect(() => {
        fetchApps();
    }, []);

    const fetchApps = async () => {
        const res = await getApps();
        setApps(res.data);
    };

    const handleDelete = async (id: string, e: React.MouseEvent) => {
        e.stopPropagation();
        if (window.confirm("Удалить приложение?")) {
            await deleteApp(id);
            fetchApps();
        }
    };

    const handleRowClick = (id?: string) => {
        if (!id) return; // защита от undefined
        navigate(`/edit/${id}`);
    };

    return (
        <div className="app-list-container">
            <h2 className="app-list-title">Список приложений</h2>

            <table className="app-table">
                <thead>
                <tr>
                    <th>Название</th>
                    <th>Пакет</th>
                    <th>Версия</th>
                    <th>Статус</th>
                    <th>Действия</th>
                </tr>
                </thead>
                <tbody>
                {apps.map((app) => (
                    <tr
                        key={app.id}
                        className="app-row"
                        onClick={() => handleRowClick(app.id)}
                    >
                        <td>{app.name}</td>
                        <td>{app.packageName}</td>
                        <td>{app.version || "-"}</td>
                        <td>
                            {app.status === "ACTIVE" ? (
                                <span className="status-active">Активно</span>
                            ) : (
                                <span className="status-inactive">Неактивно</span>
                            )}
                        </td>
                        <td>
                            <button
                                className="delete-button"
                                onClick={(e) => handleDelete(app.id!, e)}
                            >
                                Удалить
                            </button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
};

export default AppList;
