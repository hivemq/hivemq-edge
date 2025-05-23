import { InheritedColorConfig } from '@nivo/colors';
import { SunburstCommonProps, ComputedDatum, DataProps, DatumId, SunburstCustomLayerProps } from './types';
export declare const useSunburst: <RawDatum>({ data, id, value, valueFormat, radius, cornerRadius, colors, colorBy, inheritColorFromParent, childColor, }: {
    data: RawDatum;
    id?: import("@nivo/core").PropertyAccessor<RawDatum, DatumId> | undefined;
    value?: import("@nivo/core").PropertyAccessor<RawDatum, number> | undefined;
    valueFormat?: import("@nivo/core").ValueFormat<number, void> | undefined;
    radius: number;
    cornerRadius?: number | undefined;
    colors?: import("@nivo/colors").OrdinalColorScaleConfig<Omit<ComputedDatum<RawDatum>, "color" | "fill">> | undefined;
    colorBy?: "id" | "depth" | undefined;
    inheritColorFromParent?: boolean | undefined;
    childColor?: InheritedColorConfig<ComputedDatum<RawDatum>> | undefined;
}) => {
    arcGenerator: import("@nivo/arcs").ArcGenerator;
    nodes: ComputedDatum<RawDatum>[];
};
/**
 * Memoize the context to pass to custom layers.
 */
export declare const useSunburstLayerContext: <RawDatum>({ nodes, arcGenerator, centerX, centerY, radius, }: SunburstCustomLayerProps<RawDatum>) => SunburstCustomLayerProps<RawDatum>;
//# sourceMappingURL=hooks.d.ts.map