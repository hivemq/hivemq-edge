/// <reference types="react" />
import { OrdinalColorScaleConfig } from '@nivo/colors';
import { SunburstLayerId } from './types';
export declare const defaultProps: {
    id: string;
    value: string;
    cornerRadius: number;
    layers: SunburstLayerId[];
    colors: OrdinalColorScaleConfig<any>;
    colorBy: "id";
    inheritColorFromParent: boolean;
    childColor: {
        from: string;
    };
    borderWidth: number;
    borderColor: string;
    enableArcLabels: boolean;
    arcLabel: string;
    arcLabelsRadiusOffset: number;
    arcLabelsSkipAngle: number;
    arcLabelsTextColor: {
        theme: string;
    };
    animate: boolean;
    motionConfig: string;
    transitionMode: "startAngle" | "middleAngle" | "endAngle" | "innerRadius" | "centerRadius" | "outerRadius" | "pushIn" | "pushOut";
    isInteractive: boolean;
    defs: never[];
    fill: never[];
    tooltip: <RawDatum>({ id, formattedValue, color, }: import("./types").ComputedDatum<RawDatum>) => JSX.Element;
    role: string;
};
//# sourceMappingURL=props.d.ts.map