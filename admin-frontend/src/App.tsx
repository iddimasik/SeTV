import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Dashboard from "./pages/Dashboard";
import AppDetails from "./pages/AppDetails";
import AppForm from "./components/AppForm";

const App: React.FC = () => {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/create" element={<AppForm />} />
                <Route path="/edit/:id" element={<AppForm />} />
                <Route path="/details/:id" element={<AppDetails />} />
            </Routes>
        </Router>
    );
};

export default App;
