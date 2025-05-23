/// <reference types="react" />
import { ArcDatum, ChordCommonProps } from './types';
interface ChordLabelsProps {
    arcs: ArcDatum[];
    radius: number;
    rotation: number;
    color: ChordCommonProps['labelTextColor'];
}
export declare const ChordLabels: import("react").MemoExoticComponent<({ arcs, radius, rotation, color }: ChordLabelsProps) => JSX.Element>;
export {};
//# sourceMappingURL=ChordLabels.d.ts.map