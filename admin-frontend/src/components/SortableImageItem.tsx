import { CSS } from "@dnd-kit/utilities";
import { useSortable } from "@dnd-kit/sortable";
import { AppImage } from "../types/app";

type Props = {
    image: AppImage & { localId: string };
    onRemove: (id: string) => void;
};

const SortableImageItem: React.FC<Props> = ({ image, onRemove }) => {
    const {
        attributes,
        listeners,
        setNodeRef,
        transform,
        transition,
    } = useSortable({ id: image.localId });

    const style: React.CSSProperties = {
        transform: CSS.Transform.toString(transform),
        transition,
    };

    return (
        <div
            ref={setNodeRef}
            style={style}
            className="image-preview-wrapper-container"
        >
            {/* ⬇️ drag ТОЛЬКО за этот блок */}
            <div
                className="image-preview-wrapper"
                {...attributes}
                {...listeners}
                style={{ cursor: "grab" }}
            >
                <img
                    src={image.imageUrl}
                    className="image-preview"
                    draggable={false}
                />
            </div>

            <button
                type="button"
                className="remove-image-button"
                onClick={() => onRemove(image.localId)}
            >
                ×
            </button>

            <a href={image.imageUrl} target="_blank" rel="noreferrer">
                {image.imageUrl}
            </a>
        </div>
    );
};

export default SortableImageItem;
