import { CSSProp } from 'styled-components';
declare global {
    interface Window {
        hljs: {
            highlightElement: (element: HTMLElement) => void;
        };
    }
}
declare module 'react' {
    interface Attributes {
        css?: CSSProp;
    }
}
