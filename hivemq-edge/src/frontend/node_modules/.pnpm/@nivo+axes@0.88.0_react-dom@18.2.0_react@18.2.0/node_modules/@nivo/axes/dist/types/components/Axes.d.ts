/// <reference types="react" />
import { ScaleValue, AnyScale } from '@nivo/scales';
import { AxisProps } from '../types';
export declare const Axes: import("react").MemoExoticComponent<(<X extends ScaleValue, Y extends ScaleValue>({ xScale, yScale, width, height, top, right, bottom, left, }: {
    xScale: AnyScale;
    yScale: AnyScale;
    width: number;
    height: number;
    top?: AxisProps<X> | null | undefined;
    right?: AxisProps<Y> | null | undefined;
    bottom?: AxisProps<X> | null | undefined;
    left?: AxisProps<Y> | null | undefined;
}) => JSX.Element)>;
//# sourceMappingURL=Axes.d.ts.map