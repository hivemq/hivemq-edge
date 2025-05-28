import { AnimationConfig } from '@react-spring/web';
import { BarCommonProps, BarDatum } from './types';
import { BarTotalsData } from './compute/totals';
interface Props<RawDatum extends BarDatum> {
    data: BarTotalsData[];
    springConfig: Partial<AnimationConfig>;
    animate: boolean;
    layout?: BarCommonProps<RawDatum>['layout'];
}
export declare const BarTotals: <RawDatum extends BarDatum>({ data, springConfig, animate, layout, }: Props<RawDatum>) => JSX.Element;
export {};
//# sourceMappingURL=BarTotals.d.ts.map