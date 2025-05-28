/// <reference types="react" />
import { SpringValues } from '@react-spring/web';
import { ArcAnimatedProps, ArcDatum, ArcGenerator, ChordCommonProps } from './types';
interface ChordArcProps {
    arc: ArcDatum;
    animatedProps: SpringValues<ArcAnimatedProps>;
    arcGenerator: ArcGenerator;
    borderWidth: number;
    setCurrent: (arc: ArcDatum | null) => void;
    isInteractive: ChordCommonProps['isInteractive'];
    onMouseEnter?: ChordCommonProps['onArcMouseEnter'];
    onMouseMove?: ChordCommonProps['onArcMouseMove'];
    onMouseLeave?: ChordCommonProps['onArcMouseLeave'];
    onClick?: ChordCommonProps['onArcClick'];
    tooltip: ChordCommonProps['arcTooltip'];
}
export declare const ChordArc: import("react").MemoExoticComponent<({ arc, animatedProps, borderWidth, arcGenerator, setCurrent, isInteractive, onMouseEnter, onMouseMove, onMouseLeave, onClick, tooltip, }: ChordArcProps) => JSX.Element>;
export {};
//# sourceMappingURL=ChordArc.d.ts.map