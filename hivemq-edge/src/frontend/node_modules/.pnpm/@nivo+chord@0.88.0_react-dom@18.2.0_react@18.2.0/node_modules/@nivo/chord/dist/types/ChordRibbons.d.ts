/// <reference types="react" />
import { ChordCommonProps, ChordSvgProps, RibbonDatum, RibbonGenerator } from './types';
interface ChordRibbonsProps {
    ribbons: RibbonDatum[];
    ribbonGenerator: RibbonGenerator;
    borderWidth: ChordCommonProps['ribbonBorderWidth'];
    borderColor: ChordCommonProps['ribbonBorderColor'];
    getOpacity: (ribbon: RibbonDatum) => number;
    blendMode: NonNullable<ChordSvgProps['ribbonBlendMode']>;
    isInteractive: ChordCommonProps['isInteractive'];
    setCurrent: (ribbon: RibbonDatum | null) => void;
    tooltip: NonNullable<ChordSvgProps['ribbonTooltip']>;
    onMouseEnter: ChordSvgProps['onRibbonMouseEnter'];
    onMouseMove: ChordSvgProps['onRibbonMouseMove'];
    onMouseLeave: ChordSvgProps['onRibbonMouseLeave'];
    onClick: ChordSvgProps['onRibbonClick'];
}
export declare const ChordRibbons: import("react").MemoExoticComponent<({ ribbons, ribbonGenerator, borderWidth, borderColor, getOpacity, blendMode, isInteractive, setCurrent, onMouseEnter, onMouseMove, onMouseLeave, onClick, tooltip, }: ChordRibbonsProps) => JSX.Element>;
export {};
//# sourceMappingURL=ChordRibbons.d.ts.map