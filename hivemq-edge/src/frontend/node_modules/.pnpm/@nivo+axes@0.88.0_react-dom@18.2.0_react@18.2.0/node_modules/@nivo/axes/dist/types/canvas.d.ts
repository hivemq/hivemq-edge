import { CompleteTheme } from '@nivo/core';
import { ScaleValue, AnyScale, TicksSpec } from '@nivo/scales';
import { AxisLegendPosition, CanvasAxisProps, ValueFormatter } from './types';
export declare const renderAxisToCanvas: <Value extends ScaleValue>(ctx: CanvasRenderingContext2D, { axis, scale, x, y, length, ticksPosition, tickValues, tickSize, tickPadding, tickRotation, format: _format, legend, legendPosition, legendOffset, theme, }: {
    axis: 'x' | 'y';
    scale: AnyScale;
    x?: number | undefined;
    y?: number | undefined;
    length: number;
    ticksPosition: 'before' | 'after';
    tickValues?: TicksSpec<Value> | undefined;
    tickSize?: number | undefined;
    tickPadding?: number | undefined;
    tickRotation?: number | undefined;
    format?: string | ValueFormatter<Value> | undefined;
    legend?: string | undefined;
    legendPosition?: AxisLegendPosition | undefined;
    legendOffset?: number | undefined;
    theme: CompleteTheme;
}) => void;
export declare const renderAxesToCanvas: <X extends ScaleValue, Y extends ScaleValue>(ctx: CanvasRenderingContext2D, { xScale, yScale, width, height, top, right, bottom, left, theme, }: {
    xScale: AnyScale;
    yScale: AnyScale;
    width: number;
    height: number;
    top?: CanvasAxisProps<X> | null | undefined;
    right?: CanvasAxisProps<Y> | null | undefined;
    bottom?: CanvasAxisProps<X> | null | undefined;
    left?: CanvasAxisProps<Y> | null | undefined;
    theme: CompleteTheme;
}) => void;
export declare const renderGridLinesToCanvas: <Value extends ScaleValue>(ctx: CanvasRenderingContext2D, { width, height, scale, axis, values, }: {
    width: number;
    height: number;
    scale: AnyScale;
    axis: 'x' | 'y';
    values?: TicksSpec<Value> | undefined;
}) => void;
//# sourceMappingURL=canvas.d.ts.map