// store/auth.ts

// Ключ для localStorage
const TOKEN_KEY = "token";

/**
 * Сохраняем JWT в localStorage
 */
export function setToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
}

/**
 * Получаем JWT из localStorage
 */
export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

/**
 * Удаляем токен (logout)
 */
export function logout() {
    localStorage.removeItem(TOKEN_KEY);
}

/**
 * Проверяем, авторизован ли пользователь
 */
export function isAuthenticated(): boolean {
    return getToken() !== null;
}

/**
 * Вспомогательная функция для fetch запросов с JWT
 */
export async function fetchWithAuth(
    url: string,
    options: RequestInit = {}
): Promise<any> {
    const token = getToken();
    if (!token) throw new Error("Нет токена, пользователь не авторизован");

    const headers = {
        ...options.headers,
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
    };

    const res = await fetch(url, { ...options, headers });

    if (res.status === 401) {
        logout();
        throw new Error("Сессия истекла, войдите снова");
    }

    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Ошибка запроса");
    }

    return res.json();
}
