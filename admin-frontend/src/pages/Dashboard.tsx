import React from "react";
import { Link } from "react-router-dom";
import AppList from "../components/AppList";
import "./Dashboard.css";

const Dashboard: React.FC = () => {
    return (
        <div className="dashboard-container">
            <div className="dashboard-box">
                <div className="dashboard-header">
                    <h1 className="dashboard-title">Панель управления</h1>
                    <p className="dashboard-subtitle">
                        Управление приложениями Android TV
                    </p>
                </div>

                <div className="dashboard-actions">
                    <Link to="/create" className="add-button">
                        + Добавить приложение
                    </Link>
                </div>

                <AppList />
            </div>
        </div>
    );
};

export default Dashboard;
