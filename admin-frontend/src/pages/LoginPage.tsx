import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./LoginPage.css";

async function loginApi(login: string, password: string): Promise<string> {
    const res = await fetch(`${import.meta.env.VITE_API_URL}/auth/login`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ login, password }),
    });

    if (!res.ok) {
        throw new Error("Неверный логин или пароль");
    }

    const data = await res.json();
    return data.token;
}

export default function LoginPage() {
    const [login, setLogin] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            const token = await loginApi(login, password);
            localStorage.setItem("token", token);
            navigate("/admin");
        } catch (err: any) {
            setError(err.message || "Ошибка входа");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-box">
                <h2 className="login-title">Введите данные для входа</h2>

                <form className="login-form" onSubmit={handleSubmit}>
                    <input
                        className="login-input"
                        type="text"
                        placeholder="Логин"
                        value={login}
                        onChange={(e) => setLogin(e.target.value)}
                        required
                    />

                    <input
                        className="login-input"
                        type="password"
                        placeholder="Пароль"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />

                    <button
                        className="login-button"
                        type="submit"
                        disabled={loading}
                    >
                        {loading ? "Вход..." : "Войти"}
                    </button>

                    {error && <p className="login-error">{error}</p>}
                </form>
            </div>
        </div>
    );
}
