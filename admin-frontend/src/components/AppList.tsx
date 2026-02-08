import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getApps, deleteApp } from "../api/appApi";
import { App } from "../types/app";
import "./AppList.css";

const AppList: React.FC = () => {
    const [apps, setApps] = useState<App[]>([]);
    const navigate = useNavigate();

    useEffect(() => {
        fetchApps();
    }, []);

    const fetchApps = async () => {
        const res = await getApps();

        const sortedApps = res.data.sort((a, b) =>
            a.name.localeCompare(b.name, undefined, {
                sensitivity: "base",
            })
        );

        setApps(sortedApps);
    };

    const handleDelete = async (
        id: string,
        e: React.MouseEvent
    ) => {
        e.stopPropagation();

        if (window.confirm("Удалить приложение?")) {
            await deleteApp(id);
            fetchApps();
        }
    };

    const handleRowClick = (id?: string) => {
        if (!id) return;
        navigate(`/edit/${id}`);
    };

    const renderStatus = (status: App["status"]) => {
        switch (status) {
            case "ACTIVE":
                return (
                    <span className="status-active">
                        Активно
                    </span>
                );
            case "HIDDEN":
                return (
                    <span className="status-hidden">
                        Скрыто
                    </span>
                );
            case "DEPRECATED":
                return (
                    <span className="status-deprecated">
                        Устарело
                    </span>
                );
            default:
                return "-";
        }
    };

    return (
        <div className="app-list-container">
            <h2 className="app-list-title">
                Список приложений
            </h2>

            <table className="app-table">
                <thead>
                <tr>
                    <th></th>
                    <th>Название</th>
                    <th>Пакет</th>
                    <th>Версия</th>
                    <th>Статус</th>
                    <th>★</th>
                    <th>Действия</th>
                </tr>
                </thead>

                <tbody>
                {apps.map((app) => (
                    <tr
                        key={app.id}
                        className="app-row"
                        onClick={() =>
                            handleRowClick(app.id)
                        }
                    >
                        <td className="icon-cell">
                            {app.iconUrl ? (
                                <img
                                    src={app.iconUrl}
                                    alt={app.name}
                                    className="app-icon"
                                />
                            ) : (
                                <div className="icon-placeholder" />
                            )}
                        </td>

                        <td>{app.name}</td>
                        <td>{app.packageName}</td>
                        <td>{app.version || "-"}</td>
                        <td>{renderStatus(app.status)}</td>

                        <td>
                            {app.featured ? "★" : ""}
                        </td>

                        <td>
                            <button
                                className="delete-button"
                                onClick={(e) =>
                                    handleDelete(
                                        app.id!,
                                        e
                                    )
                                }
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
