/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
// @ts-nocheck
(() => {
  const headAppId = import.meta.env.VITE_MONITORING_HEAP
        // DO NOT MODIFY. Copied from Heap Analytics setup
  window.heap=window.heap||[],heap.load=function(e,t){window.heap.appid=e,window.heap.config=t=t||{};const r=document.createElement("script");r.type="text/javascript",r.async=!0,r.src="https://cdn.heapanalytics.com/js/heap-"+e+".js";const a=document.getElementsByTagName("script")[0];a.parentNode.insertBefore(r,a);for(let n=function(e){return function(){heap.push([e].concat(Array.prototype.slice.call(arguments,0)))}},p=["addEventProperties","addUserProperties","clearEventProperties","identify","resetIdentity","removeEventProperty","setEventProperties","track","unsetEventProperty"],o=0;o<p.length;o++)heap[p[o]]=n(p[o])};
  heap.load(headAppId);
})()
