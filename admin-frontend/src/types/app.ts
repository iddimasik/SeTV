export type AppStatus = "ACTIVE" | "HIDDEN" | "DEPRECATED";

/* ================= IMAGES ================= */

/**
 * Image DTO
 * id — есть только у сохранённых изображений
 * localId — используется ТОЛЬКО на фронтенде (React key)
 */
export interface AppImage {
    id?: string;            // id из базы (отсутствует у новых)
    imageUrl: string;
    sortOrder?: number;     // бэк может выставлять сам
    localId?: string;       // временный id для фронта
}

/* ================= RESPONSE DTO ================= */

/**
 * Полная модель приложения, приходящая с бэкенда
 */
export interface App {
    id: string;

    name: string;
    packageName: string;

    version?: string;
    description?: string;

    iconUrl?: string;
    apkUrl?: string;

    category?: string;

    status: AppStatus;
    featured: boolean;

    images: AppImage[];
}

/* ================= REQUEST DTO ================= */

export interface AppRequest {
    name: string;
    packageName: string;

    version?: string;
    description?: string;

    iconUrl?: string;
    apkUrl?: string;

    category?: string;

    status: AppStatus;
    featured: boolean;

    images?: AppImage[]; // <-- ВАЖНО: соответствует AppForm
}
