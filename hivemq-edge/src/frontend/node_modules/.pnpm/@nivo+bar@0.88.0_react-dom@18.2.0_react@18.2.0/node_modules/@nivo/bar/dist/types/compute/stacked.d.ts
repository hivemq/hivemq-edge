import { Margin } from '@nivo/core';
import { OrdinalColorScale } from '@nivo/colors';
import { ScaleBand } from '@nivo/scales';
import { BarDatum, BarSvgProps, ComputedBarDatum, ComputedDatum } from '../types';
/**
 * Generates x/y scales & bars for stacked bar chart.
 */
export declare const generateStackedBars: <RawDatum extends BarDatum>({ data, layout, minValue, maxValue, reverse, width, height, padding, valueScale, indexScale: indexScaleConfig, hiddenIds, ...props }: Pick<Required<BarSvgProps<RawDatum>>, "data" | "width" | "height" | "keys" | "maxValue" | "minValue" | "innerPadding" | "padding" | "valueScale" | "indexScale" | "layout" | "reverse"> & {
    formatValue: (value: number) => string;
    getColor: OrdinalColorScale<ComputedDatum<RawDatum>>;
    getIndex: (datum: RawDatum) => string;
    getTooltipLabel: (datum: ComputedDatum<RawDatum>) => string;
    margin: Margin;
    hiddenIds?: readonly (string | number)[] | undefined;
}) => {
    xScale: ScaleBand<string> | import("@nivo/scales").ScaleLog | import("@nivo/scales").ScaleSymlog | import("@nivo/scales").ScaleLinear<number> | import("@nivo/scales").ScaleTime<Date | import("d3-scale").NumberValue> | import("@nivo/scales").ScalePoint<import("@nivo/scales").ScaleValue> | ScaleBand<import("@nivo/scales").ScaleValue>;
    yScale: ScaleBand<string> | import("@nivo/scales").ScaleLog | import("@nivo/scales").ScaleSymlog | import("@nivo/scales").ScaleLinear<number> | import("@nivo/scales").ScaleTime<Date | import("d3-scale").NumberValue> | import("@nivo/scales").ScalePoint<import("@nivo/scales").ScaleValue> | ScaleBand<import("@nivo/scales").ScaleValue>;
    bars: ComputedBarDatum<RawDatum>[];
};
//# sourceMappingURL=stacked.d.ts.map