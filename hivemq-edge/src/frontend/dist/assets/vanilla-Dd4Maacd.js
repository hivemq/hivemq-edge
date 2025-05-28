import{bZ as C,af as T}from"./index-cMih3SEP.js";(function(){try{var e=typeof window<"u"?window:typeof global<"u"?global:typeof self<"u"?self:{},r=new e.Error().stack;r&&(e._sentryDebugIds=e._sentryDebugIds||{},e._sentryDebugIds[r]="e29305b4-8b54-4e54-b794-6e286ccb3c02",e._sentryDebugIdIdentifier="sentry-dbid-e29305b4-8b54-4e54-b794-6e286ccb3c02")}catch{}})();var x={exports:{}},E={},w={exports:{}},g={};/**
 * @license React
 * use-sync-external-store-shim.production.min.js
 *
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var R;function U(){if(R)return g;R=1;var e=C();function r(t,n){return t===n&&(t!==0||1/t===1/n)||t!==t&&n!==n}var a=typeof Object.is=="function"?Object.is:r,S=e.useState,p=e.useEffect,y=e.useLayoutEffect,m=e.useDebugValue;function v(t,n){var i=n(),f=S({inst:{value:i,getSnapshot:n}}),o=f[0].inst,d=f[1];return y(function(){o.value=i,o.getSnapshot=n,c(o)&&d({inst:o})},[t,i,n]),p(function(){return c(o)&&d({inst:o}),t(function(){c(o)&&d({inst:o})})},[t]),m(i),i}function c(t){var n=t.getSnapshot;t=t.value;try{var i=n();return!a(t,i)}catch{return!0}}function s(t,n){return n()}var u=typeof window>"u"||typeof window.document>"u"||typeof window.document.createElement>"u"?s:v;return g.useSyncExternalStore=e.useSyncExternalStore!==void 0?e.useSyncExternalStore:u,g}var O;function A(){return O||(O=1,w.exports=U()),w.exports}/**
 * @license React
 * use-sync-external-store-shim/with-selector.production.min.js
 *
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var V;function F(){if(V)return E;V=1;var e=C(),r=A();function a(s,u){return s===u&&(s!==0||1/s===1/u)||s!==s&&u!==u}var S=typeof Object.is=="function"?Object.is:a,p=r.useSyncExternalStore,y=e.useRef,m=e.useEffect,v=e.useMemo,c=e.useDebugValue;return E.useSyncExternalStoreWithSelector=function(s,u,t,n,i){var f=y(null);if(f.current===null){var o={hasValue:!1,value:null};f.current=o}else o=f.current;f=v(function(){function q(l){if(!D){if(D=!0,_=l,l=n(l),i!==void 0&&o.hasValue){var h=o.value;if(i(h,l))return b=h}return b=l}if(h=b,S(_,l))return h;var I=n(l);return i!==void 0&&i(h,I)?h:(_=l,b=I)}var D=!1,_,b,j=t===void 0?null:t;return[function(){return q(u())},j===null?void 0:function(){return q(j())}]},[u,t,n,i]);var d=p(s,f[0],f[1]);return m(function(){o.hasValue=!0,o.value=d},[d]),c(d),d},E}var W;function L(){return W||(W=1,x.exports=F()),x.exports}var M=L();const z=T(M),P={},k=e=>{let r;const a=new Set,S=(c,s)=>{const u=typeof c=="function"?c(r):c;if(!Object.is(u,r)){const t=r;r=s??(typeof u!="object"||u===null)?u:Object.assign({},r,u),a.forEach(n=>n(r,t))}},p=()=>r,v={setState:S,getState:p,subscribe:c=>(a.add(c),()=>a.delete(c)),destroy:()=>{(P?"production":void 0)!=="production"&&console.warn("[DEPRECATED] The `destroy` method will be unsupported in a future version. Instead use unsubscribe function returned by subscribe. Everything will be garbage-collected if store is garbage-collected."),a.clear()}};return r=e(S,p,v),v},B=e=>e?k(e):k;export{B as c,z as u};
//# sourceMappingURL=vanilla-Dd4Maacd.js.map
