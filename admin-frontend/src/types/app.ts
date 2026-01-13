export interface App {
    id?: string;
    name: string;
    packageName: string;
    version?: string;
    description?: string;
    iconUrl?: string;
    bannerUrl?: string;
    apkUrl?: string;
    category?: string;
    status: string;
    featured: boolean;
}
