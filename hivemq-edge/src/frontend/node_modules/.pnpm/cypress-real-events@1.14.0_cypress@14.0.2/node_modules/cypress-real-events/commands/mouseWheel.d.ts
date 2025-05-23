import { ScrollBehaviorOptions } from "../getCypressElementCoordinates";
export interface RealMouseWheelOptions {
    /**
     * Position relative to the element where to hover the element. Only supporting "center".
     * @example cy.realMouseWheel({ position: "center" })
     */
    position?: "center";
    /**
     * Controls how the page is scrolled to bring the subject into view, if needed.
     * @example cy.realMouseWheel({ scrollBehavior: "top" });
     */
    scrollBehavior?: ScrollBehaviorOptions;
    /**
     * deltaX X delta in CSS pixels for mouse wheel event (default: 0)
     * @example cy.realMouseWheel({ deltaX: 150 });
     */
    deltaX?: number;
    /**
     * deltaY Y delta in CSS pixels for mouse wheel event (default: 0)
     * @example cy.realMouseWheel({ deltaY: 150 });
     */
    deltaY?: number;
}
/** @ignore this, update documentation for this function at index.d.ts */
export declare function realMouseWheel(subject: JQuery, options?: RealMouseWheelOptions): Promise<JQuery<HTMLElement>>;
