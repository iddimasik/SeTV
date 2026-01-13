import React from "react";
import { Link } from "react-router-dom";
import AppList from "../components/AppList";
import "./Dashboard.css"; // подключаем CSS

const Dashboard: React.FC = () => {
    return (
        <div className="dashboard-container">
            <div className="dashboard-box">
                <div className="dashboard-header">
                    <h1 className="dashboard-title">Панель управления</h1>
                </div>
                <div style={{ textAlign: 'center', marginBottom: '40px' }}>
                    <Link to="/create" className="add-button">Добавить приложение</Link>
                </div>
                <AppList />
            </div>
        </div>
    );
};

export default Dashboard;
