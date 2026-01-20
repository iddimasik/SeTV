import { useState } from "react";
import { useNavigate } from "react-router-dom";

// API-функция для логина
async function loginApi(login: string, password: string): Promise<string> {
    const res = await fetch("http://192.168.0.105:8080/api/auth/login", {
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
            localStorage.setItem("token", token); // сохраняем JWT
            navigate("/admin"); // редирект в админку
        } catch (err: any) {
            setError(err.message || "Ошибка входа");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ maxWidth: 400, margin: "100px auto", textAlign: "center" }}>
            <h2>Вход в админку</h2>
            <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                <input
                    type="text"
                    placeholder="Логин"
                    value={login}
                    onChange={(e) => setLogin(e.target.value)}
                    required
                    style={{ padding: 8, fontSize: 16 }}
                />
                <input
                    type="password"
                    placeholder="Пароль"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    style={{ padding: 8, fontSize: 16 }}
                />
                <button
                    type="submit"
                    disabled={loading}
                    style={{ padding: 10, fontSize: 16, cursor: "pointer" }}
                >
                    {loading ? "Вход..." : "Войти"}
                </button>
                {error && <p style={{ color: "red" }}>{error}</p>}
            </form>
        </div>
    );
}
