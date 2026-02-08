import { AppImage, AppRequest } from "../types/app";

/* ================= local id ================= */

export const generateLocalId = (): string =>
    Math.random().toString(36).substring(2) + Date.now().toString(36);

/* ================= APK ================= */

export const parseApk = async (
    file: File
): Promise<{
    name: string;
    packageName: string;
    versionName: string;
    apkUrl: string;
    iconUrl?: string;
}> => {
    const token = localStorage.getItem("token");

    const formData = new FormData();
    formData.append("file", file);

    const res = await fetch(
        `${import.meta.env.VITE_API_URL}/apps/parse-apk`,
        {
            method: "POST",
            headers: {
                Authorization: `Bearer ${token}`,
            },
            body: formData,
        }
    );

    if (!res.ok) {
        throw new Error("APK parse failed");
    }

    return res.json();
};

/* ================= IMAGES ================= */

export const uploadImages = async (
    files: File[]
): Promise<(AppImage & { localId: string })[]> => {
    const token = localStorage.getItem("token");

    const uploaded: (AppImage & { localId: string })[] = [];

    for (const file of files) {
        const fd = new FormData();
        fd.append("file", file);

        const res = await fetch(
            `${import.meta.env.VITE_API_URL}/apps/upload-image`,
            {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                },
                body: fd,
            }
        );

        if (!res.ok) {
            throw new Error("Image upload failed");
        }

        const data: AppImage = await res.json();

        uploaded.push({
            ...data,
            localId: generateLocalId(),
        });
    }

    return uploaded;
};

/* ================= PAYLOAD ================= */

export const buildAppPayload = (
    formData: AppRequest,
    images: (AppImage & { localId: string })[]
): AppRequest => ({
    ...formData,
    images: images.map((img, index) => ({
        imageUrl: img.imageUrl,
        sortOrder: index,
    })),
});
