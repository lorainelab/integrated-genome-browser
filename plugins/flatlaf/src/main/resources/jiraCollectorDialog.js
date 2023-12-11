console.log = function(message){
    logger.log(message);
}
/*! jQuery v2.1.4 | (c) 2005, 2015 jQuery Foundation, Inc. | jquery.org/license */
!function(a,b){"object"==typeof module&&"object"==typeof module.exports?module.exports=a.document?b(a,!0):function(a){if(!a.document)throw new Error("jQuery requires a window with a document");return b(a)}:b(a)}("undefined"!=typeof window?window:this,function(a,b){var c=[],d=c.slice,e=c.concat,f=c.push,g=c.indexOf,h={},i=h.toString,j=h.hasOwnProperty,k={},l=a.document,m="2.1.4",n=function(a,b){return new n.fn.init(a,b)},o=/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g,p=/^-ms-/,q=/-([\da-z])/gi,r=function(a,b){return b.toUpperCase()};n.fn=n.prototype={jquery:m,constructor:n,selector:"",length:0,toArray:function(){return d.call(this)},get:function(a){return null!=a?0>a?this[a+this.length]:this[a]:d.call(this)},pushStack:function(a){var b=n.merge(this.constructor(),a);return b.prevObject=this,b.context=this.context,b},each:function(a,b){return n.each(this,a,b)},map:function(a){return this.pushStack(n.map(this,function(b,c){return a.call(b,c,b)}))},slice:function(){return this.pushStack(d.apply(this,arguments))},first:function(){return this.eq(0)},last:function(){return this.eq(-1)},eq:function(a){var b=this.length,c=+a+(0>a?b:0);return this.pushStack(c>=0&&b>c?[this[c]]:[])},end:function(){return this.prevObject||this.constructor(null)},push:f,sort:c.sort,splice:c.splice},n.extend=n.fn.extend=function(){var a,b,c,d,e,f,g=arguments[0]||{},h=1,i=arguments.length,j=!1;for("boolean"==typeof g&&(j=g,g=arguments[h]||{},h++),"object"==typeof g||n.isFunction(g)||(g={}),h===i&&(g=this,h--);i>h;h++)if(null!=(a=arguments[h]))for(b in a)c=g[b],d=a[b],g!==d&&(j&&d&&(n.isPlainObject(d)||(e=n.isArray(d)))?(e?(e=!1,f=c&&n.isArray(c)?c:[]):f=c&&n.isPlainObject(c)?c:{},g[b]=n.extend(j,f,d)):void 0!==d&&(g[b]=d));return g},n.extend({expando:"jQuery"+(m+Math.random()).replace(/\D/g,""),isReady:!0,error:function(a){throw new Error(a)},noop:function(){},isFunction:function(a){return"function"===n.type(a)},isArray:Array.isArray,isWindow:function(a){return null!=a&&a===a.window},isNumeric:function(a){return!n.isArray(a)&&a-parseFloat(a)+1>=0},isPlainObject:function(a){return"object"!==n.type(a)||a.nodeType||n.isWindow(a)?!1:a.constructor&&!j.call(a.constructor.prototype,"isPrototypeOf")?!1:!0},isEmptyObject:function(a){var b;for(b in a)return!1;return!0},type:function(a){return null==a?a+"":"object"==typeof a||"function"==typeof a?h[i.call(a)]||"object":typeof a},globalEval:function(a){var b,c=eval;a=n.trim(a),a&&(1===a.indexOf("use strict")?(b=l.createElement("script"),b.text=a,l.head.appendChild(b).parentNode.removeChild(b)):c(a))},camelCase:function(a){return a.replace(p,"ms-").replace(q,r)},nodeName:function(a,b){return a.nodeName&&a.nodeName.toLowerCase()===b.toLowerCase()},each:function(a,b,c){var d,e=0,f=a.length,g=s(a);if(c){if(g){for(;f>e;e++)if(d=b.apply(a[e],c),d===!1)break}else for(e in a)if(d=b.apply(a[e],c),d===!1)break}else if(g){for(;f>e;e++)if(d=b.call(a[e],e,a[e]),d===!1)break}else for(e in a)if(d=b.call(a[e],e,a[e]),d===!1)break;return a},trim:function(a){return null==a?"":(a+"").replace(o,"")},makeArray:function(a,b){var c=b||[];return null!=a&&(s(Object(a))?n.merge(c,"string"==typeof a?[a]:a):f.call(c,a)),c},inArray:function(a,b,c){return null==b?-1:g.call(b,a,c)},merge:function(a,b){for(var c=+b.length,d=0,e=a.length;c>d;d++)a[e++]=b[d];return a.length=e,a},grep:function(a,b,c){for(var d,e=[],f=0,g=a.length,h=!c;g>f;f++)d=!b(a[f],f),d!==h&&e.push(a[f]);return e},map:function(a,b,c){var d,f=0,g=a.length,h=s(a),i=[];if(h)for(;g>f;f++)d=b(a[f],f,c),null!=d&&i.push(d);else for(f in a)d=b(a[f],f,c),null!=d&&i.push(d);return e.apply([],i)},guid:1,proxy:function(a,b){var c,e,f;return"string"==typeof b&&(c=a[b],b=a,a=c),n.isFunction(a)?(e=d.call(arguments,2),f=function(){return a.apply(b||this,e.concat(d.call(arguments)))},f.guid=a.guid=a.guid||n.guid++,f):void 0},now:Date.now,support:k}),n.each("Boolean Number String Function Array Date RegExp Object Error".split(" "),function(a,b){h["[object "+b+"]"]=b.toLowerCase()});function s(a){var b="length"in a&&a.length,c=n.type(a);return"function"===c||n.isWindow(a)?!1:1===a.nodeType&&b?!0:"array"===c||0===b||"number"==typeof b&&b>0&&b-1 in a}var t=function(a){var b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u="sizzle"+1*new Date,v=a.document,w=0,x=0,y=ha(),z=ha(),A=ha(),B=function(a,b){return a===b&&(l=!0),0},C=1<<31,D={}.hasOwnProperty,E=[],F=E.pop,G=E.push,H=E.push,I=E.slice,J=function(a,b){for(var c=0,d=a.length;d>c;c++)if(a[c]===b)return c;return-1},K="checked|selected|async|autofocus|autoplay|controls|defer|disabled|hidden|ismap|loop|multiple|open|readonly|required|scoped",L="[\\x20\\t\\r\\n\\f]",M="(?:\\\\.|[\\w-]|[^\\x00-\\xa0])+",N=M.replace("w","w#"),O="\\["+L+"*("+M+")(?:"+L+"*([*^$|!~]?=)"+L+"*(?:'((?:\\\\.|[^\\\\'])*)'|\"((?:\\\\.|[^\\\\\"])*)\"|("+N+"))|)"+L+"*\\]",P=":("+M+")(?:\\((('((?:\\\\.|[^\\\\'])*)'|\"((?:\\\\.|[^\\\\\"])*)\")|((?:\\\\.|[^\\\\()[\\]]|"+O+")*)|.*)\\)|)",Q=new RegExp(L+"+","g"),R=new RegExp("^"+L+"+|((?:^|[^\\\\])(?:\\\\.)*)"+L+"+$","g"),S=new RegExp("^"+L+"*,"+L+"*"),T=new RegExp("^"+L+"*([>+~]|"+L+")"+L+"*"),U=new RegExp("="+L+"*([^\\]'\"]*?)"+L+"*\\]","g"),V=new RegExp(P),W=new RegExp("^"+N+"$"),X={ID:new RegExp("^#("+M+")"),CLASS:new RegExp("^\\.("+M+")"),TAG:new RegExp("^("+M.replace("w","w*")+")"),ATTR:new RegExp("^"+O),PSEUDO:new RegExp("^"+P),CHILD:new RegExp("^:(only|first|last|nth|nth-last)-(child|of-type)(?:\\("+L+"*(even|odd|(([+-]|)(\\d*)n|)"+L+"*(?:([+-]|)"+L+"*(\\d+)|))"+L+"*\\)|)","i"),bool:new RegExp("^(?:"+K+")$","i"),needsContext:new RegExp("^"+L+"*[>+~]|:(even|odd|eq|gt|lt|nth|first|last)(?:\\("+L+"*((?:-\\d)?\\d*)"+L+"*\\)|)(?=[^-]|$)","i")},Y=/^(?:input|select|textarea|button)$/i,Z=/^h\d$/i,$=/^[^{]+\{\s*\[native \w/,_=/^(?:#([\w-]+)|(\w+)|\.([\w-]+))$/,aa=/[+~]/,ba=/'|\\/g,ca=new RegExp("\\\\([\\da-f]{1,6}"+L+"?|("+L+")|.)","ig"),da=function(a,b,c){var d="0x"+b-65536;return d!==d||c?b:0>d?String.fromCharCode(d+65536):String.fromCharCode(d>>10|55296,1023&d|56320)},ea=function(){m()};try{H.apply(E=I.call(v.childNodes),v.childNodes),E[v.childNodes.length].nodeType}catch(fa){H={apply:E.length?function(a,b){G.apply(a,I.call(b))}:function(a,b){var c=a.length,d=0;while(a[c++]=b[d++]);a.length=c-1}}}function ga(a,b,d,e){var f,h,j,k,l,o,r,s,w,x;if((b?b.ownerDocument||b:v)!==n&&m(b),b=b||n,d=d||[],k=b.nodeType,"string"!=typeof a||!a||1!==k&&9!==k&&11!==k)return d;if(!e&&p){if(11!==k&&(f=_.exec(a)))if(j=f[1]){if(9===k){if(h=b.getElementById(j),!h||!h.parentNode)return d;if(h.id===j)return d.push(h),d}else if(b.ownerDocument&&(h=b.ownerDocument.getElementById(j))&&t(b,h)&&h.id===j)return d.push(h),d}else{if(f[2])return H.apply(d,b.getElementsByTagName(a)),d;if((j=f[3])&&c.getElementsByClassName)return H.apply(d,b.getElementsByClassName(j)),d}if(c.qsa&&(!q||!q.test(a))){if(s=r=u,w=b,x=1!==k&&a,1===k&&"object"!==b.nodeName.toLowerCase()){o=g(a),(r=b.getAttribute("id"))?s=r.replace(ba,"\\$&"):b.setAttribute("id",s),s="[id='"+s+"'] ",l=o.length;while(l--)o[l]=s+ra(o[l]);w=aa.test(a)&&pa(b.parentNode)||b,x=o.join(",")}if(x)try{return H.apply(d,w.querySelectorAll(x)),d}catch(y){}finally{r||b.removeAttribute("id")}}}return i(a.replace(R,"$1"),b,d,e)}function ha(){var a=[];function b(c,e){return a.push(c+" ")>d.cacheLength&&delete b[a.shift()],b[c+" "]=e}return b}function ia(a){return a[u]=!0,a}function ja(a){var b=n.createElement("div");try{return!!a(b)}catch(c){return!1}finally{b.parentNode&&b.parentNode.removeChild(b),b=null}}function ka(a,b){var c=a.split("|"),e=a.length;while(e--)d.attrHandle[c[e]]=b}function la(a,b){var c=b&&a,d=c&&1===a.nodeType&&1===b.nodeType&&(~b.sourceIndex||C)-(~a.sourceIndex||C);if(d)return d;if(c)while(c=c.nextSibling)if(c===b)return-1;return a?1:-1}function ma(a){return function(b){var c=b.nodeName.toLowerCase();return"input"===c&&b.type===a}}function na(a){return function(b){var c=b.nodeName.toLowerCase();return("input"===c||"button"===c)&&b.type===a}}function oa(a){return ia(function(b){return b=+b,ia(function(c,d){var e,f=a([],c.length,b),g=f.length;while(g--)c[e=f[g]]&&(c[e]=!(d[e]=c[e]))})})}function pa(a){return a&&"undefined"!=typeof a.getElementsByTagName&&a}c=ga.support={},f=ga.isXML=function(a){var b=a&&(a.ownerDocument||a).documentElement;return b?"HTML"!==b.nodeName:!1},m=ga.setDocument=function(a){var b,e,g=a?a.ownerDocument||a:v;return g!==n&&9===g.nodeType&&g.documentElement?(n=g,o=g.documentElement,e=g.defaultView,e&&e!==e.top&&(e.addEventListener?e.addEventListener("unload",ea,!1):e.attachEvent&&e.attachEvent("onunload",ea)),p=!f(g),c.attributes=ja(function(a){return a.className="i",!a.getAttribute("className")}),c.getElementsByTagName=ja(function(a){return a.appendChild(g.createComment("")),!a.getElementsByTagName("*").length}),c.getElementsByClassName=$.test(g.getElementsByClassName),c.getById=ja(function(a){return o.appendChild(a).id=u,!g.getElementsByName||!g.getElementsByName(u).length}),c.getById?(d.find.ID=function(a,b){if("undefined"!=typeof b.getElementById&&p){var c=b.getElementById(a);return c&&c.parentNode?[c]:[]}},d.filter.ID=function(a){var b=a.replace(ca,da);return function(a){return a.getAttribute("id")===b}}):(delete d.find.ID,d.filter.ID=function(a){var b=a.replace(ca,da);return function(a){var c="undefined"!=typeof a.getAttributeNode&&a.getAttributeNode("id");return c&&c.value===b}}),d.find.TAG=c.getElementsByTagName?function(a,b){return"undefined"!=typeof b.getElementsByTagName?b.getElementsByTagName(a):c.qsa?b.querySelectorAll(a):void 0}:function(a,b){var c,d=[],e=0,f=b.getElementsByTagName(a);if("*"===a){while(c=f[e++])1===c.nodeType&&d.push(c);return d}return f},d.find.CLASS=c.getElementsByClassName&&function(a,b){return p?b.getElementsByClassName(a):void 0},r=[],q=[],(c.qsa=$.test(g.querySelectorAll))&&(ja(function(a){o.appendChild(a).innerHTML="<a id='"+u+"'></a><select id='"+u+"-\f]' msallowcapture=''><option selected=''></option></select>",a.querySelectorAll("[msallowcapture^='']").length&&q.push("[*^$]="+L+"*(?:''|\"\")"),a.querySelectorAll("[selected]").length||q.push("\\["+L+"*(?:value|"+K+")"),a.querySelectorAll("[id~="+u+"-]").length||q.push("~="),a.querySelectorAll(":checked").length||q.push(":checked"),a.querySelectorAll("a#"+u+"+*").length||q.push(".#.+[+~]")}),ja(function(a){var b=g.createElement("input");b.setAttribute("type","hidden"),a.appendChild(b).setAttribute("name","D"),a.querySelectorAll("[name=d]").length&&q.push("name"+L+"*[*^$|!~]?="),a.querySelectorAll(":enabled").length||q.push(":enabled",":disabled"),a.querySelectorAll("*,:x"),q.push(",.*:")})),(c.matchesSelector=$.test(s=o.matches||o.webkitMatchesSelector||o.mozMatchesSelector||o.oMatchesSelector||o.msMatchesSelector))&&ja(function(a){c.disconnectedMatch=s.call(a,"div"),s.call(a,"[s!='']:x"),r.push("!=",P)}),q=q.length&&new RegExp(q.join("|")),r=r.length&&new RegExp(r.join("|")),b=$.test(o.compareDocumentPosition),t=b||$.test(o.contains)?function(a,b){var c=9===a.nodeType?a.documentElement:a,d=b&&b.parentNode;return a===d||!(!d||1!==d.nodeType||!(c.contains?c.contains(d):a.compareDocumentPosition&&16&a.compareDocumentPosition(d)))}:function(a,b){if(b)while(b=b.parentNode)if(b===a)return!0;return!1},B=b?function(a,b){if(a===b)return l=!0,0;var d=!a.compareDocumentPosition-!b.compareDocumentPosition;return d?d:(d=(a.ownerDocument||a)===(b.ownerDocument||b)?a.compareDocumentPosition(b):1,1&d||!c.sortDetached&&b.compareDocumentPosition(a)===d?a===g||a.ownerDocument===v&&t(v,a)?-1:b===g||b.ownerDocument===v&&t(v,b)?1:k?J(k,a)-J(k,b):0:4&d?-1:1)}:function(a,b){if(a===b)return l=!0,0;var c,d=0,e=a.parentNode,f=b.parentNode,h=[a],i=[b];if(!e||!f)return a===g?-1:b===g?1:e?-1:f?1:k?J(k,a)-J(k,b):0;if(e===f)return la(a,b);c=a;while(c=c.parentNode)h.unshift(c);c=b;while(c=c.parentNode)i.unshift(c);while(h[d]===i[d])d++;return d?la(h[d],i[d]):h[d]===v?-1:i[d]===v?1:0},g):n},ga.matches=function(a,b){return ga(a,null,null,b)},ga.matchesSelector=function(a,b){if((a.ownerDocument||a)!==n&&m(a),b=b.replace(U,"='$1']"),!(!c.matchesSelector||!p||r&&r.test(b)||q&&q.test(b)))try{var d=s.call(a,b);if(d||c.disconnectedMatch||a.document&&11!==a.document.nodeType)return d}catch(e){}return ga(b,n,null,[a]).length>0},ga.contains=function(a,b){return(a.ownerDocument||a)!==n&&m(a),t(a,b)},ga.attr=function(a,b){(a.ownerDocument||a)!==n&&m(a);var e=d.attrHandle[b.toLowerCase()],f=e&&D.call(d.attrHandle,b.toLowerCase())?e(a,b,!p):void 0;return void 0!==f?f:c.attributes||!p?a.getAttribute(b):(f=a.getAttributeNode(b))&&f.specified?f.value:null},ga.error=function(a){throw new Error("Syntax error, unrecognized expression: "+a)},ga.uniqueSort=function(a){var b,d=[],e=0,f=0;if(l=!c.detectDuplicates,k=!c.sortStable&&a.slice(0),a.sort(B),l){while(b=a[f++])b===a[f]&&(e=d.push(f));while(e--)a.splice(d[e],1)}return k=null,a},e=ga.getText=function(a){var b,c="",d=0,f=a.nodeType;if(f){if(1===f||9===f||11===f){if("string"==typeof a.textContent)return a.textContent;for(a=a.firstChild;a;a=a.nextSibling)c+=e(a)}else if(3===f||4===f)return a.nodeValue}else while(b=a[d++])c+=e(b);return c},d=ga.selectors={cacheLength:50,createPseudo:ia,match:X,attrHandle:{},find:{},relative:{">":{dir:"parentNode",first:!0}," ":{dir:"parentNode"},"+":{dir:"previousSibling",first:!0},"~":{dir:"previousSibling"}},preFilter:{ATTR:function(a){return a[1]=a[1].replace(ca,da),a[3]=(a[3]||a[4]||a[5]||"").replace(ca,da),"~="===a[2]&&(a[3]=" "+a[3]+" "),a.slice(0,4)},CHILD:function(a){return a[1]=a[1].toLowerCase(),"nth"===a[1].slice(0,3)?(a[3]||ga.error(a[0]),a[4]=+(a[4]?a[5]+(a[6]||1):2*("even"===a[3]||"odd"===a[3])),a[5]=+(a[7]+a[8]||"odd"===a[3])):a[3]&&ga.error(a[0]),a},PSEUDO:function(a){var b,c=!a[6]&&a[2];return X.CHILD.test(a[0])?null:(a[3]?a[2]=a[4]||a[5]||"":c&&V.test(c)&&(b=g(c,!0))&&(b=c.indexOf(")",c.length-b)-c.length)&&(a[0]=a[0].slice(0,b),a[2]=c.slice(0,b)),a.slice(0,3))}},filter:{TAG:function(a){var b=a.replace(ca,da).toLowerCase();return"*"===a?function(){return!0}:function(a){return a.nodeName&&a.nodeName.toLowerCase()===b}},CLASS:function(a){var b=y[a+" "];return b||(b=new RegExp("(^|"+L+")"+a+"("+L+"|$)"))&&y(a,function(a){return b.test("string"==typeof a.className&&a.className||"undefined"!=typeof a.getAttribute&&a.getAttribute("class")||"")})},ATTR:function(a,b,c){return function(d){var e=ga.attr(d,a);return null==e?"!="===b:b?(e+="","="===b?e===c:"!="===b?e!==c:"^="===b?c&&0===e.indexOf(c):"*="===b?c&&e.indexOf(c)>-1:"$="===b?c&&e.slice(-c.length)===c:"~="===b?(" "+e.replace(Q," ")+" ").indexOf(c)>-1:"|="===b?e===c||e.slice(0,c.length+1)===c+"-":!1):!0}},CHILD:function(a,b,c,d,e){var f="nth"!==a.slice(0,3),g="last"!==a.slice(-4),h="of-type"===b;return 1===d&&0===e?function(a){return!!a.parentNode}:function(b,c,i){var j,k,l,m,n,o,p=f!==g?"nextSibling":"previousSibling",q=b.parentNode,r=h&&b.nodeName.toLowerCase(),s=!i&&!h;if(q){if(f){while(p){l=b;while(l=l[p])if(h?l.nodeName.toLowerCase()===r:1===l.nodeType)return!1;o=p="only"===a&&!o&&"nextSibling"}return!0}if(o=[g?q.firstChild:q.lastChild],g&&s){k=q[u]||(q[u]={}),j=k[a]||[],n=j[0]===w&&j[1],m=j[0]===w&&j[2],l=n&&q.childNodes[n];while(l=++n&&l&&l[p]||(m=n=0)||o.pop())if(1===l.nodeType&&++m&&l===b){k[a]=[w,n,m];break}}else if(s&&(j=(b[u]||(b[u]={}))[a])&&j[0]===w)m=j[1];else while(l=++n&&l&&l[p]||(m=n=0)||o.pop())if((h?l.nodeName.toLowerCase()===r:1===l.nodeType)&&++m&&(s&&((l[u]||(l[u]={}))[a]=[w,m]),l===b))break;return m-=e,m===d||m%d===0&&m/d>=0}}},PSEUDO:function(a,b){var c,e=d.pseudos[a]||d.setFilters[a.toLowerCase()]||ga.error("unsupported pseudo: "+a);return e[u]?e(b):e.length>1?(c=[a,a,"",b],d.setFilters.hasOwnProperty(a.toLowerCase())?ia(function(a,c){var d,f=e(a,b),g=f.length;while(g--)d=J(a,f[g]),a[d]=!(c[d]=f[g])}):function(a){return e(a,0,c)}):e}},pseudos:{not:ia(function(a){var b=[],c=[],d=h(a.replace(R,"$1"));return d[u]?ia(function(a,b,c,e){var f,g=d(a,null,e,[]),h=a.length;while(h--)(f=g[h])&&(a[h]=!(b[h]=f))}):function(a,e,f){return b[0]=a,d(b,null,f,c),b[0]=null,!c.pop()}}),has:ia(function(a){return function(b){return ga(a,b).length>0}}),contains:ia(function(a){return a=a.replace(ca,da),function(b){return(b.textContent||b.innerText||e(b)).indexOf(a)>-1}}),lang:ia(function(a){return W.test(a||"")||ga.error("unsupported lang: "+a),a=a.replace(ca,da).toLowerCase(),function(b){var c;do if(c=p?b.lang:b.getAttribute("xml:lang")||b.getAttribute("lang"))return c=c.toLowerCase(),c===a||0===c.indexOf(a+"-");while((b=b.parentNode)&&1===b.nodeType);return!1}}),target:function(b){var c=a.location&&a.location.hash;return c&&c.slice(1)===b.id},root:function(a){return a===o},focus:function(a){return a===n.activeElement&&(!n.hasFocus||n.hasFocus())&&!!(a.type||a.href||~a.tabIndex)},enabled:function(a){return a.disabled===!1},disabled:function(a){return a.disabled===!0},checked:function(a){var b=a.nodeName.toLowerCase();return"input"===b&&!!a.checked||"option"===b&&!!a.selected},selected:function(a){return a.parentNode&&a.parentNode.selectedIndex,a.selected===!0},empty:function(a){for(a=a.firstChild;a;a=a.nextSibling)if(a.nodeType<6)return!1;return!0},parent:function(a){return!d.pseudos.empty(a)},header:function(a){return Z.test(a.nodeName)},input:function(a){return Y.test(a.nodeName)},button:function(a){var b=a.nodeName.toLowerCase();return"input"===b&&"button"===a.type||"button"===b},text:function(a){var b;return"input"===a.nodeName.toLowerCase()&&"text"===a.type&&(null==(b=a.getAttribute("type"))||"text"===b.toLowerCase())},first:oa(function(){return[0]}),last:oa(function(a,b){return[b-1]}),eq:oa(function(a,b,c){return[0>c?c+b:c]}),even:oa(function(a,b){for(var c=0;b>c;c+=2)a.push(c);return a}),odd:oa(function(a,b){for(var c=1;b>c;c+=2)a.push(c);return a}),lt:oa(function(a,b,c){for(var d=0>c?c+b:c;--d>=0;)a.push(d);return a}),gt:oa(function(a,b,c){for(var d=0>c?c+b:c;++d<b;)a.push(d);return a})}},d.pseudos.nth=d.pseudos.eq;for(b in{radio:!0,checkbox:!0,file:!0,password:!0,image:!0})d.pseudos[b]=ma(b);for(b in{submit:!0,reset:!0})d.pseudos[b]=na(b);function qa(){}qa.prototype=d.filters=d.pseudos,d.setFilters=new qa,g=ga.tokenize=function(a,b){var c,e,f,g,h,i,j,k=z[a+" "];if(k)return b?0:k.slice(0);h=a,i=[],j=d.preFilter;while(h){(!c||(e=S.exec(h)))&&(e&&(h=h.slice(e[0].length)||h),i.push(f=[])),c=!1,(e=T.exec(h))&&(c=e.shift(),f.push({value:c,type:e[0].replace(R," ")}),h=h.slice(c.length));for(g in d.filter)!(e=X[g].exec(h))||j[g]&&!(e=j[g](e))||(c=e.shift(),f.push({value:c,type:g,matches:e}),h=h.slice(c.length));if(!c)break}return b?h.length:h?ga.error(a):z(a,i).slice(0)};function ra(a){for(var b=0,c=a.length,d="";c>b;b++)d+=a[b].value;return d}function sa(a,b,c){var d=b.dir,e=c&&"parentNode"===d,f=x++;return b.first?function(b,c,f){while(b=b[d])if(1===b.nodeType||e)return a(b,c,f)}:function(b,c,g){var h,i,j=[w,f];if(g){while(b=b[d])if((1===b.nodeType||e)&&a(b,c,g))return!0}else while(b=b[d])if(1===b.nodeType||e){if(i=b[u]||(b[u]={}),(h=i[d])&&h[0]===w&&h[1]===f)return j[2]=h[2];if(i[d]=j,j[2]=a(b,c,g))return!0}}}function ta(a){return a.length>1?function(b,c,d){var e=a.length;while(e--)if(!a[e](b,c,d))return!1;return!0}:a[0]}function ua(a,b,c){for(var d=0,e=b.length;e>d;d++)ga(a,b[d],c);return c}function va(a,b,c,d,e){for(var f,g=[],h=0,i=a.length,j=null!=b;i>h;h++)(f=a[h])&&(!c||c(f,d,e))&&(g.push(f),j&&b.push(h));return g}function wa(a,b,c,d,e,f){return d&&!d[u]&&(d=wa(d)),e&&!e[u]&&(e=wa(e,f)),ia(function(f,g,h,i){var j,k,l,m=[],n=[],o=g.length,p=f||ua(b||"*",h.nodeType?[h]:h,[]),q=!a||!f&&b?p:va(p,m,a,h,i),r=c?e||(f?a:o||d)?[]:g:q;if(c&&c(q,r,h,i),d){j=va(r,n),d(j,[],h,i),k=j.length;while(k--)(l=j[k])&&(r[n[k]]=!(q[n[k]]=l))}if(f){if(e||a){if(e){j=[],k=r.length;while(k--)(l=r[k])&&j.push(q[k]=l);e(null,r=[],j,i)}k=r.length;while(k--)(l=r[k])&&(j=e?J(f,l):m[k])>-1&&(f[j]=!(g[j]=l))}}else r=va(r===g?r.splice(o,r.length):r),e?e(null,g,r,i):H.apply(g,r)})}function xa(a){for(var b,c,e,f=a.length,g=d.relative[a[0].type],h=g||d.relative[" "],i=g?1:0,k=sa(function(a){return a===b},h,!0),l=sa(function(a){return J(b,a)>-1},h,!0),m=[function(a,c,d){var e=!g&&(d||c!==j)||((b=c).nodeType?k(a,c,d):l(a,c,d));return b=null,e}];f>i;i++)if(c=d.relative[a[i].type])m=[sa(ta(m),c)];else{if(c=d.filter[a[i].type].apply(null,a[i].matches),c[u]){for(e=++i;f>e;e++)if(d.relative[a[e].type])break;return wa(i>1&&ta(m),i>1&&ra(a.slice(0,i-1).concat({value:" "===a[i-2].type?"*":""})).replace(R,"$1"),c,e>i&&xa(a.slice(i,e)),f>e&&xa(a=a.slice(e)),f>e&&ra(a))}m.push(c)}return ta(m)}function ya(a,b){var c=b.length>0,e=a.length>0,f=function(f,g,h,i,k){var l,m,o,p=0,q="0",r=f&&[],s=[],t=j,u=f||e&&d.find.TAG("*",k),v=w+=null==t?1:Math.random()||.1,x=u.length;for(k&&(j=g!==n&&g);q!==x&&null!=(l=u[q]);q++){if(e&&l){m=0;while(o=a[m++])if(o(l,g,h)){i.push(l);break}k&&(w=v)}c&&((l=!o&&l)&&p--,f&&r.push(l))}if(p+=q,c&&q!==p){m=0;while(o=b[m++])o(r,s,g,h);if(f){if(p>0)while(q--)r[q]||s[q]||(s[q]=F.call(i));s=va(s)}H.apply(i,s),k&&!f&&s.length>0&&p+b.length>1&&ga.uniqueSort(i)}return k&&(w=v,j=t),r};return c?ia(f):f}return h=ga.compile=function(a,b){var c,d=[],e=[],f=A[a+" "];if(!f){b||(b=g(a)),c=b.length;while(c--)f=xa(b[c]),f[u]?d.push(f):e.push(f);f=A(a,ya(e,d)),f.selector=a}return f},i=ga.select=function(a,b,e,f){var i,j,k,l,m,n="function"==typeof a&&a,o=!f&&g(a=n.selector||a);if(e=e||[],1===o.length){if(j=o[0]=o[0].slice(0),j.length>2&&"ID"===(k=j[0]).type&&c.getById&&9===b.nodeType&&p&&d.relative[j[1].type]){if(b=(d.find.ID(k.matches[0].replace(ca,da),b)||[])[0],!b)return e;n&&(b=b.parentNode),a=a.slice(j.shift().value.length)}i=X.needsContext.test(a)?0:j.length;while(i--){if(k=j[i],d.relative[l=k.type])break;if((m=d.find[l])&&(f=m(k.matches[0].replace(ca,da),aa.test(j[0].type)&&pa(b.parentNode)||b))){if(j.splice(i,1),a=f.length&&ra(j),!a)return H.apply(e,f),e;break}}}return(n||h(a,o))(f,b,!p,e,aa.test(a)&&pa(b.parentNode)||b),e},c.sortStable=u.split("").sort(B).join("")===u,c.detectDuplicates=!!l,m(),c.sortDetached=ja(function(a){return 1&a.compareDocumentPosition(n.createElement("div"))}),ja(function(a){return a.innerHTML="<a href='#'></a>","#"===a.firstChild.getAttribute("href")})||ka("type|href|height|width",function(a,b,c){return c?void 0:a.getAttribute(b,"type"===b.toLowerCase()?1:2)}),c.attributes&&ja(function(a){return a.innerHTML="<input/>",a.firstChild.setAttribute("value",""),""===a.firstChild.getAttribute("value")})||ka("value",function(a,b,c){return c||"input"!==a.nodeName.toLowerCase()?void 0:a.defaultValue}),ja(function(a){return null==a.getAttribute("disabled")})||ka(K,function(a,b,c){var d;return c?void 0:a[b]===!0?b.toLowerCase():(d=a.getAttributeNode(b))&&d.specified?d.value:null}),ga}(a);n.find=t,n.expr=t.selectors,n.expr[":"]=n.expr.pseudos,n.unique=t.uniqueSort,n.text=t.getText,n.isXMLDoc=t.isXML,n.contains=t.contains;var u=n.expr.match.needsContext,v=/^<(\w+)\s*\/?>(?:<\/\1>|)$/,w=/^.[^:#\[\.,]*$/;function x(a,b,c){if(n.isFunction(b))return n.grep(a,function(a,d){return!!b.call(a,d,a)!==c});if(b.nodeType)return n.grep(a,function(a){return a===b!==c});if("string"==typeof b){if(w.test(b))return n.filter(b,a,c);b=n.filter(b,a)}return n.grep(a,function(a){return g.call(b,a)>=0!==c})}n.filter=function(a,b,c){var d=b[0];return c&&(a=":not("+a+")"),1===b.length&&1===d.nodeType?n.find.matchesSelector(d,a)?[d]:[]:n.find.matches(a,n.grep(b,function(a){return 1===a.nodeType}))},n.fn.extend({find:function(a){var b,c=this.length,d=[],e=this;if("string"!=typeof a)return this.pushStack(n(a).filter(function(){for(b=0;c>b;b++)if(n.contains(e[b],this))return!0}));for(b=0;c>b;b++)n.find(a,e[b],d);return d=this.pushStack(c>1?n.unique(d):d),d.selector=this.selector?this.selector+" "+a:a,d},filter:function(a){return this.pushStack(x(this,a||[],!1))},not:function(a){return this.pushStack(x(this,a||[],!0))},is:function(a){return!!x(this,"string"==typeof a&&u.test(a)?n(a):a||[],!1).length}});var y,z=/^(?:\s*(<[\w\W]+>)[^>]*|#([\w-]*))$/,A=n.fn.init=function(a,b){var c,d;if(!a)return this;if("string"==typeof a){if(c="<"===a[0]&&">"===a[a.length-1]&&a.length>=3?[null,a,null]:z.exec(a),!c||!c[1]&&b)return!b||b.jquery?(b||y).find(a):this.constructor(b).find(a);if(c[1]){if(b=b instanceof n?b[0]:b,n.merge(this,n.parseHTML(c[1],b&&b.nodeType?b.ownerDocument||b:l,!0)),v.test(c[1])&&n.isPlainObject(b))for(c in b)n.isFunction(this[c])?this[c](b[c]):this.attr(c,b[c]);return this}return d=l.getElementById(c[2]),d&&d.parentNode&&(this.length=1,this[0]=d),this.context=l,this.selector=a,this}return a.nodeType?(this.context=this[0]=a,this.length=1,this):n.isFunction(a)?"undefined"!=typeof y.ready?y.ready(a):a(n):(void 0!==a.selector&&(this.selector=a.selector,this.context=a.context),n.makeArray(a,this))};A.prototype=n.fn,y=n(l);var B=/^(?:parents|prev(?:Until|All))/,C={children:!0,contents:!0,next:!0,prev:!0};n.extend({dir:function(a,b,c){var d=[],e=void 0!==c;while((a=a[b])&&9!==a.nodeType)if(1===a.nodeType){if(e&&n(a).is(c))break;d.push(a)}return d},sibling:function(a,b){for(var c=[];a;a=a.nextSibling)1===a.nodeType&&a!==b&&c.push(a);return c}}),n.fn.extend({has:function(a){var b=n(a,this),c=b.length;return this.filter(function(){for(var a=0;c>a;a++)if(n.contains(this,b[a]))return!0})},closest:function(a,b){for(var c,d=0,e=this.length,f=[],g=u.test(a)||"string"!=typeof a?n(a,b||this.context):0;e>d;d++)for(c=this[d];c&&c!==b;c=c.parentNode)if(c.nodeType<11&&(g?g.index(c)>-1:1===c.nodeType&&n.find.matchesSelector(c,a))){f.push(c);break}return this.pushStack(f.length>1?n.unique(f):f)},index:function(a){return a?"string"==typeof a?g.call(n(a),this[0]):g.call(this,a.jquery?a[0]:a):this[0]&&this[0].parentNode?this.first().prevAll().length:-1},add:function(a,b){return this.pushStack(n.unique(n.merge(this.get(),n(a,b))))},addBack:function(a){return this.add(null==a?this.prevObject:this.prevObject.filter(a))}});function D(a,b){while((a=a[b])&&1!==a.nodeType);return a}n.each({parent:function(a){var b=a.parentNode;return b&&11!==b.nodeType?b:null},parents:function(a){return n.dir(a,"parentNode")},parentsUntil:function(a,b,c){return n.dir(a,"parentNode",c)},next:function(a){return D(a,"nextSibling")},prev:function(a){return D(a,"previousSibling")},nextAll:function(a){return n.dir(a,"nextSibling")},prevAll:function(a){return n.dir(a,"previousSibling")},nextUntil:function(a,b,c){return n.dir(a,"nextSibling",c)},prevUntil:function(a,b,c){return n.dir(a,"previousSibling",c)},siblings:function(a){return n.sibling((a.parentNode||{}).firstChild,a)},children:function(a){return n.sibling(a.firstChild)},contents:function(a){return a.contentDocument||n.merge([],a.childNodes)}},function(a,b){n.fn[a]=function(c,d){var e=n.map(this,b,c);return"Until"!==a.slice(-5)&&(d=c),d&&"string"==typeof d&&(e=n.filter(d,e)),this.length>1&&(C[a]||n.unique(e),B.test(a)&&e.reverse()),this.pushStack(e)}});var E=/\S+/g,F={};function G(a){var b=F[a]={};return n.each(a.match(E)||[],function(a,c){b[c]=!0}),b}n.Callbacks=function(a){a="string"==typeof a?F[a]||G(a):n.extend({},a);var b,c,d,e,f,g,h=[],i=!a.once&&[],j=function(l){for(b=a.memory&&l,c=!0,g=e||0,e=0,f=h.length,d=!0;h&&f>g;g++)if(h[g].apply(l[0],l[1])===!1&&a.stopOnFalse){b=!1;break}d=!1,h&&(i?i.length&&j(i.shift()):b?h=[]:k.disable())},k={add:function(){if(h){var c=h.length;!function g(b){n.each(b,function(b,c){var d=n.type(c);"function"===d?a.unique&&k.has(c)||h.push(c):c&&c.length&&"string"!==d&&g(c)})}(arguments),d?f=h.length:b&&(e=c,j(b))}return this},remove:function(){return h&&n.each(arguments,function(a,b){var c;while((c=n.inArray(b,h,c))>-1)h.splice(c,1),d&&(f>=c&&f--,g>=c&&g--)}),this},has:function(a){return a?n.inArray(a,h)>-1:!(!h||!h.length)},empty:function(){return h=[],f=0,this},disable:function(){return h=i=b=void 0,this},disabled:function(){return!h},lock:function(){return i=void 0,b||k.disable(),this},locked:function(){return!i},fireWith:function(a,b){return!h||c&&!i||(b=b||[],b=[a,b.slice?b.slice():b],d?i.push(b):j(b)),this},fire:function(){return k.fireWith(this,arguments),this},fired:function(){return!!c}};return k},n.extend({Deferred:function(a){var b=[["resolve","done",n.Callbacks("once memory"),"resolved"],["reject","fail",n.Callbacks("once memory"),"rejected"],["notify","progress",n.Callbacks("memory")]],c="pending",d={state:function(){return c},always:function(){return e.done(arguments).fail(arguments),this},then:function(){var a=arguments;return n.Deferred(function(c){n.each(b,function(b,f){var g=n.isFunction(a[b])&&a[b];e[f[1]](function(){var a=g&&g.apply(this,arguments);a&&n.isFunction(a.promise)?a.promise().done(c.resolve).fail(c.reject).progress(c.notify):c[f[0]+"With"](this===d?c.promise():this,g?[a]:arguments)})}),a=null}).promise()},promise:function(a){return null!=a?n.extend(a,d):d}},e={};return d.pipe=d.then,n.each(b,function(a,f){var g=f[2],h=f[3];d[f[1]]=g.add,h&&g.add(function(){c=h},b[1^a][2].disable,b[2][2].lock),e[f[0]]=function(){return e[f[0]+"With"](this===e?d:this,arguments),this},e[f[0]+"With"]=g.fireWith}),d.promise(e),a&&a.call(e,e),e},when:function(a){var b=0,c=d.call(arguments),e=c.length,f=1!==e||a&&n.isFunction(a.promise)?e:0,g=1===f?a:n.Deferred(),h=function(a,b,c){return function(e){b[a]=this,c[a]=arguments.length>1?d.call(arguments):e,c===i?g.notifyWith(b,c):--f||g.resolveWith(b,c)}},i,j,k;if(e>1)for(i=new Array(e),j=new Array(e),k=new Array(e);e>b;b++)c[b]&&n.isFunction(c[b].promise)?c[b].promise().done(h(b,k,c)).fail(g.reject).progress(h(b,j,i)):--f;return f||g.resolveWith(k,c),g.promise()}});var H;n.fn.ready=function(a){return n.ready.promise().done(a),this},n.extend({isReady:!1,readyWait:1,holdReady:function(a){a?n.readyWait++:n.ready(!0)},ready:function(a){(a===!0?--n.readyWait:n.isReady)||(n.isReady=!0,a!==!0&&--n.readyWait>0||(H.resolveWith(l,[n]),n.fn.triggerHandler&&(n(l).triggerHandler("ready"),n(l).off("ready"))))}});function I(){l.removeEventListener("DOMContentLoaded",I,!1),a.removeEventListener("load",I,!1),n.ready()}n.ready.promise=function(b){return H||(H=n.Deferred(),"complete"===l.readyState?setTimeout(n.ready):(l.addEventListener("DOMContentLoaded",I,!1),a.addEventListener("load",I,!1))),H.promise(b)},n.ready.promise();var J=n.access=function(a,b,c,d,e,f,g){var h=0,i=a.length,j=null==c;if("object"===n.type(c)){e=!0;for(h in c)n.access(a,b,h,c[h],!0,f,g)}else if(void 0!==d&&(e=!0,n.isFunction(d)||(g=!0),j&&(g?(b.call(a,d),b=null):(j=b,b=function(a,b,c){return j.call(n(a),c)})),b))for(;i>h;h++)b(a[h],c,g?d:d.call(a[h],h,b(a[h],c)));return e?a:j?b.call(a):i?b(a[0],c):f};n.acceptData=function(a){return 1===a.nodeType||9===a.nodeType||!+a.nodeType};function K(){Object.defineProperty(this.cache={},0,{get:function(){return{}}}),this.expando=n.expando+K.uid++}K.uid=1,K.accepts=n.acceptData,K.prototype={key:function(a){if(!K.accepts(a))return 0;var b={},c=a[this.expando];if(!c){c=K.uid++;try{b[this.expando]={value:c},Object.defineProperties(a,b)}catch(d){b[this.expando]=c,n.extend(a,b)}}return this.cache[c]||(this.cache[c]={}),c},set:function(a,b,c){var d,e=this.key(a),f=this.cache[e];if("string"==typeof b)f[b]=c;else if(n.isEmptyObject(f))n.extend(this.cache[e],b);else for(d in b)f[d]=b[d];return f},get:function(a,b){var c=this.cache[this.key(a)];return void 0===b?c:c[b]},access:function(a,b,c){var d;return void 0===b||b&&"string"==typeof b&&void 0===c?(d=this.get(a,b),void 0!==d?d:this.get(a,n.camelCase(b))):(this.set(a,b,c),void 0!==c?c:b)},remove:function(a,b){var c,d,e,f=this.key(a),g=this.cache[f];if(void 0===b)this.cache[f]={};else{n.isArray(b)?d=b.concat(b.map(n.camelCase)):(e=n.camelCase(b),b in g?d=[b,e]:(d=e,d=d in g?[d]:d.match(E)||[])),c=d.length;while(c--)delete g[d[c]]}},hasData:function(a){return!n.isEmptyObject(this.cache[a[this.expando]]||{})},discard:function(a){a[this.expando]&&delete this.cache[a[this.expando]]}};var L=new K,M=new K,N=/^(?:\{[\w\W]*\}|\[[\w\W]*\])$/,O=/([A-Z])/g;function P(a,b,c){var d;if(void 0===c&&1===a.nodeType)if(d="data-"+b.replace(O,"-$1").toLowerCase(),c=a.getAttribute(d),"string"==typeof c){try{c="true"===c?!0:"false"===c?!1:"null"===c?null:+c+""===c?+c:N.test(c)?n.parseJSON(c):c}catch(e){}M.set(a,b,c)}else c=void 0;return c}n.extend({hasData:function(a){return M.hasData(a)||L.hasData(a)},data:function(a,b,c){
return M.access(a,b,c)},removeData:function(a,b){M.remove(a,b)},_data:function(a,b,c){return L.access(a,b,c)},_removeData:function(a,b){L.remove(a,b)}}),n.fn.extend({data:function(a,b){var c,d,e,f=this[0],g=f&&f.attributes;if(void 0===a){if(this.length&&(e=M.get(f),1===f.nodeType&&!L.get(f,"hasDataAttrs"))){c=g.length;while(c--)g[c]&&(d=g[c].name,0===d.indexOf("data-")&&(d=n.camelCase(d.slice(5)),P(f,d,e[d])));L.set(f,"hasDataAttrs",!0)}return e}return"object"==typeof a?this.each(function(){M.set(this,a)}):J(this,function(b){var c,d=n.camelCase(a);if(f&&void 0===b){if(c=M.get(f,a),void 0!==c)return c;if(c=M.get(f,d),void 0!==c)return c;if(c=P(f,d,void 0),void 0!==c)return c}else this.each(function(){var c=M.get(this,d);M.set(this,d,b),-1!==a.indexOf("-")&&void 0!==c&&M.set(this,a,b)})},null,b,arguments.length>1,null,!0)},removeData:function(a){return this.each(function(){M.remove(this,a)})}}),n.extend({queue:function(a,b,c){var d;return a?(b=(b||"fx")+"queue",d=L.get(a,b),c&&(!d||n.isArray(c)?d=L.access(a,b,n.makeArray(c)):d.push(c)),d||[]):void 0},dequeue:function(a,b){b=b||"fx";var c=n.queue(a,b),d=c.length,e=c.shift(),f=n._queueHooks(a,b),g=function(){n.dequeue(a,b)};"inprogress"===e&&(e=c.shift(),d--),e&&("fx"===b&&c.unshift("inprogress"),delete f.stop,e.call(a,g,f)),!d&&f&&f.empty.fire()},_queueHooks:function(a,b){var c=b+"queueHooks";return L.get(a,c)||L.access(a,c,{empty:n.Callbacks("once memory").add(function(){L.remove(a,[b+"queue",c])})})}}),n.fn.extend({queue:function(a,b){var c=2;return"string"!=typeof a&&(b=a,a="fx",c--),arguments.length<c?n.queue(this[0],a):void 0===b?this:this.each(function(){var c=n.queue(this,a,b);n._queueHooks(this,a),"fx"===a&&"inprogress"!==c[0]&&n.dequeue(this,a)})},dequeue:function(a){return this.each(function(){n.dequeue(this,a)})},clearQueue:function(a){return this.queue(a||"fx",[])},promise:function(a,b){var c,d=1,e=n.Deferred(),f=this,g=this.length,h=function(){--d||e.resolveWith(f,[f])};"string"!=typeof a&&(b=a,a=void 0),a=a||"fx";while(g--)c=L.get(f[g],a+"queueHooks"),c&&c.empty&&(d++,c.empty.add(h));return h(),e.promise(b)}});var Q=/[+-]?(?:\d*\.|)\d+(?:[eE][+-]?\d+|)/.source,R=["Top","Right","Bottom","Left"],S=function(a,b){return a=b||a,"none"===n.css(a,"display")||!n.contains(a.ownerDocument,a)},T=/^(?:checkbox|radio)$/i;!function(){var a=l.createDocumentFragment(),b=a.appendChild(l.createElement("div")),c=l.createElement("input");c.setAttribute("type","radio"),c.setAttribute("checked","checked"),c.setAttribute("name","t"),b.appendChild(c),k.checkClone=b.cloneNode(!0).cloneNode(!0).lastChild.checked,b.innerHTML="<textarea>x</textarea>",k.noCloneChecked=!!b.cloneNode(!0).lastChild.defaultValue}();var U="undefined";k.focusinBubbles="onfocusin"in a;var V=/^key/,W=/^(?:mouse|pointer|contextmenu)|click/,X=/^(?:focusinfocus|focusoutblur)$/,Y=/^([^.]*)(?:\.(.+)|)$/;function Z(){return!0}function $(){return!1}function _(){try{return l.activeElement}catch(a){}}n.event={global:{},add:function(a,b,c,d,e){var f,g,h,i,j,k,l,m,o,p,q,r=L.get(a);if(r){c.handler&&(f=c,c=f.handler,e=f.selector),c.guid||(c.guid=n.guid++),(i=r.events)||(i=r.events={}),(g=r.handle)||(g=r.handle=function(b){return typeof n!==U&&n.event.triggered!==b.type?n.event.dispatch.apply(a,arguments):void 0}),b=(b||"").match(E)||[""],j=b.length;while(j--)h=Y.exec(b[j])||[],o=q=h[1],p=(h[2]||"").split(".").sort(),o&&(l=n.event.special[o]||{},o=(e?l.delegateType:l.bindType)||o,l=n.event.special[o]||{},k=n.extend({type:o,origType:q,data:d,handler:c,guid:c.guid,selector:e,needsContext:e&&n.expr.match.needsContext.test(e),namespace:p.join(".")},f),(m=i[o])||(m=i[o]=[],m.delegateCount=0,l.setup&&l.setup.call(a,d,p,g)!==!1||a.addEventListener&&a.addEventListener(o,g,!1)),l.add&&(l.add.call(a,k),k.handler.guid||(k.handler.guid=c.guid)),e?m.splice(m.delegateCount++,0,k):m.push(k),n.event.global[o]=!0)}},remove:function(a,b,c,d,e){var f,g,h,i,j,k,l,m,o,p,q,r=L.hasData(a)&&L.get(a);if(r&&(i=r.events)){b=(b||"").match(E)||[""],j=b.length;while(j--)if(h=Y.exec(b[j])||[],o=q=h[1],p=(h[2]||"").split(".").sort(),o){l=n.event.special[o]||{},o=(d?l.delegateType:l.bindType)||o,m=i[o]||[],h=h[2]&&new RegExp("(^|\\.)"+p.join("\\.(?:.*\\.|)")+"(\\.|$)"),g=f=m.length;while(f--)k=m[f],!e&&q!==k.origType||c&&c.guid!==k.guid||h&&!h.test(k.namespace)||d&&d!==k.selector&&("**"!==d||!k.selector)||(m.splice(f,1),k.selector&&m.delegateCount--,l.remove&&l.remove.call(a,k));g&&!m.length&&(l.teardown&&l.teardown.call(a,p,r.handle)!==!1||n.removeEvent(a,o,r.handle),delete i[o])}else for(o in i)n.event.remove(a,o+b[j],c,d,!0);n.isEmptyObject(i)&&(delete r.handle,L.remove(a,"events"))}},trigger:function(b,c,d,e){var f,g,h,i,k,m,o,p=[d||l],q=j.call(b,"type")?b.type:b,r=j.call(b,"namespace")?b.namespace.split("."):[];if(g=h=d=d||l,3!==d.nodeType&&8!==d.nodeType&&!X.test(q+n.event.triggered)&&(q.indexOf(".")>=0&&(r=q.split("."),q=r.shift(),r.sort()),k=q.indexOf(":")<0&&"on"+q,b=b[n.expando]?b:new n.Event(q,"object"==typeof b&&b),b.isTrigger=e?2:3,b.namespace=r.join("."),b.namespace_re=b.namespace?new RegExp("(^|\\.)"+r.join("\\.(?:.*\\.|)")+"(\\.|$)"):null,b.result=void 0,b.target||(b.target=d),c=null==c?[b]:n.makeArray(c,[b]),o=n.event.special[q]||{},e||!o.trigger||o.trigger.apply(d,c)!==!1)){if(!e&&!o.noBubble&&!n.isWindow(d)){for(i=o.delegateType||q,X.test(i+q)||(g=g.parentNode);g;g=g.parentNode)p.push(g),h=g;h===(d.ownerDocument||l)&&p.push(h.defaultView||h.parentWindow||a)}f=0;while((g=p[f++])&&!b.isPropagationStopped())b.type=f>1?i:o.bindType||q,m=(L.get(g,"events")||{})[b.type]&&L.get(g,"handle"),m&&m.apply(g,c),m=k&&g[k],m&&m.apply&&n.acceptData(g)&&(b.result=m.apply(g,c),b.result===!1&&b.preventDefault());return b.type=q,e||b.isDefaultPrevented()||o._default&&o._default.apply(p.pop(),c)!==!1||!n.acceptData(d)||k&&n.isFunction(d[q])&&!n.isWindow(d)&&(h=d[k],h&&(d[k]=null),n.event.triggered=q,d[q](),n.event.triggered=void 0,h&&(d[k]=h)),b.result}},dispatch:function(a){a=n.event.fix(a);var b,c,e,f,g,h=[],i=d.call(arguments),j=(L.get(this,"events")||{})[a.type]||[],k=n.event.special[a.type]||{};if(i[0]=a,a.delegateTarget=this,!k.preDispatch||k.preDispatch.call(this,a)!==!1){h=n.event.handlers.call(this,a,j),b=0;while((f=h[b++])&&!a.isPropagationStopped()){a.currentTarget=f.elem,c=0;while((g=f.handlers[c++])&&!a.isImmediatePropagationStopped())(!a.namespace_re||a.namespace_re.test(g.namespace))&&(a.handleObj=g,a.data=g.data,e=((n.event.special[g.origType]||{}).handle||g.handler).apply(f.elem,i),void 0!==e&&(a.result=e)===!1&&(a.preventDefault(),a.stopPropagation()))}return k.postDispatch&&k.postDispatch.call(this,a),a.result}},handlers:function(a,b){var c,d,e,f,g=[],h=b.delegateCount,i=a.target;if(h&&i.nodeType&&(!a.button||"click"!==a.type))for(;i!==this;i=i.parentNode||this)if(i.disabled!==!0||"click"!==a.type){for(d=[],c=0;h>c;c++)f=b[c],e=f.selector+" ",void 0===d[e]&&(d[e]=f.needsContext?n(e,this).index(i)>=0:n.find(e,this,null,[i]).length),d[e]&&d.push(f);d.length&&g.push({elem:i,handlers:d})}return h<b.length&&g.push({elem:this,handlers:b.slice(h)}),g},props:"altKey bubbles cancelable ctrlKey currentTarget eventPhase metaKey relatedTarget shiftKey target timeStamp view which".split(" "),fixHooks:{},keyHooks:{props:"char charCode key keyCode".split(" "),filter:function(a,b){return null==a.which&&(a.which=null!=b.charCode?b.charCode:b.keyCode),a}},mouseHooks:{props:"button buttons clientX clientY offsetX offsetY pageX pageY screenX screenY toElement".split(" "),filter:function(a,b){var c,d,e,f=b.button;return null==a.pageX&&null!=b.clientX&&(c=a.target.ownerDocument||l,d=c.documentElement,e=c.body,a.pageX=b.clientX+(d&&d.scrollLeft||e&&e.scrollLeft||0)-(d&&d.clientLeft||e&&e.clientLeft||0),a.pageY=b.clientY+(d&&d.scrollTop||e&&e.scrollTop||0)-(d&&d.clientTop||e&&e.clientTop||0)),a.which||void 0===f||(a.which=1&f?1:2&f?3:4&f?2:0),a}},fix:function(a){if(a[n.expando])return a;var b,c,d,e=a.type,f=a,g=this.fixHooks[e];g||(this.fixHooks[e]=g=W.test(e)?this.mouseHooks:V.test(e)?this.keyHooks:{}),d=g.props?this.props.concat(g.props):this.props,a=new n.Event(f),b=d.length;while(b--)c=d[b],a[c]=f[c];return a.target||(a.target=l),3===a.target.nodeType&&(a.target=a.target.parentNode),g.filter?g.filter(a,f):a},special:{load:{noBubble:!0},focus:{trigger:function(){return this!==_()&&this.focus?(this.focus(),!1):void 0},delegateType:"focusin"},blur:{trigger:function(){return this===_()&&this.blur?(this.blur(),!1):void 0},delegateType:"focusout"},click:{trigger:function(){return"checkbox"===this.type&&this.click&&n.nodeName(this,"input")?(this.click(),!1):void 0},_default:function(a){return n.nodeName(a.target,"a")}},beforeunload:{postDispatch:function(a){void 0!==a.result&&a.originalEvent&&(a.originalEvent.returnValue=a.result)}}},simulate:function(a,b,c,d){var e=n.extend(new n.Event,c,{type:a,isSimulated:!0,originalEvent:{}});d?n.event.trigger(e,null,b):n.event.dispatch.call(b,e),e.isDefaultPrevented()&&c.preventDefault()}},n.removeEvent=function(a,b,c){a.removeEventListener&&a.removeEventListener(b,c,!1)},n.Event=function(a,b){return this instanceof n.Event?(a&&a.type?(this.originalEvent=a,this.type=a.type,this.isDefaultPrevented=a.defaultPrevented||void 0===a.defaultPrevented&&a.returnValue===!1?Z:$):this.type=a,b&&n.extend(this,b),this.timeStamp=a&&a.timeStamp||n.now(),void(this[n.expando]=!0)):new n.Event(a,b)},n.Event.prototype={isDefaultPrevented:$,isPropagationStopped:$,isImmediatePropagationStopped:$,preventDefault:function(){var a=this.originalEvent;this.isDefaultPrevented=Z,a&&a.preventDefault&&a.preventDefault()},stopPropagation:function(){var a=this.originalEvent;this.isPropagationStopped=Z,a&&a.stopPropagation&&a.stopPropagation()},stopImmediatePropagation:function(){var a=this.originalEvent;this.isImmediatePropagationStopped=Z,a&&a.stopImmediatePropagation&&a.stopImmediatePropagation(),this.stopPropagation()}},n.each({mouseenter:"mouseover",mouseleave:"mouseout",pointerenter:"pointerover",pointerleave:"pointerout"},function(a,b){n.event.special[a]={delegateType:b,bindType:b,handle:function(a){var c,d=this,e=a.relatedTarget,f=a.handleObj;return(!e||e!==d&&!n.contains(d,e))&&(a.type=f.origType,c=f.handler.apply(this,arguments),a.type=b),c}}}),k.focusinBubbles||n.each({focus:"focusin",blur:"focusout"},function(a,b){var c=function(a){n.event.simulate(b,a.target,n.event.fix(a),!0)};n.event.special[b]={setup:function(){var d=this.ownerDocument||this,e=L.access(d,b);e||d.addEventListener(a,c,!0),L.access(d,b,(e||0)+1)},teardown:function(){var d=this.ownerDocument||this,e=L.access(d,b)-1;e?L.access(d,b,e):(d.removeEventListener(a,c,!0),L.remove(d,b))}}}),n.fn.extend({on:function(a,b,c,d,e){var f,g;if("object"==typeof a){"string"!=typeof b&&(c=c||b,b=void 0);for(g in a)this.on(g,b,c,a[g],e);return this}if(null==c&&null==d?(d=b,c=b=void 0):null==d&&("string"==typeof b?(d=c,c=void 0):(d=c,c=b,b=void 0)),d===!1)d=$;else if(!d)return this;return 1===e&&(f=d,d=function(a){return n().off(a),f.apply(this,arguments)},d.guid=f.guid||(f.guid=n.guid++)),this.each(function(){n.event.add(this,a,d,c,b)})},one:function(a,b,c,d){return this.on(a,b,c,d,1)},off:function(a,b,c){var d,e;if(a&&a.preventDefault&&a.handleObj)return d=a.handleObj,n(a.delegateTarget).off(d.namespace?d.origType+"."+d.namespace:d.origType,d.selector,d.handler),this;if("object"==typeof a){for(e in a)this.off(e,b,a[e]);return this}return(b===!1||"function"==typeof b)&&(c=b,b=void 0),c===!1&&(c=$),this.each(function(){n.event.remove(this,a,c,b)})},trigger:function(a,b){return this.each(function(){n.event.trigger(a,b,this)})},triggerHandler:function(a,b){var c=this[0];return c?n.event.trigger(a,b,c,!0):void 0}});var aa=/<(?!area|br|col|embed|hr|img|input|link|meta|param)(([\w:]+)[^>]*)\/>/gi,ba=/<([\w:]+)/,ca=/<|&#?\w+;/,da=/<(?:script|style|link)/i,ea=/checked\s*(?:[^=]|=\s*.checked.)/i,fa=/^$|\/(?:java|ecma)script/i,ga=/^true\/(.*)/,ha=/^\s*<!(?:\[CDATA\[|--)|(?:\]\]|--)>\s*$/g,ia={option:[1,"<select multiple='multiple'>","</select>"],thead:[1,"<table>","</table>"],col:[2,"<table><colgroup>","</colgroup></table>"],tr:[2,"<table><tbody>","</tbody></table>"],td:[3,"<table><tbody><tr>","</tr></tbody></table>"],_default:[0,"",""]};ia.optgroup=ia.option,ia.tbody=ia.tfoot=ia.colgroup=ia.caption=ia.thead,ia.th=ia.td;function ja(a,b){return n.nodeName(a,"table")&&n.nodeName(11!==b.nodeType?b:b.firstChild,"tr")?a.getElementsByTagName("tbody")[0]||a.appendChild(a.ownerDocument.createElement("tbody")):a}function ka(a){return a.type=(null!==a.getAttribute("type"))+"/"+a.type,a}function la(a){var b=ga.exec(a.type);return b?a.type=b[1]:a.removeAttribute("type"),a}function ma(a,b){for(var c=0,d=a.length;d>c;c++)L.set(a[c],"globalEval",!b||L.get(b[c],"globalEval"))}function na(a,b){var c,d,e,f,g,h,i,j;if(1===b.nodeType){if(L.hasData(a)&&(f=L.access(a),g=L.set(b,f),j=f.events)){delete g.handle,g.events={};for(e in j)for(c=0,d=j[e].length;d>c;c++)n.event.add(b,e,j[e][c])}M.hasData(a)&&(h=M.access(a),i=n.extend({},h),M.set(b,i))}}function oa(a,b){var c=a.getElementsByTagName?a.getElementsByTagName(b||"*"):a.querySelectorAll?a.querySelectorAll(b||"*"):[];return void 0===b||b&&n.nodeName(a,b)?n.merge([a],c):c}function pa(a,b){var c=b.nodeName.toLowerCase();"input"===c&&T.test(a.type)?b.checked=a.checked:("input"===c||"textarea"===c)&&(b.defaultValue=a.defaultValue)}n.extend({clone:function(a,b,c){var d,e,f,g,h=a.cloneNode(!0),i=n.contains(a.ownerDocument,a);if(!(k.noCloneChecked||1!==a.nodeType&&11!==a.nodeType||n.isXMLDoc(a)))for(g=oa(h),f=oa(a),d=0,e=f.length;e>d;d++)pa(f[d],g[d]);if(b)if(c)for(f=f||oa(a),g=g||oa(h),d=0,e=f.length;e>d;d++)na(f[d],g[d]);else na(a,h);return g=oa(h,"script"),g.length>0&&ma(g,!i&&oa(a,"script")),h},buildFragment:function(a,b,c,d){for(var e,f,g,h,i,j,k=b.createDocumentFragment(),l=[],m=0,o=a.length;o>m;m++)if(e=a[m],e||0===e)if("object"===n.type(e))n.merge(l,e.nodeType?[e]:e);else if(ca.test(e)){f=f||k.appendChild(b.createElement("div")),g=(ba.exec(e)||["",""])[1].toLowerCase(),h=ia[g]||ia._default,f.innerHTML=h[1]+e.replace(aa,"<$1></$2>")+h[2],j=h[0];while(j--)f=f.lastChild;n.merge(l,f.childNodes),f=k.firstChild,f.textContent=""}else l.push(b.createTextNode(e));k.textContent="",m=0;while(e=l[m++])if((!d||-1===n.inArray(e,d))&&(i=n.contains(e.ownerDocument,e),f=oa(k.appendChild(e),"script"),i&&ma(f),c)){j=0;while(e=f[j++])fa.test(e.type||"")&&c.push(e)}return k},cleanData:function(a){for(var b,c,d,e,f=n.event.special,g=0;void 0!==(c=a[g]);g++){if(n.acceptData(c)&&(e=c[L.expando],e&&(b=L.cache[e]))){if(b.events)for(d in b.events)f[d]?n.event.remove(c,d):n.removeEvent(c,d,b.handle);L.cache[e]&&delete L.cache[e]}delete M.cache[c[M.expando]]}}}),n.fn.extend({text:function(a){return J(this,function(a){return void 0===a?n.text(this):this.empty().each(function(){(1===this.nodeType||11===this.nodeType||9===this.nodeType)&&(this.textContent=a)})},null,a,arguments.length)},append:function(){return this.domManip(arguments,function(a){if(1===this.nodeType||11===this.nodeType||9===this.nodeType){var b=ja(this,a);b.appendChild(a)}})},prepend:function(){return this.domManip(arguments,function(a){if(1===this.nodeType||11===this.nodeType||9===this.nodeType){var b=ja(this,a);b.insertBefore(a,b.firstChild)}})},before:function(){return this.domManip(arguments,function(a){this.parentNode&&this.parentNode.insertBefore(a,this)})},after:function(){return this.domManip(arguments,function(a){this.parentNode&&this.parentNode.insertBefore(a,this.nextSibling)})},remove:function(a,b){for(var c,d=a?n.filter(a,this):this,e=0;null!=(c=d[e]);e++)b||1!==c.nodeType||n.cleanData(oa(c)),c.parentNode&&(b&&n.contains(c.ownerDocument,c)&&ma(oa(c,"script")),c.parentNode.removeChild(c));return this},empty:function(){for(var a,b=0;null!=(a=this[b]);b++)1===a.nodeType&&(n.cleanData(oa(a,!1)),a.textContent="");return this},clone:function(a,b){return a=null==a?!1:a,b=null==b?a:b,this.map(function(){return n.clone(this,a,b)})},html:function(a){return J(this,function(a){var b=this[0]||{},c=0,d=this.length;if(void 0===a&&1===b.nodeType)return b.innerHTML;if("string"==typeof a&&!da.test(a)&&!ia[(ba.exec(a)||["",""])[1].toLowerCase()]){a=a.replace(aa,"<$1></$2>");try{for(;d>c;c++)b=this[c]||{},1===b.nodeType&&(n.cleanData(oa(b,!1)),b.innerHTML=a);b=0}catch(e){}}b&&this.empty().append(a)},null,a,arguments.length)},replaceWith:function(){var a=arguments[0];return this.domManip(arguments,function(b){a=this.parentNode,n.cleanData(oa(this)),a&&a.replaceChild(b,this)}),a&&(a.length||a.nodeType)?this:this.remove()},detach:function(a){return this.remove(a,!0)},domManip:function(a,b){a=e.apply([],a);var c,d,f,g,h,i,j=0,l=this.length,m=this,o=l-1,p=a[0],q=n.isFunction(p);if(q||l>1&&"string"==typeof p&&!k.checkClone&&ea.test(p))return this.each(function(c){var d=m.eq(c);q&&(a[0]=p.call(this,c,d.html())),d.domManip(a,b)});if(l&&(c=n.buildFragment(a,this[0].ownerDocument,!1,this),d=c.firstChild,1===c.childNodes.length&&(c=d),d)){for(f=n.map(oa(c,"script"),ka),g=f.length;l>j;j++)h=c,j!==o&&(h=n.clone(h,!0,!0),g&&n.merge(f,oa(h,"script"))),b.call(this[j],h,j);if(g)for(i=f[f.length-1].ownerDocument,n.map(f,la),j=0;g>j;j++)h=f[j],fa.test(h.type||"")&&!L.access(h,"globalEval")&&n.contains(i,h)&&(h.src?n._evalUrl&&n._evalUrl(h.src):n.globalEval(h.textContent.replace(ha,"")))}return this}}),n.each({appendTo:"append",prependTo:"prepend",insertBefore:"before",insertAfter:"after",replaceAll:"replaceWith"},function(a,b){n.fn[a]=function(a){for(var c,d=[],e=n(a),g=e.length-1,h=0;g>=h;h++)c=h===g?this:this.clone(!0),n(e[h])[b](c),f.apply(d,c.get());return this.pushStack(d)}});var qa,ra={};function sa(b,c){var d,e=n(c.createElement(b)).appendTo(c.body),f=a.getDefaultComputedStyle&&(d=a.getDefaultComputedStyle(e[0]))?d.display:n.css(e[0],"display");return e.detach(),f}function ta(a){var b=l,c=ra[a];return c||(c=sa(a,b),"none"!==c&&c||(qa=(qa||n("<iframe frameborder='0' width='0' height='0'/>")).appendTo(b.documentElement),b=qa[0].contentDocument,b.write(),b.close(),c=sa(a,b),qa.detach()),ra[a]=c),c}var ua=/^margin/,va=new RegExp("^("+Q+")(?!px)[a-z%]+$","i"),wa=function(b){return b.ownerDocument.defaultView.opener?b.ownerDocument.defaultView.getComputedStyle(b,null):a.getComputedStyle(b,null)};function xa(a,b,c){var d,e,f,g,h=a.style;return c=c||wa(a),c&&(g=c.getPropertyValue(b)||c[b]),c&&(""!==g||n.contains(a.ownerDocument,a)||(g=n.style(a,b)),va.test(g)&&ua.test(b)&&(d=h.width,e=h.minWidth,f=h.maxWidth,h.minWidth=h.maxWidth=h.width=g,g=c.width,h.width=d,h.minWidth=e,h.maxWidth=f)),void 0!==g?g+"":g}function ya(a,b){return{get:function(){return a()?void delete this.get:(this.get=b).apply(this,arguments)}}}!function(){var b,c,d=l.documentElement,e=l.createElement("div"),f=l.createElement("div");if(f.style){f.style.backgroundClip="content-box",f.cloneNode(!0).style.backgroundClip="",k.clearCloneStyle="content-box"===f.style.backgroundClip,e.style.cssText="border:0;width:0;height:0;top:0;left:-9999px;margin-top:1px;position:absolute",e.appendChild(f);function g(){f.style.cssText="-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;display:block;margin-top:1%;top:1%;border:1px;padding:1px;width:4px;position:absolute",f.innerHTML="",d.appendChild(e);var g=a.getComputedStyle(f,null);b="1%"!==g.top,c="4px"===g.width,d.removeChild(e)}a.getComputedStyle&&n.extend(k,{pixelPosition:function(){return g(),b},boxSizingReliable:function(){return null==c&&g(),c},reliableMarginRight:function(){var b,c=f.appendChild(l.createElement("div"));return c.style.cssText=f.style.cssText="-webkit-box-sizing:content-box;-moz-box-sizing:content-box;box-sizing:content-box;display:block;margin:0;border:0;padding:0",c.style.marginRight=c.style.width="0",f.style.width="1px",d.appendChild(e),b=!parseFloat(a.getComputedStyle(c,null).marginRight),d.removeChild(e),f.removeChild(c),b}})}}(),n.swap=function(a,b,c,d){var e,f,g={};for(f in b)g[f]=a.style[f],a.style[f]=b[f];e=c.apply(a,d||[]);for(f in b)a.style[f]=g[f];return e};var za=/^(none|table(?!-c[ea]).+)/,Aa=new RegExp("^("+Q+")(.*)$","i"),Ba=new RegExp("^([+-])=("+Q+")","i"),Ca={position:"absolute",visibility:"hidden",display:"block"},Da={letterSpacing:"0",fontWeight:"400"},Ea=["Webkit","O","Moz","ms"];function Fa(a,b){if(b in a)return b;var c=b[0].toUpperCase()+b.slice(1),d=b,e=Ea.length;while(e--)if(b=Ea[e]+c,b in a)return b;return d}function Ga(a,b,c){var d=Aa.exec(b);return d?Math.max(0,d[1]-(c||0))+(d[2]||"px"):b}function Ha(a,b,c,d,e){for(var f=c===(d?"border":"content")?4:"width"===b?1:0,g=0;4>f;f+=2)"margin"===c&&(g+=n.css(a,c+R[f],!0,e)),d?("content"===c&&(g-=n.css(a,"padding"+R[f],!0,e)),"margin"!==c&&(g-=n.css(a,"border"+R[f]+"Width",!0,e))):(g+=n.css(a,"padding"+R[f],!0,e),"padding"!==c&&(g+=n.css(a,"border"+R[f]+"Width",!0,e)));return g}function Ia(a,b,c){var d=!0,e="width"===b?a.offsetWidth:a.offsetHeight,f=wa(a),g="border-box"===n.css(a,"boxSizing",!1,f);if(0>=e||null==e){if(e=xa(a,b,f),(0>e||null==e)&&(e=a.style[b]),va.test(e))return e;d=g&&(k.boxSizingReliable()||e===a.style[b]),e=parseFloat(e)||0}return e+Ha(a,b,c||(g?"border":"content"),d,f)+"px"}function Ja(a,b){for(var c,d,e,f=[],g=0,h=a.length;h>g;g++)d=a[g],d.style&&(f[g]=L.get(d,"olddisplay"),c=d.style.display,b?(f[g]||"none"!==c||(d.style.display=""),""===d.style.display&&S(d)&&(f[g]=L.access(d,"olddisplay",ta(d.nodeName)))):(e=S(d),"none"===c&&e||L.set(d,"olddisplay",e?c:n.css(d,"display"))));for(g=0;h>g;g++)d=a[g],d.style&&(b&&"none"!==d.style.display&&""!==d.style.display||(d.style.display=b?f[g]||"":"none"));return a}n.extend({cssHooks:{opacity:{get:function(a,b){if(b){var c=xa(a,"opacity");return""===c?"1":c}}}},cssNumber:{columnCount:!0,fillOpacity:!0,flexGrow:!0,flexShrink:!0,fontWeight:!0,lineHeight:!0,opacity:!0,order:!0,orphans:!0,widows:!0,zIndex:!0,zoom:!0},cssProps:{"float":"cssFloat"},style:function(a,b,c,d){if(a&&3!==a.nodeType&&8!==a.nodeType&&a.style){var e,f,g,h=n.camelCase(b),i=a.style;return b=n.cssProps[h]||(n.cssProps[h]=Fa(i,h)),g=n.cssHooks[b]||n.cssHooks[h],void 0===c?g&&"get"in g&&void 0!==(e=g.get(a,!1,d))?e:i[b]:(f=typeof c,"string"===f&&(e=Ba.exec(c))&&(c=(e[1]+1)*e[2]+parseFloat(n.css(a,b)),f="number"),null!=c&&c===c&&("number"!==f||n.cssNumber[h]||(c+="px"),k.clearCloneStyle||""!==c||0!==b.indexOf("background")||(i[b]="inherit"),g&&"set"in g&&void 0===(c=g.set(a,c,d))||(i[b]=c)),void 0)}},css:function(a,b,c,d){var e,f,g,h=n.camelCase(b);return b=n.cssProps[h]||(n.cssProps[h]=Fa(a.style,h)),g=n.cssHooks[b]||n.cssHooks[h],g&&"get"in g&&(e=g.get(a,!0,c)),void 0===e&&(e=xa(a,b,d)),"normal"===e&&b in Da&&(e=Da[b]),""===c||c?(f=parseFloat(e),c===!0||n.isNumeric(f)?f||0:e):e}}),n.each(["height","width"],function(a,b){n.cssHooks[b]={get:function(a,c,d){return c?za.test(n.css(a,"display"))&&0===a.offsetWidth?n.swap(a,Ca,function(){return Ia(a,b,d)}):Ia(a,b,d):void 0},set:function(a,c,d){var e=d&&wa(a);return Ga(a,c,d?Ha(a,b,d,"border-box"===n.css(a,"boxSizing",!1,e),e):0)}}}),n.cssHooks.marginRight=ya(k.reliableMarginRight,function(a,b){return b?n.swap(a,{display:"inline-block"},xa,[a,"marginRight"]):void 0}),n.each({margin:"",padding:"",border:"Width"},function(a,b){n.cssHooks[a+b]={expand:function(c){for(var d=0,e={},f="string"==typeof c?c.split(" "):[c];4>d;d++)e[a+R[d]+b]=f[d]||f[d-2]||f[0];return e}},ua.test(a)||(n.cssHooks[a+b].set=Ga)}),n.fn.extend({css:function(a,b){return J(this,function(a,b,c){var d,e,f={},g=0;if(n.isArray(b)){for(d=wa(a),e=b.length;e>g;g++)f[b[g]]=n.css(a,b[g],!1,d);return f}return void 0!==c?n.style(a,b,c):n.css(a,b)},a,b,arguments.length>1)},show:function(){return Ja(this,!0)},hide:function(){return Ja(this)},toggle:function(a){return"boolean"==typeof a?a?this.show():this.hide():this.each(function(){S(this)?n(this).show():n(this).hide()})}});function Ka(a,b,c,d,e){return new Ka.prototype.init(a,b,c,d,e)}n.Tween=Ka,Ka.prototype={constructor:Ka,init:function(a,b,c,d,e,f){this.elem=a,this.prop=c,this.easing=e||"swing",this.options=b,this.start=this.now=this.cur(),this.end=d,this.unit=f||(n.cssNumber[c]?"":"px")},cur:function(){var a=Ka.propHooks[this.prop];return a&&a.get?a.get(this):Ka.propHooks._default.get(this)},run:function(a){var b,c=Ka.propHooks[this.prop];return this.options.duration?this.pos=b=n.easing[this.easing](a,this.options.duration*a,0,1,this.options.duration):this.pos=b=a,this.now=(this.end-this.start)*b+this.start,this.options.step&&this.options.step.call(this.elem,this.now,this),c&&c.set?c.set(this):Ka.propHooks._default.set(this),this}},Ka.prototype.init.prototype=Ka.prototype,Ka.propHooks={_default:{get:function(a){var b;return null==a.elem[a.prop]||a.elem.style&&null!=a.elem.style[a.prop]?(b=n.css(a.elem,a.prop,""),b&&"auto"!==b?b:0):a.elem[a.prop]},set:function(a){n.fx.step[a.prop]?n.fx.step[a.prop](a):a.elem.style&&(null!=a.elem.style[n.cssProps[a.prop]]||n.cssHooks[a.prop])?n.style(a.elem,a.prop,a.now+a.unit):a.elem[a.prop]=a.now}}},Ka.propHooks.scrollTop=Ka.propHooks.scrollLeft={set:function(a){a.elem.nodeType&&a.elem.parentNode&&(a.elem[a.prop]=a.now)}},n.easing={linear:function(a){return a},swing:function(a){return.5-Math.cos(a*Math.PI)/2}},n.fx=Ka.prototype.init,n.fx.step={};var La,Ma,Na=/^(?:toggle|show|hide)$/,Oa=new RegExp("^(?:([+-])=|)("+Q+")([a-z%]*)$","i"),Pa=/queueHooks$/,Qa=[Va],Ra={"*":[function(a,b){var c=this.createTween(a,b),d=c.cur(),e=Oa.exec(b),f=e&&e[3]||(n.cssNumber[a]?"":"px"),g=(n.cssNumber[a]||"px"!==f&&+d)&&Oa.exec(n.css(c.elem,a)),h=1,i=20;if(g&&g[3]!==f){f=f||g[3],e=e||[],g=+d||1;do h=h||".5",g/=h,n.style(c.elem,a,g+f);while(h!==(h=c.cur()/d)&&1!==h&&--i)}return e&&(g=c.start=+g||+d||0,c.unit=f,c.end=e[1]?g+(e[1]+1)*e[2]:+e[2]),c}]};function Sa(){return setTimeout(function(){La=void 0}),La=n.now()}function Ta(a,b){var c,d=0,e={height:a};for(b=b?1:0;4>d;d+=2-b)c=R[d],e["margin"+c]=e["padding"+c]=a;return b&&(e.opacity=e.width=a),e}function Ua(a,b,c){for(var d,e=(Ra[b]||[]).concat(Ra["*"]),f=0,g=e.length;g>f;f++)if(d=e[f].call(c,b,a))return d}function Va(a,b,c){var d,e,f,g,h,i,j,k,l=this,m={},o=a.style,p=a.nodeType&&S(a),q=L.get(a,"fxshow");c.queue||(h=n._queueHooks(a,"fx"),null==h.unqueued&&(h.unqueued=0,i=h.empty.fire,h.empty.fire=function(){h.unqueued||i()}),h.unqueued++,l.always(function(){l.always(function(){h.unqueued--,n.queue(a,"fx").length||h.empty.fire()})})),1===a.nodeType&&("height"in b||"width"in b)&&(c.overflow=[o.overflow,o.overflowX,o.overflowY],j=n.css(a,"display"),k="none"===j?L.get(a,"olddisplay")||ta(a.nodeName):j,"inline"===k&&"none"===n.css(a,"float")&&(o.display="inline-block")),c.overflow&&(o.overflow="hidden",l.always(function(){o.overflow=c.overflow[0],o.overflowX=c.overflow[1],o.overflowY=c.overflow[2]}));for(d in b)if(e=b[d],Na.exec(e)){if(delete b[d],f=f||"toggle"===e,e===(p?"hide":"show")){if("show"!==e||!q||void 0===q[d])continue;p=!0}m[d]=q&&q[d]||n.style(a,d)}else j=void 0;if(n.isEmptyObject(m))"inline"===("none"===j?ta(a.nodeName):j)&&(o.display=j);else{q?"hidden"in q&&(p=q.hidden):q=L.access(a,"fxshow",{}),f&&(q.hidden=!p),p?n(a).show():l.done(function(){n(a).hide()}),l.done(function(){var b;L.remove(a,"fxshow");for(b in m)n.style(a,b,m[b])});for(d in m)g=Ua(p?q[d]:0,d,l),d in q||(q[d]=g.start,p&&(g.end=g.start,g.start="width"===d||"height"===d?1:0))}}function Wa(a,b){var c,d,e,f,g;for(c in a)if(d=n.camelCase(c),e=b[d],f=a[c],n.isArray(f)&&(e=f[1],f=a[c]=f[0]),c!==d&&(a[d]=f,delete a[c]),g=n.cssHooks[d],g&&"expand"in g){f=g.expand(f),delete a[d];for(c in f)c in a||(a[c]=f[c],b[c]=e)}else b[d]=e}function Xa(a,b,c){var d,e,f=0,g=Qa.length,h=n.Deferred().always(function(){delete i.elem}),i=function(){if(e)return!1;for(var b=La||Sa(),c=Math.max(0,j.startTime+j.duration-b),d=c/j.duration||0,f=1-d,g=0,i=j.tweens.length;i>g;g++)j.tweens[g].run(f);return h.notifyWith(a,[j,f,c]),1>f&&i?c:(h.resolveWith(a,[j]),!1)},j=h.promise({elem:a,props:n.extend({},b),opts:n.extend(!0,{specialEasing:{}},c),originalProperties:b,originalOptions:c,startTime:La||Sa(),duration:c.duration,tweens:[],createTween:function(b,c){var d=n.Tween(a,j.opts,b,c,j.opts.specialEasing[b]||j.opts.easing);return j.tweens.push(d),d},stop:function(b){var c=0,d=b?j.tweens.length:0;if(e)return this;for(e=!0;d>c;c++)j.tweens[c].run(1);return b?h.resolveWith(a,[j,b]):h.rejectWith(a,[j,b]),this}}),k=j.props;for(Wa(k,j.opts.specialEasing);g>f;f++)if(d=Qa[f].call(j,a,k,j.opts))return d;return n.map(k,Ua,j),n.isFunction(j.opts.start)&&j.opts.start.call(a,j),n.fx.timer(n.extend(i,{elem:a,anim:j,queue:j.opts.queue})),j.progress(j.opts.progress).done(j.opts.done,j.opts.complete).fail(j.opts.fail).always(j.opts.always)}n.Animation=n.extend(Xa,{tweener:function(a,b){n.isFunction(a)?(b=a,a=["*"]):a=a.split(" ");for(var c,d=0,e=a.length;e>d;d++)c=a[d],Ra[c]=Ra[c]||[],Ra[c].unshift(b)},prefilter:function(a,b){b?Qa.unshift(a):Qa.push(a)}}),n.speed=function(a,b,c){var d=a&&"object"==typeof a?n.extend({},a):{complete:c||!c&&b||n.isFunction(a)&&a,duration:a,easing:c&&b||b&&!n.isFunction(b)&&b};return d.duration=n.fx.off?0:"number"==typeof d.duration?d.duration:d.duration in n.fx.speeds?n.fx.speeds[d.duration]:n.fx.speeds._default,(null==d.queue||d.queue===!0)&&(d.queue="fx"),d.old=d.complete,d.complete=function(){n.isFunction(d.old)&&d.old.call(this),d.queue&&n.dequeue(this,d.queue)},d},n.fn.extend({fadeTo:function(a,b,c,d){return this.filter(S).css("opacity",0).show().end().animate({opacity:b},a,c,d)},animate:function(a,b,c,d){var e=n.isEmptyObject(a),f=n.speed(b,c,d),g=function(){var b=Xa(this,n.extend({},a),f);(e||L.get(this,"finish"))&&b.stop(!0)};return g.finish=g,e||f.queue===!1?this.each(g):this.queue(f.queue,g)},stop:function(a,b,c){var d=function(a){var b=a.stop;delete a.stop,b(c)};return"string"!=typeof a&&(c=b,b=a,a=void 0),b&&a!==!1&&this.queue(a||"fx",[]),this.each(function(){var b=!0,e=null!=a&&a+"queueHooks",f=n.timers,g=L.get(this);if(e)g[e]&&g[e].stop&&d(g[e]);else for(e in g)g[e]&&g[e].stop&&Pa.test(e)&&d(g[e]);for(e=f.length;e--;)f[e].elem!==this||null!=a&&f[e].queue!==a||(f[e].anim.stop(c),b=!1,f.splice(e,1));(b||!c)&&n.dequeue(this,a)})},finish:function(a){return a!==!1&&(a=a||"fx"),this.each(function(){var b,c=L.get(this),d=c[a+"queue"],e=c[a+"queueHooks"],f=n.timers,g=d?d.length:0;for(c.finish=!0,n.queue(this,a,[]),e&&e.stop&&e.stop.call(this,!0),b=f.length;b--;)f[b].elem===this&&f[b].queue===a&&(f[b].anim.stop(!0),f.splice(b,1));for(b=0;g>b;b++)d[b]&&d[b].finish&&d[b].finish.call(this);delete c.finish})}}),n.each(["toggle","show","hide"],function(a,b){var c=n.fn[b];n.fn[b]=function(a,d,e){return null==a||"boolean"==typeof a?c.apply(this,arguments):this.animate(Ta(b,!0),a,d,e)}}),n.each({slideDown:Ta("show"),slideUp:Ta("hide"),slideToggle:Ta("toggle"),fadeIn:{opacity:"show"},fadeOut:{opacity:"hide"},fadeToggle:{opacity:"toggle"}},function(a,b){n.fn[a]=function(a,c,d){return this.animate(b,a,c,d)}}),n.timers=[],n.fx.tick=function(){var a,b=0,c=n.timers;for(La=n.now();b<c.length;b++)a=c[b],a()||c[b]!==a||c.splice(b--,1);c.length||n.fx.stop(),La=void 0},n.fx.timer=function(a){n.timers.push(a),a()?n.fx.start():n.timers.pop()},n.fx.interval=13,n.fx.start=function(){Ma||(Ma=setInterval(n.fx.tick,n.fx.interval))},n.fx.stop=function(){clearInterval(Ma),Ma=null},n.fx.speeds={slow:600,fast:200,_default:400},n.fn.delay=function(a,b){return a=n.fx?n.fx.speeds[a]||a:a,b=b||"fx",this.queue(b,function(b,c){var d=setTimeout(b,a);c.stop=function(){clearTimeout(d)}})},function(){var a=l.createElement("input"),b=l.createElement("select"),c=b.appendChild(l.createElement("option"));a.type="checkbox",k.checkOn=""!==a.value,k.optSelected=c.selected,b.disabled=!0,k.optDisabled=!c.disabled,a=l.createElement("input"),a.value="t",a.type="radio",k.radioValue="t"===a.value}();var Ya,Za,$a=n.expr.attrHandle;n.fn.extend({attr:function(a,b){return J(this,n.attr,a,b,arguments.length>1)},removeAttr:function(a){return this.each(function(){n.removeAttr(this,a)})}}),n.extend({attr:function(a,b,c){var d,e,f=a.nodeType;if(a&&3!==f&&8!==f&&2!==f)return typeof a.getAttribute===U?n.prop(a,b,c):(1===f&&n.isXMLDoc(a)||(b=b.toLowerCase(),d=n.attrHooks[b]||(n.expr.match.bool.test(b)?Za:Ya)),
void 0===c?d&&"get"in d&&null!==(e=d.get(a,b))?e:(e=n.find.attr(a,b),null==e?void 0:e):null!==c?d&&"set"in d&&void 0!==(e=d.set(a,c,b))?e:(a.setAttribute(b,c+""),c):void n.removeAttr(a,b))},removeAttr:function(a,b){var c,d,e=0,f=b&&b.match(E);if(f&&1===a.nodeType)while(c=f[e++])d=n.propFix[c]||c,n.expr.match.bool.test(c)&&(a[d]=!1),a.removeAttribute(c)},attrHooks:{type:{set:function(a,b){if(!k.radioValue&&"radio"===b&&n.nodeName(a,"input")){var c=a.value;return a.setAttribute("type",b),c&&(a.value=c),b}}}}}),Za={set:function(a,b,c){return b===!1?n.removeAttr(a,c):a.setAttribute(c,c),c}},n.each(n.expr.match.bool.source.match(/\w+/g),function(a,b){var c=$a[b]||n.find.attr;$a[b]=function(a,b,d){var e,f;return d||(f=$a[b],$a[b]=e,e=null!=c(a,b,d)?b.toLowerCase():null,$a[b]=f),e}});var _a=/^(?:input|select|textarea|button)$/i;n.fn.extend({prop:function(a,b){return J(this,n.prop,a,b,arguments.length>1)},removeProp:function(a){return this.each(function(){delete this[n.propFix[a]||a]})}}),n.extend({propFix:{"for":"htmlFor","class":"className"},prop:function(a,b,c){var d,e,f,g=a.nodeType;if(a&&3!==g&&8!==g&&2!==g)return f=1!==g||!n.isXMLDoc(a),f&&(b=n.propFix[b]||b,e=n.propHooks[b]),void 0!==c?e&&"set"in e&&void 0!==(d=e.set(a,c,b))?d:a[b]=c:e&&"get"in e&&null!==(d=e.get(a,b))?d:a[b]},propHooks:{tabIndex:{get:function(a){return a.hasAttribute("tabindex")||_a.test(a.nodeName)||a.href?a.tabIndex:-1}}}}),k.optSelected||(n.propHooks.selected={get:function(a){var b=a.parentNode;return b&&b.parentNode&&b.parentNode.selectedIndex,null}}),n.each(["tabIndex","readOnly","maxLength","cellSpacing","cellPadding","rowSpan","colSpan","useMap","frameBorder","contentEditable"],function(){n.propFix[this.toLowerCase()]=this});var ab=/[\t\r\n\f]/g;n.fn.extend({addClass:function(a){var b,c,d,e,f,g,h="string"==typeof a&&a,i=0,j=this.length;if(n.isFunction(a))return this.each(function(b){n(this).addClass(a.call(this,b,this.className))});if(h)for(b=(a||"").match(E)||[];j>i;i++)if(c=this[i],d=1===c.nodeType&&(c.className?(" "+c.className+" ").replace(ab," "):" ")){f=0;while(e=b[f++])d.indexOf(" "+e+" ")<0&&(d+=e+" ");g=n.trim(d),c.className!==g&&(c.className=g)}return this},removeClass:function(a){var b,c,d,e,f,g,h=0===arguments.length||"string"==typeof a&&a,i=0,j=this.length;if(n.isFunction(a))return this.each(function(b){n(this).removeClass(a.call(this,b,this.className))});if(h)for(b=(a||"").match(E)||[];j>i;i++)if(c=this[i],d=1===c.nodeType&&(c.className?(" "+c.className+" ").replace(ab," "):"")){f=0;while(e=b[f++])while(d.indexOf(" "+e+" ")>=0)d=d.replace(" "+e+" "," ");g=a?n.trim(d):"",c.className!==g&&(c.className=g)}return this},toggleClass:function(a,b){var c=typeof a;return"boolean"==typeof b&&"string"===c?b?this.addClass(a):this.removeClass(a):this.each(n.isFunction(a)?function(c){n(this).toggleClass(a.call(this,c,this.className,b),b)}:function(){if("string"===c){var b,d=0,e=n(this),f=a.match(E)||[];while(b=f[d++])e.hasClass(b)?e.removeClass(b):e.addClass(b)}else(c===U||"boolean"===c)&&(this.className&&L.set(this,"__className__",this.className),this.className=this.className||a===!1?"":L.get(this,"__className__")||"")})},hasClass:function(a){for(var b=" "+a+" ",c=0,d=this.length;d>c;c++)if(1===this[c].nodeType&&(" "+this[c].className+" ").replace(ab," ").indexOf(b)>=0)return!0;return!1}});var bb=/\r/g;n.fn.extend({val:function(a){var b,c,d,e=this[0];{if(arguments.length)return d=n.isFunction(a),this.each(function(c){var e;1===this.nodeType&&(e=d?a.call(this,c,n(this).val()):a,null==e?e="":"number"==typeof e?e+="":n.isArray(e)&&(e=n.map(e,function(a){return null==a?"":a+""})),b=n.valHooks[this.type]||n.valHooks[this.nodeName.toLowerCase()],b&&"set"in b&&void 0!==b.set(this,e,"value")||(this.value=e))});if(e)return b=n.valHooks[e.type]||n.valHooks[e.nodeName.toLowerCase()],b&&"get"in b&&void 0!==(c=b.get(e,"value"))?c:(c=e.value,"string"==typeof c?c.replace(bb,""):null==c?"":c)}}}),n.extend({valHooks:{option:{get:function(a){var b=n.find.attr(a,"value");return null!=b?b:n.trim(n.text(a))}},select:{get:function(a){for(var b,c,d=a.options,e=a.selectedIndex,f="select-one"===a.type||0>e,g=f?null:[],h=f?e+1:d.length,i=0>e?h:f?e:0;h>i;i++)if(c=d[i],!(!c.selected&&i!==e||(k.optDisabled?c.disabled:null!==c.getAttribute("disabled"))||c.parentNode.disabled&&n.nodeName(c.parentNode,"optgroup"))){if(b=n(c).val(),f)return b;g.push(b)}return g},set:function(a,b){var c,d,e=a.options,f=n.makeArray(b),g=e.length;while(g--)d=e[g],(d.selected=n.inArray(d.value,f)>=0)&&(c=!0);return c||(a.selectedIndex=-1),f}}}}),n.each(["radio","checkbox"],function(){n.valHooks[this]={set:function(a,b){return n.isArray(b)?a.checked=n.inArray(n(a).val(),b)>=0:void 0}},k.checkOn||(n.valHooks[this].get=function(a){return null===a.getAttribute("value")?"on":a.value})}),n.each("blur focus focusin focusout load resize scroll unload click dblclick mousedown mouseup mousemove mouseover mouseout mouseenter mouseleave change select submit keydown keypress keyup error contextmenu".split(" "),function(a,b){n.fn[b]=function(a,c){return arguments.length>0?this.on(b,null,a,c):this.trigger(b)}}),n.fn.extend({hover:function(a,b){return this.mouseenter(a).mouseleave(b||a)},bind:function(a,b,c){return this.on(a,null,b,c)},unbind:function(a,b){return this.off(a,null,b)},delegate:function(a,b,c,d){return this.on(b,a,c,d)},undelegate:function(a,b,c){return 1===arguments.length?this.off(a,"**"):this.off(b,a||"**",c)}});var cb=n.now(),db=/\?/;n.parseJSON=function(a){return JSON.parse(a+"")},n.parseXML=function(a){var b,c;if(!a||"string"!=typeof a)return null;try{c=new DOMParser,b=c.parseFromString(a,"text/xml")}catch(d){b=void 0}return(!b||b.getElementsByTagName("parsererror").length)&&n.error("Invalid XML: "+a),b};var eb=/#.*$/,fb=/([?&])_=[^&]*/,gb=/^(.*?):[ \t]*([^\r\n]*)$/gm,hb=/^(?:about|app|app-storage|.+-extension|file|res|widget):$/,ib=/^(?:GET|HEAD)$/,jb=/^\/\//,kb=/^([\w.+-]+:)(?:\/\/(?:[^\/?#]*@|)([^\/?#:]*)(?::(\d+)|)|)/,lb={},mb={},nb="*/".concat("*"),ob=a.location.href,pb=kb.exec(ob.toLowerCase())||[];function qb(a){return function(b,c){"string"!=typeof b&&(c=b,b="*");var d,e=0,f=b.toLowerCase().match(E)||[];if(n.isFunction(c))while(d=f[e++])"+"===d[0]?(d=d.slice(1)||"*",(a[d]=a[d]||[]).unshift(c)):(a[d]=a[d]||[]).push(c)}}function rb(a,b,c,d){var e={},f=a===mb;function g(h){var i;return e[h]=!0,n.each(a[h]||[],function(a,h){var j=h(b,c,d);return"string"!=typeof j||f||e[j]?f?!(i=j):void 0:(b.dataTypes.unshift(j),g(j),!1)}),i}return g(b.dataTypes[0])||!e["*"]&&g("*")}function sb(a,b){var c,d,e=n.ajaxSettings.flatOptions||{};for(c in b)void 0!==b[c]&&((e[c]?a:d||(d={}))[c]=b[c]);return d&&n.extend(!0,a,d),a}function tb(a,b,c){var d,e,f,g,h=a.contents,i=a.dataTypes;while("*"===i[0])i.shift(),void 0===d&&(d=a.mimeType||b.getResponseHeader("Content-Type"));if(d)for(e in h)if(h[e]&&h[e].test(d)){i.unshift(e);break}if(i[0]in c)f=i[0];else{for(e in c){if(!i[0]||a.converters[e+" "+i[0]]){f=e;break}g||(g=e)}f=f||g}return f?(f!==i[0]&&i.unshift(f),c[f]):void 0}function ub(a,b,c,d){var e,f,g,h,i,j={},k=a.dataTypes.slice();if(k[1])for(g in a.converters)j[g.toLowerCase()]=a.converters[g];f=k.shift();while(f)if(a.responseFields[f]&&(c[a.responseFields[f]]=b),!i&&d&&a.dataFilter&&(b=a.dataFilter(b,a.dataType)),i=f,f=k.shift())if("*"===f)f=i;else if("*"!==i&&i!==f){if(g=j[i+" "+f]||j["* "+f],!g)for(e in j)if(h=e.split(" "),h[1]===f&&(g=j[i+" "+h[0]]||j["* "+h[0]])){g===!0?g=j[e]:j[e]!==!0&&(f=h[0],k.unshift(h[1]));break}if(g!==!0)if(g&&a["throws"])b=g(b);else try{b=g(b)}catch(l){return{state:"parsererror",error:g?l:"No conversion from "+i+" to "+f}}}return{state:"success",data:b}}n.extend({active:0,lastModified:{},etag:{},ajaxSettings:{url:ob,type:"GET",isLocal:hb.test(pb[1]),global:!0,processData:!0,async:!0,contentType:"application/x-www-form-urlencoded; charset=UTF-8",accepts:{"*":nb,text:"text/plain",html:"text/html",xml:"application/xml, text/xml",json:"application/json, text/javascript"},contents:{xml:/xml/,html:/html/,json:/json/},responseFields:{xml:"responseXML",text:"responseText",json:"responseJSON"},converters:{"* text":String,"text html":!0,"text json":n.parseJSON,"text xml":n.parseXML},flatOptions:{url:!0,context:!0}},ajaxSetup:function(a,b){return b?sb(sb(a,n.ajaxSettings),b):sb(n.ajaxSettings,a)},ajaxPrefilter:qb(lb),ajaxTransport:qb(mb),ajax:function(a,b){"object"==typeof a&&(b=a,a=void 0),b=b||{};var c,d,e,f,g,h,i,j,k=n.ajaxSetup({},b),l=k.context||k,m=k.context&&(l.nodeType||l.jquery)?n(l):n.event,o=n.Deferred(),p=n.Callbacks("once memory"),q=k.statusCode||{},r={},s={},t=0,u="canceled",v={readyState:0,getResponseHeader:function(a){var b;if(2===t){if(!f){f={};while(b=gb.exec(e))f[b[1].toLowerCase()]=b[2]}b=f[a.toLowerCase()]}return null==b?null:b},getAllResponseHeaders:function(){return 2===t?e:null},setRequestHeader:function(a,b){var c=a.toLowerCase();return t||(a=s[c]=s[c]||a,r[a]=b),this},overrideMimeType:function(a){return t||(k.mimeType=a),this},statusCode:function(a){var b;if(a)if(2>t)for(b in a)q[b]=[q[b],a[b]];else v.always(a[v.status]);return this},abort:function(a){var b=a||u;return c&&c.abort(b),x(0,b),this}};if(o.promise(v).complete=p.add,v.success=v.done,v.error=v.fail,k.url=((a||k.url||ob)+"").replace(eb,"").replace(jb,pb[1]+"//"),k.type=b.method||b.type||k.method||k.type,k.dataTypes=n.trim(k.dataType||"*").toLowerCase().match(E)||[""],null==k.crossDomain&&(h=kb.exec(k.url.toLowerCase()),k.crossDomain=!(!h||h[1]===pb[1]&&h[2]===pb[2]&&(h[3]||("http:"===h[1]?"80":"443"))===(pb[3]||("http:"===pb[1]?"80":"443")))),k.data&&k.processData&&"string"!=typeof k.data&&(k.data=n.param(k.data,k.traditional)),rb(lb,k,b,v),2===t)return v;i=n.event&&k.global,i&&0===n.active++&&n.event.trigger("ajaxStart"),k.type=k.type.toUpperCase(),k.hasContent=!ib.test(k.type),d=k.url,k.hasContent||(k.data&&(d=k.url+=(db.test(d)?"&":"?")+k.data,delete k.data),k.cache===!1&&(k.url=fb.test(d)?d.replace(fb,"$1_="+cb++):d+(db.test(d)?"&":"?")+"_="+cb++)),k.ifModified&&(n.lastModified[d]&&v.setRequestHeader("If-Modified-Since",n.lastModified[d]),n.etag[d]&&v.setRequestHeader("If-None-Match",n.etag[d])),(k.data&&k.hasContent&&k.contentType!==!1||b.contentType)&&v.setRequestHeader("Content-Type",k.contentType),v.setRequestHeader("Accept",k.dataTypes[0]&&k.accepts[k.dataTypes[0]]?k.accepts[k.dataTypes[0]]+("*"!==k.dataTypes[0]?", "+nb+"; q=0.01":""):k.accepts["*"]);for(j in k.headers)v.setRequestHeader(j,k.headers[j]);if(k.beforeSend&&(k.beforeSend.call(l,v,k)===!1||2===t))return v.abort();u="abort";for(j in{success:1,error:1,complete:1})v[j](k[j]);if(c=rb(mb,k,b,v)){v.readyState=1,i&&m.trigger("ajaxSend",[v,k]),k.async&&k.timeout>0&&(g=setTimeout(function(){v.abort("timeout")},k.timeout));try{t=1,c.send(r,x)}catch(w){if(!(2>t))throw w;x(-1,w)}}else x(-1,"No Transport");function x(a,b,f,h){var j,r,s,u,w,x=b;2!==t&&(t=2,g&&clearTimeout(g),c=void 0,e=h||"",v.readyState=a>0?4:0,j=a>=200&&300>a||304===a,f&&(u=tb(k,v,f)),u=ub(k,u,v,j),j?(k.ifModified&&(w=v.getResponseHeader("Last-Modified"),w&&(n.lastModified[d]=w),w=v.getResponseHeader("etag"),w&&(n.etag[d]=w)),204===a||"HEAD"===k.type?x="nocontent":304===a?x="notmodified":(x=u.state,r=u.data,s=u.error,j=!s)):(s=x,(a||!x)&&(x="error",0>a&&(a=0))),v.status=a,v.statusText=(b||x)+"",j?o.resolveWith(l,[r,x,v]):o.rejectWith(l,[v,x,s]),v.statusCode(q),q=void 0,i&&m.trigger(j?"ajaxSuccess":"ajaxError",[v,k,j?r:s]),p.fireWith(l,[v,x]),i&&(m.trigger("ajaxComplete",[v,k]),--n.active||n.event.trigger("ajaxStop")))}return v},getJSON:function(a,b,c){return n.get(a,b,c,"json")},getScript:function(a,b){return n.get(a,void 0,b,"script")}}),n.each(["get","post"],function(a,b){n[b]=function(a,c,d,e){return n.isFunction(c)&&(e=e||d,d=c,c=void 0),n.ajax({url:a,type:b,dataType:e,data:c,success:d})}}),n._evalUrl=function(a){return n.ajax({url:a,type:"GET",dataType:"script",async:!1,global:!1,"throws":!0})},n.fn.extend({wrapAll:function(a){var b;return n.isFunction(a)?this.each(function(b){n(this).wrapAll(a.call(this,b))}):(this[0]&&(b=n(a,this[0].ownerDocument).eq(0).clone(!0),this[0].parentNode&&b.insertBefore(this[0]),b.map(function(){var a=this;while(a.firstElementChild)a=a.firstElementChild;return a}).append(this)),this)},wrapInner:function(a){return this.each(n.isFunction(a)?function(b){n(this).wrapInner(a.call(this,b))}:function(){var b=n(this),c=b.contents();c.length?c.wrapAll(a):b.append(a)})},wrap:function(a){var b=n.isFunction(a);return this.each(function(c){n(this).wrapAll(b?a.call(this,c):a)})},unwrap:function(){return this.parent().each(function(){n.nodeName(this,"body")||n(this).replaceWith(this.childNodes)}).end()}}),n.expr.filters.hidden=function(a){return a.offsetWidth<=0&&a.offsetHeight<=0},n.expr.filters.visible=function(a){return!n.expr.filters.hidden(a)};var vb=/%20/g,wb=/\[\]$/,xb=/\r?\n/g,yb=/^(?:submit|button|image|reset|file)$/i,zb=/^(?:input|select|textarea|keygen)/i;function Ab(a,b,c,d){var e;if(n.isArray(b))n.each(b,function(b,e){c||wb.test(a)?d(a,e):Ab(a+"["+("object"==typeof e?b:"")+"]",e,c,d)});else if(c||"object"!==n.type(b))d(a,b);else for(e in b)Ab(a+"["+e+"]",b[e],c,d)}n.param=function(a,b){var c,d=[],e=function(a,b){b=n.isFunction(b)?b():null==b?"":b,d[d.length]=encodeURIComponent(a)+"="+encodeURIComponent(b)};if(void 0===b&&(b=n.ajaxSettings&&n.ajaxSettings.traditional),n.isArray(a)||a.jquery&&!n.isPlainObject(a))n.each(a,function(){e(this.name,this.value)});else for(c in a)Ab(c,a[c],b,e);return d.join("&").replace(vb,"+")},n.fn.extend({serialize:function(){return n.param(this.serializeArray())},serializeArray:function(){return this.map(function(){var a=n.prop(this,"elements");return a?n.makeArray(a):this}).filter(function(){var a=this.type;return this.name&&!n(this).is(":disabled")&&zb.test(this.nodeName)&&!yb.test(a)&&(this.checked||!T.test(a))}).map(function(a,b){var c=n(this).val();return null==c?null:n.isArray(c)?n.map(c,function(a){return{name:b.name,value:a.replace(xb,"\r\n")}}):{name:b.name,value:c.replace(xb,"\r\n")}}).get()}}),n.ajaxSettings.xhr=function(){try{return new XMLHttpRequest}catch(a){}};var Bb=0,Cb={},Db={0:200,1223:204},Eb=n.ajaxSettings.xhr();a.attachEvent&&a.attachEvent("onunload",function(){for(var a in Cb)Cb[a]()}),k.cors=!!Eb&&"withCredentials"in Eb,k.ajax=Eb=!!Eb,n.ajaxTransport(function(a){var b;return k.cors||Eb&&!a.crossDomain?{send:function(c,d){var e,f=a.xhr(),g=++Bb;if(f.open(a.type,a.url,a.async,a.username,a.password),a.xhrFields)for(e in a.xhrFields)f[e]=a.xhrFields[e];a.mimeType&&f.overrideMimeType&&f.overrideMimeType(a.mimeType),a.crossDomain||c["X-Requested-With"]||(c["X-Requested-With"]="XMLHttpRequest");for(e in c)f.setRequestHeader(e,c[e]);b=function(a){return function(){b&&(delete Cb[g],b=f.onload=f.onerror=null,"abort"===a?f.abort():"error"===a?d(f.status,f.statusText):d(Db[f.status]||f.status,f.statusText,"string"==typeof f.responseText?{text:f.responseText}:void 0,f.getAllResponseHeaders()))}},f.onload=b(),f.onerror=b("error"),b=Cb[g]=b("abort");try{f.send(a.hasContent&&a.data||null)}catch(h){if(b)throw h}},abort:function(){b&&b()}}:void 0}),n.ajaxSetup({accepts:{script:"text/javascript, application/javascript, application/ecmascript, application/x-ecmascript"},contents:{script:/(?:java|ecma)script/},converters:{"text script":function(a){return n.globalEval(a),a}}}),n.ajaxPrefilter("script",function(a){void 0===a.cache&&(a.cache=!1),a.crossDomain&&(a.type="GET")}),n.ajaxTransport("script",function(a){if(a.crossDomain){var b,c;return{send:function(d,e){b=n("<script>").prop({async:!0,charset:a.scriptCharset,src:a.url}).on("load error",c=function(a){b.remove(),c=null,a&&e("error"===a.type?404:200,a.type)}),l.head.appendChild(b[0])},abort:function(){c&&c()}}}});var Fb=[],Gb=/(=)\?(?=&|$)|\?\?/;n.ajaxSetup({jsonp:"callback",jsonpCallback:function(){var a=Fb.pop()||n.expando+"_"+cb++;return this[a]=!0,a}}),n.ajaxPrefilter("json jsonp",function(b,c,d){var e,f,g,h=b.jsonp!==!1&&(Gb.test(b.url)?"url":"string"==typeof b.data&&!(b.contentType||"").indexOf("application/x-www-form-urlencoded")&&Gb.test(b.data)&&"data");return h||"jsonp"===b.dataTypes[0]?(e=b.jsonpCallback=n.isFunction(b.jsonpCallback)?b.jsonpCallback():b.jsonpCallback,h?b[h]=b[h].replace(Gb,"$1"+e):b.jsonp!==!1&&(b.url+=(db.test(b.url)?"&":"?")+b.jsonp+"="+e),b.converters["script json"]=function(){return g||n.error(e+" was not called"),g[0]},b.dataTypes[0]="json",f=a[e],a[e]=function(){g=arguments},d.always(function(){a[e]=f,b[e]&&(b.jsonpCallback=c.jsonpCallback,Fb.push(e)),g&&n.isFunction(f)&&f(g[0]),g=f=void 0}),"script"):void 0}),n.parseHTML=function(a,b,c){if(!a||"string"!=typeof a)return null;"boolean"==typeof b&&(c=b,b=!1),b=b||l;var d=v.exec(a),e=!c&&[];return d?[b.createElement(d[1])]:(d=n.buildFragment([a],b,e),e&&e.length&&n(e).remove(),n.merge([],d.childNodes))};var Hb=n.fn.load;n.fn.load=function(a,b,c){if("string"!=typeof a&&Hb)return Hb.apply(this,arguments);var d,e,f,g=this,h=a.indexOf(" ");return h>=0&&(d=n.trim(a.slice(h)),a=a.slice(0,h)),n.isFunction(b)?(c=b,b=void 0):b&&"object"==typeof b&&(e="POST"),g.length>0&&n.ajax({url:a,type:e,dataType:"html",data:b}).done(function(a){f=arguments,g.html(d?n("<div>").append(n.parseHTML(a)).find(d):a)}).complete(c&&function(a,b){g.each(c,f||[a.responseText,b,a])}),this},n.each(["ajaxStart","ajaxStop","ajaxComplete","ajaxError","ajaxSuccess","ajaxSend"],function(a,b){n.fn[b]=function(a){return this.on(b,a)}}),n.expr.filters.animated=function(a){return n.grep(n.timers,function(b){return a===b.elem}).length};var Ib=a.document.documentElement;function Jb(a){return n.isWindow(a)?a:9===a.nodeType&&a.defaultView}n.offset={setOffset:function(a,b,c){var d,e,f,g,h,i,j,k=n.css(a,"position"),l=n(a),m={};"static"===k&&(a.style.position="relative"),h=l.offset(),f=n.css(a,"top"),i=n.css(a,"left"),j=("absolute"===k||"fixed"===k)&&(f+i).indexOf("auto")>-1,j?(d=l.position(),g=d.top,e=d.left):(g=parseFloat(f)||0,e=parseFloat(i)||0),n.isFunction(b)&&(b=b.call(a,c,h)),null!=b.top&&(m.top=b.top-h.top+g),null!=b.left&&(m.left=b.left-h.left+e),"using"in b?b.using.call(a,m):l.css(m)}},n.fn.extend({offset:function(a){if(arguments.length)return void 0===a?this:this.each(function(b){n.offset.setOffset(this,a,b)});var b,c,d=this[0],e={top:0,left:0},f=d&&d.ownerDocument;if(f)return b=f.documentElement,n.contains(b,d)?(typeof d.getBoundingClientRect!==U&&(e=d.getBoundingClientRect()),c=Jb(f),{top:e.top+c.pageYOffset-b.clientTop,left:e.left+c.pageXOffset-b.clientLeft}):e},position:function(){if(this[0]){var a,b,c=this[0],d={top:0,left:0};return"fixed"===n.css(c,"position")?b=c.getBoundingClientRect():(a=this.offsetParent(),b=this.offset(),n.nodeName(a[0],"html")||(d=a.offset()),d.top+=n.css(a[0],"borderTopWidth",!0),d.left+=n.css(a[0],"borderLeftWidth",!0)),{top:b.top-d.top-n.css(c,"marginTop",!0),left:b.left-d.left-n.css(c,"marginLeft",!0)}}},offsetParent:function(){return this.map(function(){var a=this.offsetParent||Ib;while(a&&!n.nodeName(a,"html")&&"static"===n.css(a,"position"))a=a.offsetParent;return a||Ib})}}),n.each({scrollLeft:"pageXOffset",scrollTop:"pageYOffset"},function(b,c){var d="pageYOffset"===c;n.fn[b]=function(e){return J(this,function(b,e,f){var g=Jb(b);return void 0===f?g?g[c]:b[e]:void(g?g.scrollTo(d?a.pageXOffset:f,d?f:a.pageYOffset):b[e]=f)},b,e,arguments.length,null)}}),n.each(["top","left"],function(a,b){n.cssHooks[b]=ya(k.pixelPosition,function(a,c){return c?(c=xa(a,b),va.test(c)?n(a).position()[b]+"px":c):void 0})}),n.each({Height:"height",Width:"width"},function(a,b){n.each({padding:"inner"+a,content:b,"":"outer"+a},function(c,d){n.fn[d]=function(d,e){var f=arguments.length&&(c||"boolean"!=typeof d),g=c||(d===!0||e===!0?"margin":"border");return J(this,function(b,c,d){var e;return n.isWindow(b)?b.document.documentElement["client"+a]:9===b.nodeType?(e=b.documentElement,Math.max(b.body["scroll"+a],e["scroll"+a],b.body["offset"+a],e["offset"+a],e["client"+a])):void 0===d?n.css(b,c,g):n.style(b,c,d,g)},b,f?d:void 0,f,null)}})}),n.fn.size=function(){return this.length},n.fn.andSelf=n.fn.addBack,"function"==typeof define&&define.amd&&define("jquery",[],function(){return n});var Kb=a.jQuery,Lb=a.$;return n.noConflict=function(b){return a.$===n&&(a.$=Lb),b&&a.jQuery===n&&(a.jQuery=Kb),n},typeof b===U&&(a.jQuery=a.$=n),n});

