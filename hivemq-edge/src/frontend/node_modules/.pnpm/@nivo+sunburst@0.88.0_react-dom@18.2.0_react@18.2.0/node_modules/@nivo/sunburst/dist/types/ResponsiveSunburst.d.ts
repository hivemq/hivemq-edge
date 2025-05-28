/// <reference types="react" />
import { SunburstSvgProps } from './types';
type ResponsiveSunburstProps<RawDatum> = Partial<Omit<SunburstSvgProps<RawDatum>, 'data' | 'width' | 'height'>> & Pick<SunburstSvgProps<RawDatum>, 'data'>;
export declare const ResponsiveSunburst: <RawDatum>(props: ResponsiveSunburstProps<RawDatum>) => JSX.Element;
export {};
//# sourceMappingURL=ResponsiveSunburst.d.ts.map