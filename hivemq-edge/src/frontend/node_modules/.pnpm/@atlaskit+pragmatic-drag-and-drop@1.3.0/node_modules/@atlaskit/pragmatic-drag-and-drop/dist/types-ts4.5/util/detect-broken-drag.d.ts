export declare function getBindingsForBrokenDrags({ onDragEnd }: {
    onDragEnd: () => void;
}): readonly [
    {
        readonly type: "pointermove";
        readonly listener: () => void;
    },
    {
        readonly type: "pointerdown";
        readonly listener: () => void;
    }
];