! function(e, t) {
    function n(e) {
        var t, n, r = M[e] = {};
        for (e = e.split(/\s+/), t = 0, n = e.length; n > t; t++) r[e[t]] = !
            0;
        return r
    }

    function r(e, n, r) {
        if (r === t && 1 === e.nodeType) {
            var i = "data-" + n.replace(q, "-$1")
                .toLowerCase();
            if (r = e.getAttribute(i), "string" == typeof r) {
                try {
                    r = "true" === r ? !0 : "false" === r ? !1 : "null" ===
                        r ? null : $.isNumeric(r) ? parseFloat(r) : B.test(
                            r) ? $.parseJSON(r) : r
                } catch (o) {}
                $.data(e, n, r)
            } else r = t
        }
        return r
    }

    function i(e) {
        for (var t in e)
            if (("data" !== t || !$.isEmptyObject(e[t])) && "toJSON" !== t)
                return !1;
        return !0
    }

    function o(e, t, n) {
        var r = t + "defer",
            i = t + "queue",
            o = t + "mark",
            a = $._data(e, r);
        !a || "queue" !== n && $._data(e, i) || "mark" !== n && $._data(e,
            o) || setTimeout(function() {
            $._data(e, i) || $._data(e, o) || ($.removeData(e, r, !
                0), a.fire())
        }, 0)
    }

    function a() {
        return !1
    }

    function s() {
        return !0
    }

    function l(e) {
        return !e || !e.parentNode || 11 === e.parentNode.nodeType
    }

    function u(e, t, n) {
        if (t = t || 0, $.isFunction(t)) return $.grep(e, function(e, r) {
            var i = !!t.call(e, r, e);
            return i === n
        });
        if (t.nodeType) return $.grep(e, function(e) {
            return e === t === n
        });
        if ("string" == typeof t) {
            var r = $.grep(e, function(e) {
                return 1 === e.nodeType
            });
            if (ft.test(t)) return $.filter(t, r, !n);
            t = $.filter(t, r)
        }
        return $.grep(e, function(e) {
            return $.inArray(e, t) >= 0 === n
        })
    }

    function c(e) {
        var t = gt.split("|"),
            n = e.createDocumentFragment();
        if (n.createElement)
            for (; t.length;) n.createElement(t.pop());
        return n
    }

    function f(e) {
        return $.nodeName(e, "table") ? e.getElementsByTagName("tbody")[0] ||
            e.appendChild(e.ownerDocument.createElement("tbody")) : e
    }

    function d(e, t) {
        if (1 === t.nodeType && $.hasData(e)) {
            var n, r, i, o = $._data(e),
                a = $._data(t, o),
                s = o.events;
            if (s) {
                delete a.handle, a.events = {};
                for (n in s)
                    for (r = 0, i = s[n].length; i > r; r++) $.event.add(t,
                        n + (s[n][r].namespace ? "." : "") + s[n][r].namespace,
                        s[n][r], s[n][r].data)
            }
            a.data && (a.data = $.extend({}, a.data))
        }
    }

    function p(e, t) {
        var n;
        1 === t.nodeType && (t.clearAttributes && t.clearAttributes(), t.mergeAttributes &&
            t.mergeAttributes(e), n = t.nodeName.toLowerCase(),
            "object" === n ? t.outerHTML = e.outerHTML : "input" !== n ||
            "checkbox" !== e.type && "radio" !== e.type ? "option" ===
            n ? t.selected = e.defaultSelected : ("input" === n ||
                "textarea" === n) && (t.defaultValue = e.defaultValue) :
            (e.checked && (t.defaultChecked = t.checked = e.checked), t
                .value !== e.value && (t.value = e.value)), t.removeAttribute(
                $.expando))
    }

    function h(e) {
        return "undefined" != typeof e.getElementsByTagName ? e.getElementsByTagName(
            "*") : "undefined" != typeof e.querySelectorAll ? e.querySelectorAll(
            "*") : []
    }

    function g(e) {
        ("checkbox" === e.type || "radio" === e.type) && (e.defaultChecked =
            e.checked)
    }

    function m(e) {
        var t = (e.nodeName || "")
            .toLowerCase();
        "input" === t ? g(e) : "script" !== t && "undefined" != typeof e.getElementsByTagName &&
            $.grep(e.getElementsByTagName("input"), g)
    }

    function y(e) {
        var t = D.createElement("div");
        return jt.appendChild(t), t.innerHTML = e.outerHTML, t.firstChild
    }

    function v(e, t) {
        t.src ? $.ajax({
            url: t.src,
            async: !1,
            dataType: "script"
        }) : $.globalEval((t.text || t.textContent || t.innerHTML || "")
            .replace(Et, "/*$0*/")), t.parentNode && t.parentNode.removeChild(
            t)
    }

    function b(e, t, n) {
        var r = "width" === t ? e.offsetWidth : e.offsetHeight,
            i = "width" === t ? qt : It,
            o = 0,
            a = i.length;
        if (r > 0) {
            if ("border" !== n)
                for (; a > o; o++) n || (r -= parseFloat($.css(e, "padding" +
                    i[o])) || 0), "margin" === n ? r += parseFloat($.css(
                    e, n + i[o])) || 0 : r -= parseFloat($.css(e,
                    "border" + i[o] + "Width")) || 0;
            return r + "px"
        }
        if (r = Lt(e, t, t), (0 > r || null == r) && (r = e.style[t] || 0),
            r = parseFloat(r) || 0, n)
            for (; a > o; o++) r += parseFloat($.css(e, "padding" + i[o])) ||
                0, "padding" !== n && (r += parseFloat($.css(e, "border" +
                    i[o] + "Width")) || 0), "margin" === n && (r +=
                    parseFloat($.css(e, n + i[o])) || 0);
        return r + "px"
    }

    function x(e) {
        return function(t, n) {
            if ("string" != typeof t && (n = t, t = "*"), $.isFunction(
                n))
                for (var r, i, o, a = t.toLowerCase()
                    .split(nn), s = 0, l = a.length; l > s; s++) r = a[
                    s], o = /^\+/.test(r), o && (r = r.substr(1) ||
                    "*"), i = e[r] = e[r] || [], i[o ? "unshift" :
                    "push"](n)
        }
    }

    function w(e, n, r, i, o, a) {
        o = o || n.dataTypes[0], a = a || {}, a[o] = !0;
        for (var s, l = e[o], u = 0, c = l ? l.length : 0, f = e === sn; c >
            u && (f || !s); u++) s = l[u](n, r, i), "string" == typeof s &&
            (!f || a[s] ? s = t : (n.dataTypes.unshift(s), s = w(e, n, r, i,
                s, a)));
        return !f && s || a["*"] || (s = w(e, n, r, i, "*", a)), s
    }

    function T(e, n) {
        var r, i, o = $.ajaxSettings.flatOptions || {};
        for (r in n) n[r] !== t && ((o[r] ? e : i || (i = {}))[r] = n[r]);
        i && $.extend(!0, e, i)
    }

    function N(e, t, n, r) {
        if ($.isArray(t)) $.each(t, function(t, i) {
            n || zt.test(e) ? r(e, i) : N(e + "[" + ("object" ==
                    typeof i || $.isArray(i) ? t : "") + "]", i,
                n, r)
        });
        else if (n || null == t || "object" != typeof t) r(e, t);
        else
            for (var i in t) N(e + "[" + i + "]", t[i], n, r)
    }

    function C(e, n, r) {
        var i, o, a, s, l = e.contents,
            u = e.dataTypes,
            c = e.responseFields;
        for (o in c) o in r && (n[c[o]] = r[o]);
        for (;
            "*" === u[0];) u.shift(), i === t && (i = e.mimeType || n.getResponseHeader(
            "content-type"));
        if (i)
            for (o in l)
                if (l[o] && l[o].test(i)) {
                    u.unshift(o);
                    break
                }
        if (u[0] in r) a = u[0];
        else {
            for (o in r) {
                if (!u[0] || e.converters[o + " " + u[0]]) {
                    a = o;
                    break
                }
                s || (s = o)
            }
            a = a || s
        }
        return a ? (a !== u[0] && u.unshift(a), r[a]) : void 0
    }

    function S(e, n) {
        e.dataFilter && (n = e.dataFilter(n, e.dataType));
        var r, i, o, a, s, l, u, c, f = e.dataTypes,
            d = {},
            p = f.length,
            h = f[0];
        for (r = 1; p > r; r++) {
            if (1 === r)
                for (i in e.converters) "string" == typeof i && (d[i.toLowerCase()] =
                    e.converters[i]);
            if (a = h, h = f[r], "*" === h) h = a;
            else if ("*" !== a && a !== h) {
                if (s = a + " " + h, l = d[s] || d["* " + h], !l) {
                    c = t;
                    for (u in d)
                        if (o = u.split(" "), (o[0] === a || "*" === o[0]) &&
                            (c = d[o[1] + " " + h])) {
                            u = d[u], u === !0 ? l = c : c === !0 && (l = u);
                            break
                        }
                }
                l || c || $.error("No conversion from " + s.replace(" ",
                    " to ")), l !== !0 && (n = l ? l(n) : c(u(n)))
            }
        }
        return n
    }

    function k() {
        try {
            return new e.XMLHttpRequest
        } catch (t) {}
    }

    function E() {
        try {
            return new e.ActiveXObject("Microsoft.XMLHTTP")
        } catch (t) {}
    }

    function A() {
        return setTimeout(j, 0), bn = $.now()
    }

    function j() {
        bn = t
    }

    function L(e, t) {
        var n = {};
        return $.each(Nn.concat.apply([], Nn.slice(0, t)), function() {
            n[this] = e
        }), n
    }

    function F(e) {
        if (!xn[e]) {
            var t = D.body,
                n = $("<" + e + ">")
                .appendTo(t),
                r = n.css("display");
            n.remove(), ("none" === r || "" === r) && (mn || (mn = D.createElement(
                        "iframe"), mn.frameBorder = mn.width = mn.height =
                    0), t.appendChild(mn), yn && mn.createElement || (
                    yn = (mn.contentWindow || mn.contentDocument)
                    .document, yn.write(("CSS1Compat" === D.compatMode ?
                        "<!doctype html>" : "") + "<html><body>"), yn.close()
                ), n = yn.createElement(e), yn.body.appendChild(n), r =
                $.css(n, "display"), t.removeChild(mn)), xn[e] = r
        }
        return xn[e]
    }

    function _(e) {
        return $.isWindow(e) ? e : 9 === e.nodeType ? e.defaultView || e.parentWindow :
            !1
    }
    var D = e.document,
        O = e.navigator,
        P = e.location,
        $ = function() {
            function n() {
                if (!s.isReady) {
                    try {
                        D.documentElement.doScroll("left")
                    } catch (e) {
                        return void setTimeout(n, 1)
                    }
                    s.ready()
                }
            }
            var r, i, o, a, s = function(e, t) {
                    return new s.fn.init(e, t, r)
                },
                l = e.jQuery,
                u = e.$,
                c = /^(?:[^#<]*(<[\w\W]+>)[^>]*$|#([\w\-]*)$)/,
                f = /\S/,
                d = /^\s+/,
                p = /\s+$/,
                h = /^<(\w+)\s*\/?>(?:<\/\1>)?$/,
                g = /^[\],:{}\s]*$/,
                m = /\\(?:["\\\/bfnrt]|u[0-9a-fA-F]{4})/g,
                y =
                /"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g,
                v = /(?:^|:|,)(?:\s*\[)+/g,
                b = /(webkit)[ \/]([\w.]+)/,
                x = /(opera)(?:.*version)?[ \/]([\w.]+)/,
                w = /(msie) ([\w.]+)/,
                T = /(mozilla)(?:.*? rv:([\w.]+))?/,
                N = /-([a-z]|[0-9])/gi,
                C = /^-ms-/,
                S = function(e, t) {
                    return (t + "")
                        .toUpperCase()
                },
                k = O.userAgent,
                E = Object.prototype.toString,
                A = Object.prototype.hasOwnProperty,
                j = Array.prototype.push,
                L = Array.prototype.slice,
                F = String.prototype.trim,
                _ = Array.prototype.indexOf,
                P = {};
            return s.fn = s.prototype = {
                    constructor: s,
                    init: function(e, n, r) {
                        var i, o, a, l;
                        if (!e) return this;
                        if (e.nodeType) return this.context = this[0] = e,
                            this.length = 1, this;
                        if ("body" === e && !n && D.body) return this.context =
                            D, this[0] = D.body, this.selector = e,
                            this.length = 1, this;
                        if ("string" == typeof e) {
                            if (i = "<" === e.charAt(0) && ">" === e.charAt(
                                e.length - 1) && e.length >= 3 ? [null,
                                e, null
                            ] : c.exec(e), !i || !i[1] && n) return !n || n
                                .jquery ? (n || r)
                                .find(e) : this.constructor(n)
                                .find(e);
                            if (i[1]) return n = n instanceof s ? n[0] : n,
                                l = n ? n.ownerDocument || n : D, a = h
                                .exec(e), a ? s.isPlainObject(n) ? (e = [
                                    D.createElement(a[1])
                                ], s.fn.attr.call(e, n, !0)) : e = [l.createElement(
                                    a[1])] : (a = s.buildFragment([i[1]], [
                                        l
                                    ]), e = (a.cacheable ? s.clone(a.fragment) :
                                        a.fragment)
                                    .childNodes), s.merge(this, e);
                            if (o = D.getElementById(i[2]), o && o.parentNode) {
                                if (o.id !== i[2]) return r.find(e);
                                this.length = 1, this[0] = o
                            }
                            return this.context = D, this.selector = e,
                                this
                        }
                        return s.isFunction(e) ? r.ready(e) : (e.selector !==
                            t && (this.selector = e.selector, this.context =
                                e.context), s.makeArray(e, this))
                    },
                    selector: "",
                    jquery: "1.7.1",
                    length: 0,
                    size: function() {
                        return this.length
                    },
                    toArray: function() {
                        return L.call(this, 0)
                    },
                    get: function(e) {
                        return null == e ? this.toArray() : 0 > e ? this[
                            this.length + e] : this[e]
                    },
                    pushStack: function(e, t, n) {
                        var r = this.constructor();
                        return s.isArray(e) ? j.apply(r, e) : s.merge(r, e),
                            r.prevObject = this, r.context = this.context,
                            "find" === t ? r.selector = this.selector + (
                                this.selector ? " " : "") + n : t && (r.selector =
                                this.selector + "." + t + "(" + n + ")"), r
                    },
                    each: function(e, t) {
                        return s.each(this, e, t)
                    },
                    ready: function(e) {
                        return s.bindReady(), o.add(e), this
                    },
                    eq: function(e) {
                        return e = +e, -1 === e ? this.slice(e) : this.slice(
                            e, e + 1)
                    },
                    first: function() {
                        return this.eq(0)
                    },
                    last: function() {
                        return this.eq(-1)
                    },
                    slice: function() {
                        return this.pushStack(L.apply(this, arguments),
                            "slice", L.call(arguments)
                            .join(","))
                    },
                    map: function(e) {
                        return this.pushStack(s.map(this, function(t, n) {
                            return e.call(t, n, t)
                        }))
                    },
                    end: function() {
                        return this.prevObject || this.constructor(null)
                    },
                    push: j,
                    sort: [].sort,
                    splice: [].splice
                }, s.fn.init.prototype = s.fn, s.extend = s.fn.extend =
                function() {
                    var e, n, r, i, o, a, l = arguments[0] || {},
                        u = 1,
                        c = arguments.length,
                        f = !1;
                    for ("boolean" == typeof l && (f = l, l = arguments[1] || {},
                            u = 2), "object" == typeof l || s.isFunction(l) ||
                        (l = {}), c === u && (l = this, --u); c > u; u++)
                        if (null != (e = arguments[u]))
                            for (n in e) r = l[n], i = e[n], l !== i && (f && i &&
                                (s.isPlainObject(i) || (o = s.isArray(i))) ?
                                (o ? (o = !1, a = r && s.isArray(r) ? r : []) :
                                    a = r && s.isPlainObject(r) ? r : {}, l[
                                        n] = s.extend(f, a, i)) : i !== t &&
                                (l[n] = i));
                    return l
                }, s.extend({
                    noConflict: function(t) {
                        return e.$ === s && (e.$ = u), t && e.jQuery ===
                            s && (e.jQuery = l), s
                    },
                    isReady: !1,
                    readyWait: 1,
                    holdReady: function(e) {
                        e ? s.readyWait++ : s.ready(!0)
                    },
                    ready: function(e) {
                        if (e === !0 && !--s.readyWait || e !== !0 && !
                            s.isReady) {
                            if (!D.body) return setTimeout(s.ready, 1);
                            if (s.isReady = !0, e !== !0 && --s.readyWait >
                                0) return;
                            o.fireWith(D, [s]), s.fn.trigger && s(D)
                                .trigger("ready")
                                .off("ready")
                        }
                    },
                    bindReady: function() {
                        if (!o) {
                            if (o = s.Callbacks("once memory"),
                                "complete" === D.readyState) return;
                            if (setTimeout(s.ready, 1), D.addEventListener)
                                D.addEventListener("DOMContentLoaded",
                                    a, !1), e.addEventListener("load",
                                    s.ready, !1);
                            else if (D.attachEvent) {
                                D.attachEvent("onreadystatechange", a),
                                    e.attachEvent("onload", s.ready);
                                var t = !1;
                                try {
                                    t = null == e.frameElement
                                } catch (r) {}
                                D.documentElement.doScroll && t && n()
                            }
                        }
                    },
                    isFunction: function(e) {
                        return "function" === s.type(e)
                    },
                    isArray: Array.isArray || function(e) {
                        return "array" === s.type(e)
                    },
                    isWindow: function(e) {
                        return e && "object" == typeof e &&
                            "setInterval" in e
                    },
                    isNumeric: function(e) {
                        return !isNaN(parseFloat(e)) && isFinite(e)
                    },
                    type: function(e) {
                        return null == e ? String(e) : P[E.call(e)] ||
                            "object"
                    },
                    isPlainObject: function(e) {
                        if (!e || "object" !== s.type(e) || e.nodeType ||
                            s.isWindow(e)) return !1;
                        try {
                            if (e.constructor && !A.call(e,
                                "constructor") && !A.call(e.constructor
                                .prototype, "isPrototypeOf")) return !1
                        } catch (n) {
                            return !1
                        }
                        var r;
                        for (r in e);
                        return r === t || A.call(e, r)
                    },
                    isEmptyObject: function(e) {
                        for (var t in e) return !1;
                        return !0
                    },
                    error: function(e) {
                        throw new Error(e)
                    },
                    parseJSON: function(t) {
                        return "string" == typeof t && t ? (t = s.trim(
                                t), e.JSON && e.JSON.parse ? e.JSON
                            .parse(t) : g.test(t.replace(m, "@")
                                .replace(y, "]")
                                .replace(v, "")) ? new Function(
                                "return " + t)() : void s.error(
                                "Invalid JSON: " + t)) : null
                    },
                    parseXML: function(n) {
                        var r, i;
                        try {
                            e.DOMParser ? (i = new DOMParser, r = i.parseFromString(
                                n, "text/xml")) : (r = new ActiveXObject(
                                    "Microsoft.XMLDOM"), r.async =
                                "false", r.loadXML(n))
                        } catch (o) {
                            r = t
                        }
                        return r && r.documentElement && !r.getElementsByTagName(
                                "parsererror")
                            .length || s.error("Invalid XML: " + n), r
                    },
                    noop: function() {},
                    globalEval: function(t) {
                        t && f.test(t) && (e.execScript || function(t) {
                            e.eval.call(e, t)
                        })(t)
                    },
                    camelCase: function(e) {
                        return e.replace(C, "ms-")
                            .replace(N, S)
                    },
                    nodeName: function(e, t) {
                        return e.nodeName && e.nodeName.toUpperCase() ===
                            t.toUpperCase()
                    },
                    each: function(e, n, r) {
                        var i, o = 0,
                            a = e.length,
                            l = a === t || s.isFunction(e);
                        if (r)
                            if (l) {
                                for (i in e)
                                    if (n.apply(e[i], r) === !1) break
                            } else
                                for (; a > o && n.apply(e[o++], r) !==
                                    !1;);
                        else if (l) {
                            for (i in e)
                                if (n.call(e[i], i, e[i]) === !1) break
                        } else
                            for (; a > o && n.call(e[o], o, e[o++]) !==
                                !1;);
                        return e
                    },
                    trim: F ? function(e) {
                        return null == e ? "" : F.call(e)
                    } : function(e) {
                        return null == e ? "" : e.toString()
                            .replace(d, "")
                            .replace(p, "")
                    },
                    makeArray: function(e, t) {
                        var n = t || [];
                        if (null != e) {
                            var r = s.type(e);
                            null == e.length || "string" === r ||
                                "function" === r || "regexp" === r || s
                                .isWindow(e) ? j.call(n, e) : s.merge(n,
                                    e)
                        }
                        return n
                    },
                    inArray: function(e, t, n) {
                        var r;
                        if (t) {
                            if (_) return _.call(t, e, n);
                            for (r = t.length, n = n ? 0 > n ? Math.max(
                                0, r + n) : n : 0; r > n; n++)
                                if (n in t && t[n] === e) return n
                        }
                        return -1
                    },
                    merge: function(e, n) {
                        var r = e.length,
                            i = 0;
                        if ("number" == typeof n.length)
                            for (var o = n.length; o > i; i++) e[r++] =
                                n[i];
                        else
                            for (; n[i] !== t;) e[r++] = n[i++];
                        return e.length = r, e
                    },
                    grep: function(e, t, n) {
                        var r, i = [];
                        n = !!n;
                        for (var o = 0, a = e.length; a > o; o++) r = !
                            !t(e[o], o), n !== r && i.push(e[o]);
                        return i
                    },
                    map: function(e, n, r) {
                        var i, o, a = [],
                            l = 0,
                            u = e.length,
                            c = e instanceof s || u !== t && "number" ==
                            typeof u && (u > 0 && e[0] && e[u - 1] || 0 ===
                                u || s.isArray(e));
                        if (c)
                            for (; u > l; l++) i = n(e[l], l, r), null !=
                                i && (a[a.length] = i);
                        else
                            for (o in e) i = n(e[o], o, r), null != i &&
                                (a[a.length] = i);
                        return a.concat.apply([], a)
                    },
                    guid: 1,
                    proxy: function(e, n) {
                        if ("string" == typeof n) {
                            var r = e[n];
                            n = e, e = r
                        }
                        if (!s.isFunction(e)) return t;
                        var i = L.call(arguments, 2),
                            o = function() {
                                return e.apply(n, i.concat(L.call(
                                    arguments)))
                            };
                        return o.guid = e.guid = e.guid || o.guid || s.guid++,
                            o
                    },
                    access: function(e, n, r, i, o, a) {
                        var l = e.length;
                        if ("object" == typeof n) {
                            for (var u in n) s.access(e, u, n[u], i, o,
                                r);
                            return e
                        }
                        if (r !== t) {
                            i = !a && i && s.isFunction(r);
                            for (var c = 0; l > c; c++) o(e[c], n, i ?
                                r.call(e[c], c, o(e[c], n)) : r, a);
                            return e
                        }
                        return l ? o(e[0], n) : t
                    },
                    now: function() {
                        return (new Date)
                            .getTime()
                    },
                    uaMatch: function(e) {
                        e = e.toLowerCase();
                        var t = b.exec(e) || x.exec(e) || w.exec(e) ||
                            e.indexOf("compatible") < 0 && T.exec(e) || [];
                        return {
                            browser: t[1] || "",
                            version: t[2] || "0"
                        }
                    },
                    sub: function() {
                        function e(t, n) {
                            return new e.fn.init(t, n)
                        }
                        s.extend(!0, e, this), e.superclass = this, e.fn =
                            e.prototype = this(), e.fn.constructor = e,
                            e.sub = this.sub, e.fn.init = function(n, r) {
                                return r && r instanceof s && !(r instanceof e) &&
                                    (r = e(r)), s.fn.init.call(this, n,
                                        r, t)
                            }, e.fn.init.prototype = e.fn;
                        var t = e(D);
                        return e
                    },
                    browser: {}
                }), s.each(
                    "Boolean Number String Function Array Date RegExp Object".split(
                        " "), function(e, t) {
                        P["[object " + t + "]"] = t.toLowerCase()
                    }), i = s.uaMatch(k), i.browser && (s.browser[i.browser] = !
                    0, s.browser.version = i.version), s.browser.webkit && (s.browser
                    .safari = !0), f.test(" ") && (d = /^[\s\xA0]+/, p =
                    /[\s\xA0]+$/), r = s(D), D.addEventListener ? a = function() {
                    D.removeEventListener("DOMContentLoaded", a, !1), s.ready()
                } : D.attachEvent && (a = function() {
                    "complete" === D.readyState && (D.detachEvent(
                        "onreadystatechange", a), s.ready())
                }), s
        }(),
        M = {};
    $.Callbacks = function(e) {
        e = e ? M[e] || n(e) : {};
        var r, i, o, a, s, l = [],
            u = [],
            c = function(t) {
                var n, r, i, o;
                for (n = 0, r = t.length; r > n; n++) i = t[n], o = $.type(
                    i), "array" === o ? c(i) : "function" === o && (e.unique &&
                    d.has(i) || l.push(i))
            },
            f = function(t, n) {
                for (n = n || [], r = !e.memory || [t, n], i = !0, s = o ||
                    0, o = 0, a = l.length; l && a > s; s++)
                    if (l[s].apply(t, n) === !1 && e.stopOnFalse) {
                        r = !0;
                        break
                    }
                i = !1, l && (e.once ? r === !0 ? d.disable() : l = [] : u &&
                    u.length && (r = u.shift(), d.fireWith(r[0], r[1]))
                )
            },
            d = {
                add: function() {
                    if (l) {
                        var e = l.length;
                        c(arguments), i ? a = l.length : r && r !== !0 &&
                            (o = e, f(r[0], r[1]))
                    }
                    return this
                },
                remove: function() {
                    if (l)
                        for (var t = arguments, n = 0, r = t.length; r >
                            n; n++)
                            for (var o = 0; o < l.length && (t[n] !== l[
                                o] || (i && a >= o && (a--, s >=
                                    o && s--), l.splice(o--, 1), !
                                e.unique)); o++);
                    return this
                },
                has: function(e) {
                    if (l)
                        for (var t = 0, n = l.length; n > t; t++)
                            if (e === l[t]) return !0;
                    return !1
                },
                empty: function() {
                    return l = [], this
                },
                disable: function() {
                    return l = u = r = t, this
                },
                disabled: function() {
                    return !l
                },
                lock: function() {
                    return u = t, r && r !== !0 || d.disable(), this
                },
                locked: function() {
                    return !u
                },
                fireWith: function(t, n) {
                    return u && (i ? e.once || u.push([t, n]) : e.once &&
                        r || f(t, n)), this
                },
                fire: function() {
                    return d.fireWith(this, arguments), this
                },
                fired: function() {
                    return !!r
                }
            };
        return d
    };
    var H = [].slice;
    $.extend({
        Deferred: function(e) {
            var t, n = $.Callbacks("once memory"),
                r = $.Callbacks("once memory"),
                i = $.Callbacks("memory"),
                o = "pending",
                a = {
                    resolve: n,
                    reject: r,
                    notify: i
                },
                s = {
                    done: n.add,
                    fail: r.add,
                    progress: i.add,
                    state: function() {
                        return o
                    },
                    isResolved: n.fired,
                    isRejected: r.fired,
                    then: function(e, t, n) {
                        return l.done(e)
                            .fail(t)
                            .progress(n), this
                    },
                    always: function() {
                        return l.done.apply(l, arguments)
                            .fail.apply(l, arguments), this
                    },
                    pipe: function(e, t, n) {
                        return $.Deferred(function(r) {
                                $.each({
                                    done: [e,
                                        "resolve"
                                    ],
                                    fail: [t,
                                        "reject"
                                    ],
                                    progress: [n,
                                        "notify"
                                    ]
                                }, function(e, t) {
                                    var n, i = t[0],
                                        o = t[1];
                                    l[e]($.isFunction(
                                            i) ?
                                        function() {
                                            n =
                                                i
                                                .apply(
                                                    this,
                                                    arguments
                                                ),
                                                n &&
                                                $
                                                .isFunction(
                                                    n
                                                    .promise
                                                ) ?
                                                n
                                                .promise()
                                                .then(
                                                    r
                                                    .resolve,
                                                    r
                                                    .reject,
                                                    r
                                                    .notify
                                                ) :
                                                r[
                                                    o +
                                                    "With"
                                                ]
                                                (
                                                    this ===
                                                    l ?
                                                    r :
                                                    this, [
                                                        n
                                                    ]
                                                )
                                        } : r[o]
                                    )
                                })
                            })
                            .promise()
                    },
                    promise: function(e) {
                        if (null == e) e = s;
                        else
                            for (var t in s) e[t] = s[t];
                        return e
                    }
                },
                l = s.promise({});
            for (t in a) l[t] = a[t].fire, l[t + "With"] = a[t].fireWith;
            return l.done(function() {
                    o = "resolved"
                }, r.disable, i.lock)
                .fail(function() {
                    o = "rejected"
                }, n.disable, i.lock), e && e.call(l, l), l
        },
        when: function(e) {
            function t(e) {
                return function(t) {
                    r[e] = arguments.length > 1 ? H.call(
                        arguments, 0) : t, --s || l.resolveWith(
                        l, r)
                }
            }

            function n(e) {
                return function(t) {
                    a[e] = arguments.length > 1 ? H.call(
                        arguments, 0) : t, l.notifyWith(
                        u, a)
                }
            }
            var r = H.call(arguments, 0),
                i = 0,
                o = r.length,
                a = new Array(o),
                s = o,
                l = 1 >= o && e && $.isFunction(e.promise) ? e : $.Deferred(),
                u = l.promise();
            if (o > 1) {
                for (; o > i; i++) r[i] && r[i].promise && $.isFunction(
                        r[i].promise) ? r[i].promise()
                    .then(t(i), l.reject, n(i)) : --s;
                s || l.resolveWith(l, r)
            } else l !== e && l.resolveWith(l, o ? [e] : []);
            return u
        }
    }), $.support = function() {
        var t, n, r, i, o, a, s, l, u, c, f, d, p = D.createElement("div");
        if (D.documentElement, p.setAttribute("className", "t"), p.innerHTML =
            "   <link/><table></table><a href='/a' style='top:1px;float:left;opacity:.55;'>a</a><input type='checkbox'/>",
            n = p.getElementsByTagName("*"), r = p.getElementsByTagName("a")[
                0], !n || !n.length || !r) return {};
        i = D.createElement("select"), o = i.appendChild(D.createElement(
                "option")), a = p.getElementsByTagName("input")[0], t = {
                leadingWhitespace: 3 === p.firstChild.nodeType,
                tbody: !p.getElementsByTagName("tbody")
                    .length,
                htmlSerialize: !!p.getElementsByTagName("link")
                    .length,
                style: /top/.test(r.getAttribute("style")),
                hrefNormalized: "/a" === r.getAttribute("href"),
                opacity: /^0.55/.test(r.style.opacity),
                cssFloat: !!r.style.cssFloat,
                checkOn: "on" === a.value,
                optSelected: o.selected,
                getSetAttribute: "t" !== p.className,
                enctype: !!D.createElement("form")
                    .enctype,
                html5Clone: "<:nav></:nav>" !== D.createElement("nav")
                    .cloneNode(!0)
                    .outerHTML,
                submitBubbles: !0,
                changeBubbles: !0,
                focusinBubbles: !1,
                deleteExpando: !0,
                noCloneEvent: !0,
                inlineBlockNeedsLayout: !1,
                shrinkWrapBlocks: !1,
                reliableMarginRight: !0
            }, a.checked = !0, t.noCloneChecked = a.cloneNode(!0)
            .checked, i.disabled = !0, t.optDisabled = !o.disabled;
        try {
            delete p.test
        } catch (h) {
            t.deleteExpando = !1
        }
        if (!p.addEventListener && p.attachEvent && p.fireEvent && (p.attachEvent(
                    "onclick", function() {
                        t.noCloneEvent = !1
                    }), p.cloneNode(!0)
                .fireEvent("onclick")), a = D.createElement("input"), a.value =
            "t", a.setAttribute("type", "radio"), t.radioValue = "t" === a.value,
            a.setAttribute("checked", "checked"), p.appendChild(a), l = D.createDocumentFragment(),
            l.appendChild(p.lastChild), t.checkClone = l.cloneNode(!0)
            .cloneNode(!0)
            .lastChild.checked, t.appendChecked = a.checked, l.removeChild(
                a), l.appendChild(p), p.innerHTML = "", e.getComputedStyle &&
            (s = D.createElement("div"), s.style.width = "0", s.style.marginRight =
                "0", p.style.width = "2px", p.appendChild(s), t.reliableMarginRight =
                0 === (parseInt((e.getComputedStyle(s, null) || {
                        marginRight: 0
                    })
                    .marginRight, 10) || 0)), p.attachEvent)
            for (f in {
                submit: 1,
                change: 1,
                focusin: 1
            }) c = "on" + f, d = c in p, d || (p.setAttribute(c, "return;"),
                d = "function" == typeof p[c]), t[f + "Bubbles"] = d;
        return l.removeChild(p), l = i = o = s = p = a = null, $(function() {
            var e, n, r, i, o, a, s, l, c, f, h = D.getElementsByTagName(
                "body")[0];
            h && (a = 1, s =
                "position:absolute;top:0;left:0;width:1px;height:1px;margin:0;",
                l = "visibility:hidden;border:0;", c =
                "style='" + s +
                "border:5px solid #000;padding:0;'", f =
                "<div " + c + "><div></div></div><table " + c +
                " cellpadding='0' cellspacing='0'><tr><td></td></tr></table>",
                e = D.createElement("div"), e.style.cssText = l +
                "width:0;height:0;position:static;top:0;margin-top:" +
                a + "px", h.insertBefore(e, h.firstChild), p =
                D.createElement("div"), e.appendChild(p), p.innerHTML =
                "<table><tr><td style='padding:0;border:0;display:none'></td><td>t</td></tr></table>",
                u = p.getElementsByTagName("td"), d = 0 === u[0]
                .offsetHeight, u[0].style.display = "", u[1].style
                .display = "none", t.reliableHiddenOffsets = d &&
                0 === u[0].offsetHeight, p.innerHTML = "", p.style
                .width = p.style.paddingLeft = "1px", $.boxModel =
                t.boxModel = 2 === p.offsetWidth, "undefined" !=
                typeof p.style.zoom && (p.style.display =
                    "inline", p.style.zoom = 1, t.inlineBlockNeedsLayout =
                    2 === p.offsetWidth, p.style.display = "",
                    p.innerHTML =
                    "<div style='width:4px;'></div>", t.shrinkWrapBlocks =
                    2 !== p.offsetWidth), p.style.cssText = s +
                l, p.innerHTML = f, n = p.firstChild, r = n.firstChild,
                i = n.nextSibling.firstChild.firstChild, o = {
                    doesNotAddBorder: 5 !== r.offsetTop,
                    doesAddBorderForTableAndCells: 5 === i.offsetTop
                }, r.style.position = "fixed", r.style.top =
                "20px", o.fixedPosition = 20 === r.offsetTop ||
                15 === r.offsetTop, r.style.position = r.style.top =
                "", n.style.overflow = "hidden", n.style.position =
                "relative", o.subtractsBorderForOverflowNotVisible = -
                5 === r.offsetTop, o.doesNotIncludeMarginInBodyOffset =
                h.offsetTop !== a, h.removeChild(e), p = e =
                null, $.extend(t, o))
        }), t
    }();
    var B = /^(?:\{.*\}|\[.*\])$/,
        q = /([A-Z])/g;
    $.extend({
        cache: {},
        uuid: 0,
        expando: "jQuery" + ($.fn.jquery + Math.random())
            .replace(/\D/g, ""),
        noData: {
            embed: !0,
            object: "clsid:D27CDB6E-AE6D-11cf-96B8-444553540000",
            applet: !0
        },
        hasData: function(e) {
            return e = e.nodeType ? $.cache[e[$.expando]] : e[$.expando], !
                !e && !i(e)
        },
        data: function(e, n, r, i) {
            if ($.acceptData(e)) {
                var o, a, s, l = $.expando,
                    u = "string" == typeof n,
                    c = e.nodeType,
                    f = c ? $.cache : e,
                    d = c ? e[l] : e[l] && l,
                    p = "events" === n;
                if (d && f[d] && (p || i || f[d].data) || !u || r !==
                    t) return d || (c ? e[l] = d = ++$.uuid : d = l),
                    f[d] || (f[d] = {}, c || (f[d].toJSON = $.noop)), (
                        "object" == typeof n || "function" ==
                        typeof n) && (i ? f[d] = $.extend(f[d],
                        n) : f[d].data = $.extend(f[d].data,
                        n)), o = a = f[d], i || (a.data || (a.data = {}),
                        a = a.data), r !== t && (a[$.camelCase(
                        n)] = r), p && !a[n] ? o.events : (u ?
                        (s = a[n], null == s && (s = a[$.camelCase(
                            n)])) : s = a, s)
            }
        },
        removeData: function(e, t, n) {
            if ($.acceptData(e)) {
                var r, o, a, s = $.expando,
                    l = e.nodeType,
                    u = l ? $.cache : e,
                    c = l ? e[s] : s;
                if (u[c]) {
                    if (t && (r = n ? u[c] : u[c].data)) {
                        $.isArray(t) || (t in r ? t = [t] : (t = $.camelCase(
                            t), t = t in r ? [t] : t.split(
                            " ")));
                        for (o = 0, a = t.length; a > o; o++) delete r[
                            t[o]];
                        if (!(n ? i : $.isEmptyObject)(r)) return
                    }(n || (delete u[c].data, i(u[c]))) && ($.support
                        .deleteExpando || !u.setInterval ? delete u[
                            c] : u[c] = null, l && ($.support.deleteExpando ?
                            delete e[s] : e.removeAttribute ? e.removeAttribute(
                                s) : e[s] = null))
                }
            }
        },
        _data: function(e, t, n) {
            return $.data(e, t, n, !0)
        },
        acceptData: function(e) {
            if (e.nodeName) {
                var t = $.noData[e.nodeName.toLowerCase()];
                if (t) return !(t === !0 || e.getAttribute(
                    "classid") !== t)
            }
            return !0
        }
    }), $.fn.extend({
        data: function(e, n) {
            var i, o, a, s = null;
            if ("undefined" == typeof e) {
                if (this.length && (s = $.data(this[0]), 1 === this[
                    0].nodeType && !$._data(this[0],
                    "parsedAttrs"))) {
                    o = this[0].attributes;
                    for (var l = 0, u = o.length; u > l; l++) a = o[
                        l].name, 0 === a.indexOf("data-") && (a =
                        $.camelCase(a.substring(5)), r(this[0],
                            a, s[a]));
                    $._data(this[0], "parsedAttrs", !0)
                }
                return s
            }
            return "object" == typeof e ? this.each(function() {
                $.data(this, e)
            }) : (i = e.split("."), i[1] = i[1] ? "." + i[1] :
                "", n === t ? (s = this.triggerHandler(
                        "getData" + i[1] + "!", [i[0]]), s ===
                    t && this.length && (s = $.data(this[0], e),
                        s = r(this[0], e, s)), s === t && i[1] ?
                    this.data(i[0]) : s) : this.each(function() {
                    var t = $(this),
                        r = [i[0], n];
                    t.triggerHandler("setData" + i[1] + "!",
                        r), $.data(this, e, n), t.triggerHandler(
                        "changeData" + i[1] + "!", r)
                }))
        },
        removeData: function(e) {
            return this.each(function() {
                $.removeData(this, e)
            })
        }
    }), $.extend({
        _mark: function(e, t) {
            e && (t = (t || "fx") + "mark", $._data(e, t, ($._data(
                e, t) || 0) + 1))
        },
        _unmark: function(e, t, n) {
            if (e !== !0 && (n = t, t = e, e = !1), t) {
                n = n || "fx";
                var r = n + "mark",
                    i = e ? 0 : ($._data(t, r) || 1) - 1;
                i ? $._data(t, r, i) : ($.removeData(t, r, !0), o(t,
                    n, "mark"))
            }
        },
        queue: function(e, t, n) {
            var r;
            return e ? (t = (t || "fx") + "queue", r = $._data(e, t),
                n && (!r || $.isArray(n) ? r = $._data(e, t, $.makeArray(
                    n)) : r.push(n)), r || []) : void 0
        },
        dequeue: function(e, t) {
            t = t || "fx";
            var n = $.queue(e, t),
                r = n.shift(),
                i = {};
            "inprogress" === r && (r = n.shift()), r && ("fx" === t &&
                n.unshift("inprogress"), $._data(e, t + ".run",
                    i), r.call(e, function() {
                    $.dequeue(e, t)
                }, i)), n.length || ($.removeData(e, t +
                "queue " + t + ".run", !0), o(e, t, "queue"))
        }
    }), $.fn.extend({
        queue: function(e, n) {
            return "string" != typeof e && (n = e, e = "fx"), n ===
                t ? $.queue(this[0], e) : this.each(function() {
                    var t = $.queue(this, e, n);
                    "fx" === e && "inprogress" !== t[0] && $.dequeue(
                        this, e)
                })
        },
        dequeue: function(e) {
            return this.each(function() {
                $.dequeue(this, e)
            })
        },
        delay: function(e, t) {
            return e = $.fx ? $.fx.speeds[e] || e : e, t = t ||
                "fx", this.queue(t, function(t, n) {
                    var r = setTimeout(t, e);
                    n.stop = function() {
                        clearTimeout(r)
                    }
                })
        },
        clearQueue: function(e) {
            return this.queue(e || "fx", [])
        },
        promise: function(e, n) {
            function r() {
                --l || o.resolveWith(a, [a])
            }
            "string" != typeof e && (n = e, e = t), e = e || "fx";
            for (var i, o = $.Deferred(), a = this, s = a.length, l =
                1, u = e + "defer", c = e + "queue", f = e +
                "mark"; s--;)(i = $.data(a[s], u, t, !0) || ($.data(
                a[s], c, t, !0) || $.data(a[s], f, t, !
                0)) && $.data(a[s], u, $.Callbacks(
                "once memory"), !0)) && (l++, i.add(r));
            return r(), o.promise()
        }
    });
    var I, R, W, J = /[\n\t\r]/g,
        z = /\s+/,
        Q = /\r/g,
        X = /^(?:button|input)$/i,
        U = /^(?:button|input|object|select|textarea)$/i,
        G = /^a(?:rea)?$/i,
        V =
        /^(?:autofocus|autoplay|async|checked|controls|defer|disabled|hidden|loop|multiple|open|readonly|required|scoped|selected)$/i,
        Y = $.support.getSetAttribute;
    $.fn.extend({
            attr: function(e, t) {
                return $.access(this, e, t, !0, $.attr)
            },
            removeAttr: function(e) {
                return this.each(function() {
                    $.removeAttr(this, e)
                })
            },
            prop: function(e, t) {
                return $.access(this, e, t, !0, $.prop)
            },
            removeProp: function(e) {
                return e = $.propFix[e] || e, this.each(function() {
                    try {
                        this[e] = t, delete this[e]
                    } catch (n) {}
                })
            },
            addClass: function(e) {
                var t, n, r, i, o, a, s;
                if ($.isFunction(e)) return this.each(function(t) {
                    $(this)
                        .addClass(e.call(this, t, this.className))
                });
                if (e && "string" == typeof e)
                    for (t = e.split(z), n = 0, r = this.length; r > n; n++)
                        if (i = this[n], 1 === i.nodeType)
                            if (i.className || 1 !== t.length) {
                                for (o = " " + i.className + " ", a = 0,
                                    s = t.length; s > a; a++)~o.indexOf(
                                    " " + t[a] + " ") || (o += t[a] +
                                    " ");
                                i.className = $.trim(o)
                            } else i.className = e;
                return this
            },
            removeClass: function(e) {
                var n, r, i, o, a, s, l;
                if ($.isFunction(e)) return this.each(function(t) {
                    $(this)
                        .removeClass(e.call(this, t, this.className))
                });
                if (e && "string" == typeof e || e === t)
                    for (n = (e || "")
                        .split(z), r = 0, i = this.length; i > r; r++)
                        if (o = this[r], 1 === o.nodeType && o.className)
                            if (e) {
                                for (a = (" " + o.className + " ")
                                    .replace(J, " "), s = 0, l = n.length; l >
                                    s; s++) a = a.replace(" " + n[s] +
                                    " ", " ");
                                o.className = $.trim(a)
                            } else o.className = "";
                return this
            },
            toggleClass: function(e, t) {
                var n = typeof e,
                    r = "boolean" == typeof t;
                return this.each($.isFunction(e) ? function(n) {
                    $(this)
                        .toggleClass(e.call(this, n, this.className,
                            t), t)
                } : function() {
                    if ("string" === n)
                        for (var i, o = 0, a = $(this), s = t,
                            l = e.split(z); i = l[o++];) s = r ?
                            s : !a.hasClass(i), a[s ?
                                "addClass" : "removeClass"](i);
                    else("undefined" === n || "boolean" === n) &&
                        (this.className && $._data(this,
                                "__className__", this.className
                            ), this.className = this.className ||
                            e === !1 ? "" : $._data(this,
                                "__className__") || "")
                })
            },
            hasClass: function(e) {
                for (var t = " " + e + " ", n = 0, r = this.length; r >
                    n; n++)
                    if (1 === this[n].nodeType && (" " + this[n].className +
                            " ")
                        .replace(J, " ")
                        .indexOf(t) > -1) return !0;
                return !1
            },
            val: function(e) {
                var n, r, i, o = this[0];
                return arguments.length ? (i = $.isFunction(e), this.each(
                    function(r) {
                        var o, a = $(this);
                        1 === this.nodeType && (o = i ? e.call(
                                this, r, a.val()) : e, null ==
                            o ? o = "" : "number" == typeof o ?
                            o += "" : $.isArray(o) && (o =
                                $.map(o, function(e) {
                                    return null == e ?
                                        "" : e + ""
                                })), n = $.valHooks[this.nodeName
                                .toLowerCase()] || $.valHooks[
                                this.type], n && "set" in n &&
                            n.set(this, o, "value") !== t ||
                            (this.value = o))
                    })) : o ? (n = $.valHooks[o.nodeName.toLowerCase()] ||
                    $.valHooks[o.type], n && "get" in n && (r = n.get(
                        o, "value")) !== t ? r : (r = o.value,
                        "string" == typeof r ? r.replace(Q, "") :
                        null == r ? "" : r)) : void 0
            }
        }), $.extend({
            valHooks: {
                option: {
                    get: function(e) {
                        var t = e.attributes.value;
                        return !t || t.specified ? e.value : e.text
                    }
                },
                select: {
                    get: function(e) {
                        var t, n, r, i, o = e.selectedIndex,
                            a = [],
                            s = e.options,
                            l = "select-one" === e.type;
                        if (0 > o) return null;
                        for (n = l ? o : 0, r = l ? o + 1 : s.length; r >
                            n; n++)
                            if (i = s[n], !(!i.selected || ($.support.optDisabled ?
                                    i.disabled : null !== i.getAttribute(
                                        "disabled")) || i.parentNode
                                .disabled && $.nodeName(i.parentNode,
                                    "optgroup"))) {
                                if (t = $(i)
                                    .val(), l) return t;
                                a.push(t)
                            }
                        return l && !a.length && s.length ? $(s[o])
                            .val() : a
                    },
                    set: function(e, t) {
                        var n = $.makeArray(t);
                        return $(e)
                            .find("option")
                            .each(function() {
                                this.selected = $.inArray($(this)
                                    .val(), n) >= 0
                            }), n.length || (e.selectedIndex = -1), n
                    }
                }
            },
            attrFn: {
                val: !0,
                css: !0,
                html: !0,
                text: !0,
                data: !0,
                width: !0,
                height: !0,
                offset: !0
            },
            attr: function(e, n, r, i) {
                var o, a, s, l = e.nodeType;
                return e && 3 !== l && 8 !== l && 2 !== l ? i && n in $
                    .attrFn ? $(e)[n](r) : "undefined" == typeof e.getAttribute ?
                    $.prop(e, n, r) : (s = 1 !== l || !$.isXMLDoc(e), s &&
                        (n = n.toLowerCase(), a = $.attrHooks[n] || (V.test(
                            n) ? R : I)), r !== t ? null === r ? void $
                        .removeAttr(e, n) : a && "set" in a && s && (o =
                            a.set(e, r, n)) !== t ? o : (e.setAttribute(
                            n, "" + r), r) : a && "get" in a && s &&
                        null !== (o = a.get(e, n)) ? o : (o = e.getAttribute(
                            n), null === o ? t : o)) : void 0
            },
            removeAttr: function(e, t) {
                var n, r, i, o, a = 0;
                if (t && 1 === e.nodeType)
                    for (r = t.toLowerCase()
                        .split(z), o = r.length; o > a; a++) i = r[a],
                        i && (n = $.propFix[i] || i, $.attr(e, i, ""),
                            e.removeAttribute(Y ? i : n), V.test(i) &&
                            n in e && (e[n] = !1))
            },
            attrHooks: {
                type: {
                    set: function(e, t) {
                        if (X.test(e.nodeName) && e.parentNode) $.error(
                            "type property can't be changed");
                        else if (!$.support.radioValue && "radio" === t &&
                            $.nodeName(e, "input")) {
                            var n = e.value;
                            return e.setAttribute("type", t), n && (e.value =
                                n), t
                        }
                    }
                },
                value: {
                    get: function(e, t) {
                        return I && $.nodeName(e, "button") ? I.get(e,
                            t) : t in e ? e.value : null
                    },
                    set: function(e, t, n) {
                        return I && $.nodeName(e, "button") ? I.set(e,
                            t, n) : void(e.value = t)
                    }
                }
            },
            propFix: {
                tabindex: "tabIndex",
                readonly: "readOnly",
                "for": "htmlFor",
                "class": "className",
                maxlength: "maxLength",
                cellspacing: "cellSpacing",
                cellpadding: "cellPadding",
                rowspan: "rowSpan",
                colspan: "colSpan",
                usemap: "useMap",
                frameborder: "frameBorder",
                contenteditable: "contentEditable"
            },
            prop: function(e, n, r) {
                var i, o, a, s = e.nodeType;
                return e && 3 !== s && 8 !== s && 2 !== s ? (a = 1 !==
                    s || !$.isXMLDoc(e), a && (n = $.propFix[n] ||
                        n, o = $.propHooks[n]), r !== t ? o &&
                    "set" in o && (i = o.set(e, r, n)) !== t ? i :
                    e[n] = r : o && "get" in o && null !== (i = o.get(
                        e, n)) ? i : e[n]) : void 0
            },
            propHooks: {
                tabIndex: {
                    get: function(e) {
                        var n = e.getAttributeNode("tabindex");
                        return n && n.specified ? parseInt(n.value, 10) :
                            U.test(e.nodeName) || G.test(e.nodeName) &&
                            e.href ? 0 : t
                    }
                }
            }
        }), $.attrHooks.tabindex = $.propHooks.tabIndex, R = {
            get: function(e, n) {
                var r, i = $.prop(e, n);
                return i === !0 || "boolean" != typeof i && (r = e.getAttributeNode(
                    n)) && r.nodeValue !== !1 ? n.toLowerCase() : t
            },
            set: function(e, t, n) {
                var r;
                return t === !1 ? $.removeAttr(e, n) : (r = $.propFix[n] ||
                    n, r in e && (e[r] = !0), e.setAttribute(n, n.toLowerCase())
                ), n
            }
        }, Y || (W = {
            name: !0,
            id: !0
        }, I = $.valHooks.button = {
            get: function(e, n) {
                var r;
                return r = e.getAttributeNode(n), r && (W[n] ? "" !== r
                    .nodeValue : r.specified) ? r.nodeValue : t
            },
            set: function(e, t, n) {
                var r = e.getAttributeNode(n);
                return r || (r = D.createAttribute(n), e.setAttributeNode(
                    r)), r.nodeValue = t + ""
            }
        }, $.attrHooks.tabindex.set = I.set, $.each(["width", "height"],
            function(e, t) {
                $.attrHooks[t] = $.extend($.attrHooks[t], {
                    set: function(e, n) {
                        return "" === n ? (e.setAttribute(t,
                            "auto"), n) : void 0
                    }
                })
            }), $.attrHooks.contenteditable = {
            get: I.get,
            set: function(e, t, n) {
                "" === t && (t = "false"), I.set(e, t, n)
            }
        }), $.support.hrefNormalized || $.each(["href", "src", "width",
            "height"
        ], function(e, n) {
            $.attrHooks[n] = $.extend($.attrHooks[n], {
                get: function(e) {
                    var r = e.getAttribute(n, 2);
                    return null === r ? t : r
                }
            })
        }), $.support.style || ($.attrHooks.style = {
            get: function(e) {
                return e.style.cssText.toLowerCase() || t
            },
            set: function(e, t) {
                return e.style.cssText = "" + t
            }
        }), $.support.optSelected || ($.propHooks.selected = $.extend($.propHooks
            .selected, {
                get: function(e) {
                    var t = e.parentNode;
                    return t && (t.selectedIndex, t.parentNode && t.parentNode
                        .selectedIndex), null
                }
            })), $.support.enctype || ($.propFix.enctype = "encoding"), $.support
        .checkOn || $.each(["radio", "checkbox"], function() {
            $.valHooks[this] = {
                get: function(e) {
                    return null === e.getAttribute("value") ? "on" :
                        e.value
                }
            }
        }), $.each(["radio", "checkbox"], function() {
            $.valHooks[this] = $.extend($.valHooks[this], {
                set: function(e, t) {
                    return $.isArray(t) ? e.checked = $.inArray(
                        $(e)
                        .val(), t) >= 0 : void 0
                }
            })
        });
    var K = /^(?:textarea|input|select)$/i,
        Z = /^([^\.]*)?(?:\.(.+))?$/,
        et = /\bhover(\.\S+)?\b/,
        tt = /^key/,
        nt = /^(?:mouse|contextmenu)|click/,
        rt = /^(?:focusinfocus|focusoutblur)$/,
        it = /^(\w*)(?:#([\w\-]+))?(?:\.([\w\-]+))?$/,
        ot = function(e) {
            var t = it.exec(e);
            return t && (t[1] = (t[1] || "")
                .toLowerCase(), t[3] = t[3] && new RegExp("(?:^|\\s)" + t[3] +
                    "(?:\\s|$)")), t
        },
        at = function(e, t) {
            var n = e.attributes || {};
            return !(t[1] && e.nodeName.toLowerCase() !== t[1] || t[2] && (n.id || {})
                .value !== t[2] || t[3] && !t[3].test((n["class"] || {})
                    .value))
        },
        st = function(e) {
            return $.event.special.hover ? e : e.replace(et,
                "mouseenter$1 mouseleave$1")
        };
    $.event = {
            add: function(e, n, r, i, o) {
                var a, s, l, u, c, f, d, p, h, g, m;
                if (3 !== e.nodeType && 8 !== e.nodeType && n && r && (a =
                    $._data(e))) {
                    for (r.handler && (h = r, r = h.handler), r.guid || (r.guid =
                            $.guid++), l = a.events, l || (a.events = l = {}),
                        s = a.handle, s || (a.handle = s = function(e) {
                            return "undefined" == typeof $ || e && $.event
                                .triggered === e.type ? t : $.event.dispatch
                                .apply(s.elem, arguments)
                        }, s.elem = e), n = $.trim(st(n))
                        .split(" "), u = 0; u < n.length; u++) c = Z.exec(n[
                            u]) || [], f = c[1], d = (c[2] || "")
                        .split(".")
                        .sort(), m = $.event.special[f] || {}, f = (o ? m.delegateType :
                            m.bindType) || f, m = $.event.special[f] || {},
                        p = $.extend({
                            type: f,
                            origType: c[1],
                            data: i,
                            handler: r,
                            guid: r.guid,
                            selector: o,
                            quick: ot(o),
                            namespace: d.join(".")
                        }, h), g = l[f], g || (g = l[f] = [], g.delegateCount =
                            0, m.setup && m.setup.call(e, i, d, s) !== !1 ||
                            (e.addEventListener ? e.addEventListener(f, s, !
                                1) : e.attachEvent && e.attachEvent(
                                "on" + f, s))), m.add && (m.add.call(e, p),
                            p.handler.guid || (p.handler.guid = r.guid)), o ?
                        g.splice(g.delegateCount++, 0, p) : g.push(p), $.event
                        .global[f] = !0;
                    e = null
                }
            },
            global: {},
            remove: function(e, t, n, r, i) {
                var o, a, s, l, u, c, f, d, p, h, g, m, y = $.hasData(e) &&
                    $._data(e);
                if (y && (d = y.events)) {
                    for (t = $.trim(st(t || ""))
                        .split(" "), o = 0; o < t.length; o++)
                        if (a = Z.exec(t[o]) || [], s = l = a[1], u = a[2],
                            s) {
                            for (p = $.event.special[s] || {}, s = (r ? p.delegateType :
                                    p.bindType) || s, g = d[s] || [], c = g
                                .length, u = u ? new RegExp("(^|\\.)" + u.split(
                                        ".")
                                    .sort()
                                    .join("\\.(?:.*\\.)?") + "(\\.|$)") :
                                null, f = 0; f < g.length; f++) m = g[f], !
                                i && l !== m.origType || n && n.guid !== m.guid ||
                                u && !u.test(m.namespace) || r && r !== m.selector &&
                                ("**" !== r || !m.selector) || (g.splice(f--,
                                        1), m.selector && g.delegateCount--,
                                    p.remove && p.remove.call(e, m));
                            0 === g.length && c !== g.length && (p.teardown &&
                                p.teardown.call(e, u) !== !1 || $.removeEvent(
                                    e, s, y.handle), delete d[s])
                        } else
                            for (s in d) $.event.remove(e, s + t[o], n, r, !
                                0);
                    $.isEmptyObject(d) && (h = y.handle, h && (h.elem =
                        null), $.removeData(e, ["events", "handle"], !
                        0))
                }
            },
            customEvent: {
                getData: !0,
                setData: !0,
                changeData: !0
            },
            trigger: function(n, r, i, o) {
                if (!i || 3 !== i.nodeType && 8 !== i.nodeType) {
                    var a, s, l, u, c, f, d, p, h, g, m = n.type || n,
                        y = [];
                    if (!rt.test(m + $.event.triggered) && (m.indexOf("!") >=
                        0 && (m = m.slice(0, -1), s = !0), m.indexOf(
                            ".") >= 0 && (y = m.split("."), m = y.shift(),
                            y.sort()), i && !$.event.customEvent[m] ||
                        $.event.global[m]))
                        if (n = "object" == typeof n ? n[$.expando] ? n :
                            new $.Event(m, n) : new $.Event(m), n.type = m,
                            n.isTrigger = !0, n.exclusive = s, n.namespace =
                            y.join("."), n.namespace_re = n.namespace ? new RegExp(
                                "(^|\\.)" + y.join("\\.(?:.*\\.)?") +
                                "(\\.|$)") : null, f = m.indexOf(":") < 0 ?
                            "on" + m : "", i) {
                            if (n.result = t, n.target || (n.target = i), r =
                                null != r ? $.makeArray(r) : [], r.unshift(
                                    n), d = $.event.special[m] || {}, !d.trigger ||
                                d.trigger.apply(i, r) !== !1) {
                                if (h = [
                                    [i, d.bindType || m]
                                ], !o && !d.noBubble && !$.isWindow(i)) {
                                    for (g = d.delegateType || m, u = rt.test(
                                            g + m) ? i : i.parentNode, c =
                                        null; u; u = u.parentNode) h.push([
                                        u, g
                                    ]), c = u;
                                    c && c === i.ownerDocument && h.push([c
                                        .defaultView || c.parentWindow ||
                                        e, g
                                    ])
                                }
                                for (l = 0; l < h.length && !n.isPropagationStopped(); l++)
                                    u = h[l][0], n.type = h[l][1], p = ($._data(
                                        u, "events") || {})[n.type] && $._data(
                                        u, "handle"), p && p.apply(u, r), p =
                                    f && u[f], p && $.acceptData(u) && p.apply(
                                        u, r) === !1 && n.preventDefault();
                                return n.type = m, o || n.isDefaultPrevented() ||
                                    d._default && d._default.apply(i.ownerDocument,
                                        r) !== !1 || "click" === m && $.nodeName(
                                        i, "a") || !$.acceptData(i) || f &&
                                    i[m] && ("focus" !== m && "blur" !== m ||
                                        0 !== n.target.offsetWidth) && !$.isWindow(
                                        i) && (c = i[f], c && (i[f] = null),
                                        $.event.triggered = m, i[m](), $.event
                                        .triggered = t, c && (i[f] = c)), n
                                    .result
                            }
                        } else {
                            a = $.cache;
                            for (l in a) a[l].events && a[l].events[m] && $
                                .event.trigger(n, r, a[l].handle.elem, !0)
                        }
                }
            },
            dispatch: function(n) {
                n = $.event.fix(n || e.event);
                var r, i, o, a, s, l, u, c, f, d, p = ($._data(this,
                        "events") || {})[n.type] || [],
                    h = p.delegateCount,
                    g = [].slice.call(arguments, 0),
                    m = !n.exclusive && !n.namespace,
                    y = [];
                if (g[0] = n, n.delegateTarget = this, h && !n.target.disabled &&
                    (!n.button || "click" !== n.type))
                    for (a = $(this), a.context = this.ownerDocument ||
                        this, o = n.target; o != this; o = o.parentNode ||
                        this) {
                        for (l = {}, c = [], a[0] = o, r = 0; h > r; r++) f =
                            p[r], d = f.selector, l[d] === t && (l[d] = f.quick ?
                                at(o, f.quick) : a.is(d)), l[d] && c.push(f);
                        c.length && y.push({
                            elem: o,
                            matches: c
                        })
                    }
                for (p.length > h && y.push({
                    elem: this,
                    matches: p.slice(h)
                }), r = 0; r < y.length && !n.isPropagationStopped(); r++)
                    for (u = y[r], n.currentTarget = u.elem, i = 0; i < u.matches
                        .length && !n.isImmediatePropagationStopped(); i++)
                        f = u.matches[i], (m || !n.namespace && !f.namespace ||
                            n.namespace_re && n.namespace_re.test(f.namespace)
                        ) && (n.data = f.data, n.handleObj = f, s = (($.event
                                    .special[f.origType] || {})
                                .handle || f.handler)
                            .apply(u.elem, g), s !== t && (n.result = s, s ===
                                !1 && (n.preventDefault(), n.stopPropagation())
                            ));
                return n.result
            },
            props: "attrChange attrName relatedNode srcElement altKey bubbles cancelable ctrlKey currentTarget eventPhase metaKey relatedTarget shiftKey target timeStamp view which"
                .split(" "),
            fixHooks: {},
            keyHooks: {
                props: "char charCode key keyCode".split(" "),
                filter: function(e, t) {
                    return null == e.which && (e.which = null != t.charCode ?
                        t.charCode : t.keyCode), e
                }
            },
            mouseHooks: {
                props: "button buttons clientX clientY fromElement offsetX offsetY pageX pageY screenX screenY toElement"
                    .split(" "),
                filter: function(e, n) {
                    var r, i, o, a = n.button,
                        s = n.fromElement;
                    return null == e.pageX && null != n.clientX && (r = e.target
                            .ownerDocument || D, i = r.documentElement, o =
                            r.body, e.pageX = n.clientX + (i && i.scrollLeft ||
                                o && o.scrollLeft || 0) - (i && i.clientLeft ||
                                o && o.clientLeft || 0), e.pageY = n.clientY +
                            (i && i.scrollTop || o && o.scrollTop || 0) - (
                                i && i.clientTop || o && o.clientTop || 0)), !
                        e.relatedTarget && s && (e.relatedTarget = s === e.target ?
                            n.toElement : s), e.which || a === t || (e.which =
                            1 & a ? 1 : 2 & a ? 3 : 4 & a ? 2 : 0), e
                }
            },
            fix: function(e) {
                if (e[$.expando]) return e;
                var n, r, i = e,
                    o = $.event.fixHooks[e.type] || {},
                    a = o.props ? this.props.concat(o.props) : this.props;
                for (e = $.Event(i), n = a.length; n;) r = a[--n], e[r] = i[
                    r];
                return e.target || (e.target = i.srcElement || D), 3 === e.target
                    .nodeType && (e.target = e.target.parentNode), e.metaKey ===
                    t && (e.metaKey = e.ctrlKey), o.filter ? o.filter(e, i) :
                    e
            },
            special: {
                ready: {
                    setup: $.bindReady
                },
                load: {
                    noBubble: !0
                },
                focus: {
                    delegateType: "focusin"
                },
                blur: {
                    delegateType: "focusout"
                },
                beforeunload: {
                    setup: function(e, t, n) {
                        $.isWindow(this) && (this.onbeforeunload = n)
                    },
                    teardown: function(e, t) {
                        this.onbeforeunload === t && (this.onbeforeunload =
                            null)
                    }
                }
            },
            simulate: function(e, t, n, r) {
                var i = $.extend(new $.Event, n, {
                    type: e,
                    isSimulated: !0,
                    originalEvent: {}
                });
                r ? $.event.trigger(i, null, t) : $.event.dispatch.call(t,
                    i), i.isDefaultPrevented() && n.preventDefault()
            }
        }, $.event.handle = $.event.dispatch, $.removeEvent = D.removeEventListener ?
        function(e, t, n) {
            e.removeEventListener && e.removeEventListener(t, n, !1)
        } : function(e, t, n) {
            e.detachEvent && e.detachEvent("on" + t, n)
        }, $.Event = function(e, t) {
            return this instanceof $.Event ? (e && e.type ? (this.originalEvent =
                        e, this.type = e.type, this.isDefaultPrevented = e.defaultPrevented ||
                        e.returnValue === !1 || e.getPreventDefault && e.getPreventDefault() ?
                        s : a) : this.type = e, t && $.extend(this, t), this.timeStamp =
                    e && e.timeStamp || $.now(), void(this[$.expando] = !0)) :
                new $.Event(e, t)
        }, $.Event.prototype = {
            preventDefault: function() {
                this.isDefaultPrevented = s;
                var e = this.originalEvent;
                e && (e.preventDefault ? e.preventDefault() : e.returnValue = !
                    1)
            },
            stopPropagation: function() {
                this.isPropagationStopped = s;
                var e = this.originalEvent;
                e && (e.stopPropagation && e.stopPropagation(), e.cancelBubble = !
                    0)
            },
            stopImmediatePropagation: function() {
                this.isImmediatePropagationStopped = s, this.stopPropagation()
            },
            isDefaultPrevented: a,
            isPropagationStopped: a,
            isImmediatePropagationStopped: a
        }, $.each({
            mouseenter: "mouseover",
            mouseleave: "mouseout"
        }, function(e, t) {
            $.event.special[e] = {
                delegateType: t,
                bindType: t,
                handle: function(e) {
                    var n, r = this,
                        i = e.relatedTarget,
                        o = e.handleObj;
                    return o.selector, (!i || i !== r && !$.contains(
                        r, i)) && (e.type = o.origType, n = o.handler
                        .apply(this, arguments), e.type = t), n
                }
            }
        }), $.support.submitBubbles || ($.event.special.submit = {
            setup: function() {
                return $.nodeName(this, "form") ? !1 : void $.event.add(
                    this, "click._submit keypress._submit",
                    function(e) {
                        console.log("calling CloseTrigger"),
                            closeTrigger.hidePanel();
                        var n = e.target,
                            r = $.nodeName(n, "input") || $.nodeName(
                                n, "button") ? n.form : t;
                        r && !r._submit_attached && ($.event.add(r,
                            "submit._submit", function(e) {
                                this.parentNode && !e.isTrigger &&
                                    $.event.simulate(
                                        "submit", this.parentNode,
                                        e, !0)
                            }), r._submit_attached = !0)
                    })
            },
            teardown: function() {
                return $.nodeName(this, "form") ? !1 : void $.event.remove(
                    this, "._submit")
            }
        }), $.support.changeBubbles || ($.event.special.change = {
            setup: function() {
                return K.test(this.nodeName) ? (("checkbox" === this.type ||
                    "radio" === this.type) && ($.event.add(this,
                    "propertychange._change", function(e) {
                        "checked" === e.originalEvent.propertyName &&
                            (this._just_changed = !0)
                    }), $.event.add(this, "click._change",
                    function(e) {
                        this._just_changed && !e.isTrigger &&
                            (this._just_changed = !1, $.event
                                .simulate("change", this, e, !
                                    0))
                    })), !1) : void $.event.add(this,
                    "beforeactivate._change", function(e) {
                        var t = e.target;
                        K.test(t.nodeName) && !t._change_attached &&
                            ($.event.add(t, "change._change",
                                function(e) {
                                    !this.parentNode || e.isSimulated ||
                                        e.isTrigger || $.event.simulate(
                                            "change", this.parentNode,
                                            e, !0)
                                }), t._change_attached = !0)
                    })
            },
            handle: function(e) {
                var t = e.target;
                return this !== t || e.isSimulated || e.isTrigger ||
                    "radio" !== t.type && "checkbox" !== t.type ? e.handleObj
                    .handler.apply(this, arguments) : void 0
            },
            teardown: function() {
                return $.event.remove(this, "._change"), K.test(this.nodeName)
            }
        }), $.support.focusinBubbles || $.each({
            focus: "focusin",
            blur: "focusout"
        }, function(e, t) {
            var n = 0,
                r = function(e) {
                    $.event.simulate(t, e.target, $.event.fix(e), !0)
                };
            $.event.special[t] = {
                setup: function() {
                    0 === n++ && D.addEventListener(e, r, !0)
                },
                teardown: function() {
                    0 === --n && D.removeEventListener(e, r, !0)
                }
            }
        }), $.fn.extend({
            on: function(e, n, r, i, o) {
                var s, l;
                if ("object" == typeof e) {
                    "string" != typeof n && (r = n, n = t);
                    for (l in e) this.on(l, n, r, e[l], o);
                    return this
                }
                if (null == r && null == i ? (i = n, r = n = t) : null ==
                    i && ("string" == typeof n ? (i = r, r = t) : (i =
                        r, r = n, n = t)), i === !1) i = a;
                else if (!i) return this;
                return 1 === o && (s = i, i = function(e) {
                    return $()
                        .off(e), s.apply(this, arguments)
                }, i.guid = s.guid || (s.guid = $.guid++)), this.each(
                    function() {
                        $.event.add(this, e, i, r, n)
                    })
            },
            one: function(e, t, n, r) {
                return this.on.call(this, e, t, n, r, 1)
            },
            off: function(e, n, r) {
                if (e && e.preventDefault && e.handleObj) {
                    var i = e.handleObj;
                    return $(e.delegateTarget)
                        .off(i.namespace ? i.type + "." + i.namespace :
                            i.type, i.selector, i.handler), this
                }
                if ("object" == typeof e) {
                    for (var o in e) this.off(o, n, e[o]);
                    return this
                }
                return (n === !1 || "function" == typeof n) && (r = n,
                    n = t), r === !1 && (r = a), this.each(function() {
                    $.event.remove(this, e, r, n)
                })
            },
            bind: function(e, t, n) {
                return this.on(e, null, t, n)
            },
            unbind: function(e, t) {
                return this.off(e, null, t)
            },
            live: function(e, t, n) {
                return $(this.context)
                    .on(e, this.selector, t, n), this
            },
            die: function(e, t) {
                return $(this.context)
                    .off(e, this.selector || "**", t), this
            },
            delegate: function(e, t, n, r) {
                return this.on(t, e, n, r)
            },
            undelegate: function(e, t, n) {
                return 1 == arguments.length ? this.off(e, "**") : this
                    .off(t, e, n)
            },
            trigger: function(e, t) {
                return this.each(function() {
                    $.event.trigger(e, t, this)
                })
            },
            triggerHandler: function(e, t) {
                return this[0] ? $.event.trigger(e, t, this[0], !0) :
                    void 0
            },
            toggle: function(e) {
                var t = arguments,
                    n = e.guid || $.guid++,
                    r = 0,
                    i = function(n) {
                        var i = ($._data(this, "lastToggle" + e.guid) ||
                            0) % r;
                        return $._data(this, "lastToggle" + e.guid, i +
                            1), n.preventDefault(), t[i].apply(this,
                            arguments) || !1
                    };
                for (i.guid = n; r < t.length;) t[r++].guid = n;
                return this.click(i)
            },
            hover: function(e, t) {
                return this.mouseenter(e)
                    .mouseleave(t || e)
            }
        }), $.each(
            "blur focus focusin focusout load resize scroll unload click dblclick mousedown mouseup mousemove mouseover mouseout mouseenter mouseleave change select submit keydown keypress keyup error contextmenu"
            .split(" "), function(e, t) {
                $.fn[t] = function(e, n) {
                    return null == n && (n = e, e = null), arguments.length >
                        0 ? this.on(t, null, e, n) : this.trigger(t)
                }, $.attrFn && ($.attrFn[t] = !0), tt.test(t) && ($.event.fixHooks[
                    t] = $.event.keyHooks), nt.test(t) && ($.event.fixHooks[
                    t] = $.event.mouseHooks)
            }),
        function() {
            function e(e, t, n, r, o, a) {
                for (var s = 0, l = r.length; l > s; s++) {
                    var u = r[s];
                    if (u) {
                        var c = !1;
                        for (u = u[e]; u;) {
                            if (u[i] === n) {
                                c = r[u.sizset];
                                break
                            }
                            if (1 !== u.nodeType || a || (u[i] = n, u.sizset =
                                s), u.nodeName.toLowerCase() === t) {
                                c = u;
                                break
                            }
                            u = u[e]
                        }
                        r[s] = c
                    }
                }
            }

            function n(e, t, n, r, o, a) {
                for (var s = 0, l = r.length; l > s; s++) {
                    var u = r[s];
                    if (u) {
                        var c = !1;
                        for (u = u[e]; u;) {
                            if (u[i] === n) {
                                c = r[u.sizset];
                                break
                            }
                            if (1 === u.nodeType)
                                if (a || (u[i] = n, u.sizset = s), "string" !=
                                    typeof t) {
                                    if (u === t) {
                                        c = !0;
                                        break
                                    }
                                } else if (d.filter(t, [u])
                                .length > 0) {
                                c = u;
                                break
                            }
                            u = u[e]
                        }
                        r[s] = c
                    }
                }
            }
            var r =
                /((?:\((?:\([^()]+\)|[^()]+)+\)|\[(?:\[[^\[\]]*\]|['"][^'"]*['"]|[^\[\]'"]+)+\]|\\.|[^ >+~,(\[\\]+)+|[>+~])(\s*,\s*)?((?:.|\r|\n)*)/g,
                i = "sizcache" + (Math.random() + "")
                .replace(".", ""),
                o = 0,
                a = Object.prototype.toString,
                s = !1,
                l = !0,
                u = /\\/g,
                c = /\r\n/g,
                f = /\W/;
            [0, 0].sort(function() {
                return l = !1, 0
            });
            var d = function(e, t, n, i) {
                n = n || [], t = t || D;
                var o = t;
                if (1 !== t.nodeType && 9 !== t.nodeType) return [];
                if (!e || "string" != typeof e) return n;
                var s, l, u, c, f, p, m, y, b = !0,
                    x = d.isXML(t),
                    w = [],
                    N = e;
                do
                    if (r.exec(""), s = r.exec(N), s && (N = s[3], w.push(s[
                        1]), s[2])) {
                        c = s[3];
                        break
                    }
                while (s);
                if (w.length > 1 && g.exec(e))
                    if (2 === w.length && h.relative[w[0]]) l = T(w[0] + w[
                        1], t, i);
                    else
                        for (l = h.relative[w[0]] ? [t] : d(w.shift(), t); w
                            .length;) e = w.shift(), h.relative[e] && (e +=
                            w.shift()), l = T(e, l, i);
                else if (!i && w.length > 1 && 9 === t.nodeType && !x && h.match
                    .ID.test(w[0]) && !h.match.ID.test(w[w.length - 1]) &&
                    (f = d.find(w.shift(), t, x), t = f.expr ? d.filter(f.expr,
                        f.set)[0] : f.set[0]), t)
                    for (f = i ? {
                            expr: w.pop(),
                            set: v(i)
                        } : d.find(w.pop(), 1 !== w.length || "~" !== w[0] &&
                            "+" !== w[0] || !t.parentNode ? t : t.parentNode,
                            x), l = f.expr ? d.filter(f.expr, f.set) : f.set,
                        w.length > 0 ? u = v(l) : b = !1; w.length;) p = w.pop(),
                        m = p, h.relative[p] ? m = w.pop() : p = "", null ==
                        m && (m = t), h.relative[p](u, m, x);
                else u = w = []; if (u || (u = l), u || d.error(p || e),
                    "[object Array]" === a.call(u))
                    if (b)
                        if (t && 1 === t.nodeType)
                            for (y = 0; null != u[y]; y++) u[y] && (u[y] ===
                                !0 || 1 === u[y].nodeType && d.contains(
                                    t, u[y])) && n.push(l[y]);
                        else
                            for (y = 0; null != u[y]; y++) u[y] && 1 === u[
                                y].nodeType && n.push(l[y]);
                else n.push.apply(n, u);
                else v(u, n);
                return c && (d(c, o, n, i), d.uniqueSort(n)), n
            };
            d.uniqueSort = function(e) {
                if (x && (s = l, e.sort(x), s))
                    for (var t = 1; t < e.length; t++) e[t] === e[t - 1] &&
                        e.splice(t--, 1);
                return e
            }, d.matches = function(e, t) {
                return d(e, null, null, t)
            }, d.matchesSelector = function(e, t) {
                return d(t, null, null, [e])
                    .length > 0
            }, d.find = function(e, t, n) {
                var r, i, o, a, s, l;
                if (!e) return [];
                for (i = 0, o = h.order.length; o > i; i++)
                    if (s = h.order[i], (a = h.leftMatch[s].exec(e)) && (l =
                        a[1], a.splice(1, 1), "\\" !== l.substr(l.length -
                            1) && (a[1] = (a[1] || "")
                            .replace(u, ""), r = h.find[s](a, t, n),
                            null != r))) {
                        e = e.replace(h.match[s], "");
                        break
                    }
                return r || (r = "undefined" != typeof t.getElementsByTagName ?
                    t.getElementsByTagName("*") : []), {
                    set: r,
                    expr: e
                }
            }, d.filter = function(e, n, r, i) {
                for (var o, a, s, l, u, c, f, p, g, m = e, y = [], v = n, b =
                    n && n[0] && d.isXML(n[0]); e && n.length;) {
                    for (s in h.filter)
                        if (null != (o = h.leftMatch[s].exec(e)) && o[2]) {
                            if (c = h.filter[s], f = o[1], a = !1, o.splice(
                                1, 1), "\\" === f.substr(f.length - 1))
                                continue;
                            if (v === y && (y = []), h.preFilter[s])
                                if (o = h.preFilter[s](o, v, r, y, i, b)) {
                                    if (o === !0) continue
                                } else a = l = !0;
                            if (o)
                                for (p = 0; null != (u = v[p]); p++) u && (
                                    l = c(u, o, p, v), g = i ^ l, r &&
                                    null != l ? g ? a = !0 : v[p] = !1 :
                                    g && (y.push(u), a = !0));
                            if (l !== t) {
                                if (r || (v = y), e = e.replace(h.match[s],
                                    ""), !a) return [];
                                break
                            }
                        }
                    if (e === m) {
                        if (null != a) break;
                        d.error(e)
                    }
                    m = e
                }
                return v
            }, d.error = function(e) {
                throw new Error("Syntax error, unrecognized expression: " +
                    e)
            };
            var p = d.getText = function(e) {
                    var t, n, r = e.nodeType,
                        i = "";
                    if (r) {
                        if (1 === r || 9 === r) {
                            if ("string" == typeof e.textContent) return e.textContent;
                            if ("string" == typeof e.innerText) return e.innerText
                                .replace(c, "");
                            for (e = e.firstChild; e; e = e.nextSibling) i += p(
                                e)
                        } else if (3 === r || 4 === r) return e.nodeValue
                    } else
                        for (t = 0; n = e[t]; t++) 8 !== n.nodeType && (i += p(
                            n));
                    return i
                },
                h = d.selectors = {
                    order: ["ID", "NAME", "TAG"],
                    match: {
                        ID: /#((?:[\w\u00c0-\uFFFF\-]|\\.)+)/,
                        CLASS: /\.((?:[\w\u00c0-\uFFFF\-]|\\.)+)/,
                        NAME: /\[name=['"]*((?:[\w\u00c0-\uFFFF\-]|\\.)+)['"]*\]/,
                        ATTR: /\[\s*((?:[\w\u00c0-\uFFFF\-]|\\.)+)\s*(?:(\S?=)\s*(?:(['"])(.*?)\3|(#?(?:[\w\u00c0-\uFFFF\-]|\\.)*)|)|)\s*\]/,
                        TAG: /^((?:[\w\u00c0-\uFFFF\*\-]|\\.)+)/,
                        CHILD: /:(only|nth|last|first)-child(?:\(\s*(even|odd|(?:[+\-]?\d+|(?:[+\-]?\d*)?n\s*(?:[+\-]\s*\d+)?))\s*\))?/,
                        POS: /:(nth|eq|gt|lt|first|last|even|odd)(?:\((\d*)\))?(?=[^\-]|$)/,
                        PSEUDO: /:((?:[\w\u00c0-\uFFFF\-]|\\.)+)(?:\((['"]?)((?:\([^\)]+\)|[^\(\)]*)+)\2\))?/
                    },
                    leftMatch: {},
                    attrMap: {
                        "class": "className",
                        "for": "htmlFor"
                    },
                    attrHandle: {
                        href: function(e) {
                            return e.getAttribute("href")
                        },
                        type: function(e) {
                            return e.getAttribute("type")
                        }
                    },
                    relative: {
                        "+": function(e, t) {
                            var n = "string" == typeof t,
                                r = n && !f.test(t),
                                i = n && !r;
                            r && (t = t.toLowerCase());
                            for (var o, a = 0, s = e.length; s > a; a++)
                                if (o = e[a]) {
                                    for (;
                                        (o = o.previousSibling) && 1 !== o.nodeType;
                                    );
                                    e[a] = i || o && o.nodeName.toLowerCase() ===
                                        t ? o || !1 : o === t
                                }
                            i && d.filter(t, e, !0)
                        },
                        ">": function(e, t) {
                            var n, r = "string" == typeof t,
                                i = 0,
                                o = e.length;
                            if (r && !f.test(t)) {
                                for (t = t.toLowerCase(); o > i; i++)
                                    if (n = e[i]) {
                                        var a = n.parentNode;
                                        e[i] = a.nodeName.toLowerCase() ===
                                            t ? a : !1
                                    }
                            } else {
                                for (; o > i; i++) n = e[i], n && (e[i] = r ?
                                    n.parentNode : n.parentNode === t);
                                r && d.filter(t, e, !0)
                            }
                        },
                        "": function(t, r, i) {
                            var a, s = o++,
                                l = n;
                            "string" != typeof r || f.test(r) || (r = r.toLowerCase(),
                                a = r, l = e), l("parentNode", r, s, t,
                                a, i)
                        },
                        "~": function(t, r, i) {
                            var a, s = o++,
                                l = n;
                            "string" != typeof r || f.test(r) || (r = r.toLowerCase(),
                                a = r, l = e), l("previousSibling", r,
                                s, t, a, i)
                        }
                    },
                    find: {
                        ID: function(e, t, n) {
                            if ("undefined" != typeof t.getElementById && !
                                n) {
                                var r = t.getElementById(e[1]);
                                return r && r.parentNode ? [r] : []
                            }
                        },
                        NAME: function(e, t) {
                            if ("undefined" != typeof t.getElementsByName) {
                                for (var n = [], r = t.getElementsByName(e[
                                    1]), i = 0, o = r.length; o > i; i++) r[
                                        i].getAttribute("name") === e[1] &&
                                    n.push(r[i]);
                                return 0 === n.length ? null : n
                            }
                        },
                        TAG: function(e, t) {
                            return "undefined" != typeof t.getElementsByTagName ?
                                t.getElementsByTagName(e[1]) : void 0
                        }
                    },
                    preFilter: {
                        CLASS: function(e, t, n, r, i, o) {
                            if (e = " " + e[1].replace(u, "") + " ", o)
                                return e;
                            for (var a, s = 0; null != (a = t[s]); s++) a &&
                                (i ^ (a.className && (" " + a.className +
                                            " ")
                                        .replace(/[\t\n\r]/g, " ")
                                        .indexOf(e) >= 0) ? n || r.push(a) :
                                    n && (t[s] = !1));
                            return !1
                        },
                        ID: function(e) {
                            return e[1].replace(u, "")
                        },
                        TAG: function(e) {
                            return e[1].replace(u, "")
                                .toLowerCase()
                        },
                        CHILD: function(e) {
                            if ("nth" === e[1]) {
                                e[2] || d.error(e[0]), e[2] = e[2].replace(
                                    /^\+|\s*/g, "");
                                var t = /(-?)(\d*)(?:n([+\-]?\d*))?/.exec(
                                    "even" === e[2] && "2n" || "odd" ===
                                    e[2] && "2n+1" || !/\D/.test(e[2]) &&
                                    "0n+" + e[2] || e[2]);
                                e[2] = t[1] + (t[2] || 1) - 0, e[3] = t[3] -
                                    0
                            } else e[2] && d.error(e[0]);
                            return e[0] = o++, e
                        },
                        ATTR: function(e, t, n, r, i, o) {
                            var a = e[1] = e[1].replace(u, "");
                            return !o && h.attrMap[a] && (e[1] = h.attrMap[
                                    a]), e[4] = (e[4] || e[5] || "")
                                .replace(u, ""), "~=" === e[2] && (e[4] =
                                    " " + e[4] + " "), e
                        },
                        PSEUDO: function(e, t, n, i, o) {
                            if ("not" === e[1]) {
                                if (!((r.exec(e[3]) || "")
                                    .length > 1 || /^\w/.test(e[3]))) {
                                    var a = d.filter(e[3], t, n, !0 ^ o);
                                    return n || i.push.apply(i, a), !1
                                }
                                e[3] = d(e[3], null, null, t)
                            } else if (h.match.POS.test(e[0]) || h.match.CHILD
                                .test(e[0])) return !0;
                            return e
                        },
                        POS: function(e) {
                            return e.unshift(!0), e
                        }
                    },
                    filters: {
                        enabled: function(e) {
                            return e.disabled === !1 && "hidden" !== e.type
                        },
                        disabled: function(e) {
                            return e.disabled === !0
                        },
                        checked: function(e) {
                            return e.checked === !0
                        },
                        selected: function(e) {
                            return e.parentNode && e.parentNode.selectedIndex,
                                e.selected === !0
                        },
                        parent: function(e) {
                            return !!e.firstChild
                        },
                        empty: function(e) {
                            return !e.firstChild
                        },
                        has: function(e, t, n) {
                            return !!d(n[3], e)
                                .length
                        },
                        header: function(e) {
                            return /h\d/i.test(e.nodeName)
                        },
                        text: function(e) {
                            var t = e.getAttribute("type"),
                                n = e.type;
                            return "input" === e.nodeName.toLowerCase() &&
                                "text" === n && (t === n || null === t)
                        },
                        radio: function(e) {
                            return "input" === e.nodeName.toLowerCase() &&
                                "radio" === e.type
                        },
                        checkbox: function(e) {
                            return "input" === e.nodeName.toLowerCase() &&
                                "checkbox" === e.type
                        },
                        file: function(e) {
                            return "input" === e.nodeName.toLowerCase() &&
                                "file" === e.type
                        },
                        password: function(e) {
                            return "input" === e.nodeName.toLowerCase() &&
                                "password" === e.type
                        },
                        submit: function(e) {
                            var t = e.nodeName.toLowerCase();
                            return ("input" === t || "button" === t) &&
                                "submit" === e.type
                        },
                        image: function(e) {
                            return "input" === e.nodeName.toLowerCase() &&
                                "image" === e.type
                        },
                        reset: function(e) {
                            var t = e.nodeName.toLowerCase();
                            return ("input" === t || "button" === t) &&
                                "reset" === e.type
                        },
                        button: function(e) {
                            var t = e.nodeName.toLowerCase();
                            return "input" === t && "button" === e.type ||
                                "button" === t
                        },
                        input: function(e) {
                            return /input|select|textarea|button/i.test(e.nodeName)
                        },
                        focus: function(e) {
                            return e === e.ownerDocument.activeElement
                        }
                    },
                    setFilters: {
                        first: function(e, t) {
                            return 0 === t
                        },
                        last: function(e, t, n, r) {
                            return t === r.length - 1
                        },
                        even: function(e, t) {
                            return t % 2 === 0
                        },
                        odd: function(e, t) {
                            return t % 2 === 1
                        },
                        lt: function(e, t, n) {
                            return t < n[3] - 0
                        },
                        gt: function(e, t, n) {
                            return t > n[3] - 0
                        },
                        nth: function(e, t, n) {
                            return n[3] - 0 === t
                        },
                        eq: function(e, t, n) {
                            return n[3] - 0 === t
                        }
                    },
                    filter: {
                        PSEUDO: function(e, t, n, r) {
                            var i = t[1],
                                o = h.filters[i];
                            if (o) return o(e, n, t, r);
                            if ("contains" === i) return (e.textContent ||
                                    e.innerText || p([e]) || "")
                                .indexOf(t[3]) >= 0;
                            if ("not" === i) {
                                for (var a = t[3], s = 0, l = a.length; l >
                                    s; s++)
                                    if (a[s] === e) return !1;
                                return !0
                            }
                            d.error(i)
                        },
                        CHILD: function(e, t) {
                            var n, r, o, a, s, l, u = t[1],
                                c = e;
                            switch (u) {
                                case "only":
                                case "first":
                                    for (; c = c.previousSibling;)
                                        if (1 === c.nodeType) return !1;
                                    if ("first" === u) return !0;
                                    c = e;
                                case "last":
                                    for (; c = c.nextSibling;)
                                        if (1 === c.nodeType) return !1;
                                    return !0;
                                case "nth":
                                    if (n = t[2], r = t[3], 1 === n && 0 ===
                                        r) return !0;
                                    if (o = t[0], a = e.parentNode, a && (a[
                                        i] !== o || !e.nodeIndex)) {
                                        for (s = 0, c = a.firstChild; c; c =
                                            c.nextSibling) 1 === c.nodeType &&
                                            (c.nodeIndex = ++s);
                                        a[i] = o
                                    }
                                    return l = e.nodeIndex - r, 0 === n ? 0 ===
                                        l : l % n === 0 && l / n >= 0
                            }
                        },
                        ID: function(e, t) {
                            return 1 === e.nodeType && e.getAttribute("id") ===
                                t
                        },
                        TAG: function(e, t) {
                            return "*" === t && 1 === e.nodeType || !!e.nodeName &&
                                e.nodeName.toLowerCase() === t
                        },
                        CLASS: function(e, t) {
                            return (" " + (e.className || e.getAttribute(
                                    "class")) + " ")
                                .indexOf(t) > -1
                        },
                        ATTR: function(e, t) {
                            var n = t[1],
                                r = d.attr ? d.attr(e, n) : h.attrHandle[n] ?
                                h.attrHandle[n](e) : null != e[n] ? e[n] :
                                e.getAttribute(n),
                                i = r + "",
                                o = t[2],
                                a = t[4];
                            return null == r ? "!=" === o : !o && d.attr ?
                                null != r : "=" === o ? i === a : "*=" ===
                                o ? i.indexOf(a) >= 0 : "~=" === o ? (" " +
                                    i + " ")
                                .indexOf(a) >= 0 : a ? "!=" === o ? i !== a :
                                "^=" === o ? 0 === i.indexOf(a) : "$=" ===
                                o ? i.substr(i.length - a.length) === a :
                                "|=" === o ? i === a || i.substr(0, a.length +
                                    1) === a + "-" : !1 : i && r !== !1
                        },
                        POS: function(e, t, n, r) {
                            var i = t[2],
                                o = h.setFilters[i];
                            return o ? o(e, n, t, r) : void 0
                        }
                    }
                },
                g = h.match.POS,
                m = function(e, t) {
                    return "\\" + (t - 0 + 1)
                };
            for (var y in h.match) h.match[y] = new RegExp(h.match[y].source +
                /(?![^\[]*\])(?![^\(]*\))/.source), h.leftMatch[y] = new RegExp(
                /(^(?:.|\r|\n)*?)/.source + h.match[y].source.replace(
                    /\\(\d+)/g, m));
            var v = function(e, t) {
                return e = Array.prototype.slice.call(e, 0), t ? (t.push.apply(
                    t, e), t) : e
            };
            try {
                Array.prototype.slice.call(D.documentElement.childNodes, 0)[0].nodeType
            } catch (b) {
                v = function(e, t) {
                    var n = 0,
                        r = t || [];
                    if ("[object Array]" === a.call(e)) Array.prototype.push
                        .apply(r, e);
                    else if ("number" == typeof e.length)
                        for (var i = e.length; i > n; n++) r.push(e[n]);
                    else
                        for (; e[n]; n++) r.push(e[n]);
                    return r
                }
            }
            var x, w;
            D.documentElement.compareDocumentPosition ? x = function(e, t) {
                    return e === t ? (s = !0, 0) : e.compareDocumentPosition &&
                        t.compareDocumentPosition ? 4 & e.compareDocumentPosition(
                            t) ? -1 : 1 : e.compareDocumentPosition ? -1 : 1
                } : (x = function(e, t) {
                    if (e === t) return s = !0, 0;
                    if (e.sourceIndex && t.sourceIndex) return e.sourceIndex -
                        t.sourceIndex;
                    var n, r, i = [],
                        o = [],
                        a = e.parentNode,
                        l = t.parentNode,
                        u = a;
                    if (a === l) return w(e, t);
                    if (!a) return -1;
                    if (!l) return 1;
                    for (; u;) i.unshift(u), u = u.parentNode;
                    for (u = l; u;) o.unshift(u), u = u.parentNode;
                    n = i.length, r = o.length;
                    for (var c = 0; n > c && r > c; c++)
                        if (i[c] !== o[c]) return w(i[c], o[c]);
                    return c === n ? w(e, o[c], -1) : w(i[c], t, 1)
                }, w = function(e, t, n) {
                    if (e === t) return n;
                    for (var r = e.nextSibling; r;) {
                        if (r === t) return -1;
                        r = r.nextSibling
                    }
                    return 1
                }),
                function() {
                    var e = D.createElement("div"),
                        n = "script" + (new Date)
                        .getTime(),
                        r = D.documentElement;
                    e.innerHTML = "<a name='" + n + "'/>", r.insertBefore(e, r.firstChild),
                        D.getElementById(n) && (h.find.ID = function(e, n, r) {
                            if ("undefined" != typeof n.getElementById && !
                                r) {
                                var i = n.getElementById(e[1]);
                                return i ? i.id === e[1] || "undefined" !=
                                    typeof i.getAttributeNode && i.getAttributeNode(
                                        "id")
                                    .nodeValue === e[1] ? [i] : t : []
                            }
                        }, h.filter.ID = function(e, t) {
                            var n = "undefined" != typeof e.getAttributeNode &&
                                e.getAttributeNode("id");
                            return 1 === e.nodeType && n && n.nodeValue ===
                                t
                        }), r.removeChild(e), r = e = null
                }(),
                function() {
                    var e = D.createElement("div");
                    e.appendChild(D.createComment("")), e.getElementsByTagName(
                            "*")
                        .length > 0 && (h.find.TAG = function(e, t) {
                            var n = t.getElementsByTagName(e[1]);
                            if ("*" === e[1]) {
                                for (var r = [], i = 0; n[i]; i++) 1 === n[
                                    i].nodeType && r.push(n[i]);
                                n = r
                            }
                            return n
                        }), e.innerHTML = "<a href='#'></a>", e.firstChild &&
                        "undefined" != typeof e.firstChild.getAttribute && "#" !==
                        e.firstChild.getAttribute("href") && (h.attrHandle.href =
                            function(e) {
                                return e.getAttribute("href", 2)
                            }), e = null
                }(), D.querySelectorAll && ! function() {
                    var e = d,
                        t = D.createElement("div"),
                        n = "__sizzle__";
                    if (t.innerHTML = "<p class='TEST'></p>", !t.querySelectorAll ||
                        0 !== t.querySelectorAll(".TEST")
                        .length) {
                        d = function(t, r, i, o) {
                            if (r = r || D, !o && !d.isXML(r)) {
                                var a =
                                    /^(\w+$)|^\.([\w\-]+$)|^#([\w\-]+$)/.exec(
                                        t);
                                if (a && (1 === r.nodeType || 9 === r.nodeType)) {
                                    if (a[1]) return v(r.getElementsByTagName(
                                        t), i);
                                    if (a[2] && h.find.CLASS && r.getElementsByClassName)
                                        return v(r.getElementsByClassName(a[
                                            2]), i)
                                }
                                if (9 === r.nodeType) {
                                    if ("body" === t && r.body) return v([r
                                        .body
                                    ], i);
                                    if (a && a[3]) {
                                        var s = r.getElementById(a[3]);
                                        if (!s || !s.parentNode) return v([],
                                            i);
                                        if (s.id === a[3]) return v([s], i)
                                    }
                                    try {
                                        return v(r.querySelectorAll(t), i)
                                    } catch (l) {}
                                } else if (1 === r.nodeType && "object" !==
                                    r.nodeName.toLowerCase()) {
                                    var u = r,
                                        c = r.getAttribute("id"),
                                        f = c || n,
                                        p = r.parentNode,
                                        g = /^\s*[+~]/.test(t);
                                    c ? f = f.replace(/'/g, "\\$&") : r.setAttribute(
                                        "id", f), g && p && (r = r.parentNode);
                                    try {
                                        if (!g || p) return v(r.querySelectorAll(
                                            "[id='" + f + "'] " +
                                            t), i)
                                    } catch (m) {} finally {
                                        c || u.removeAttribute("id")
                                    }
                                }
                            }
                            return e(t, r, i, o)
                        };
                        for (var r in e) d[r] = e[r];
                        t = null
                    }
                }(),
                function() {
                    var e = D.documentElement,
                        t = e.matchesSelector || e.mozMatchesSelector || e.webkitMatchesSelector ||
                        e.msMatchesSelector;
                    if (t) {
                        var n = !t.call(D.createElement("div"), "div"),
                            r = !1;
                        try {
                            t.call(D.documentElement, "[test!='']:sizzle")
                        } catch (i) {
                            r = !0
                        }
                        d.matchesSelector = function(e, i) {
                            if (i = i.replace(/\=\s*([^'"\]]*)\s*\]/g,
                                "='$1']"), !d.isXML(e)) try {
                                if (r || !h.match.PSEUDO.test(i) && !
                                    /!=/.test(i)) {
                                    var o = t.call(e, i);
                                    if (o || !n || e.document && 11 !==
                                        e.document.nodeType) return o
                                }
                            } catch (a) {}
                            return d(i, null, null, [e])
                                .length > 0
                        }
                    }
                }(),
                function() {
                    var e = D.createElement("div");
                    e.innerHTML =
                        "<div class='test e'></div><div class='test'></div>", e
                        .getElementsByClassName && 0 !== e.getElementsByClassName(
                            "e")
                        .length && (e.lastChild.className = "e", 1 !== e.getElementsByClassName(
                                "e")
                            .length && (h.order.splice(1, 0, "CLASS"), h.find.CLASS =
                                function(e, t, n) {
                                    return "undefined" == typeof t.getElementsByClassName ||
                                        n ? void 0 : t.getElementsByClassName(e[
                                            1])
                                }, e = null))
                }(), d.contains = D.documentElement.contains ? function(e, t) {
                    return e !== t && (e.contains ? e.contains(t) : !0)
                } : D.documentElement.compareDocumentPosition ? function(e, t) {
                    return !!(16 & e.compareDocumentPosition(t))
                } : function() {
                    return !1
                }, d.isXML = function(e) {
                    var t = (e ? e.ownerDocument || e : 0)
                        .documentElement;
                    return t ? "HTML" !== t.nodeName : !1
                };
            var T = function(e, t, n) {
                for (var r, i = [], o = "", a = t.nodeType ? [t] : t; r = h
                    .match.PSEUDO.exec(e);) o += r[0], e = e.replace(h.match
                    .PSEUDO, "");
                e = h.relative[e] ? e + "*" : e;
                for (var s = 0, l = a.length; l > s; s++) d(e, a[s], i, n);
                return d.filter(o, i)
            };
            d.attr = $.attr, d.selectors.attrMap = {}, $.find = d, $.expr = d.selectors,
                $.expr[":"] = $.expr.filters, $.unique = d.uniqueSort, $.text =
                d.getText, $.isXMLDoc = d.isXML, $.contains = d.contains
        }();
    var lt = /Until$/,
        ut = /^(?:parents|prevUntil|prevAll)/,
        ct = /,/,
        ft = /^.[^:#\[\.,]*$/,
        dt = Array.prototype.slice,
        pt = $.expr.match.POS,
        ht = {
            children: !0,
            contents: !0,
            next: !0,
            prev: !0
        };
    $.fn.extend({
        find: function(e) {
            var t, n, r = this;
            if ("string" != typeof e) return $(e)
                .filter(function() {
                    for (t = 0, n = r.length; n > t; t++)
                        if ($.contains(r[t], this)) return !
                            0
                });
            var i, o, a, s = this.pushStack("", "find", e);
            for (t = 0, n = this.length; n > t; t++)
                if (i = s.length, $.find(e, this[t], s), t > 0)
                    for (o = i; o < s.length; o++)
                        for (a = 0; i > a; a++)
                            if (s[a] === s[o]) {
                                s.splice(o--, 1);
                                break
                            }
            return s
        },
        has: function(e) {
            var t = $(e);
            return this.filter(function() {
                for (var e = 0, n = t.length; n > e; e++)
                    if ($.contains(this, t[e])) return !0
            })
        },
        not: function(e) {
            return this.pushStack(u(this, e, !1), "not", e)
        },
        filter: function(e) {
            return this.pushStack(u(this, e, !0), "filter", e)
        },
        is: function(e) {
            return !!e && ("string" == typeof e ? pt.test(e) ? $(e,
                    this.context)
                .index(this[0]) >= 0 : $.filter(e, this)
                .length > 0 : this.filter(e)
                .length > 0)
        },
        closest: function(e, t) {
            var n, r, i = [],
                o = this[0];
            if ($.isArray(e)) {
                for (var a = 1; o && o.ownerDocument && o !== t;) {
                    for (n = 0; n < e.length; n++) $(o)
                        .is(e[n]) && i.push({
                            selector: e[n],
                            elem: o,
                            level: a
                        });
                    o = o.parentNode, a++
                }
                return i
            }
            var s = pt.test(e) || "string" != typeof e ? $(e, t ||
                this.context) : 0;
            for (n = 0, r = this.length; r > n; n++)
                for (o = this[n]; o;) {
                    if (s ? s.index(o) > -1 : $.find.matchesSelector(
                        o, e)) {
                        i.push(o);
                        break
                    }
                    if (o = o.parentNode, !o || !o.ownerDocument ||
                        o === t || 11 === o.nodeType) break
                }
            return i = i.length > 1 ? $.unique(i) : i, this.pushStack(
                i, "closest", e)
        },
        index: function(e) {
            return e ? "string" == typeof e ? $.inArray(this[0], $(
                    e)) : $.inArray(e.jquery ? e[0] : e, this) :
                this[0] && this[0].parentNode ? this.prevAll()
                .length : -1
        },
        add: function(e, t) {
            var n = "string" == typeof e ? $(e, t) : $.makeArray(e &&
                    e.nodeType ? [e] : e),
                r = $.merge(this.get(), n);
            return this.pushStack(l(n[0]) || l(r[0]) ? r : $.unique(
                r))
        },
        andSelf: function() {
            return this.add(this.prevObject)
        }
    }), $.each({
        parent: function(e) {
            var t = e.parentNode;
            return t && 11 !== t.nodeType ? t : null
        },
        parents: function(e) {
            return $.dir(e, "parentNode")
        },
        parentsUntil: function(e, t, n) {
            return $.dir(e, "parentNode", n)
        },
        next: function(e) {
            return $.nth(e, 2, "nextSibling")
        },
        prev: function(e) {
            return $.nth(e, 2, "previousSibling")
        },
        nextAll: function(e) {
            return $.dir(e, "nextSibling")
        },
        prevAll: function(e) {
            return $.dir(e, "previousSibling")
        },
        nextUntil: function(e, t, n) {
            return $.dir(e, "nextSibling", n)
        },
        prevUntil: function(e, t, n) {
            return $.dir(e, "previousSibling", n)
        },
        siblings: function(e) {
            return $.sibling(e.parentNode.firstChild, e)
        },
        children: function(e) {
            return $.sibling(e.firstChild)
        },
        contents: function(e) {
            return $.nodeName(e, "iframe") ? e.contentDocument || e
                .contentWindow.document : $.makeArray(e.childNodes)
        }
    }, function(e, t) {
        $.fn[e] = function(n, r) {
            var i = $.map(this, t, n);
            return lt.test(e) || (r = n), r && "string" == typeof r &&
                (i = $.filter(r, i)), i = this.length > 1 && !ht[e] ?
                $.unique(i) : i, (this.length > 1 || ct.test(r)) &&
                ut.test(e) && (i = i.reverse()), this.pushStack(i,
                    e, dt.call(arguments)
                    .join(","))
        }
    }), $.extend({
        filter: function(e, t, n) {
            return n && (e = ":not(" + e + ")"), 1 === t.length ? $
                .find.matchesSelector(t[0], e) ? [t[0]] : [] : $.find
                .matches(e, t)
        },
        dir: function(e, n, r) {
            for (var i = [], o = e[n]; o && 9 !== o.nodeType && (r ===
                t || 1 !== o.nodeType || !$(o)
                .is(r));) 1 === o.nodeType && i.push(o), o = o[n];
            return i
        },
        nth: function(e, t, n) {
            t = t || 1;
            for (var r = 0; e && (1 !== e.nodeType || ++r !== t); e =
                e[n]);
            return e
        },
        sibling: function(e, t) {
            for (var n = []; e; e = e.nextSibling) 1 === e.nodeType &&
                e !== t && n.push(e);
            return n
        }
    });
    var gt =
        "abbr|article|aside|audio|canvas|datalist|details|figcaption|figure|footer|header|hgroup|mark|meter|nav|output|progress|section|summary|time|video",
        mt = / jQuery\d+="(?:\d+|null)"/g,
        yt = /^\s+/,
        vt =
        /<(?!area|br|col|embed|hr|img|input|link|meta|param)(([\w:]+)[^>]*)\/>/gi,
        bt = /<([\w:]+)/,
        xt = /<tbody/i,
        wt = /<|&#?\w+;/,
        Tt = /<(?:script|style)/i,
        Nt = /<(?:script|object|embed|option|style)/i,
        Ct = new RegExp("<(?:" + gt + ")", "i"),
        St = /checked\s*(?:[^=]|=\s*.checked.)/i,
        kt = /\/(java|ecma)script/i,
        Et = /^\s*<!(?:\[CDATA\[|\-\-)/,
        At = {
            option: [1, "<select multiple='multiple'>", "</select>"],
            legend: [1, "<fieldset>", "</fieldset>"],
            thead: [1, "<table>", "</table>"],
            tr: [2, "<table><tbody>", "</tbody></table>"],
            td: [3, "<table><tbody><tr>", "</tr></tbody></table>"],
            col: [2, "<table><tbody></tbody><colgroup>", "</colgroup></table>"],
            area: [1, "<map>", "</map>"],
            _default: [0, "", ""]
        },
        jt = c(D);
    At.optgroup = At.option, At.tbody = At.tfoot = At.colgroup = At.caption =
        At.thead, At.th = At.td, $.support.htmlSerialize || (At._default = [1,
            "div<div>", "</div>"
        ]), $.fn.extend({
            text: function(e) {
                return $.isFunction(e) ? this.each(function(t) {
                        var n = $(this);
                        n.text(e.call(this, t, n.text()))
                    }) : "object" != typeof e && e !== t ? this.empty()
                    .append((this[0] && this[0].ownerDocument || D)
                        .createTextNode(e)) : $.text(this)
            },
            wrapAll: function(e) {
                if ($.isFunction(e)) return this.each(function(t) {
                    $(this)
                        .wrapAll(e.call(this, t))
                });
                if (this[0]) {
                    var t = $(e, this[0].ownerDocument)
                        .eq(0)
                        .clone(!0);
                    this[0].parentNode && t.insertBefore(this[0]), t.map(
                            function() {
                                for (var e = this; e.firstChild && 1 ===
                                    e.firstChild.nodeType;) e = e.firstChild;
                                return e
                            })
                        .append(this)
                }
                return this
            },
            wrapInner: function(e) {
                return this.each($.isFunction(e) ? function(t) {
                    $(this)
                        .wrapInner(e.call(this, t))
                } : function() {
                    var t = $(this),
                        n = t.contents();
                    n.length ? n.wrapAll(e) : t.append(e)
                })
            },
            wrap: function(e) {
                var t = $.isFunction(e);
                return this.each(function(n) {
                    $(this)
                        .wrapAll(t ? e.call(this, n) : e)
                })
            },
            unwrap: function() {
                return this.parent()
                    .each(function() {
                        $.nodeName(this, "body") || $(this)
                            .replaceWith(this.childNodes)
                    })
                    .end()
            },
            append: function() {
                return this.domManip(arguments, !0, function(e) {
                    1 === this.nodeType && this.appendChild(e)
                })
            },
            prepend: function() {
                return this.domManip(arguments, !0, function(e) {
                    1 === this.nodeType && this.insertBefore(e,
                        this.firstChild)
                })
            },
            before: function() {
                if (this[0] && this[0].parentNode) return this.domManip(
                    arguments, !1, function(e) {
                        this.parentNode.insertBefore(e, this)
                    });
                if (arguments.length) {
                    var e = $.clean(arguments);
                    return e.push.apply(e, this.toArray()), this.pushStack(
                        e, "before", arguments)
                }
            },
            after: function() {
                if (this[0] && this[0].parentNode) return this.domManip(
                    arguments, !1, function(e) {
                        this.parentNode.insertBefore(e, this.nextSibling)
                    });
                if (arguments.length) {
                    var e = this.pushStack(this, "after", arguments);
                    return e.push.apply(e, $.clean(arguments)), e
                }
            },
            remove: function(e, t) {
                for (var n, r = 0; null != (n = this[r]); r++)(!e || $.filter(
                        e, [n])
                    .length) && (t || 1 !== n.nodeType || ($.cleanData(
                    n.getElementsByTagName("*")), $.cleanData(
                    [n])), n.parentNode && n.parentNode.removeChild(
                    n));
                return this
            },
            empty: function() {
                for (var e, t = 0; null != (e = this[t]); t++)
                    for (1 === e.nodeType && $.cleanData(e.getElementsByTagName(
                        "*")); e.firstChild;) e.removeChild(e.firstChild);
                return this
            },
            clone: function(e, t) {
                return e = null == e ? !1 : e, t = null == t ? e : t,
                    this.map(function() {
                        return $.clone(this, e, t)
                    })
            },
            html: function(e) {
                if (e === t) return this[0] && 1 === this[0].nodeType ?
                    this[0].innerHTML.replace(mt, "") : null;
                if ("string" != typeof e || Tt.test(e) || !$.support.leadingWhitespace &&
                    yt.test(e) || At[(bt.exec(e) || ["", ""])[1].toLowerCase()]
                ) $.isFunction(e) ? this.each(function(t) {
                        var n = $(this);
                        n.html(e.call(this, t, n.html()))
                    }) : this.empty()
                    .append(e);
                else {
                    e = e.replace(vt, "<$1></$2>");
                    try {
                        for (var n = 0, r = this.length; r > n; n++) 1 ===
                            this[n].nodeType && ($.cleanData(this[n].getElementsByTagName(
                                "*")), this[n].innerHTML = e)
                    } catch (i) {
                        this.empty()
                            .append(e)
                    }
                }
                return this
            },
            replaceWith: function(e) {
                return this[0] && this[0].parentNode ? $.isFunction(e) ?
                    this.each(function(t) {
                        var n = $(this),
                            r = n.html();
                        n.replaceWith(e.call(this, t, r))
                    }) : ("string" != typeof e && (e = $(e)
                        .detach()), this.each(function() {
                        var t = this.nextSibling,
                            n = this.parentNode;
                        $(this)
                            .remove(), t ? $(t)
                            .before(e) : $(n)
                            .append(e)
                    })) : this.length ? this.pushStack($($.isFunction(e) ?
                        e() : e), "replaceWith", e) : this
            },
            detach: function(e) {
                return this.remove(e, !0)
            },
            domManip: function(e, n, r) {
                var i, o, a, s, l = e[0],
                    u = [];
                if (!$.support.checkClone && 3 === arguments.length &&
                    "string" == typeof l && St.test(l)) return this.each(
                    function() {
                        $(this)
                            .domManip(e, n, r, !0)
                    });
                if ($.isFunction(l)) return this.each(function(i) {
                    var o = $(this);
                    e[0] = l.call(this, i, n ? o.html() : t),
                        o.domManip(e, n, r)
                });
                if (this[0]) {
                    if (s = l && l.parentNode, i = $.support.parentNode &&
                        s && 11 === s.nodeType && s.childNodes.length ===
                        this.length ? {
                            fragment: s
                        } : $.buildFragment(e, this, u), a = i.fragment,
                        o = 1 === a.childNodes.length ? a = a.firstChild :
                        a.firstChild) {
                        n = n && $.nodeName(o, "tr");
                        for (var c = 0, d = this.length, p = d - 1; d >
                            c; c++) r.call(n ? f(this[c], o) : this[c],
                            i.cacheable || d > 1 && p > c ? $.clone(
                                a, !0, !0) : a)
                    }
                    u.length && $.each(u, v)
                }
                return this
            }
        }), $.buildFragment = function(e, t, n) {
            var r, i, o, a, s = e[0];
            return t && t[0] && (a = t[0].ownerDocument || t[0]), a.createDocumentFragment ||
                (a = D), !(1 === e.length && "string" == typeof s && s.length <
                    512 && a === D && "<" === s.charAt(0)) || Nt.test(s) || !$.support
                .checkClone && St.test(s) || !$.support.html5Clone && Ct.test(s) ||
                (i = !0, o = $.fragments[s], o && 1 !== o && (r = o)), r || (r =
                    a.createDocumentFragment(), $.clean(e, a, r, n)), i && ($.fragments[
                    s] = o ? r : 1), {
                    fragment: r,
                    cacheable: i
                }
        }, $.fragments = {}, $.each({
            appendTo: "append",
            prependTo: "prepend",
            insertBefore: "before",
            insertAfter: "after",
            replaceAll: "replaceWith"
        }, function(e, t) {
            $.fn[e] = function(n) {
                var r = [],
                    i = $(n),
                    o = 1 === this.length && this[0].parentNode;
                if (o && 11 === o.nodeType && 1 === o.childNodes.length &&
                    1 === i.length) return i[t](this[0]), this;
                for (var a = 0, s = i.length; s > a; a++) {
                    var l = (a > 0 ? this.clone(!0) : this)
                        .get();
                    $(i[a])[t](l), r = r.concat(l)
                }
                return this.pushStack(r, e, i.selector)
            }
        }), $.extend({
            clone: function(e, t, n) {
                var r, i, o, a = $.support.html5Clone || !Ct.test("<" +
                    e.nodeName) ? e.cloneNode(!0) : y(e);
                if (!($.support.noCloneEvent && $.support.noCloneChecked ||
                    1 !== e.nodeType && 11 !== e.nodeType || $.isXMLDoc(
                        e)))
                    for (p(e, a), r = h(e), i = h(a), o = 0; r[o]; ++o)
                        i[o] && p(r[o], i[o]);
                if (t && (d(e, a), n))
                    for (r = h(e), i = h(a), o = 0; r[o]; ++o) d(r[o],
                        i[o]);
                return r = i = null, a
            },
            clean: function(e, t, n, r) {
                var i;
                t = t || D, "undefined" == typeof t.createElement && (t =
                    t.ownerDocument || t[0] && t[0].ownerDocument ||
                    D);
                for (var o, a, s = [], l = 0; null != (a = e[l]); l++)
                    if ("number" == typeof a && (a += ""), a) {
                        if ("string" == typeof a)
                            if (wt.test(a)) {
                                a = a.replace(vt, "<$1></$2>");
                                var u = (bt.exec(a) || ["", ""])[1].toLowerCase(),
                                    f = At[u] || At._default,
                                    d = f[0],
                                    p = t.createElement("div");
                                for (t === D ? jt.appendChild(p) : c(t)
                                    .appendChild(p), p.innerHTML = f[1] +
                                    a + f[2]; d--;) p = p.lastChild;
                                if (!$.support.tbody) {
                                    var h = xt.test(a),
                                        g = "table" !== u || h ?
                                        "<table>" !== f[1] || h ? [] :
                                        p.childNodes : p.firstChild &&
                                        p.firstChild.childNodes;
                                    for (o = g.length - 1; o >= 0; --o)
                                        $.nodeName(g[o], "tbody") && !g[
                                            o].childNodes.length && g[o]
                                        .parentNode.removeChild(g[o])
                                }!$.support.leadingWhitespace && yt.test(
                                        a) && p.insertBefore(t.createTextNode(
                                        yt.exec(a)[0]), p.firstChild),
                                    a = p.childNodes
                            } else a = t.createTextNode(a);
                        var y;
                        if (!$.support.appendChecked)
                            if (a[0] && "number" == typeof(y = a.length))
                                for (o = 0; y > o; o++) m(a[o]);
                            else m(a);
                        a.nodeType ? s.push(a) : s = $.merge(s, a)
                    }
                if (n)
                    for (i = function(e) {
                        return !e.type || kt.test(e.type)
                    }, l = 0; s[l]; l++)
                        if (!r || !$.nodeName(s[l], "script") || s[l].type &&
                            "text/javascript" !== s[l].type.toLowerCase()
                        ) {
                            if (1 === s[l].nodeType) {
                                var v = $.grep(s[l].getElementsByTagName(
                                    "script"), i);
                                s.splice.apply(s, [l + 1, 0].concat(v))
                            }
                            n.appendChild(s[l])
                        } else r.push(s[l].parentNode ? s[l].parentNode
                            .removeChild(s[l]) : s[l]);
                return s
            },
            cleanData: function(e) {
                for (var t, n, r, i = $.cache, o = $.event.special, a =
                    $.support.deleteExpando, s = 0; null != (r = e[
                    s]); s++)
                    if ((!r.nodeName || !$.noData[r.nodeName.toLowerCase()]) &&
                        (n = r[$.expando])) {
                        if (t = i[n], t && t.events) {
                            for (var l in t.events) o[l] ? $.event.remove(
                                r, l) : $.removeEvent(r, l, t.handle);
                            t.handle && (t.handle.elem = null)
                        }
                        a ? delete r[$.expando] : r.removeAttribute &&
                            r.removeAttribute($.expando), delete i[n]
                    }
            }
        });
    var Lt, Ft, _t, Dt = /alpha\([^)]*\)/i,
        Ot = /opacity=([^)]*)/,
        Pt = /([A-Z]|^ms)/g,
        $t = /^-?\d+(?:px)?$/i,
        Mt = /^-?\d/,
        Ht = /^([\-+])=([\-+.\de]+)/,
        Bt = {
            position: "absolute",
            visibility: "hidden",
            display: "block"
        },
        qt = ["Left", "Right"],
        It = ["Top", "Bottom"];
    $.fn.css = function(e, n) {
        return 2 === arguments.length && n === t ? this : $.access(this, e,
            n, !0, function(e, n, r) {
                return r !== t ? $.style(e, n, r) : $.css(e, n)
            })
    }, $.extend({
        cssHooks: {
            opacity: {
                get: function(e, t) {
                    if (t) {
                        var n = Lt(e, "opacity", "opacity");
                        return "" === n ? "1" : n
                    }
                    return e.style.opacity
                }
            }
        },
        cssNumber: {
            fillOpacity: !0,
            fontWeight: !0,
            lineHeight: !0,
            opacity: !0,
            orphans: !0,
            widows: !0,
            zIndex: !0,
            zoom: !0
        },
        cssProps: {
            "float": $.support.cssFloat ? "cssFloat" : "styleFloat"
        },
        style: function(e, n, r, i) {
            if (e && 3 !== e.nodeType && 8 !== e.nodeType && e.style) {
                var o, a, s = $.camelCase(n),
                    l = e.style,
                    u = $.cssHooks[s];
                if (n = $.cssProps[s] || s, r === t) return u &&
                    "get" in u && (o = u.get(e, !1, i)) !== t ?
                    o : l[n];
                if (a = typeof r, "string" === a && (o = Ht.exec(r)) &&
                    (r = +(o[1] + 1) * +o[2] + parseFloat($.css(e,
                        n)), a = "number"), !(null == r || "number" ===
                        a && isNaN(r) || ("number" !== a || $.cssNumber[
                                s] || (r += "px"), u && "set" in u &&
                            (r = u.set(e, r)) === t))) try {
                    l[n] = r
                } catch (c) {}
            }
        },
        css: function(e, n, r) {
            var i, o;
            return n = $.camelCase(n), o = $.cssHooks[n], n = $.cssProps[
                    n] || n, "cssFloat" === n && (n = "float"), o &&
                "get" in o && (i = o.get(e, !0, r)) !== t ? i : Lt ?
                Lt(e, n) : void 0
        },
        swap: function(e, t, n) {
            var r = {};
            for (var i in t) r[i] = e.style[i], e.style[i] = t[i];
            n.call(e);
            for (i in t) e.style[i] = r[i]
        }
    }), $.curCSS = $.css, $.each(["height", "width"], function(e, t) {
        $.cssHooks[t] = {
            get: function(e, n, r) {
                var i;
                return n ? 0 !== e.offsetWidth ? b(e, t, r) : (
                    $.swap(e, Bt, function() {
                        i = b(e, t, r)
                    }), i) : void 0
            },
            set: function(e, t) {
                return $t.test(t) ? (t = parseFloat(t), t >= 0 ?
                    t + "px" : void 0) : t
            }
        }
    }), $.support.opacity || ($.cssHooks.opacity = {
        get: function(e, t) {
            return Ot.test((t && e.currentStyle ? e.currentStyle.filter :
                    e.style.filter) || "") ? parseFloat(RegExp.$1) /
                100 + "" : t ? "1" : ""
        },
        set: function(e, t) {
            var n = e.style,
                r = e.currentStyle,
                i = $.isNumeric(t) ? "alpha(opacity=" + 100 * t +
                ")" : "",
                o = r && r.filter || n.filter || "";
            n.zoom = 1, t >= 1 && "" === $.trim(o.replace(Dt, "")) &&
                (n.removeAttribute("filter"), r && !r.filter) || (n
                    .filter = Dt.test(o) ? o.replace(Dt, i) : o +
                    " " + i)
        }
    }), $(function() {
        $.support.reliableMarginRight || ($.cssHooks.marginRight = {
            get: function(e, t) {
                var n;
                return $.swap(e, {
                    display: "inline-block"
                }, function() {
                    n = t ? Lt(e, "margin-right",
                            "marginRight") : e.style
                        .marginRight
                }), n
            }
        })
    }), D.defaultView && D.defaultView.getComputedStyle && (Ft = function(e,
        t) {
        var n, r, i;
        return t = t.replace(Pt, "-$1")
            .toLowerCase(), (r = e.ownerDocument.defaultView) && (i = r
                .getComputedStyle(e, null)) && (n = i.getPropertyValue(
                t), "" !== n || $.contains(e.ownerDocument.documentElement,
                e) || (n = $.style(e, t))), n
    }), D.documentElement.currentStyle && (_t = function(e, t) {
        var n, r, i, o = e.currentStyle && e.currentStyle[t],
            a = e.style;
        return null === o && a && (i = a[t]) && (o = i), !$t.test(o) &&
            Mt.test(o) && (n = a.left, r = e.runtimeStyle && e.runtimeStyle
                .left, r && (e.runtimeStyle.left = e.currentStyle.left),
                a.left = "fontSize" === t ? "1em" : o || 0, o = a.pixelLeft +
                "px", a.left = n, r && (e.runtimeStyle.left = r)), "" ===
            o ? "auto" : o
    }), Lt = Ft || _t, $.expr && $.expr.filters && ($.expr.filters.hidden =
        function(e) {
            var t = e.offsetWidth,
                n = e.offsetHeight;
            return 0 === t && 0 === n || !$.support.reliableHiddenOffsets &&
                "none" === (e.style && e.style.display || $.css(e,
                    "display"))
        }, $.expr.filters.visible = function(e) {
            return !$.expr.filters.hidden(e)
        });
    var Rt, Wt, Jt = /%20/g,
        zt = /\[\]$/,
        Qt = /\r?\n/g,
        Xt = /#.*$/,
        Ut = /^(.*?):[ \t]*([^\r\n]*)\r?$/gm,
        Gt =
        /^(?:color|date|datetime|datetime-local|email|hidden|month|number|password|range|search|tel|text|time|url|week)$/i,
        Vt = /^(?:about|app|app\-storage|.+\-extension|file|res|widget):$/,
        Yt = /^(?:GET|HEAD)$/,
        Kt = /^\/\//,
        Zt = /\?/,
        en = /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi,
        tn = /^(?:select|textarea)/i,
        nn = /\s+/,
        rn = /([?&])_=[^&]*/,
        on = /^([\w\+\.\-]+:)(?:\/\/([^\/?#:]*)(?::(\d+))?)?/,
        an = $.fn.load,
        sn = {},
        ln = {},
        un = ["*/"] + ["*"];
    try {
        Rt = P.href
    } catch (cn) {
        Rt = D.createElement("a"), Rt.href = "", Rt = Rt.href
    }
    Wt = on.exec(Rt.toLowerCase()) || [], $.fn.extend({
        load: function(e, n, r) {
            if ("string" != typeof e && an) return an.apply(this,
                arguments);
            if (!this.length) return this;
            var i = e.indexOf(" ");
            if (i >= 0) {
                var o = e.slice(i, e.length);
                e = e.slice(0, i)
            }
            var a = "GET";
            n && ($.isFunction(n) ? (r = n, n = t) : "object" ==
                typeof n && (n = $.param(n, $.ajaxSettings.traditional),
                    a = "POST"));
            var s = this;
            return $.ajax({
                url: e,
                type: a,
                dataType: "html",
                data: n,
                complete: function(e, t, n) {
                    n = e.responseText, e.isResolved() &&
                        (e.done(function(e) {
                            n = e
                        }), s.html(o ? $("<div>")
                            .append(n.replace(en,
                                ""))
                            .find(o) : n)), r && s.each(
                            r, [n, t, e])
                }
            }), this
        },
        serialize: function() {
            return $.param(this.serializeArray())
        },
        serializeArray: function() {
            return this.map(function() {
                    return this.elements ? $.makeArray(this.elements) :
                        this
                })
                .filter(function() {
                    return this.name && !this.disabled && (this
                        .checked || tn.test(this.nodeName) ||
                        Gt.test(this.type))
                })
                .map(function(e, t) {
                    var n = $(this)
                        .val();
                    return null == n ? null : $.isArray(n) ? $.map(
                        n, function(e) {
                            return {
                                name: t.name,
                                value: e.replace(Qt, "\r\n")
                            }
                        }) : {
                        name: t.name,
                        value: n.replace(Qt, "\r\n")
                    }
                })
                .get()
        }
    }), $.each(
        "ajaxStart ajaxStop ajaxComplete ajaxError ajaxSuccess ajaxSend".split(
            " "), function(e, t) {
            $.fn[t] = function(e) {
                return this.on(t, e)
            }
        }), $.each(["get", "post"], function(e, n) {
        $[n] = function(e, r, i, o) {
            return $.isFunction(r) && (o = o || i, i = r, r = t), $
                .ajax({
                    type: n,
                    url: e,
                    data: r,
                    success: i,
                    dataType: o
                })
        }
    }), $.extend({
        getScript: function(e, n) {
            return $.get(e, t, n, "script")
        },
        getJSON: function(e, t, n) {
            return $.get(e, t, n, "json")
        },
        ajaxSetup: function(e, t) {
            return t ? T(e, $.ajaxSettings) : (t = e, e = $.ajaxSettings),
                T(e, t), e
        },
        ajaxSettings: {
            url: Rt,
            isLocal: Vt.test(Wt[1]),
            global: !0,
            type: "GET",
            contentType: "application/x-www-form-urlencoded",
            processData: !0,
            async: !0,
            accepts: {
                xml: "application/xml, text/xml",
                html: "text/html",
                text: "text/plain",
                json: "application/json, text/javascript",
                "*": un
            },
            contents: {
                xml: /xml/,
                html: /html/,
                json: /json/
            },
            responseFields: {
                xml: "responseXML",
                text: "responseText"
            },
            converters: {
                "* text": e.String,
                "text html": !0,
                "text json": $.parseJSON,
                "text xml": $.parseXML
            },
            flatOptions: {
                context: !0,
                url: !0
            }
        },
        ajaxPrefilter: x(sn),
        ajaxTransport: x(ln),
        ajax: function(e, n) {
            function r(e, n, r, a) {
                if (2 !== x) {
                    x = 2, l && clearTimeout(l), s = t, o = a ||
                        "", T.readyState = e > 0 ? 4 : 0;
                    var u, f, v, b, w, N = n,
                        k = r ? C(d, T, r) : t;
                    if (e >= 200 && 300 > e || 304 === e)
                        if (d.ifModified && ((b = T.getResponseHeader(
                                "Last-Modified")) && ($.lastModified[
                                i] = b), (w = T.getResponseHeader(
                                "Etag")) && ($.etag[i] = w)),
                            304 === e) N = "notmodified", u = !
                            0;
                        else try {
                            f = S(d, k), N = "success", u = !
                                0
                        } catch (E) {
                            N = "parsererror", v = E
                        } else v = N, (!N || e) && (N =
                            "error", 0 > e && (e = 0));
                    T.status = e, T.statusText = "" + (n || N),
                        u ? g.resolveWith(p, [f, N, T]) : g.rejectWith(
                            p, [T, N, v]), T.statusCode(y), y =
                        t, c && h.trigger("ajax" + (u ?
                            "Success" : "Error"), [T, d, u ?
                            f : v
                        ]), m.fireWith(p, [T, N]), c && (h.trigger(
                                "ajaxComplete", [T, d]), --$.active ||
                            $.event.trigger("ajaxStop"))
                }
            }
            "object" == typeof e && (n = e, e = t), n = n || {};
            var i, o, a, s, l, u, c, f, d = $.ajaxSetup({}, n),
                p = d.context || d,
                h = p !== d && (p.nodeType || p instanceof $) ? $(p) :
                $.event,
                g = $.Deferred(),
                m = $.Callbacks("once memory"),
                y = d.statusCode || {},
                v = {},
                b = {},
                x = 0,
                T = {
                    readyState: 0,
                    setRequestHeader: function(e, t) {
                        if (!x) {
                            var n = e.toLowerCase();
                            e = b[n] = b[n] || e, v[e] = t
                        }
                        return this
                    },
                    getAllResponseHeaders: function() {
                        return 2 === x ? o : null
                    },
                    getResponseHeader: function(e) {
                        var n;
                        if (2 === x) {
                            if (!a)
                                for (a = {}; n = Ut.exec(o);) a[
                                    n[1].toLowerCase()] = n[
                                    2];
                            n = a[e.toLowerCase()]
                        }
                        return n === t ? null : n
                    },
                    overrideMimeType: function(e) {
                        return x || (d.mimeType = e), this
                    },
                    abort: function(e) {
                        return e = e || "abort", s && s.abort(e),
                            r(0, e), this
                    }
                };
            if (g.promise(T), T.success = T.done, T.error = T.fail,
                T.complete = m.add, T.statusCode = function(e) {
                    if (e) {
                        var t;
                        if (2 > x)
                            for (t in e) y[t] = [y[t], e[t]];
                        else t = e[T.status], T.then(t, t)
                    }
                    return this
                }, d.url = ((e || d.url) + "")
                .replace(Xt, "")
                .replace(Kt, Wt[1] + "//"), d.dataTypes = $.trim(d.dataType ||
                    "*")
                .toLowerCase()
                .split(nn), null == d.crossDomain && (u = on.exec(d
                    .url.toLowerCase()), d.crossDomain = !(!u ||
                    u[1] == Wt[1] && u[2] == Wt[2] && (u[3] ||
                        ("http:" === u[1] ? 80 : 443)) == (Wt[3] ||
                        ("http:" === Wt[1] ? 80 : 443)))), d.data &&
                d.processData && "string" != typeof d.data && (d.data =
                    $.param(d.data, d.traditional)), w(sn, d, n, T),
                2 === x) return !1;
            if (c = d.global, d.type = d.type.toUpperCase(), d.hasContent = !
                Yt.test(d.type), c && 0 === $.active++ && $.event.trigger(
                    "ajaxStart"), !d.hasContent && (d.data && (d.url +=
                    (Zt.test(d.url) ? "&" : "?") + d.data,
                    delete d.data), i = d.url, d.cache === !1)) {
                var N = $.now(),
                    k = d.url.replace(rn, "$1_=" + N);
                d.url = k + (k === d.url ? (Zt.test(d.url) ? "&" :
                    "?") + "_=" + N : "")
            }(d.data && d.hasContent && d.contentType !== !1 || n.contentType) &&
            T.setRequestHeader("Content-Type", d.contentType), d.ifModified &&
                (i = i || d.url, $.lastModified[i] && T.setRequestHeader(
                    "If-Modified-Since", $.lastModified[i]), $.etag[
                    i] && T.setRequestHeader("If-None-Match", $
                    .etag[i])), T.setRequestHeader("Accept", d.dataTypes[
                    0] && d.accepts[d.dataTypes[0]] ? d.accepts[
                    d.dataTypes[0]] + ("*" !== d.dataTypes[0] ?
                    ", " + un + "; q=0.01" : "") : d.accepts[
                    "*"]);
            for (f in d.headers) T.setRequestHeader(f, d.headers[f]);
            if (d.beforeSend && (d.beforeSend.call(p, T, d) === !1 ||
                2 === x)) return T.abort(), !1;
            for (f in {
                success: 1,
                error: 1,
                complete: 1
            }) T[f](d[f]);
            if (s = w(ln, d, n, T)) {
                T.readyState = 1, c && h.trigger("ajaxSend", [T, d]),
                    d.async && d.timeout > 0 && (l = setTimeout(
                        function() {
                            T.abort("timeout")
                        }, d.timeout));
                try {
                    x = 1, s.send(v, r)
                } catch (E) {
                    if (!(2 > x)) throw E;
                    r(-1, E)
                }
            } else r(-1, "No Transport");
            return T
        },
        param: function(e, n) {
            var r = [],
                i = function(e, t) {
                    t = $.isFunction(t) ? t() : t, r[r.length] =
                        encodeURIComponent(e) + "=" +
                        encodeURIComponent(t)
                };
            if (n === t && (n = $.ajaxSettings.traditional), $.isArray(
                e) || e.jquery && !$.isPlainObject(e)) $.each(e,
                function() {
                    i(this.name, this.value)
                });
            else
                for (var o in e) N(o, e[o], n, i);
            return r.join("&")
                .replace(Jt, "+")
        }
    }), $.extend({
        active: 0,
        lastModified: {},
        etag: {}
    });
    var fn = $.now(),
        dn = /(\=)\?(&|$)|\?\?/i;
    $.ajaxSetup({
        jsonp: "callback",
        jsonpCallback: function() {
            return $.expando + "_" + fn++
        }
    }), $.ajaxPrefilter("json jsonp", function(t, n, r) {
        var i = "application/x-www-form-urlencoded" === t.contentType &&
            "string" == typeof t.data;
        if ("jsonp" === t.dataTypes[0] || t.jsonp !== !1 && (dn.test(t.url) ||
            i && dn.test(t.data))) {
            var o, a = t.jsonpCallback = $.isFunction(t.jsonpCallback) ?
                t.jsonpCallback() : t.jsonpCallback,
                s = e[a],
                l = t.url,
                u = t.data,
                c = "$1" + a + "$2";
            return t.jsonp !== !1 && (l = l.replace(dn, c), t.url === l &&
                (i && (u = u.replace(dn, c)), t.data === u && (l +=
                    (/\?/.test(l) ? "&" : "?") + t.jsonp + "=" +
                    a))), t.url = l, t.data = u, e[a] = function(e) {
                o = [e]
            }, r.always(function() {
                e[a] = s, o && $.isFunction(s) && e[a](o[0])
            }), t.converters["script json"] = function() {
                return o || $.error(a + " was not called"), o[0]
            }, t.dataTypes[0] = "json", "script"
        }
    }), $.ajaxSetup({
        accepts: {
            script: "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript"
        },
        contents: {
            script: /javascript|ecmascript/
        },
        converters: {
            "text script": function(e) {
                return $.globalEval(e), e
            }
        }
    }), $.ajaxPrefilter("script", function(e) {
        e.cache === t && (e.cache = !1), e.crossDomain && (e.type =
            "GET", e.global = !1)
    }), $.ajaxTransport("script", function(e) {
        if (e.crossDomain) {
            var n, r = D.head || D.getElementsByTagName("head")[0] || D
                .documentElement;
            return {
                send: function(i, o) {
                    n = D.createElement("script"), n.async =
                        "async", e.scriptCharset && (n.charset = e.scriptCharset),
                        n.src = e.url, n.onload = n.onreadystatechange =
                        function(e, i) {
                            (i || !n.readyState ||
                                /loaded|complete/.test(n.readyState)
                            ) && (n.onload = n.onreadystatechange =
                                null, r && n.parentNode && r.removeChild(
                                    n), n = t, i || o(200,
                                    "success"))
                        }, r.insertBefore(n, r.firstChild)
                },
                abort: function() {
                    n && n.onload(0, 1)
                }
            }
        }
    });
    var pn, hn = e.ActiveXObject ? function() {
            for (var e in pn) pn[e](0, 1)
        } : !1,
        gn = 0;
    $.ajaxSettings.xhr = e.ActiveXObject ? function() {
            return !this.isLocal && k() || E()
        } : k,
        function(e) {
            $.extend($.support, {
                ajax: !!e,
                cors: !!e && "withCredentials" in e
            })
        }($.ajaxSettings.xhr()), $.support.ajax && $.ajaxTransport(function(n) {
            if (!n.crossDomain || $.support.cors) {
                var r;
                return {
                    send: function(i, o) {
                        var a, s, l = n.xhr();
                        if (n.username ? l.open(n.type, n.url, n.async,
                            n.username, n.password) : l.open(n.type,
                            n.url, n.async), n.xhrFields)
                            for (s in n.xhrFields) l[s] = n.xhrFields[s];
                        n.mimeType && l.overrideMimeType && l.overrideMimeType(
                            n.mimeType), n.crossDomain || i[
                            "X-Requested-With"] || (i[
                                "X-Requested-With"] =
                            "XMLHttpRequest");
                        try {
                            for (s in i) l.setRequestHeader(s, i[s])
                        } catch (u) {}
                        l.send(n.hasContent && n.data || null), r =
                            function(e, i) {
                                var s, u, c, f, d;
                                try {
                                    if (r && (i || 4 === l.readyState))
                                        if (r = t, a && (l.onreadystatechange =
                                            $.noop, hn && delete pn[
                                                a]), i) 4 !== l.readyState &&
                                            l.abort();
                                        else {
                                            s = l.status, c = l.getAllResponseHeaders(),
                                                f = {}, d = l.responseXML,
                                                d && d.documentElement &&
                                                (f.xml = d), f.text = l
                                                .responseText;
                                            try {
                                                u = l.statusText
                                            } catch (p) {
                                                u = ""
                                            }
                                            s || !n.isLocal || n.crossDomain ?
                                                1223 === s && (s = 204) :
                                                s = f.text ? 200 : 404
                                        }
                                } catch (h) {
                                    i || o(-1, h)
                                }
                                f && o(s, u, f, c)
                            }, n.async && 4 !== l.readyState ? (a = ++
                                gn, hn && (pn || (pn = {}, $(e)
                                    .unload(hn)), pn[a] = r), l.onreadystatechange =
                                r) : r()
                    },
                    abort: function() {
                        r && r(0, 1)
                    }
                }
            }
        });
    var mn, yn, vn, bn, xn = {},
        wn = /^(?:toggle|show|hide)$/,
        Tn = /^([+\-]=)?([\d+.\-]+)([a-z%]*)$/i,
        Nn = [
            ["height", "marginTop", "marginBottom", "paddingTop",
                "paddingBottom"
            ],
            ["width", "marginLeft", "marginRight", "paddingLeft",
                "paddingRight"
            ],
            ["opacity"]
        ];
    $.fn.extend({
        show: function(e, t, n) {
            var r, i;
            if (e || 0 === e) return this.animate(L("show", 3), e,
                t, n);
            for (var o = 0, a = this.length; a > o; o++) r = this[o],
                r.style && (i = r.style.display, $._data(r,
                    "olddisplay") || "none" !== i || (i = r.style
                    .display = ""), "" === i && "none" === $.css(
                    r, "display") && $._data(r, "olddisplay", F(
                    r.nodeName)));
            for (o = 0; a > o; o++) r = this[o], r.style && (i = r.style
                .display, ("" === i || "none" === i) && (r.style
                    .display = $._data(r, "olddisplay") || ""));
            return this
        },
        hide: function(e, t, n) {
            if (e || 0 === e) return this.animate(L("hide", 3), e,
                t, n);
            for (var r, i, o = 0, a = this.length; a > o; o++) r =
                this[o], r.style && (i = $.css(r, "display"),
                    "none" === i || $._data(r, "olddisplay") || $._data(
                        r, "olddisplay", i));
            for (o = 0; a > o; o++) this[o].style && (this[o].style
                .display = "none");
            return this
        },
        _toggle: $.fn.toggle,
        toggle: function(e, t, n) {
            var r = "boolean" == typeof e;
            return $.isFunction(e) && $.isFunction(t) ? this._toggle
                .apply(this, arguments) : null == e || r ? this.each(
                    function() {
                        var t = r ? e : $(this)
                            .is(":hidden");
                        $(this)[t ? "show" : "hide"]()
                    }) : this.animate(L("toggle", 3), e, t, n),
                this
        },
        fadeTo: function(e, t, n, r) {
            return this.filter(":hidden")
                .css("opacity", 0)
                .show()
                .end()
                .animate({
                    opacity: t
                }, e, n, r)
        },
        animate: function(e, t, n, r) {
            function i() {
                o.queue === !1 && $._mark(this);
                var t, n, r, i, a, s, l, u, c, f = $.extend({},
                        o),
                    d = 1 === this.nodeType,
                    p = d && $(this)
                    .is(":hidden");
                f.animatedProperties = {};
                for (r in e) {
                    if (t = $.camelCase(r), r !== t && (e[t] =
                            e[r], delete e[r]), n = e[t], $.isArray(
                            n) ? (f.animatedProperties[t] = n[1],
                            n = e[t] = n[0]) : f.animatedProperties[
                            t] = f.specialEasing && f.specialEasing[
                            t] || f.easing || "swing", "hide" ===
                        n && p || "show" === n && !p) return f.complete
                        .call(this);
                    !d || "height" !== t && "width" !== t || (f
                        .overflow = [this.style.overflow,
                            this.style.overflowX, this.style
                            .overflowY
                        ], "inline" === $.css(this,
                            "display") && "none" === $.css(
                            this, "float") && ($.support.inlineBlockNeedsLayout &&
                            "inline" !== F(this.nodeName) ?
                            this.style.zoom = 1 : this.style
                            .display = "inline-block"))
                }
                null != f.overflow && (this.style.overflow =
                    "hidden");
                for (r in e) i = new $.fx(this, f, r), n = e[r],
                    wn.test(n) ? (c = $._data(this, "toggle" +
                        r) || ("toggle" === n ? p ? "show" :
                        "hide" : 0), c ? ($._data(this,
                        "toggle" + r, "show" === c ?
                        "hide" : "show"), i[c]()) : i[n]()) : (
                        a = Tn.exec(n), s = i.cur(), a ? (l =
                            parseFloat(a[2]), u = a[3] || ($.cssNumber[
                                r] ? "" : "px"), "px" !== u &&
                            ($.style(this, r, (l || 1) + u), s =
                                (l || 1) / i.cur() * s, $.style(
                                    this, r, s + u)), a[1] && (
                                l = ("-=" === a[1] ? -1 : 1) *
                                l + s), i.custom(s, l, u)) : i.custom(
                            s, n, ""));
                return !0
            }
            var o = $.speed(t, n, r);
            return $.isEmptyObject(e) ? this.each(o.complete, [!1]) :
                (e = $.extend({}, e), o.queue === !1 ? this.each(i) :
                    this.queue(o.queue, i))
        },
        stop: function(e, n, r) {
            return "string" != typeof e && (r = n, n = e, e = t), n &&
                e !== !1 && this.queue(e || "fx", []), this.each(
                    function() {
                        function t(e, t, n) {
                            var i = t[n];
                            $.removeData(e, n, !0), i.stop(r)
                        }
                        var n, i = !1,
                            o = $.timers,
                            a = $._data(this);
                        if (r || $._unmark(!0, this), null == e)
                            for (n in a) a[n] && a[n].stop && n.indexOf(
                                ".run") === n.length - 4 && t(
                                this, a, n);
                        else a[n = e + ".run"] && a[n].stop && t(
                            this, a, n);
                        for (n = o.length; n--;) o[n].elem !== this ||
                            null != e && o[n].queue !== e || (r ? o[
                                    n](!0) : o[n].saveState(), i = !
                                0, o.splice(n, 1));
                        r && i || $.dequeue(this, e)
                    })
        }
    }), $.each({
        slideDown: L("show", 1),
        slideUp: L("hide", 1),
        slideToggle: L("toggle", 1),
        fadeIn: {
            opacity: "show"
        },
        fadeOut: {
            opacity: "hide"
        },
        fadeToggle: {
            opacity: "toggle"
        }
    }, function(e, t) {
        $.fn[e] = function(e, n, r) {
            return this.animate(t, e, n, r)
        }
    }), $.extend({
        speed: function(e, t, n) {
            var r = e && "object" == typeof e ? $.extend({}, e) : {
                complete: n || !n && t || $.isFunction(e) && e,
                duration: e,
                easing: n && t || t && !$.isFunction(t) && t
            };
            return r.duration = $.fx.off ? 0 : "number" == typeof r
                .duration ? r.duration : r.duration in $.fx.speeds ?
                $.fx.speeds[r.duration] : $.fx.speeds._default, (
                    null == r.queue || r.queue === !0) && (r.queue =
                    "fx"), r.old = r.complete, r.complete =
                function(e) {
                    $.isFunction(r.old) && r.old.call(this), r.queue ?
                        $.dequeue(this, r.queue) : e !== !1 && $._unmark(
                            this)
                }, r
        },
        easing: {
            linear: function(e, t, n, r) {
                return n + r * e
            },
            swing: function(e, t, n, r) {
                return (-Math.cos(e * Math.PI) / 2 + .5) * r + n
            }
        },
        timers: [],
        fx: function(e, t, n) {
            this.options = t, this.elem = e, this.prop = n, t.orig =
                t.orig || {}
        }
    }), $.fx.prototype = {
        update: function() {
            this.options.step && this.options.step.call(this.elem, this
                    .now, this), ($.fx.step[this.prop] || $.fx.step._default)
                (this)
        },
        cur: function() {
            if (null != this.elem[this.prop] && (!this.elem.style ||
                null == this.elem.style[this.prop])) return this.elem[
                this.prop];
            var e, t = $.css(this.elem, this.prop);
            return isNaN(e = parseFloat(t)) ? t && "auto" !== t ? t : 0 :
                e
        },
        custom: function(e, n, r) {
            function i(e) {
                return o.step(e)
            }
            var o = this,
                a = $.fx;
            this.startTime = bn || A(), this.end = n, this.now = this.start =
                e, this.pos = this.state = 0, this.unit = r || this.unit ||
                ($.cssNumber[this.prop] ? "" : "px"), i.queue = this.options
                .queue, i.elem = this.elem, i.saveState = function() {
                    o.options.hide && $._data(o.elem, "fxshow" + o.prop) ===
                        t && $._data(o.elem, "fxshow" + o.prop, o.start)
                }, i() && $.timers.push(i) && !vn && (vn = setInterval(
                    a.tick, a.interval))
        },
        show: function() {
            var e = $._data(this.elem, "fxshow" + this.prop);
            this.options.orig[this.prop] = e || $.style(this.elem, this
                    .prop), this.options.show = !0, e !== t ? this.custom(
                    this.cur(), e) : this.custom("width" === this.prop ||
                    "height" === this.prop ? 1 : 0, this.cur()), $(this
                    .elem)
                .show()
        },
        hide: function() {
            this.options.orig[this.prop] = $._data(this.elem, "fxshow" +
                    this.prop) || $.style(this.elem, this.prop), this.options
                .hide = !0, this.custom(this.cur(), 0)
        },
        step: function(e) {
            var t, n, r, i = bn || A(),
                o = !0,
                a = this.elem,
                s = this.options;
            if (e || i >= s.duration + this.startTime) {
                this.now = this.end, this.pos = this.state = 1, this.update(),
                    s.animatedProperties[this.prop] = !0;
                for (t in s.animatedProperties) s.animatedProperties[t] !==
                    !0 && (o = !1);
                if (o) {
                    if (null == s.overflow || $.support.shrinkWrapBlocks ||
                        $.each(["", "X", "Y"], function(e, t) {
                            a.style["overflow" + t] = s.overflow[e]
                        }), s.hide && $(a)
                        .hide(), s.hide || s.show)
                        for (t in s.animatedProperties) $.style(a, t, s
                            .orig[t]), $.removeData(a, "fxshow" + t, !
                            0), $.removeData(a, "toggle" + t, !0);
                    r = s.complete, r && (s.complete = !1, r.call(a))
                }
                return !1
            }
            return 1 / 0 == s.duration ? this.now = i : (n = i - this.startTime,
                this.state = n / s.duration, this.pos = $.easing[s.animatedProperties[
                    this.prop]](this.state, n, 0, 1, s.duration),
                this.now = this.start + (this.end - this.start) *
                this.pos), this.update(), !0
        }
    }, $.extend($.fx, {
        tick: function() {
            for (var e, t = $.timers, n = 0; n < t.length; n++) e =
                t[n], e() || t[n] !== e || t.splice(n--, 1);
            t.length || $.fx.stop()
        },
        interval: 13,
        stop: function() {
            clearInterval(vn), vn = null
        },
        speeds: {
            slow: 600,
            fast: 200,
            _default: 400
        },
        step: {
            opacity: function(e) {
                $.style(e.elem, "opacity", e.now)
            },
            _default: function(e) {
                e.elem.style && null != e.elem.style[e.prop] ? e.elem
                    .style[e.prop] = e.now + e.unit : e.elem[e.prop] =
                    e.now
            }
        }
    }), $.each(["width", "height"], function(e, t) {
        $.fx.step[t] = function(e) {
            $.style(e.elem, t, Math.max(0, e.now) + e.unit)
        }
    }), $.expr && $.expr.filters && ($.expr.filters.animated = function(e) {
        return $.grep($.timers, function(t) {
                return e === t.elem
            })
            .length
    });
    var Cn = /^t(?:able|d|h)$/i,
        Sn = /^(?:body|html)$/i;
    $.fn.offset = "getBoundingClientRect" in D.documentElement ? function(e) {
            var t, n = this[0];
            if (e) return this.each(function(t) {
                $.offset.setOffset(this, e, t)
            });
            if (!n || !n.ownerDocument) return null;
            if (n === n.ownerDocument.body) return $.offset.bodyOffset(n);
            try {
                t = n.getBoundingClientRect()
            } catch (r) {}
            var i = n.ownerDocument,
                o = i.documentElement;
            if (!t || !$.contains(o, n)) return t ? {
                top: t.top,
                left: t.left
            } : {
                top: 0,
                left: 0
            };
            var a = i.body,
                s = _(i),
                l = o.clientTop || a.clientTop || 0,
                u = o.clientLeft || a.clientLeft || 0,
                c = s.pageYOffset || $.support.boxModel && o.scrollTop || a.scrollTop,
                f = s.pageXOffset || $.support.boxModel && o.scrollLeft || a.scrollLeft,
                d = t.top + c - l,
                p = t.left + f - u;
            return {
                top: d,
                left: p
            }
        } : function(e) {
            var t = this[0];
            if (e) return this.each(function(t) {
                $.offset.setOffset(this, e, t)
            });
            if (!t || !t.ownerDocument) return null;
            if (t === t.ownerDocument.body) return $.offset.bodyOffset(t);
            for (var n, r = t.offsetParent, i = t, o = t.ownerDocument, a = o.documentElement,
                    s = o.body, l = o.defaultView, u = l ? l.getComputedStyle(t,
                        null) : t.currentStyle, c = t.offsetTop, f = t.offsetLeft;
                (t = t.parentNode) && t !== s && t !== a && (!$.support.fixedPosition ||
                    "fixed" !== u.position);) n = l ? l.getComputedStyle(t,
                    null) : t.currentStyle, c -= t.scrollTop, f -= t.scrollLeft,
                t === r && (c += t.offsetTop, f += t.offsetLeft, !$.support.doesNotAddBorder ||
                    $.support.doesAddBorderForTableAndCells && Cn.test(t.nodeName) ||
                    (c += parseFloat(n.borderTopWidth) || 0, f += parseFloat(n.borderLeftWidth) ||
                        0), i = r, r = t.offsetParent), $.support.subtractsBorderForOverflowNotVisible &&
                "visible" !== n.overflow && (c += parseFloat(n.borderTopWidth) ||
                    0, f += parseFloat(n.borderLeftWidth) || 0), u = n;
            return ("relative" === u.position || "static" === u.position) && (c +=
                    s.offsetTop, f += s.offsetLeft), $.support.fixedPosition &&
                "fixed" === u.position && (c += Math.max(a.scrollTop, s.scrollTop),
                    f += Math.max(a.scrollLeft, s.scrollLeft)), {
                    top: c,
                    left: f
                }
        }, $.offset = {
            bodyOffset: function(e) {
                var t = e.offsetTop,
                    n = e.offsetLeft;
                return $.support.doesNotIncludeMarginInBodyOffset && (t +=
                    parseFloat($.css(e, "marginTop")) || 0, n +=
                    parseFloat($.css(e, "marginLeft")) || 0), {
                    top: t,
                    left: n
                }
            },
            setOffset: function(e, t, n) {
                var r = $.css(e, "position");
                "static" === r && (e.style.position = "relative");
                var i, o, a = $(e),
                    s = a.offset(),
                    l = $.css(e, "top"),
                    u = $.css(e, "left"),
                    c = ("absolute" === r || "fixed" === r) && $.inArray(
                        "auto", [l, u]) > -1,
                    f = {},
                    d = {};
                c ? (d = a.position(), i = d.top, o = d.left) : (i =
                    parseFloat(l) || 0, o = parseFloat(u) || 0), $.isFunction(
                    t) && (t = t.call(e, n, s)), null != t.top && (f.top =
                    t.top - s.top + i), null != t.left && (f.left = t.left -
                    s.left + o), "using" in t ? t.using.call(e, f) : a.css(
                    f)
            }
        }, $.fn.extend({
            position: function() {
                if (!this[0]) return null;
                var e = this[0],
                    t = this.offsetParent(),
                    n = this.offset(),
                    r = Sn.test(t[0].nodeName) ? {
                        top: 0,
                        left: 0
                    } : t.offset();
                return n.top -= parseFloat($.css(e, "marginTop")) || 0,
                    n.left -= parseFloat($.css(e, "marginLeft")) || 0,
                    r.top += parseFloat($.css(t[0], "borderTopWidth")) ||
                    0, r.left += parseFloat($.css(t[0],
                        "borderLeftWidth")) || 0, {
                        top: n.top - r.top,
                        left: n.left - r.left
                    }
            },
            offsetParent: function() {
                return this.map(function() {
                    for (var e = this.offsetParent || D.body; e &&
                        !Sn.test(e.nodeName) && "static" === $.css(
                            e, "position");) e = e.offsetParent;
                    return e
                })
            }
        }), $.each(["Left", "Top"], function(e, n) {
            var r = "scroll" + n;
            $.fn[r] = function(n) {
                var i, o;
                return n === t ? (i = this[0]) ? (o = _(i), o ?
                    "pageXOffset" in o ? o[e ? "pageYOffset" :
                        "pageXOffset"] : $.support.boxModel && o.document
                    .documentElement[r] || o.document.body[r] : i[r]
                ) : null : this.each(function() {
                    o = _(this), o ? o.scrollTo(e ? $(o)
                        .scrollLeft() : n, e ? n : $(o)
                        .scrollTop()) : this[r] = n
                })
            }
        }), $.each(["Height", "Width"], function(e, n) {
            var r = n.toLowerCase();
            $.fn["inner" + n] = function() {
                var e = this[0];
                return e ? e.style ? parseFloat($.css(e, r, "padding")) :
                    this[r]() : null
            }, $.fn["outer" + n] = function(e) {
                var t = this[0];
                return t ? t.style ? parseFloat($.css(t, r, e ?
                    "margin" : "border")) : this[r]() : null
            }, $.fn[r] = function(e) {
                var i = this[0];
                if (!i) return null == e ? null : this;
                if ($.isFunction(e)) return this.each(function(t) {
                    var n = $(this);
                    n[r](e.call(this, t, n[r]()))
                });
                if ($.isWindow(i)) {
                    var o = i.document.documentElement["client" + n],
                        a = i.document.body;
                    return "CSS1Compat" === i.document.compatMode && o ||
                        a && a["client" + n] || o
                }
                if (9 === i.nodeType) return Math.max(i.documentElement[
                    "client" + n], i.body["scroll" + n], i.documentElement[
                    "scroll" + n], i.body["offset" + n], i.documentElement[
                    "offset" + n]);
                if (e === t) {
                    var s = $.css(i, r),
                        l = parseFloat(s);
                    return $.isNumeric(l) ? l : s
                }
                return this.css(r, "string" == typeof e ? e : e + "px")
            }
        }), e.jQuery = e.$ = $, "function" == typeof define && define.amd &&
        define.amd.jQuery && define("jquery", [], function() {
            return $
        })
}(window),
function($) {
    $ = jQuery.noConflict(!0), "undefined" == typeof window.jQuery && (window.jQuery =
        $), "undefined" == typeof window.$ && (window.$ = $);
    var ATL_JQ = function() {
            return $.apply($, arguments)
        },
        css =
        ".atlwdg-blanket {background: black;height: 100%;left: 0;opacity: .5;position: fixed;top: 0;width: 100%;z-index: 1000000;}.atlwdg-popup {background: white;border: 1px solid #666;position: fixed;top: 50%;left: 50%;z-index: 10000011;}.atlwdg-popup.atlwdg-box-shadow {-moz-box-shadow: 10px 10px 20px rgba(0,0,0,0.5);-webkit-box-shadow: 10px 10px 20px rgba(0,0,0,0.5);box-shadow: 10px 10px 20px rgba(0,0,0,0.5);background-color: white;}.atlwdg-hidden {display: none;}.atlwdg-trigger {position: fixed;background: #013466;padding: 5px;border: 2px solid white;border-top: none;font-weight: bold;color: white !important;display: block;white-space: nowrap;text-decoration: none !important;font-family: arial, FreeSans, Helvetica, sans-serif;font-size: 12px;box-shadow: 5px 5px 10px rgba(0, 0, 0, 0.5);-webkit-box-shadow: 5px 5px 10px rgba(0, 0, 0, 0.5);-moz-box-shadow: 5px 5px 10px rgba(0, 0, 0, 0.5);border-radius: 0 0 5px 5px;-moz-border-radius: 0 0 5px 5px;}.atlwdg-trigger.atlwdg-TOP {left: 45%;top: 0;}.atlwdg-trigger.atlwdg-RIGHT {left: 100%;top: 40%;-webkit-transform-origin: top left;-webkit-transform: rotate(90deg);-moz-transform: rotate(90deg);-moz-transform-origin: top left;-ms-transform: rotate(90deg);-ms-transform-origin: top left;}.atlwdg-trigger.atlwdg-SUBTLE {right: 0;bottom: 0;border: 1px solid #ccc;border-bottom: none;border-right: none;background-color: #f5f5f5;color: #444 !important;font-size: 11px;padding: 6px;box-shadow: -1px -1px 2px rgba(0, 0, 0, 0.5);border-radius: 2px 0 0 0;}.atlwdg-loading {position: absolute;top: 220px;left: 295px;}@media print {.atlwdg-trigger { display: none; }}",
        cssIE =
        ".atlwdg-trigger {position: absolute;}.atlwdg-blanket {position: absolute;filter: alpha(opacity=50);width: 110%;}.atlwdg-popup {position: absolute;}.atlwdg-trigger.atlwdg-RIGHT {left: auto;right: 0;filter: progid:DXImageTransform.Microsoft.BasicImage(rotation=1);}";
    ATL_JQ.isQuirksMode = function() {
        return "CSS1Compat" != document.compatMode
    }, ATL_JQ.IssueDialog = function(options) {
        var $body = $("body"),
            that = this,
            showDialog = function() {
                return that.show(), !1
            };
        if (options.baseUrl || (options.baseUrl =
                "https://jira.transvar.org"), this.options = options, this.frameUrl =
            options.baseUrl + "/rest/collectors/1.0/template/form/" + this.options
            .collectorId + "?os_authType=none", $("head")
            .append("<style type='text/css'>" + css + "</style>"), "CUSTOM" ===
            this.options.triggerPosition) {
            var oldTriggerFunction;
            if (this.options.triggerFunction) try {
                oldTriggerFunction = eval("(" + this.options.triggerFunction +
                    ")")
            } catch (ex) {}
            $(function() {
                try {
                    var e;
                    e = window.ATL_JQ_PAGE_PROPS && (window.ATL_JQ_PAGE_PROPS
                            .triggerFunction || window.ATL_JQ_PAGE_PROPS
                            .b04cfbf9 && window.ATL_JQ_PAGE_PROPS.b04cfbf9
                            .triggerFunction) ? window.ATL_JQ_PAGE_PROPS
                        .triggerFunction || window.ATL_JQ_PAGE_PROPS
                        .b04cfbf9.triggerFunction :
                        oldTriggerFunction, $.isFunction(e) && e(
                            showDialog)
                } catch (t) {}
            })
        } else if ($.isFunction(this.options.triggerPosition)) try {
            this.options.triggerPosition(showDialog)
        } catch (ex) {} else if (this.options.triggerPosition && this.options
            .triggerText) {
            var triggerClass = "atlwdg-trigger atlwdg-" + this.options.triggerPosition,
                $trigger = $("<a href='#' id='atlwdg-trigger'/>")
                .addClass(triggerClass)
                .text(this.options.triggerText);
            $body.append($trigger), $trigger.click(showDialog)
        }
        var $iframeContainer = $("<div id='atlwdg-container'/>")
            .addClass("atlwdg-popup atlwdg-box-shadow atlwdg-hidden"),
            $blanket = $(
                "<div id='atlwdg-blanket' class='atlwdg-blanket'/>")
            .hide();
        $body.append($blanket)
            .append($iframeContainer);
        var browser = function(e) {
            e = e.toLowerCase();
            var t = /(msie) ([\w.]+)/.exec(e) || [];
            return {
                isIE: t[1] ? !0 : !1,
                version: t[2] || "0"
            }
        }(navigator.userAgent);
        if (browser.isIE && (ATL_JQ.isQuirksMode() || browser.version < 9)) {
            $("head")
                .append("<style type='text/css'>" + cssIE + "</style>");
            var triggerAdjuster = function() {};
            if ("TOP" === this.options.triggerPosition) triggerAdjuster =
                function() {
                    $("#atlwdg-trigger")
                        .css("top", $(window)
                            .scrollTop() + "px")
                };
            else if ("RIGHT" === this.options.triggerPosition)
                triggerAdjuster = function() {
                    var e = $("#atlwdg-trigger");
                    e.css("top", $(window)
                            .height() / 2 - e.outerWidth() / 2 + $(window)
                            .scrollTop() + "px"), ATL_JQ.isQuirksMode() ||
                        "8.0" !== browser.version || e.css("right", -(e.outerHeight() -
                            e.outerWidth()) + "px")
                };
            else if ("SUBTLE" === this.options.triggerPosition) {
                var outerHeight = $trigger.outerHeight();
                triggerAdjuster = function() {
                    var e = $(window);
                    $trigger.css("top", e.scrollTop() + e.height() -
                        outerHeight + "px")
                }
            }
            $(window)
                .bind("scroll resize", triggerAdjuster), triggerAdjuster()
        }
    }, ATL_JQ.IssueDialog.prototype = {
        hideDialog: void 0,
        updateContainerPosition: function() {
            var e = 810,
                t = 542;
            $("#atlwdg-container")
                .css({
                    height: t + "px",
                    width: e + "px",
                    "margin-top": -Math.round(t / 2) + "px",
                    "margin-left": -Math.round(e / 2) + "px"
                }), $("#atlwdg-frame")
                .height("100%")
                .width("100%");
        },
        show: function() {
            var e = this,
                t = $("#atlwdg-container"),
                n = $("body"),
                r = $(
                    '<iframe seamless id="atlwdg-frame" scrolling="no" frameborder="0" src="' +
                    this.frameUrl +
                    '"></iframe>'
                ),
                i = $(
                    '<img class="atlwdg-loading" style="display:none;" src="' +
                    this.options.baseUrl +
                    '/images/throbber/loading_barber_pole_horz.gif">');
            hideDialog = function(t) {
                27 === t.keyCode && e.hide()
            }, t.append(i);
            var o = setTimeout(function() {
                i.show()
            }, 300);
            n.css("overflow", "hidden")
                .keydown(hideDialog), window.scroll(0, 0);
            var a = "";
            if (this.options.collectFeedback) {
                var s = this.options.collectFeedback();
                for (var l in s) s.hasOwnProperty(l) && void 0 !== s[l] &&
                    "" !== s[l] && "string" == typeof s[l] && (a += "*" +
                        l + "*: " + s[l] + "\n")
            }
            var u = {};
            this.options.fieldValues && !$.isEmptyObject(this.options.fieldValues) &&
                $.extend(u, this.options.fieldValues), r.load(function() {
                    var t = {
                        feedbackString: a,
                        fieldValues: u
                    };
                    r[0].contentWindow.postMessage(JSON.stringify(t),
                            e.options.baseUrl), $(window)
                        .bind("message", function(t) {
                            t.originalEvent.data &&
                                "cancelFeedbackDialog" === t.originalEvent
                                .data && e.hide()
                        })
                }), r.load(function() {
                    clearTimeout(o), i.hide(), r.show()
                });
            var c = ((new Date)
                .getTime(), document.createElement("a"));
            c.href = "https://jira.transvar.org", t.append(r), this.updateContainerPosition(),
                t.show(), $("#atlwdg-blanket")
                .show()
        },
        hide: function() {
            $("body")
                .css("overflow", "auto")
                .unbind("keydown", hideDialog), $("#atlwdg-container")
                .hide()
                .empty(), $("#atlwdg-blanket")
                .hide(), console.log("calling CloseTrigger"),
                closeTrigger.hidePanel()
        }
    };
    var filterStrings = function(e, t) {
        for (var n in e)
            if (e.hasOwnProperty(n)) {
                var r = e[n];
                if (void 0 === t && $.isArray(r)) filterStrings(r, n);
                else if ("string" != typeof r) {
                    var i = void 0 === t ? n : t + ":" + n;
                    console.log(
                        "bootstrap.js:filterStrings ignoring key for value '" +
                        i + "'; typeof must be string"), delete e[n]
                }
            }
        return e
    };
    ATL_JQ(function() {
        var e = function(e, t) {
            if (e.enabled) {
                var n = !1,
                    r = {};
                if (window.ATL_JQ_PAGE_PROPS && (r = window.ATL_JQ_PAGE_PROPS
                    .fieldValues, window.ATL_JQ_PAGE_PROPS.hasOwnProperty(
                        t) && (r = window.ATL_JQ_PAGE_PROPS[t].fieldValues),
                    $.isFunction(r) ? $.extend(r, filterStrings(
                        r())) : $.isPlainObject(r) && $.extend(
                        r, filterStrings(r))), e.recordWebInfo) {
                    var i = {
                        Location: window.location.href,
                        "User-Agent": navigator.userAgent,
                        Referrer: document.referrer,
                        "Screen Resolution": screen.width +
                            " x " + screen.height
                    };
                    if (window.ATL_JQ_PAGE_PROPS) {
                        var o = window.ATL_JQ_PAGE_PROPS.environment;
                        window.ATL_JQ_PAGE_PROPS.hasOwnProperty(t) &&
                            (o = window.ATL_JQ_PAGE_PROPS[t].environment),
                            $.isFunction(o) ? $.extend(i, o()) : $.extend(
                                i, o)
                    }
                    n = function() {
                        return i
                    }
                }
                new ATL_JQ.IssueDialog({
                    collectorId: t,
                    fieldValues: r,
                    collectFeedback: n,
                    triggerText: e.triggerText,
                    triggerPosition: e.triggerPosition,
                    triggerFunction: e.triggerFunction,
                    baseUrl: e.baseUrl
                })
            }
        };
        if ("undefined" != typeof ATL_JQ_CONFIGS)
            for (var t in ATL_JQ_CONFIGS) {
                var n = ATL_JQ_CONFIGS[t];
                delete ATL_JQ_CONFIGS[t], e(n, t)
            } else {
                var r = "https://jira.transvar.org";
                $.ajax({
                    url: r +
                        "/rest/collectors/1.0/configuration/trigger/b04cfbf9?os_authType=none",
                    dataType: "jsonp",
                    crossDomain: !0,
                    jsonpCallback: "trigger_b04cfbf9",
                    cache: !0,
                    success: function(t) {
                        e(t, "b04cfbf9")
                    }
                })
            }
    })
}(jQuery);
var JSON;
JSON || (JSON = {}),
    function() {
        function f(e) {
            return 10 > e ? "0" + e : e
        }

        function quote(e) {
            return escapable.lastIndex = 0, escapable.test(e) ? '"' + e.replace(
                escapable, function(e) {
                    var t = meta[e];
                    return "string" == typeof t ? t : "\\u" + ("0000" +
                            e.charCodeAt(0)
                            .toString(16))
                        .slice(-4)
                }) + '"' : '"' + e + '"'
        }

        function str(e, t) {
            var n, r, i, o, a, s = gap,
                l = t[e];
            switch (l && "object" == typeof l && "function" == typeof l.toJSON &&
                (l = l.toJSON(e)), "function" == typeof rep && (l = rep.call(
                    t, e, l)), typeof l) {
                case "string":
                    return quote(l);
                case "number":
                    return isFinite(l) ? String(l) : "null";
                case "boolean":
                case "null":
                    return String(l);
                case "object":
                    if (!l) return "null";
                    if (gap += indent, a = [], "[object Array]" === Object.prototype
                        .toString.apply(l)) {
                        for (o = l.length, n = 0; o > n; n += 1) a[n] = str(
                            n, l) || "null";
                        return i = 0 === a.length ? "[]" : gap ? "[\n" +
                            gap + a.join(",\n" + gap) + "\n" + s + "]" :
                            "[" + a.join(",") + "]", gap = s, i
                    }
                    if (rep && "object" == typeof rep)
                        for (o = rep.length, n = 0; o > n; n += 1) "string" ==
                            typeof rep[n] && (r = rep[n], i = str(r, l), i &&
                                a.push(quote(r) + (gap ? ": " : ":") + i));
                    else
                        for (r in l) Object.prototype.hasOwnProperty.call(l,
                            r) && (i = str(r, l), i && a.push(quote(r) +
                            (gap ? ": " : ":") + i));
                    return i = 0 === a.length ? "{}" : gap ? "{\n" + gap +
                        a.join(",\n" + gap) + "\n" + s + "}" : "{" + a.join(
                            ",") + "}", gap = s, i
            }
        }
        "function" != typeof Date.prototype.toJSON && (Date.prototype.toJSON =
            function() {
                return isFinite(this.valueOf()) ? this.getUTCFullYear() +
                    "-" + f(this.getUTCMonth() + 1) + "-" + f(this.getUTCDate()) +
                    "T" + f(this.getUTCHours()) + ":" + f(this.getUTCMinutes()) +
                    ":" + f(this.getUTCSeconds()) + "Z" : null
            }, String.prototype.toJSON = Number.prototype.toJSON = Boolean.prototype
            .toJSON = function() {
                return this.valueOf()
            });
        var cx =
            /[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
            escapable =
            /[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
            gap, indent, meta = {
                "\b": "\\b",
                "	": "\\t",
                "\n": "\\n",
                "\f": "\\f",
                "\r": "\\r",
                '"': '\\"',
                "\\": "\\\\"
            },
            rep;
        "function" != typeof JSON.stringify && (JSON.stringify = function(e, t,
            n) {
            var r;
            if (gap = "", indent = "", "number" == typeof n)
                for (r = 0; n > r; r += 1) indent += " ";
            else "string" == typeof n && (indent = n); if (rep = t, t &&
                "function" != typeof t && ("object" != typeof t ||
                    "number" != typeof t.length)) throw new Error(
                "JSON.stringify");
            return str("", {
                "": e
            })
        }), "function" != typeof JSON.parse && (JSON.parse = function(text,
            reviver) {
            function walk(e, t) {
                var n, r, i = e[t];
                if (i && "object" == typeof i)
                    for (n in i) Object.prototype.hasOwnProperty.call(
                        i, n) && (r = walk(i, n), void 0 !== r ?
                        i[n] = r : delete i[n]);
                return reviver.call(e, t, i)
            }
            var j;
            if (text = String(text), cx.lastIndex = 0, cx.test(text) &&
                (text = text.replace(cx, function(e) {
                    return "\\u" + ("0000" + e.charCodeAt(0)
                            .toString(16))
                        .slice(-4)
                })), /^[\],:{}\s]*$/.test(text.replace(
                        /\\(?:["\\\/bfnrt]|u[0-9a-fA-F]{4})/g, "@")
                    .replace(
                        /"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g,
                        "]")
                    .replace(/(?:^|:|,)(?:\s*\[)+/g, ""))) return j =
                eval("(" + text + ")"), "function" == typeof reviver ?
                walk({
                    "": j
                }, "") : j;
            throw new SyntaxError("JSON.parse")
        })
    }();
 window.ATL_JQ_PAGE_PROPS = {
        "triggerFunction": function (showCollectorDialog) {
        setTimeout(function () {
        if (showCollectorDialog !== undefined)
                showCollectorDialog();
        }, 200);
        }
//                fieldValues: {
//                    summary: 'Feedback for new website designs',
//                    description: 'The font doesn\'t quite look right',
//                    priority: '2',

//                }
        };
