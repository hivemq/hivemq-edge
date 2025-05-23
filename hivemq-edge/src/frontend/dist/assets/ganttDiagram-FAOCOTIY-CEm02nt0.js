import{_ as c,g as ze,s as Pe,m as Oe,n as Ne,a as Re,b as Be,c as lt,d as qe,aq as j,l as St,j as He,i as Ge,t as Xe,u as je}from"./PropertyPanelController-B_GTknPx.js";import{af as Gt}from"./index-cMih3SEP.js";import{R as pe,t as Ue,v as ge,w as ve,C as be,x as Lt,y as Ze,s as vt}from"./index-Dis5-5gm.js";import{t as Ke,a as ne,b as ie,c as Qe,d as Je,e as $e,f as tr,g as er,h as rr,i as nr,j as ae,k as se,l as oe,s as ce,m as le}from"./time-kNxt45NV.js";import{l as ir}from"./linear-DZrC6_ME.js";import{d as ar,r as sr}from"./math-BXzRHGEW.js";import"./index-iD7yky8Q.js";import"./types-CNm55BG_.js";import"./usePolicyGuards-DliXcvYq.js";import"./useDataHubDraftStore-BdIk6zJn.js";import"./index-UJo6opCj.js";import"./vanilla-Dd4Maacd.js";import"./middleware-DWkoLS6u.js";import"./index-BXWYaCHO.js";import"./without-BTTCd72_.js";import"./index-BTlOwmq4.js";import"./ArrayFieldTemplate-BvbIrrVp.js";import"./pick-DicWcAKr.js";import"./chunk-TRO7245M-Czmm3RtS.js";import"./chunk-JARCRF6W-CHhnzfpl.js";import"./chunk-ZPFGWTBB-27OtcFRz.js";import"./chunk-W7WUSNWJ-DJm2I2MC.js";import"./index-C23MLgnh.js";import"./chunk-RDF2AYID-BFNDpMsD.js";import"./creatable-select-CDfhMLkq.js";import"./use-chakra-select-props-D5NVA-bJ.js";import"./chunk-KC77MHL3-CpO6TSLY.js";import"./chunk-HB6KBUMZ-BnKuEGjs.js";import"./chunk-OCNORRQU-qVjYAyQ7.js";import"./index-DDmdO6wr.js";import"./select-Cl6Duh2E.js";import"./config.utils-FEQhkVx6.js";import"./toast-utils-BddXXXrS.js";import"./PaginatedTable-CpimMJK6.js";import"./datetime-B2_BsC7v.js";import"./chunk-KO6Q73AP-Bk5HKb-M.js";import"./chunk-VTV6N5LE-DWhDJEfB.js";import"./chunk-RPO2WXNL-BTibYaQa.js";import"./chunk-EL2VKIZQ-C-XtNqj-.js";import"./useListProtocolAdapters-B80RXV7q.js";import"./ArrayFieldItemTemplate-CQ7KDBCe.js";import"./color-Cs2FUV59.js";import"./useGetAllSchemas-BHgSFldN.js";import"./utils-Da84xa3e.js";import"./SchemaNode.utils-CqHD45U7.js";import"./index-C46KiF-i.js";import"./useGetAllDataPolicies-C_g1xUlx.js";import"./chunk-FHHZMTWR-BZ2wKB7t.js";import"./useGetAllBehaviorPolicies-C8fnxDpN.js";import"./step-BUvoxw0L.js";import"./bump-DxSIDwFB.js";/* empty css              */import"./init-D7C4L9XJ.js";(function(){try{var t=typeof window<"u"?window:typeof global<"u"?global:typeof self<"u"?self:{},e=new t.Error().stack;e&&(t._sentryDebugIds=t._sentryDebugIds||{},t._sentryDebugIds[e]="f28bff69-a6c8-4173-adf2-672fd1d5c0df",t._sentryDebugIdIdentifier="sentry-dbid-f28bff69-a6c8-4173-adf2-672fd1d5c0df")}catch{}})();const Et=18,xe=.96422,Te=1,we=.82521,_e=4/29,ut=6/29,De=3*ut*ut,or=ut*ut*ut;function Ce(t){if(t instanceof $)return new $(t.l,t.a,t.b,t.opacity);if(t instanceof rt)return Se(t);t instanceof pe||(t=Ue(t));var e=zt(t.r),r=zt(t.g),n=zt(t.b),i=Yt((.2225045*e+.7168786*r+.0606169*n)/Te),f,d;return e===r&&r===n?f=d=i:(f=Yt((.4360747*e+.3850649*r+.1430804*n)/xe),d=Yt((.0139322*e+.0971045*r+.7141733*n)/we)),new $(116*i-16,500*(f-i),200*(i-d),t.opacity)}function cr(t,e,r,n){return arguments.length===1?Ce(t):new $(t,e,r,n??1)}function $(t,e,r,n){this.l=+t,this.a=+e,this.b=+r,this.opacity=+n}ge($,cr,ve(be,{brighter(t){return new $(this.l+Et*(t??1),this.a,this.b,this.opacity)},darker(t){return new $(this.l-Et*(t??1),this.a,this.b,this.opacity)},rgb(){var t=(this.l+16)/116,e=isNaN(this.a)?t:t+this.a/500,r=isNaN(this.b)?t:t-this.b/200;return e=xe*Wt(e),t=Te*Wt(t),r=we*Wt(r),new pe(Vt(3.1338561*e-1.6168667*t-.4906146*r),Vt(-.9787684*e+1.9161415*t+.033454*r),Vt(.0719453*e-.2289914*t+1.4052427*r),this.opacity)}}));function Yt(t){return t>or?Math.pow(t,1/3):t/De+_e}function Wt(t){return t>ut?t*t*t:De*(t-_e)}function Vt(t){return 255*(t<=.0031308?12.92*t:1.055*Math.pow(t,1/2.4)-.055)}function zt(t){return(t/=255)<=.04045?t/12.92:Math.pow((t+.055)/1.055,2.4)}function lr(t){if(t instanceof rt)return new rt(t.h,t.c,t.l,t.opacity);if(t instanceof $||(t=Ce(t)),t.a===0&&t.b===0)return new rt(NaN,0<t.l&&t.l<100?0:NaN,t.l,t.opacity);var e=Math.atan2(t.b,t.a)*ar;return new rt(e<0?e+360:e,Math.sqrt(t.a*t.a+t.b*t.b),t.l,t.opacity)}function Ot(t,e,r,n){return arguments.length===1?lr(t):new rt(t,e,r,n??1)}function rt(t,e,r,n){this.h=+t,this.c=+e,this.l=+r,this.opacity=+n}function Se(t){if(isNaN(t.h))return new $(t.l,0,0,t.opacity);var e=t.h*sr;return new $(t.l,Math.cos(e)*t.c,Math.sin(e)*t.c,t.opacity)}ge(rt,Ot,ve(be,{brighter(t){return new rt(this.h,this.c,this.l+Et*(t??1),this.opacity)},darker(t){return new rt(this.h,this.c,this.l-Et*(t??1),this.opacity)},rgb(){return Se(this).rgb()}}));function ur(t){return function(e,r){var n=t((e=Ot(e)).h,(r=Ot(r)).h),i=Lt(e.c,r.c),f=Lt(e.l,r.l),d=Lt(e.opacity,r.opacity);return function(v){return e.h=n(v),e.c=i(v),e.l=f(v),e.opacity=d(v),e+""}}}const dr=ur(Ze);function fr(t,e){let r;if(e===void 0)for(const n of t)n!=null&&(r<n||r===void 0&&n>=n)&&(r=n);else{let n=-1;for(let i of t)(i=e(i,++n,t))!=null&&(r<i||r===void 0&&i>=i)&&(r=i)}return r}function hr(t,e){let r;if(e===void 0)for(const n of t)n!=null&&(r>n||r===void 0&&n>=n)&&(r=n);else{let n=-1;for(let i of t)(i=e(i,++n,t))!=null&&(r>i||r===void 0&&i>=i)&&(r=i)}return r}function mr(t){return t}var xt=1,Pt=2,Nt=3,bt=4,ue=1e-6;function kr(t){return"translate("+t+",0)"}function yr(t){return"translate(0,"+t+")"}function pr(t){return e=>+t(e)}function gr(t,e){return e=Math.max(0,t.bandwidth()-e*2)/2,t.round()&&(e=Math.round(e)),r=>+t(r)+e}function vr(){return!this.__axis}function Ee(t,e){var r=[],n=null,i=null,f=6,d=6,v=3,A=typeof window<"u"&&window.devicePixelRatio>1?0:.5,g=t===xt||t===bt?-1:1,S=t===bt||t===Pt?"x":"y",M=t===xt||t===Nt?kr:yr;function C(T){var q=n??(e.ticks?e.ticks.apply(e,r):e.domain()),m=i??(e.tickFormat?e.tickFormat.apply(e,r):mr),E=Math.max(f,0)+v,L=e.range(),I=+L[0]+A,O=+L[L.length-1]+A,N=(e.bandwidth?gr:pr)(e.copy(),A),X=T.selection?T.selection():T,R=X.selectAll(".domain").data([null]),V=X.selectAll(".tick").data(q,e).order(),p=V.exit(),w=V.enter().append("g").attr("class","tick"),x=V.select("line"),b=V.select("text");R=R.merge(R.enter().insert("path",".tick").attr("class","domain").attr("stroke","currentColor")),V=V.merge(w),x=x.merge(w.append("line").attr("stroke","currentColor").attr(S+"2",g*f)),b=b.merge(w.append("text").attr("fill","currentColor").attr(S,g*E).attr("dy",t===xt?"0em":t===Nt?"0.71em":"0.32em")),T!==X&&(R=R.transition(T),V=V.transition(T),x=x.transition(T),b=b.transition(T),p=p.transition(T).attr("opacity",ue).attr("transform",function(k){return isFinite(k=N(k))?M(k+A):this.getAttribute("transform")}),w.attr("opacity",ue).attr("transform",function(k){var D=this.parentNode.__axis;return M((D&&isFinite(D=D(k))?D:N(k))+A)})),p.remove(),R.attr("d",t===bt||t===Pt?d?"M"+g*d+","+I+"H"+A+"V"+O+"H"+g*d:"M"+A+","+I+"V"+O:d?"M"+I+","+g*d+"V"+A+"H"+O+"V"+g*d:"M"+I+","+A+"H"+O),V.attr("opacity",1).attr("transform",function(k){return M(N(k)+A)}),x.attr(S+"2",g*f),b.attr(S,g*E).text(m),X.filter(vr).attr("fill","none").attr("font-size",10).attr("font-family","sans-serif").attr("text-anchor",t===Pt?"start":t===bt?"end":"middle"),X.each(function(){this.__axis=N})}return C.scale=function(T){return arguments.length?(e=T,C):e},C.ticks=function(){return r=Array.from(arguments),C},C.tickArguments=function(T){return arguments.length?(r=T==null?[]:Array.from(T),C):r.slice()},C.tickValues=function(T){return arguments.length?(n=T==null?null:Array.from(T),C):n&&n.slice()},C.tickFormat=function(T){return arguments.length?(i=T,C):i},C.tickSize=function(T){return arguments.length?(f=d=+T,C):f},C.tickSizeInner=function(T){return arguments.length?(f=+T,C):f},C.tickSizeOuter=function(T){return arguments.length?(d=+T,C):d},C.tickPadding=function(T){return arguments.length?(v=+T,C):v},C.offset=function(T){return arguments.length?(A=+T,C):A},C}function br(t){return Ee(xt,t)}function xr(t){return Ee(Nt,t)}var Tt={exports:{}},Tr=Tt.exports,de;function wr(){return de||(de=1,function(t,e){(function(r,n){t.exports=n()})(Tr,function(){var r="day";return function(n,i,f){var d=function(g){return g.add(4-g.isoWeekday(),r)},v=i.prototype;v.isoWeekYear=function(){return d(this).year()},v.isoWeek=function(g){if(!this.$utils().u(g))return this.add(7*(g-this.isoWeek()),r);var S,M,C,T,q=d(this),m=(S=this.isoWeekYear(),M=this.$u,C=(M?f.utc:f)().year(S).startOf("year"),T=4-C.isoWeekday(),C.isoWeekday()>4&&(T+=7),C.add(T,r));return q.diff(m,"week")+1},v.isoWeekday=function(g){return this.$utils().u(g)?this.day()||7:this.day(this.day()%7?g:g-7)};var A=v.startOf;v.startOf=function(g,S){var M=this.$utils(),C=!!M.u(S)||S;return M.p(g)==="isoweek"?C?this.date(this.date()-(this.isoWeekday()-1)).startOf("day"):this.date(this.date()-1-(this.isoWeekday()-1)+7).endOf("day"):A.bind(this)(g,S)}}})}(Tt)),Tt.exports}var _r=wr();const Dr=Gt(_r);var wt={exports:{}},Cr=wt.exports,fe;function Sr(){return fe||(fe=1,function(t,e){(function(r,n){t.exports=n()})(Cr,function(){var r={LTS:"h:mm:ss A",LT:"h:mm A",L:"MM/DD/YYYY",LL:"MMMM D, YYYY",LLL:"MMMM D, YYYY h:mm A",LLLL:"dddd, MMMM D, YYYY h:mm A"},n=/(\[[^[]*\])|([-_:/.,()\s]+)|(A|a|YYYY|YY?|MM?M?M?|Do|DD?|hh?|HH?|mm?|ss?|S{1,3}|z|ZZ?)/g,i=/\d\d/,f=/\d\d?/,d=/\d*[^-_:/,()\s\d]+/,v={},A=function(m){return(m=+m)+(m>68?1900:2e3)},g=function(m){return function(E){this[m]=+E}},S=[/[+-]\d\d:?(\d\d)?|Z/,function(m){(this.zone||(this.zone={})).offset=function(E){if(!E||E==="Z")return 0;var L=E.match(/([+-]|\d\d)/g),I=60*L[1]+(+L[2]||0);return I===0?0:L[0]==="+"?-I:I}(m)}],M=function(m){var E=v[m];return E&&(E.indexOf?E:E.s.concat(E.f))},C=function(m,E){var L,I=v.meridiem;if(I){for(var O=1;O<=24;O+=1)if(m.indexOf(I(O,0,E))>-1){L=O>12;break}}else L=m===(E?"pm":"PM");return L},T={A:[d,function(m){this.afternoon=C(m,!1)}],a:[d,function(m){this.afternoon=C(m,!0)}],S:[/\d/,function(m){this.milliseconds=100*+m}],SS:[i,function(m){this.milliseconds=10*+m}],SSS:[/\d{3}/,function(m){this.milliseconds=+m}],s:[f,g("seconds")],ss:[f,g("seconds")],m:[f,g("minutes")],mm:[f,g("minutes")],H:[f,g("hours")],h:[f,g("hours")],HH:[f,g("hours")],hh:[f,g("hours")],D:[f,g("day")],DD:[i,g("day")],Do:[d,function(m){var E=v.ordinal,L=m.match(/\d+/);if(this.day=L[0],E)for(var I=1;I<=31;I+=1)E(I).replace(/\[|\]/g,"")===m&&(this.day=I)}],M:[f,g("month")],MM:[i,g("month")],MMM:[d,function(m){var E=M("months"),L=(M("monthsShort")||E.map(function(I){return I.slice(0,3)})).indexOf(m)+1;if(L<1)throw new Error;this.month=L%12||L}],MMMM:[d,function(m){var E=M("months").indexOf(m)+1;if(E<1)throw new Error;this.month=E%12||E}],Y:[/[+-]?\d+/,g("year")],YY:[i,function(m){this.year=A(m)}],YYYY:[/\d{4}/,g("year")],Z:S,ZZ:S};function q(m){var E,L;E=m,L=v&&v.formats;for(var I=(m=E.replace(/(\[[^\]]+])|(LTS?|l{1,4}|L{1,4})/g,function(w,x,b){var k=b&&b.toUpperCase();return x||L[b]||r[b]||L[k].replace(/(\[[^\]]+])|(MMMM|MM|DD|dddd)/g,function(D,o,u){return o||u.slice(1)})})).match(n),O=I.length,N=0;N<O;N+=1){var X=I[N],R=T[X],V=R&&R[0],p=R&&R[1];I[N]=p?{regex:V,parser:p}:X.replace(/^\[|\]$/g,"")}return function(w){for(var x={},b=0,k=0;b<O;b+=1){var D=I[b];if(typeof D=="string")k+=D.length;else{var o=D.regex,u=D.parser,y=w.slice(k),h=o.exec(y)[0];u.call(x,h),w=w.replace(h,"")}}return function(_){var a=_.afternoon;if(a!==void 0){var l=_.hours;a?l<12&&(_.hours+=12):l===12&&(_.hours=0),delete _.afternoon}}(x),x}}return function(m,E,L){L.p.customParseFormat=!0,m&&m.parseTwoDigitYear&&(A=m.parseTwoDigitYear);var I=E.prototype,O=I.parse;I.parse=function(N){var X=N.date,R=N.utc,V=N.args;this.$u=R;var p=V[1];if(typeof p=="string"){var w=V[2]===!0,x=V[3]===!0,b=w||x,k=V[2];x&&(k=V[2]),v=this.$locale(),!w&&k&&(v=L.Ls[k]),this.$d=function(y,h,_){try{if(["x","X"].indexOf(h)>-1)return new Date((h==="X"?1e3:1)*y);var a=q(h)(y),l=a.year,s=a.month,W=a.day,F=a.hours,Y=a.minutes,H=a.seconds,z=a.milliseconds,P=a.zone,U=new Date,nt=W||(l||s?1:U.getDate()),it=l||U.getFullYear(),st=0;l&&!s||(st=s>0?s-1:U.getMonth());var ht=F||0,ot=Y||0,G=H||0,Q=z||0;return P?new Date(Date.UTC(it,st,nt,ht,ot,G,Q+60*P.offset*1e3)):_?new Date(Date.UTC(it,st,nt,ht,ot,G,Q)):new Date(it,st,nt,ht,ot,G,Q)}catch{return new Date("")}}(X,p,R),this.init(),k&&k!==!0&&(this.$L=this.locale(k).$L),b&&X!=this.format(p)&&(this.$d=new Date("")),v={}}else if(p instanceof Array)for(var D=p.length,o=1;o<=D;o+=1){V[1]=p[o-1];var u=L.apply(this,V);if(u.isValid()){this.$d=u.$d,this.$L=u.$L,this.init();break}o===D&&(this.$d=new Date(""))}else O.call(this,N)}}})}(wt)),wt.exports}var Er=Sr();const Mr=Gt(Er);var _t={exports:{}},Ar=_t.exports,he;function Ir(){return he||(he=1,function(t,e){(function(r,n){t.exports=n()})(Ar,function(){return function(r,n){var i=n.prototype,f=i.format;i.format=function(d){var v=this,A=this.$locale();if(!this.isValid())return f.bind(this)(d);var g=this.$utils(),S=(d||"YYYY-MM-DDTHH:mm:ssZ").replace(/\[([^\]]+)]|Q|wo|ww|w|WW|W|zzz|z|gggg|GGGG|Do|X|x|k{1,2}|S/g,function(M){switch(M){case"Q":return Math.ceil((v.$M+1)/3);case"Do":return A.ordinal(v.$D);case"gggg":return v.weekYear();case"GGGG":return v.isoWeekYear();case"wo":return A.ordinal(v.week(),"W");case"w":case"ww":return g.s(v.week(),M==="w"?1:2,"0");case"W":case"WW":return g.s(v.isoWeek(),M==="W"?1:2,"0");case"k":case"kk":return g.s(String(v.$H===0?24:v.$H),M==="k"?1:2,"0");case"X":return Math.floor(v.$d.getTime()/1e3);case"x":return v.$d.getTime();case"z":return"["+v.offsetName()+"]";case"zzz":return"["+v.offsetName("long")+"]";default:return M}});return f.bind(this)(S)}}})}(_t)),_t.exports}var Fr=Ir();const Lr=Gt(Fr);var Rt=function(){var t=c(function(D,o,u,y){for(u=u||{},y=D.length;y--;u[D[y]]=o);return u},"o"),e=[6,8,10,12,13,14,15,16,17,18,20,21,22,23,24,25,26,27,28,29,30,31,33,35,36,38,40],r=[1,26],n=[1,27],i=[1,28],f=[1,29],d=[1,30],v=[1,31],A=[1,32],g=[1,33],S=[1,34],M=[1,9],C=[1,10],T=[1,11],q=[1,12],m=[1,13],E=[1,14],L=[1,15],I=[1,16],O=[1,19],N=[1,20],X=[1,21],R=[1,22],V=[1,23],p=[1,25],w=[1,35],x={trace:c(function(){},"trace"),yy:{},symbols_:{error:2,start:3,gantt:4,document:5,EOF:6,line:7,SPACE:8,statement:9,NL:10,weekday:11,weekday_monday:12,weekday_tuesday:13,weekday_wednesday:14,weekday_thursday:15,weekday_friday:16,weekday_saturday:17,weekday_sunday:18,weekend:19,weekend_friday:20,weekend_saturday:21,dateFormat:22,inclusiveEndDates:23,topAxis:24,axisFormat:25,tickInterval:26,excludes:27,includes:28,todayMarker:29,title:30,acc_title:31,acc_title_value:32,acc_descr:33,acc_descr_value:34,acc_descr_multiline_value:35,section:36,clickStatement:37,taskTxt:38,taskData:39,click:40,callbackname:41,callbackargs:42,href:43,clickStatementDebug:44,$accept:0,$end:1},terminals_:{2:"error",4:"gantt",6:"EOF",8:"SPACE",10:"NL",12:"weekday_monday",13:"weekday_tuesday",14:"weekday_wednesday",15:"weekday_thursday",16:"weekday_friday",17:"weekday_saturday",18:"weekday_sunday",20:"weekend_friday",21:"weekend_saturday",22:"dateFormat",23:"inclusiveEndDates",24:"topAxis",25:"axisFormat",26:"tickInterval",27:"excludes",28:"includes",29:"todayMarker",30:"title",31:"acc_title",32:"acc_title_value",33:"acc_descr",34:"acc_descr_value",35:"acc_descr_multiline_value",36:"section",38:"taskTxt",39:"taskData",40:"click",41:"callbackname",42:"callbackargs",43:"href"},productions_:[0,[3,3],[5,0],[5,2],[7,2],[7,1],[7,1],[7,1],[11,1],[11,1],[11,1],[11,1],[11,1],[11,1],[11,1],[19,1],[19,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,2],[9,2],[9,1],[9,1],[9,1],[9,2],[37,2],[37,3],[37,3],[37,4],[37,3],[37,4],[37,2],[44,2],[44,3],[44,3],[44,4],[44,3],[44,4],[44,2]],performAction:c(function(o,u,y,h,_,a,l){var s=a.length-1;switch(_){case 1:return a[s-1];case 2:this.$=[];break;case 3:a[s-1].push(a[s]),this.$=a[s-1];break;case 4:case 5:this.$=a[s];break;case 6:case 7:this.$=[];break;case 8:h.setWeekday("monday");break;case 9:h.setWeekday("tuesday");break;case 10:h.setWeekday("wednesday");break;case 11:h.setWeekday("thursday");break;case 12:h.setWeekday("friday");break;case 13:h.setWeekday("saturday");break;case 14:h.setWeekday("sunday");break;case 15:h.setWeekend("friday");break;case 16:h.setWeekend("saturday");break;case 17:h.setDateFormat(a[s].substr(11)),this.$=a[s].substr(11);break;case 18:h.enableInclusiveEndDates(),this.$=a[s].substr(18);break;case 19:h.TopAxis(),this.$=a[s].substr(8);break;case 20:h.setAxisFormat(a[s].substr(11)),this.$=a[s].substr(11);break;case 21:h.setTickInterval(a[s].substr(13)),this.$=a[s].substr(13);break;case 22:h.setExcludes(a[s].substr(9)),this.$=a[s].substr(9);break;case 23:h.setIncludes(a[s].substr(9)),this.$=a[s].substr(9);break;case 24:h.setTodayMarker(a[s].substr(12)),this.$=a[s].substr(12);break;case 27:h.setDiagramTitle(a[s].substr(6)),this.$=a[s].substr(6);break;case 28:this.$=a[s].trim(),h.setAccTitle(this.$);break;case 29:case 30:this.$=a[s].trim(),h.setAccDescription(this.$);break;case 31:h.addSection(a[s].substr(8)),this.$=a[s].substr(8);break;case 33:h.addTask(a[s-1],a[s]),this.$="task";break;case 34:this.$=a[s-1],h.setClickEvent(a[s-1],a[s],null);break;case 35:this.$=a[s-2],h.setClickEvent(a[s-2],a[s-1],a[s]);break;case 36:this.$=a[s-2],h.setClickEvent(a[s-2],a[s-1],null),h.setLink(a[s-2],a[s]);break;case 37:this.$=a[s-3],h.setClickEvent(a[s-3],a[s-2],a[s-1]),h.setLink(a[s-3],a[s]);break;case 38:this.$=a[s-2],h.setClickEvent(a[s-2],a[s],null),h.setLink(a[s-2],a[s-1]);break;case 39:this.$=a[s-3],h.setClickEvent(a[s-3],a[s-1],a[s]),h.setLink(a[s-3],a[s-2]);break;case 40:this.$=a[s-1],h.setLink(a[s-1],a[s]);break;case 41:case 47:this.$=a[s-1]+" "+a[s];break;case 42:case 43:case 45:this.$=a[s-2]+" "+a[s-1]+" "+a[s];break;case 44:case 46:this.$=a[s-3]+" "+a[s-2]+" "+a[s-1]+" "+a[s];break}},"anonymous"),table:[{3:1,4:[1,2]},{1:[3]},t(e,[2,2],{5:3}),{6:[1,4],7:5,8:[1,6],9:7,10:[1,8],11:17,12:r,13:n,14:i,15:f,16:d,17:v,18:A,19:18,20:g,21:S,22:M,23:C,24:T,25:q,26:m,27:E,28:L,29:I,30:O,31:N,33:X,35:R,36:V,37:24,38:p,40:w},t(e,[2,7],{1:[2,1]}),t(e,[2,3]),{9:36,11:17,12:r,13:n,14:i,15:f,16:d,17:v,18:A,19:18,20:g,21:S,22:M,23:C,24:T,25:q,26:m,27:E,28:L,29:I,30:O,31:N,33:X,35:R,36:V,37:24,38:p,40:w},t(e,[2,5]),t(e,[2,6]),t(e,[2,17]),t(e,[2,18]),t(e,[2,19]),t(e,[2,20]),t(e,[2,21]),t(e,[2,22]),t(e,[2,23]),t(e,[2,24]),t(e,[2,25]),t(e,[2,26]),t(e,[2,27]),{32:[1,37]},{34:[1,38]},t(e,[2,30]),t(e,[2,31]),t(e,[2,32]),{39:[1,39]},t(e,[2,8]),t(e,[2,9]),t(e,[2,10]),t(e,[2,11]),t(e,[2,12]),t(e,[2,13]),t(e,[2,14]),t(e,[2,15]),t(e,[2,16]),{41:[1,40],43:[1,41]},t(e,[2,4]),t(e,[2,28]),t(e,[2,29]),t(e,[2,33]),t(e,[2,34],{42:[1,42],43:[1,43]}),t(e,[2,40],{41:[1,44]}),t(e,[2,35],{43:[1,45]}),t(e,[2,36]),t(e,[2,38],{42:[1,46]}),t(e,[2,37]),t(e,[2,39])],defaultActions:{},parseError:c(function(o,u){if(u.recoverable)this.trace(o);else{var y=new Error(o);throw y.hash=u,y}},"parseError"),parse:c(function(o){var u=this,y=[0],h=[],_=[null],a=[],l=this.table,s="",W=0,F=0,Y=2,H=1,z=a.slice.call(arguments,1),P=Object.create(this.lexer),U={yy:{}};for(var nt in this.yy)Object.prototype.hasOwnProperty.call(this.yy,nt)&&(U.yy[nt]=this.yy[nt]);P.setInput(o,U.yy),U.yy.lexer=P,U.yy.parser=this,typeof P.yylloc>"u"&&(P.yylloc={});var it=P.yylloc;a.push(it);var st=P.options&&P.options.ranges;typeof U.yy.parseError=="function"?this.parseError=U.yy.parseError:this.parseError=Object.getPrototypeOf(this).parseError;function ht(K){y.length=y.length-2*K,_.length=_.length-K,a.length=a.length-K}c(ht,"popStack");function ot(){var K;return K=h.pop()||P.lex()||H,typeof K!="number"&&(K instanceof Array&&(h=K,K=h.pop()),K=u.symbols_[K]||K),K}c(ot,"lex");for(var G,Q,Z,It,ct={},pt,tt,re,gt;;){if(Q=y[y.length-1],this.defaultActions[Q]?Z=this.defaultActions[Q]:((G===null||typeof G>"u")&&(G=ot()),Z=l[Q]&&l[Q][G]),typeof Z>"u"||!Z.length||!Z[0]){var Ft="";gt=[];for(pt in l[Q])this.terminals_[pt]&&pt>Y&&gt.push("'"+this.terminals_[pt]+"'");P.showPosition?Ft="Parse error on line "+(W+1)+`:
`+P.showPosition()+`
Expecting `+gt.join(", ")+", got '"+(this.terminals_[G]||G)+"'":Ft="Parse error on line "+(W+1)+": Unexpected "+(G==H?"end of input":"'"+(this.terminals_[G]||G)+"'"),this.parseError(Ft,{text:P.match,token:this.terminals_[G]||G,line:P.yylineno,loc:it,expected:gt})}if(Z[0]instanceof Array&&Z.length>1)throw new Error("Parse Error: multiple actions possible at state: "+Q+", token: "+G);switch(Z[0]){case 1:y.push(G),_.push(P.yytext),a.push(P.yylloc),y.push(Z[1]),G=null,F=P.yyleng,s=P.yytext,W=P.yylineno,it=P.yylloc;break;case 2:if(tt=this.productions_[Z[1]][1],ct.$=_[_.length-tt],ct._$={first_line:a[a.length-(tt||1)].first_line,last_line:a[a.length-1].last_line,first_column:a[a.length-(tt||1)].first_column,last_column:a[a.length-1].last_column},st&&(ct._$.range=[a[a.length-(tt||1)].range[0],a[a.length-1].range[1]]),It=this.performAction.apply(ct,[s,F,W,U.yy,Z[1],_,a].concat(z)),typeof It<"u")return It;tt&&(y=y.slice(0,-1*tt*2),_=_.slice(0,-1*tt),a=a.slice(0,-1*tt)),y.push(this.productions_[Z[1]][0]),_.push(ct.$),a.push(ct._$),re=l[y[y.length-2]][y[y.length-1]],y.push(re);break;case 3:return!0}}return!0},"parse")},b=function(){var D={EOF:1,parseError:c(function(u,y){if(this.yy.parser)this.yy.parser.parseError(u,y);else throw new Error(u)},"parseError"),setInput:c(function(o,u){return this.yy=u||this.yy||{},this._input=o,this._more=this._backtrack=this.done=!1,this.yylineno=this.yyleng=0,this.yytext=this.matched=this.match="",this.conditionStack=["INITIAL"],this.yylloc={first_line:1,first_column:0,last_line:1,last_column:0},this.options.ranges&&(this.yylloc.range=[0,0]),this.offset=0,this},"setInput"),input:c(function(){var o=this._input[0];this.yytext+=o,this.yyleng++,this.offset++,this.match+=o,this.matched+=o;var u=o.match(/(?:\r\n?|\n).*/g);return u?(this.yylineno++,this.yylloc.last_line++):this.yylloc.last_column++,this.options.ranges&&this.yylloc.range[1]++,this._input=this._input.slice(1),o},"input"),unput:c(function(o){var u=o.length,y=o.split(/(?:\r\n?|\n)/g);this._input=o+this._input,this.yytext=this.yytext.substr(0,this.yytext.length-u),this.offset-=u;var h=this.match.split(/(?:\r\n?|\n)/g);this.match=this.match.substr(0,this.match.length-1),this.matched=this.matched.substr(0,this.matched.length-1),y.length-1&&(this.yylineno-=y.length-1);var _=this.yylloc.range;return this.yylloc={first_line:this.yylloc.first_line,last_line:this.yylineno+1,first_column:this.yylloc.first_column,last_column:y?(y.length===h.length?this.yylloc.first_column:0)+h[h.length-y.length].length-y[0].length:this.yylloc.first_column-u},this.options.ranges&&(this.yylloc.range=[_[0],_[0]+this.yyleng-u]),this.yyleng=this.yytext.length,this},"unput"),more:c(function(){return this._more=!0,this},"more"),reject:c(function(){if(this.options.backtrack_lexer)this._backtrack=!0;else return this.parseError("Lexical error on line "+(this.yylineno+1)+`. You can only invoke reject() in the lexer when the lexer is of the backtracking persuasion (options.backtrack_lexer = true).
`+this.showPosition(),{text:"",token:null,line:this.yylineno});return this},"reject"),less:c(function(o){this.unput(this.match.slice(o))},"less"),pastInput:c(function(){var o=this.matched.substr(0,this.matched.length-this.match.length);return(o.length>20?"...":"")+o.substr(-20).replace(/\n/g,"")},"pastInput"),upcomingInput:c(function(){var o=this.match;return o.length<20&&(o+=this._input.substr(0,20-o.length)),(o.substr(0,20)+(o.length>20?"...":"")).replace(/\n/g,"")},"upcomingInput"),showPosition:c(function(){var o=this.pastInput(),u=new Array(o.length+1).join("-");return o+this.upcomingInput()+`
`+u+"^"},"showPosition"),test_match:c(function(o,u){var y,h,_;if(this.options.backtrack_lexer&&(_={yylineno:this.yylineno,yylloc:{first_line:this.yylloc.first_line,last_line:this.last_line,first_column:this.yylloc.first_column,last_column:this.yylloc.last_column},yytext:this.yytext,match:this.match,matches:this.matches,matched:this.matched,yyleng:this.yyleng,offset:this.offset,_more:this._more,_input:this._input,yy:this.yy,conditionStack:this.conditionStack.slice(0),done:this.done},this.options.ranges&&(_.yylloc.range=this.yylloc.range.slice(0))),h=o[0].match(/(?:\r\n?|\n).*/g),h&&(this.yylineno+=h.length),this.yylloc={first_line:this.yylloc.last_line,last_line:this.yylineno+1,first_column:this.yylloc.last_column,last_column:h?h[h.length-1].length-h[h.length-1].match(/\r?\n?/)[0].length:this.yylloc.last_column+o[0].length},this.yytext+=o[0],this.match+=o[0],this.matches=o,this.yyleng=this.yytext.length,this.options.ranges&&(this.yylloc.range=[this.offset,this.offset+=this.yyleng]),this._more=!1,this._backtrack=!1,this._input=this._input.slice(o[0].length),this.matched+=o[0],y=this.performAction.call(this,this.yy,this,u,this.conditionStack[this.conditionStack.length-1]),this.done&&this._input&&(this.done=!1),y)return y;if(this._backtrack){for(var a in _)this[a]=_[a];return!1}return!1},"test_match"),next:c(function(){if(this.done)return this.EOF;this._input||(this.done=!0);var o,u,y,h;this._more||(this.yytext="",this.match="");for(var _=this._currentRules(),a=0;a<_.length;a++)if(y=this._input.match(this.rules[_[a]]),y&&(!u||y[0].length>u[0].length)){if(u=y,h=a,this.options.backtrack_lexer){if(o=this.test_match(y,_[a]),o!==!1)return o;if(this._backtrack){u=!1;continue}else return!1}else if(!this.options.flex)break}return u?(o=this.test_match(u,_[h]),o!==!1?o:!1):this._input===""?this.EOF:this.parseError("Lexical error on line "+(this.yylineno+1)+`. Unrecognized text.
`+this.showPosition(),{text:"",token:null,line:this.yylineno})},"next"),lex:c(function(){var u=this.next();return u||this.lex()},"lex"),begin:c(function(u){this.conditionStack.push(u)},"begin"),popState:c(function(){var u=this.conditionStack.length-1;return u>0?this.conditionStack.pop():this.conditionStack[0]},"popState"),_currentRules:c(function(){return this.conditionStack.length&&this.conditionStack[this.conditionStack.length-1]?this.conditions[this.conditionStack[this.conditionStack.length-1]].rules:this.conditions.INITIAL.rules},"_currentRules"),topState:c(function(u){return u=this.conditionStack.length-1-Math.abs(u||0),u>=0?this.conditionStack[u]:"INITIAL"},"topState"),pushState:c(function(u){this.begin(u)},"pushState"),stateStackSize:c(function(){return this.conditionStack.length},"stateStackSize"),options:{"case-insensitive":!0},performAction:c(function(u,y,h,_){switch(h){case 0:return this.begin("open_directive"),"open_directive";case 1:return this.begin("acc_title"),31;case 2:return this.popState(),"acc_title_value";case 3:return this.begin("acc_descr"),33;case 4:return this.popState(),"acc_descr_value";case 5:this.begin("acc_descr_multiline");break;case 6:this.popState();break;case 7:return"acc_descr_multiline_value";case 8:break;case 9:break;case 10:break;case 11:return 10;case 12:break;case 13:break;case 14:this.begin("href");break;case 15:this.popState();break;case 16:return 43;case 17:this.begin("callbackname");break;case 18:this.popState();break;case 19:this.popState(),this.begin("callbackargs");break;case 20:return 41;case 21:this.popState();break;case 22:return 42;case 23:this.begin("click");break;case 24:this.popState();break;case 25:return 40;case 26:return 4;case 27:return 22;case 28:return 23;case 29:return 24;case 30:return 25;case 31:return 26;case 32:return 28;case 33:return 27;case 34:return 29;case 35:return 12;case 36:return 13;case 37:return 14;case 38:return 15;case 39:return 16;case 40:return 17;case 41:return 18;case 42:return 20;case 43:return 21;case 44:return"date";case 45:return 30;case 46:return"accDescription";case 47:return 36;case 48:return 38;case 49:return 39;case 50:return":";case 51:return 6;case 52:return"INVALID"}},"anonymous"),rules:[/^(?:%%\{)/i,/^(?:accTitle\s*:\s*)/i,/^(?:(?!\n||)*[^\n]*)/i,/^(?:accDescr\s*:\s*)/i,/^(?:(?!\n||)*[^\n]*)/i,/^(?:accDescr\s*\{\s*)/i,/^(?:[\}])/i,/^(?:[^\}]*)/i,/^(?:%%(?!\{)*[^\n]*)/i,/^(?:[^\}]%%*[^\n]*)/i,/^(?:%%*[^\n]*[\n]*)/i,/^(?:[\n]+)/i,/^(?:\s+)/i,/^(?:%[^\n]*)/i,/^(?:href[\s]+["])/i,/^(?:["])/i,/^(?:[^"]*)/i,/^(?:call[\s]+)/i,/^(?:\([\s]*\))/i,/^(?:\()/i,/^(?:[^(]*)/i,/^(?:\))/i,/^(?:[^)]*)/i,/^(?:click[\s]+)/i,/^(?:[\s\n])/i,/^(?:[^\s\n]*)/i,/^(?:gantt\b)/i,/^(?:dateFormat\s[^#\n;]+)/i,/^(?:inclusiveEndDates\b)/i,/^(?:topAxis\b)/i,/^(?:axisFormat\s[^#\n;]+)/i,/^(?:tickInterval\s[^#\n;]+)/i,/^(?:includes\s[^#\n;]+)/i,/^(?:excludes\s[^#\n;]+)/i,/^(?:todayMarker\s[^\n;]+)/i,/^(?:weekday\s+monday\b)/i,/^(?:weekday\s+tuesday\b)/i,/^(?:weekday\s+wednesday\b)/i,/^(?:weekday\s+thursday\b)/i,/^(?:weekday\s+friday\b)/i,/^(?:weekday\s+saturday\b)/i,/^(?:weekday\s+sunday\b)/i,/^(?:weekend\s+friday\b)/i,/^(?:weekend\s+saturday\b)/i,/^(?:\d\d\d\d-\d\d-\d\d\b)/i,/^(?:title\s[^\n]+)/i,/^(?:accDescription\s[^#\n;]+)/i,/^(?:section\s[^\n]+)/i,/^(?:[^:\n]+)/i,/^(?::[^#\n;]+)/i,/^(?::)/i,/^(?:$)/i,/^(?:.)/i],conditions:{acc_descr_multiline:{rules:[6,7],inclusive:!1},acc_descr:{rules:[4],inclusive:!1},acc_title:{rules:[2],inclusive:!1},callbackargs:{rules:[21,22],inclusive:!1},callbackname:{rules:[18,19,20],inclusive:!1},href:{rules:[15,16],inclusive:!1},click:{rules:[24,25],inclusive:!1},INITIAL:{rules:[0,1,3,5,8,9,10,11,12,13,14,17,23,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52],inclusive:!0}}};return D}();x.lexer=b;function k(){this.yy={}}return c(k,"Parser"),k.prototype=x,x.Parser=k,new k}();Rt.parser=Rt;var Yr=Rt;j.extend(Dr);j.extend(Mr);j.extend(Lr);var me={friday:5,saturday:6},J="",Xt="",jt=void 0,Ut="",mt=[],kt=[],Zt=new Map,Kt=[],Mt=[],ft="",Qt="",Me=["active","done","crit","milestone"],Jt=[],yt=!1,$t=!1,te="sunday",At="saturday",Bt=0,Wr=c(function(){Kt=[],Mt=[],ft="",Jt=[],Dt=0,Ht=void 0,Ct=void 0,B=[],J="",Xt="",Qt="",jt=void 0,Ut="",mt=[],kt=[],yt=!1,$t=!1,Bt=0,Zt=new Map,Xe(),te="sunday",At="saturday"},"clear"),Vr=c(function(t){Xt=t},"setAxisFormat"),zr=c(function(){return Xt},"getAxisFormat"),Pr=c(function(t){jt=t},"setTickInterval"),Or=c(function(){return jt},"getTickInterval"),Nr=c(function(t){Ut=t},"setTodayMarker"),Rr=c(function(){return Ut},"getTodayMarker"),Br=c(function(t){J=t},"setDateFormat"),qr=c(function(){yt=!0},"enableInclusiveEndDates"),Hr=c(function(){return yt},"endDatesAreInclusive"),Gr=c(function(){$t=!0},"enableTopAxis"),Xr=c(function(){return $t},"topAxisEnabled"),jr=c(function(t){Qt=t},"setDisplayMode"),Ur=c(function(){return Qt},"getDisplayMode"),Zr=c(function(){return J},"getDateFormat"),Kr=c(function(t){mt=t.toLowerCase().split(/[\s,]+/)},"setIncludes"),Qr=c(function(){return mt},"getIncludes"),Jr=c(function(t){kt=t.toLowerCase().split(/[\s,]+/)},"setExcludes"),$r=c(function(){return kt},"getExcludes"),tn=c(function(){return Zt},"getLinks"),en=c(function(t){ft=t,Kt.push(t)},"addSection"),rn=c(function(){return Kt},"getSections"),nn=c(function(){let t=ke();const e=10;let r=0;for(;!t&&r<e;)t=ke(),r++;return Mt=B,Mt},"getTasks"),Ae=c(function(t,e,r,n){return n.includes(t.format(e.trim()))?!1:r.includes("weekends")&&(t.isoWeekday()===me[At]||t.isoWeekday()===me[At]+1)||r.includes(t.format("dddd").toLowerCase())?!0:r.includes(t.format(e.trim()))},"isInvalidDate"),an=c(function(t){te=t},"setWeekday"),sn=c(function(){return te},"getWeekday"),on=c(function(t){At=t},"setWeekend"),Ie=c(function(t,e,r,n){if(!r.length||t.manualEndTime)return;let i;t.startTime instanceof Date?i=j(t.startTime):i=j(t.startTime,e,!0),i=i.add(1,"d");let f;t.endTime instanceof Date?f=j(t.endTime):f=j(t.endTime,e,!0);const[d,v]=cn(i,f,e,r,n);t.endTime=d.toDate(),t.renderEndTime=v},"checkTaskDates"),cn=c(function(t,e,r,n,i){let f=!1,d=null;for(;t<=e;)f||(d=e.toDate()),f=Ae(t,r,n,i),f&&(e=e.add(1,"d")),t=t.add(1,"d");return[e,d]},"fixTaskDates"),qt=c(function(t,e,r){r=r.trim();const i=/^after\s+(?<ids>[\d\w- ]+)/.exec(r);if(i!==null){let d=null;for(const A of i.groups.ids.split(" ")){let g=at(A);g!==void 0&&(!d||g.endTime>d.endTime)&&(d=g)}if(d)return d.endTime;const v=new Date;return v.setHours(0,0,0,0),v}let f=j(r,e.trim(),!0);if(f.isValid())return f.toDate();{St.debug("Invalid date:"+r),St.debug("With date format:"+e.trim());const d=new Date(r);if(d===void 0||isNaN(d.getTime())||d.getFullYear()<-1e4||d.getFullYear()>1e4)throw new Error("Invalid date:"+r);return d}},"getStartDate"),Fe=c(function(t){const e=/^(\d+(?:\.\d+)?)([Mdhmswy]|ms)$/.exec(t.trim());return e!==null?[Number.parseFloat(e[1]),e[2]]:[NaN,"ms"]},"parseDuration"),Le=c(function(t,e,r,n=!1){r=r.trim();const f=/^until\s+(?<ids>[\d\w- ]+)/.exec(r);if(f!==null){let S=null;for(const C of f.groups.ids.split(" ")){let T=at(C);T!==void 0&&(!S||T.startTime<S.startTime)&&(S=T)}if(S)return S.startTime;const M=new Date;return M.setHours(0,0,0,0),M}let d=j(r,e.trim(),!0);if(d.isValid())return n&&(d=d.add(1,"d")),d.toDate();let v=j(t);const[A,g]=Fe(r);if(!Number.isNaN(A)){const S=v.add(A,g);S.isValid()&&(v=S)}return v.toDate()},"getEndDate"),Dt=0,dt=c(function(t){return t===void 0?(Dt=Dt+1,"task"+Dt):t},"parseId"),ln=c(function(t,e){let r;e.substr(0,1)===":"?r=e.substr(1,e.length):r=e;const n=r.split(","),i={};ee(n,i,Me);for(let d=0;d<n.length;d++)n[d]=n[d].trim();let f="";switch(n.length){case 1:i.id=dt(),i.startTime=t.endTime,f=n[0];break;case 2:i.id=dt(),i.startTime=qt(void 0,J,n[0]),f=n[1];break;case 3:i.id=dt(n[0]),i.startTime=qt(void 0,J,n[1]),f=n[2];break}return f&&(i.endTime=Le(i.startTime,J,f,yt),i.manualEndTime=j(f,"YYYY-MM-DD",!0).isValid(),Ie(i,J,kt,mt)),i},"compileData"),un=c(function(t,e){let r;e.substr(0,1)===":"?r=e.substr(1,e.length):r=e;const n=r.split(","),i={};ee(n,i,Me);for(let f=0;f<n.length;f++)n[f]=n[f].trim();switch(n.length){case 1:i.id=dt(),i.startTime={type:"prevTaskEnd",id:t},i.endTime={data:n[0]};break;case 2:i.id=dt(),i.startTime={type:"getStartDate",startData:n[0]},i.endTime={data:n[1]};break;case 3:i.id=dt(n[0]),i.startTime={type:"getStartDate",startData:n[1]},i.endTime={data:n[2]};break}return i},"parseData"),Ht,Ct,B=[],Ye={},dn=c(function(t,e){const r={section:ft,type:ft,processed:!1,manualEndTime:!1,renderEndTime:null,raw:{data:e},task:t,classes:[]},n=un(Ct,e);r.raw.startTime=n.startTime,r.raw.endTime=n.endTime,r.id=n.id,r.prevTaskId=Ct,r.active=n.active,r.done=n.done,r.crit=n.crit,r.milestone=n.milestone,r.order=Bt,Bt++;const i=B.push(r);Ct=r.id,Ye[r.id]=i-1},"addTask"),at=c(function(t){const e=Ye[t];return B[e]},"findTaskById"),fn=c(function(t,e){const r={section:ft,type:ft,description:t,task:t,classes:[]},n=ln(Ht,e);r.startTime=n.startTime,r.endTime=n.endTime,r.id=n.id,r.active=n.active,r.done=n.done,r.crit=n.crit,r.milestone=n.milestone,Ht=r,Mt.push(r)},"addTaskOrg"),ke=c(function(){const t=c(function(r){const n=B[r];let i="";switch(B[r].raw.startTime.type){case"prevTaskEnd":{const f=at(n.prevTaskId);n.startTime=f.endTime;break}case"getStartDate":i=qt(void 0,J,B[r].raw.startTime.startData),i&&(B[r].startTime=i);break}return B[r].startTime&&(B[r].endTime=Le(B[r].startTime,J,B[r].raw.endTime.data,yt),B[r].endTime&&(B[r].processed=!0,B[r].manualEndTime=j(B[r].raw.endTime.data,"YYYY-MM-DD",!0).isValid(),Ie(B[r],J,kt,mt))),B[r].processed},"compileTask");let e=!0;for(const[r,n]of B.entries())t(r),e=e&&n.processed;return e},"compileTasks"),hn=c(function(t,e){let r=e;lt().securityLevel!=="loose"&&(r=Ge.sanitizeUrl(e)),t.split(",").forEach(function(n){at(n)!==void 0&&(Ve(n,()=>{window.open(r,"_self")}),Zt.set(n,r))}),We(t,"clickable")},"setLink"),We=c(function(t,e){t.split(",").forEach(function(r){let n=at(r);n!==void 0&&n.classes.push(e)})},"setClass"),mn=c(function(t,e,r){if(lt().securityLevel!=="loose"||e===void 0)return;let n=[];if(typeof r=="string"){n=r.split(/,(?=(?:(?:[^"]*"){2})*[^"]*$)/);for(let f=0;f<n.length;f++){let d=n[f].trim();d.startsWith('"')&&d.endsWith('"')&&(d=d.substr(1,d.length-2)),n[f]=d}}n.length===0&&n.push(t),at(t)!==void 0&&Ve(t,()=>{je.runFunc(e,...n)})},"setClickFun"),Ve=c(function(t,e){Jt.push(function(){const r=document.querySelector(`[id="${t}"]`);r!==null&&r.addEventListener("click",function(){e()})},function(){const r=document.querySelector(`[id="${t}-text"]`);r!==null&&r.addEventListener("click",function(){e()})})},"pushFun"),kn=c(function(t,e,r){t.split(",").forEach(function(n){mn(n,e,r)}),We(t,"clickable")},"setClickEvent"),yn=c(function(t){Jt.forEach(function(e){e(t)})},"bindFunctions"),pn={getConfig:c(()=>lt().gantt,"getConfig"),clear:Wr,setDateFormat:Br,getDateFormat:Zr,enableInclusiveEndDates:qr,endDatesAreInclusive:Hr,enableTopAxis:Gr,topAxisEnabled:Xr,setAxisFormat:Vr,getAxisFormat:zr,setTickInterval:Pr,getTickInterval:Or,setTodayMarker:Nr,getTodayMarker:Rr,setAccTitle:Be,getAccTitle:Re,setDiagramTitle:Ne,getDiagramTitle:Oe,setDisplayMode:jr,getDisplayMode:Ur,setAccDescription:Pe,getAccDescription:ze,addSection:en,getSections:rn,getTasks:nn,addTask:dn,findTaskById:at,addTaskOrg:fn,setIncludes:Kr,getIncludes:Qr,setExcludes:Jr,getExcludes:$r,setClickEvent:kn,setLink:hn,getLinks:tn,bindFunctions:yn,parseDuration:Fe,isInvalidDate:Ae,setWeekday:an,getWeekday:sn,setWeekend:on};function ee(t,e,r){let n=!0;for(;n;)n=!1,r.forEach(function(i){const f="^\\s*"+i+"\\s*$",d=new RegExp(f);t[0].match(d)&&(e[i]=!0,t.shift(1),n=!0)})}c(ee,"getTaskTags");var gn=c(function(){St.debug("Something is calling, setConf, remove the call")},"setConf"),ye={monday:nr,tuesday:rr,wednesday:er,thursday:tr,friday:$e,saturday:Je,sunday:Qe},vn=c((t,e)=>{let r=[...t].map(()=>-1/0),n=[...t].sort((f,d)=>f.startTime-d.startTime||f.order-d.order),i=0;for(const f of n)for(let d=0;d<r.length;d++)if(f.startTime>=r[d]){r[d]=f.endTime,f.order=d+e,d>i&&(i=d);break}return i},"getMaxIntersections"),et,bn=c(function(t,e,r,n){const i=lt().gantt,f=lt().securityLevel;let d;f==="sandbox"&&(d=vt("#i"+e));const v=f==="sandbox"?vt(d.nodes()[0].contentDocument.body):vt("body"),A=f==="sandbox"?d.nodes()[0].contentDocument:document,g=A.getElementById(e);et=g.parentElement.offsetWidth,et===void 0&&(et=1200),i.useWidth!==void 0&&(et=i.useWidth);const S=n.db.getTasks();let M=[];for(const p of S)M.push(p.type);M=V(M);const C={};let T=2*i.topPadding;if(n.db.getDisplayMode()==="compact"||i.displayMode==="compact"){const p={};for(const x of S)p[x.section]===void 0?p[x.section]=[x]:p[x.section].push(x);let w=0;for(const x of Object.keys(p)){const b=vn(p[x],w)+1;w+=b,T+=b*(i.barHeight+i.barGap),C[x]=b}}else{T+=S.length*(i.barHeight+i.barGap);for(const p of M)C[p]=S.filter(w=>w.type===p).length}g.setAttribute("viewBox","0 0 "+et+" "+T);const q=v.select(`[id="${e}"]`),m=Ke().domain([hr(S,function(p){return p.startTime}),fr(S,function(p){return p.endTime})]).rangeRound([0,et-i.leftPadding-i.rightPadding]);function E(p,w){const x=p.startTime,b=w.startTime;let k=0;return x>b?k=1:x<b&&(k=-1),k}c(E,"taskCompare"),S.sort(E),L(S,et,T),qe(q,T,et,i.useMaxWidth),q.append("text").text(n.db.getDiagramTitle()).attr("x",et/2).attr("y",i.titleTopMargin).attr("class","titleText");function L(p,w,x){const b=i.barHeight,k=b+i.barGap,D=i.topPadding,o=i.leftPadding,u=ir().domain([0,M.length]).range(["#00B9FA","#F95002"]).interpolate(dr);O(k,D,o,w,x,p,n.db.getExcludes(),n.db.getIncludes()),N(o,D,w,x),I(p,k,D,o,b,u,w),X(k,D),R(o,D,w,x)}c(L,"makeGantt");function I(p,w,x,b,k,D,o){const y=[...new Set(p.map(l=>l.order))].map(l=>p.find(s=>s.order===l));q.append("g").selectAll("rect").data(y).enter().append("rect").attr("x",0).attr("y",function(l,s){return s=l.order,s*w+x-2}).attr("width",function(){return o-i.rightPadding/2}).attr("height",w).attr("class",function(l){for(const[s,W]of M.entries())if(l.type===W)return"section section"+s%i.numberSectionStyles;return"section section0"});const h=q.append("g").selectAll("rect").data(p).enter(),_=n.db.getLinks();if(h.append("rect").attr("id",function(l){return l.id}).attr("rx",3).attr("ry",3).attr("x",function(l){return l.milestone?m(l.startTime)+b+.5*(m(l.endTime)-m(l.startTime))-.5*k:m(l.startTime)+b}).attr("y",function(l,s){return s=l.order,s*w+x}).attr("width",function(l){return l.milestone?k:m(l.renderEndTime||l.endTime)-m(l.startTime)}).attr("height",k).attr("transform-origin",function(l,s){return s=l.order,(m(l.startTime)+b+.5*(m(l.endTime)-m(l.startTime))).toString()+"px "+(s*w+x+.5*k).toString()+"px"}).attr("class",function(l){const s="task";let W="";l.classes.length>0&&(W=l.classes.join(" "));let F=0;for(const[H,z]of M.entries())l.type===z&&(F=H%i.numberSectionStyles);let Y="";return l.active?l.crit?Y+=" activeCrit":Y=" active":l.done?l.crit?Y=" doneCrit":Y=" done":l.crit&&(Y+=" crit"),Y.length===0&&(Y=" task"),l.milestone&&(Y=" milestone "+Y),Y+=F,Y+=" "+W,s+Y}),h.append("text").attr("id",function(l){return l.id+"-text"}).text(function(l){return l.task}).attr("font-size",i.fontSize).attr("x",function(l){let s=m(l.startTime),W=m(l.renderEndTime||l.endTime);l.milestone&&(s+=.5*(m(l.endTime)-m(l.startTime))-.5*k),l.milestone&&(W=s+k);const F=this.getBBox().width;return F>W-s?W+F+1.5*i.leftPadding>o?s+b-5:W+b+5:(W-s)/2+s+b}).attr("y",function(l,s){return s=l.order,s*w+i.barHeight/2+(i.fontSize/2-2)+x}).attr("text-height",k).attr("class",function(l){const s=m(l.startTime);let W=m(l.endTime);l.milestone&&(W=s+k);const F=this.getBBox().width;let Y="";l.classes.length>0&&(Y=l.classes.join(" "));let H=0;for(const[P,U]of M.entries())l.type===U&&(H=P%i.numberSectionStyles);let z="";return l.active&&(l.crit?z="activeCritText"+H:z="activeText"+H),l.done?l.crit?z=z+" doneCritText"+H:z=z+" doneText"+H:l.crit&&(z=z+" critText"+H),l.milestone&&(z+=" milestoneText"),F>W-s?W+F+1.5*i.leftPadding>o?Y+" taskTextOutsideLeft taskTextOutside"+H+" "+z:Y+" taskTextOutsideRight taskTextOutside"+H+" "+z+" width-"+F:Y+" taskText taskText"+H+" "+z+" width-"+F}),lt().securityLevel==="sandbox"){let l;l=vt("#i"+e);const s=l.nodes()[0].contentDocument;h.filter(function(W){return _.has(W.id)}).each(function(W){var F=s.querySelector("#"+W.id),Y=s.querySelector("#"+W.id+"-text");const H=F.parentNode;var z=s.createElement("a");z.setAttribute("xlink:href",_.get(W.id)),z.setAttribute("target","_top"),H.appendChild(z),z.appendChild(F),z.appendChild(Y)})}}c(I,"drawRects");function O(p,w,x,b,k,D,o,u){if(o.length===0&&u.length===0)return;let y,h;for(const{startTime:F,endTime:Y}of D)(y===void 0||F<y)&&(y=F),(h===void 0||Y>h)&&(h=Y);if(!y||!h)return;if(j(h).diff(j(y),"year")>5){St.warn("The difference between the min and max time is more than 5 years. This will cause performance issues. Skipping drawing exclude days.");return}const _=n.db.getDateFormat(),a=[];let l=null,s=j(y);for(;s.valueOf()<=h;)n.db.isInvalidDate(s,_,o,u)?l?l.end=s:l={start:s,end:s}:l&&(a.push(l),l=null),s=s.add(1,"d");q.append("g").selectAll("rect").data(a).enter().append("rect").attr("id",function(F){return"exclude-"+F.start.format("YYYY-MM-DD")}).attr("x",function(F){return m(F.start)+x}).attr("y",i.gridLineStartPadding).attr("width",function(F){const Y=F.end.add(1,"day");return m(Y)-m(F.start)}).attr("height",k-w-i.gridLineStartPadding).attr("transform-origin",function(F,Y){return(m(F.start)+x+.5*(m(F.end)-m(F.start))).toString()+"px "+(Y*p+.5*k).toString()+"px"}).attr("class","exclude-range")}c(O,"drawExcludeDays");function N(p,w,x,b){let k=xr(m).tickSize(-b+w+i.gridLineStartPadding).tickFormat(ne(n.db.getAxisFormat()||i.axisFormat||"%Y-%m-%d"));const o=/^([1-9]\d*)(millisecond|second|minute|hour|day|week|month)$/.exec(n.db.getTickInterval()||i.tickInterval);if(o!==null){const u=o[1],y=o[2],h=n.db.getWeekday()||i.weekday;switch(y){case"millisecond":k.ticks(le.every(u));break;case"second":k.ticks(ce.every(u));break;case"minute":k.ticks(oe.every(u));break;case"hour":k.ticks(se.every(u));break;case"day":k.ticks(ae.every(u));break;case"week":k.ticks(ye[h].every(u));break;case"month":k.ticks(ie.every(u));break}}if(q.append("g").attr("class","grid").attr("transform","translate("+p+", "+(b-50)+")").call(k).selectAll("text").style("text-anchor","middle").attr("fill","#000").attr("stroke","none").attr("font-size",10).attr("dy","1em"),n.db.topAxisEnabled()||i.topAxis){let u=br(m).tickSize(-b+w+i.gridLineStartPadding).tickFormat(ne(n.db.getAxisFormat()||i.axisFormat||"%Y-%m-%d"));if(o!==null){const y=o[1],h=o[2],_=n.db.getWeekday()||i.weekday;switch(h){case"millisecond":u.ticks(le.every(y));break;case"second":u.ticks(ce.every(y));break;case"minute":u.ticks(oe.every(y));break;case"hour":u.ticks(se.every(y));break;case"day":u.ticks(ae.every(y));break;case"week":u.ticks(ye[_].every(y));break;case"month":u.ticks(ie.every(y));break}}q.append("g").attr("class","grid").attr("transform","translate("+p+", "+w+")").call(u).selectAll("text").style("text-anchor","middle").attr("fill","#000").attr("stroke","none").attr("font-size",10)}}c(N,"makeGrid");function X(p,w){let x=0;const b=Object.keys(C).map(k=>[k,C[k]]);q.append("g").selectAll("text").data(b).enter().append(function(k){const D=k[0].split(He.lineBreakRegex),o=-(D.length-1)/2,u=A.createElementNS("http://www.w3.org/2000/svg","text");u.setAttribute("dy",o+"em");for(const[y,h]of D.entries()){const _=A.createElementNS("http://www.w3.org/2000/svg","tspan");_.setAttribute("alignment-baseline","central"),_.setAttribute("x","10"),y>0&&_.setAttribute("dy","1em"),_.textContent=h,u.appendChild(_)}return u}).attr("x",10).attr("y",function(k,D){if(D>0)for(let o=0;o<D;o++)return x+=b[D-1][1],k[1]*p/2+x*p+w;else return k[1]*p/2+w}).attr("font-size",i.sectionFontSize).attr("class",function(k){for(const[D,o]of M.entries())if(k[0]===o)return"sectionTitle sectionTitle"+D%i.numberSectionStyles;return"sectionTitle"})}c(X,"vertLabels");function R(p,w,x,b){const k=n.db.getTodayMarker();if(k==="off")return;const D=q.append("g").attr("class","today"),o=new Date,u=D.append("line");u.attr("x1",m(o)+p).attr("x2",m(o)+p).attr("y1",i.titleTopMargin).attr("y2",b-i.titleTopMargin).attr("class","today"),k!==""&&u.attr("style",k.replace(/,/g,";"))}c(R,"drawToday");function V(p){const w={},x=[];for(let b=0,k=p.length;b<k;++b)Object.prototype.hasOwnProperty.call(w,p[b])||(w[p[b]]=!0,x.push(p[b]));return x}c(V,"checkUnique")},"draw"),xn={setConf:gn,draw:bn},Tn=c(t=>`
  .mermaid-main-font {
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }

  .exclude-range {
    fill: ${t.excludeBkgColor};
  }

  .section {
    stroke: none;
    opacity: 0.2;
  }

  .section0 {
    fill: ${t.sectionBkgColor};
  }

  .section2 {
    fill: ${t.sectionBkgColor2};
  }

  .section1,
  .section3 {
    fill: ${t.altSectionBkgColor};
    opacity: 0.2;
  }

  .sectionTitle0 {
    fill: ${t.titleColor};
  }

  .sectionTitle1 {
    fill: ${t.titleColor};
  }

  .sectionTitle2 {
    fill: ${t.titleColor};
  }

  .sectionTitle3 {
    fill: ${t.titleColor};
  }

  .sectionTitle {
    text-anchor: start;
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }


  /* Grid and axis */

  .grid .tick {
    stroke: ${t.gridColor};
    opacity: 0.8;
    shape-rendering: crispEdges;
  }

  .grid .tick text {
    font-family: ${t.fontFamily};
    fill: ${t.textColor};
  }

  .grid path {
    stroke-width: 0;
  }


  /* Today line */

  .today {
    fill: none;
    stroke: ${t.todayLineColor};
    stroke-width: 2px;
  }


  /* Task styling */

  /* Default task */

  .task {
    stroke-width: 2;
  }

  .taskText {
    text-anchor: middle;
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }

  .taskTextOutsideRight {
    fill: ${t.taskTextDarkColor};
    text-anchor: start;
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }

  .taskTextOutsideLeft {
    fill: ${t.taskTextDarkColor};
    text-anchor: end;
  }


  /* Special case clickable */

  .task.clickable {
    cursor: pointer;
  }

  .taskText.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }

  .taskTextOutsideLeft.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }

  .taskTextOutsideRight.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }


  /* Specific task settings for the sections*/

  .taskText0,
  .taskText1,
  .taskText2,
  .taskText3 {
    fill: ${t.taskTextColor};
  }

  .task0,
  .task1,
  .task2,
  .task3 {
    fill: ${t.taskBkgColor};
    stroke: ${t.taskBorderColor};
  }

  .taskTextOutside0,
  .taskTextOutside2
  {
    fill: ${t.taskTextOutsideColor};
  }

  .taskTextOutside1,
  .taskTextOutside3 {
    fill: ${t.taskTextOutsideColor};
  }


  /* Active task */

  .active0,
  .active1,
  .active2,
  .active3 {
    fill: ${t.activeTaskBkgColor};
    stroke: ${t.activeTaskBorderColor};
  }

  .activeText0,
  .activeText1,
  .activeText2,
  .activeText3 {
    fill: ${t.taskTextDarkColor} !important;
  }


  /* Completed task */

  .done0,
  .done1,
  .done2,
  .done3 {
    stroke: ${t.doneTaskBorderColor};
    fill: ${t.doneTaskBkgColor};
    stroke-width: 2;
  }

  .doneText0,
  .doneText1,
  .doneText2,
  .doneText3 {
    fill: ${t.taskTextDarkColor} !important;
  }


  /* Tasks on the critical line */

  .crit0,
  .crit1,
  .crit2,
  .crit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.critBkgColor};
    stroke-width: 2;
  }

  .activeCrit0,
  .activeCrit1,
  .activeCrit2,
  .activeCrit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.activeTaskBkgColor};
    stroke-width: 2;
  }

  .doneCrit0,
  .doneCrit1,
  .doneCrit2,
  .doneCrit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.doneTaskBkgColor};
    stroke-width: 2;
    cursor: pointer;
    shape-rendering: crispEdges;
  }

  .milestone {
    transform: rotate(45deg) scale(0.8,0.8);
  }

  .milestoneText {
    font-style: italic;
  }
  .doneCritText0,
  .doneCritText1,
  .doneCritText2,
  .doneCritText3 {
    fill: ${t.taskTextDarkColor} !important;
  }

  .activeCritText0,
  .activeCritText1,
  .activeCritText2,
  .activeCritText3 {
    fill: ${t.taskTextDarkColor} !important;
  }

  .titleText {
    text-anchor: middle;
    font-size: 18px;
    fill: ${t.titleColor||t.textColor};
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }
`,"getStyles"),wn=Tn,wi={parser:Yr,db:pn,renderer:xn,styles:wn};export{wi as diagram};
//# sourceMappingURL=ganttDiagram-FAOCOTIY-CEm02nt0.js.map
