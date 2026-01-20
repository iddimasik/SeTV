import React, {JSX} from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Dashboard from "./pages/Dashboard";
import AppDetails from "./pages/AppDetails";
import AppForm from "./components/AppForm";
import LoginPage from "./pages/LoginPage";
import { isAuthenticated } from "./store/auth";

const PrivateRoute = ({ children }: { children: JSX.Element }) => {
    return isAuthenticated() ? children : <Navigate to="/login" replace />;
};

const App: React.FC = () => {
    return (
        <Router>
            <Routes>
                <Route path="/login" element={<LoginPage />} />

                <Route
                    path="/"
                    element={
                        <PrivateRoute>
                            <Dashboard />
                        </PrivateRoute>
                    }
                />

                <Route
                    path="/create"
                    element={
                        <PrivateRoute>
                            <AppForm />
                        </PrivateRoute>
                    }
                />

                <Route
                    path="/edit/:id"
                    element={
                        <PrivateRoute>
                            <AppForm />
                        </PrivateRoute>
                    }
                />

                <Route
                    path="/details/:id"
                    element={
                        <PrivateRoute>
                            <AppDetails />
                        </PrivateRoute>
                    }
                />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Router>
    );
};

export default App;
