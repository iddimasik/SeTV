const TOKEN_KEY = "token";

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function logout() {
    localStorage.removeItem(TOKEN_KEY);
}

export function isAuthenticated(): boolean {
    return getToken() !== null;
}
