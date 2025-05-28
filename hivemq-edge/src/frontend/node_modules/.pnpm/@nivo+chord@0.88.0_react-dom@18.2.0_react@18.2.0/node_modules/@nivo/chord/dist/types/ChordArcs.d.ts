/// <reference types="react" />
import { ArcDatum, ArcGenerator, ChordCommonProps } from './types';
interface ChordArcsProps {
    arcs: ArcDatum[];
    arcGenerator: ArcGenerator;
    borderWidth: ChordCommonProps['arcBorderWidth'];
    borderColor: ChordCommonProps['arcBorderColor'];
    getOpacity: (arc: ArcDatum) => number;
    setCurrent: (arc: ArcDatum | null) => void;
    isInteractive: ChordCommonProps['isInteractive'];
    onMouseEnter?: ChordCommonProps['onArcMouseEnter'];
    onMouseMove?: ChordCommonProps['onArcMouseMove'];
    onMouseLeave?: ChordCommonProps['onArcMouseLeave'];
    onClick?: ChordCommonProps['onArcClick'];
    tooltip: ChordCommonProps['arcTooltip'];
}
export declare const ChordArcs: import("react").MemoExoticComponent<({ arcs, borderWidth, borderColor, getOpacity, arcGenerator, setCurrent, isInteractive, onMouseEnter, onMouseMove, onMouseLeave, onClick, tooltip, }: ChordArcsProps) => JSX.Element>;
export {};
//# sourceMappingURL=ChordArcs.d.ts.map