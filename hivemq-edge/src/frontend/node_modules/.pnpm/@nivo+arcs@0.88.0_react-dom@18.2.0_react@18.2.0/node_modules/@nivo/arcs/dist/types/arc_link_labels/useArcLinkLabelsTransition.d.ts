import { SpringValue } from '@react-spring/web';
import { InheritedColorConfig } from '@nivo/colors';
import { DatumWithArcAndColor } from '../types';
type AnimatedProps = {
    startAngle: number;
    endAngle: number;
    innerRadius: number;
    outerRadius: number;
    offset: number;
    diagonalLength: number;
    straightLength: number;
    textOffset: number;
    linkColor: string;
    textColor: string;
    opacity: number;
};
/**
 * This hook can be used to animate a group of arc link labels,
 * if you just want to compute the labels, please use `useArcLinkLabels`.
 */
export declare const useArcLinkLabelsTransition: <Datum extends DatumWithArcAndColor>({ data, offset, diagonalLength, straightLength, skipAngle, textOffset, linkColor, textColor, }: {
    data: Datum[];
    offset?: number | undefined;
    diagonalLength: number;
    straightLength: number;
    skipAngle?: number | undefined;
    textOffset: number;
    linkColor: InheritedColorConfig<Datum>;
    textColor: InheritedColorConfig<Datum>;
}) => {
    transition: import("@react-spring/core").TransitionFn<Datum, {
        offset: number;
        opacity: number;
        startAngle: number;
        endAngle: number;
        innerRadius: number;
        outerRadius: number;
        textColor: string;
        diagonalLength: number;
        straightLength: number;
        textOffset: number;
        linkColor: string;
    }>;
    interpolateLink: (startAngleValue: SpringValue<AnimatedProps['startAngle']>, endAngleValue: SpringValue<AnimatedProps['endAngle']>, innerRadiusValue: SpringValue<AnimatedProps['innerRadius']>, outerRadiusValue: SpringValue<AnimatedProps['outerRadius']>, offsetValue: SpringValue<AnimatedProps['offset']>, diagonalLengthValue: SpringValue<AnimatedProps['diagonalLength']>, straightLengthValue: SpringValue<AnimatedProps['straightLength']>) => import("@react-spring/core").Interpolation<string | null, any>;
    interpolateTextAnchor: (startAngleValue: SpringValue<AnimatedProps['startAngle']>, endAngleValue: SpringValue<AnimatedProps['endAngle']>, innerRadiusValue: SpringValue<AnimatedProps['innerRadius']>, outerRadiusValue: SpringValue<AnimatedProps['outerRadius']>) => import("@react-spring/core").Interpolation<"end" | "start", any>;
    interpolateTextPosition: (startAngleValue: SpringValue<AnimatedProps['startAngle']>, endAngleValue: SpringValue<AnimatedProps['endAngle']>, innerRadiusValue: SpringValue<AnimatedProps['innerRadius']>, outerRadiusValue: SpringValue<AnimatedProps['outerRadius']>, offsetValue: SpringValue<AnimatedProps['offset']>, diagonalLengthValue: SpringValue<AnimatedProps['diagonalLength']>, straightLengthValue: SpringValue<AnimatedProps['straightLength']>, textOffsetValue: SpringValue<AnimatedProps['textOffset']>) => import("@react-spring/core").Interpolation<string, any>;
};
export {};
//# sourceMappingURL=useArcLinkLabelsTransition.d.ts.map