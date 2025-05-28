/// <reference types="react" />
import { BarCanvasLayer, BarCommonProps, BarDatum } from './types';
export declare const BarCanvas: import("react").ForwardRefExoticComponent<Partial<BarCommonProps<BarDatum>> & import("./types").DataProps<BarDatum> & import("./types").BarHandlers<BarDatum, HTMLCanvasElement> & import("@nivo/core").Dimensions & Partial<{
    axisBottom: import("@nivo/axes").CanvasAxisProps<any> | null;
    axisLeft: import("@nivo/axes").CanvasAxisProps<any> | null;
    axisRight: import("@nivo/axes").CanvasAxisProps<any> | null;
    axisTop: import("@nivo/axes").CanvasAxisProps<any> | null;
    renderBar: (context: CanvasRenderingContext2D, props: import("./types").RenderBarProps<BarDatum>) => void;
    layers: BarCanvasLayer<BarDatum>[];
    pixelRatio: number;
}> & import("react").RefAttributes<HTMLCanvasElement>>;
//# sourceMappingURL=BarCanvas.d.ts.map