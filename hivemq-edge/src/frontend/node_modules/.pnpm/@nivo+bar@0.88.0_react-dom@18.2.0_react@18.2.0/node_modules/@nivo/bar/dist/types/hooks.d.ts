import { Margin } from '@nivo/core';
import { DataProps, BarCommonProps, BarDatum, ComputedBarDatumWithValue, LegendData, BarLegendProps } from './types';
export declare const useBar: <RawDatum extends BarDatum>({ indexBy, keys, label, tooltipLabel, valueFormat, colors, colorBy, borderColor, labelTextColor, groupMode, layout, reverse, data, minValue, maxValue, margin, width, height, padding, innerPadding, valueScale, indexScale, initialHiddenIds, enableLabel, labelSkipWidth, labelSkipHeight, legends, legendLabel, totalsOffset, }: {
    indexBy?: import("@nivo/core").PropertyAccessor<RawDatum, string> | undefined;
    label?: import("@nivo/core").PropertyAccessor<import("./types").ComputedDatum<RawDatum>, string> | undefined;
    tooltipLabel?: import("@nivo/core").PropertyAccessor<import("./types").ComputedDatum<RawDatum>, string> | undefined;
    valueFormat?: import("@nivo/core").ValueFormat<number, void> | undefined;
    colors?: import("@nivo/colors").OrdinalColorScaleConfig<import("./types").ComputedDatum<RawDatum>> | undefined;
    colorBy?: "id" | "indexValue" | undefined;
    borderColor?: import("@nivo/colors").InheritedColorConfig<ComputedBarDatumWithValue<RawDatum>> | undefined;
    labelTextColor?: import("@nivo/colors").InheritedColorConfig<ComputedBarDatumWithValue<RawDatum>> | undefined;
    groupMode?: "stacked" | "grouped" | undefined;
    layout?: "horizontal" | "vertical" | undefined;
    reverse?: boolean | undefined;
    data: readonly RawDatum[];
    keys?: readonly string[] | undefined;
    minValue?: number | "auto" | undefined;
    maxValue?: number | "auto" | undefined;
    margin: Margin;
    width: number;
    height: number;
    padding?: number | undefined;
    innerPadding?: number | undefined;
    valueScale?: import("@nivo/scales").ScaleSpec | undefined;
    indexScale?: import("@nivo/scales").ScaleBandSpec | undefined;
    initialHiddenIds?: readonly (string | number)[] | undefined;
    enableLabel?: boolean | undefined;
    labelSkipWidth?: number | undefined;
    labelSkipHeight?: number | undefined;
    legends?: readonly BarLegendProps[] | undefined;
    legendLabel?: import("@nivo/core").PropertyAccessor<import("./types").LegendLabelDatum<RawDatum>, string> | undefined;
    totalsOffset?: number | undefined;
}) => {
    bars: import("./types").ComputedBarDatum<RawDatum>[];
    barsWithValue: {
        index: number;
        key: string;
        data: import("./types").ComputedDatum<RawDatum> & {
            value: number;
        };
        x: number;
        y: number;
        absX: number;
        absY: number;
        width: number;
        height: number;
        color: string;
        label: string;
    }[];
    xScale: import("@nivo/scales").ScaleBand<string> | import("@nivo/scales").ScaleLog | import("@nivo/scales").ScaleSymlog | import("@nivo/scales").ScaleLinear<number> | import("@nivo/scales").ScaleTime<Date | import("d3-scale").NumberValue> | import("@nivo/scales").ScalePoint<import("@nivo/scales").ScaleValue> | import("@nivo/scales").ScaleBand<import("@nivo/scales").ScaleValue>;
    yScale: import("@nivo/scales").ScaleBand<string> | import("@nivo/scales").ScaleLog | import("@nivo/scales").ScaleSymlog | import("@nivo/scales").ScaleLinear<number> | import("@nivo/scales").ScaleTime<Date | import("d3-scale").NumberValue> | import("@nivo/scales").ScalePoint<import("@nivo/scales").ScaleValue> | import("@nivo/scales").ScaleBand<import("@nivo/scales").ScaleValue>;
    getIndex: (datum: RawDatum) => string;
    getLabel: (datum: import("./types").ComputedDatum<RawDatum>) => string;
    getTooltipLabel: (datum: import("./types").ComputedDatum<RawDatum>) => string;
    formatValue: (value: number) => string;
    getColor: import("@nivo/colors").OrdinalColorScale<import("./types").ComputedDatum<RawDatum>>;
    getBorderColor: import("@nivo/colors").InheritedColorConfigCustomFunction<ComputedBarDatumWithValue<RawDatum>> | ((d: ComputedBarDatumWithValue<RawDatum>) => any);
    getLabelColor: import("@nivo/colors").InheritedColorConfigCustomFunction<ComputedBarDatumWithValue<RawDatum>> | ((d: ComputedBarDatumWithValue<RawDatum>) => any);
    shouldRenderBarLabel: ({ width, height }: {
        height: number;
        width: number;
    }) => boolean;
    hiddenIds: readonly (string | number)[];
    toggleSerie: (id: string | number) => void;
    legendsWithData: [BarLegendProps, LegendData[]][];
    barTotals: import("./compute/totals").BarTotalsData[];
};
//# sourceMappingURL=hooks.d.ts.map