/// <reference types="react" />
import { SpringValues } from '@react-spring/web';
import { ChordCommonProps, ChordSvgProps, RibbonAnimatedProps, RibbonDatum, RibbonGenerator } from './types';
interface ChordRibbonProps {
    ribbon: RibbonDatum;
    ribbonGenerator: RibbonGenerator;
    animatedProps: SpringValues<RibbonAnimatedProps>;
    borderWidth: ChordCommonProps['ribbonBorderWidth'];
    blendMode: NonNullable<ChordSvgProps['ribbonBlendMode']>;
    setCurrent: (ribbon: RibbonDatum | null) => void;
    isInteractive: ChordCommonProps['isInteractive'];
    tooltip: NonNullable<ChordSvgProps['ribbonTooltip']>;
    onMouseEnter: ChordSvgProps['onRibbonMouseEnter'];
    onMouseMove: ChordSvgProps['onRibbonMouseMove'];
    onMouseLeave: ChordSvgProps['onRibbonMouseLeave'];
    onClick: ChordSvgProps['onRibbonClick'];
}
export declare const ChordRibbon: import("react").MemoExoticComponent<({ ribbon, ribbonGenerator, animatedProps, borderWidth, blendMode, isInteractive, setCurrent, onMouseEnter, onMouseMove, onMouseLeave, onClick, tooltip, }: ChordRibbonProps) => JSX.Element>;
export {};
//# sourceMappingURL=ChordRibbon.d.ts.map