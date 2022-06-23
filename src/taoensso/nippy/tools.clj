(ns taoensso.nippy.tools
  "Utils for 3rd-party tools that want to add user-configurable Nippy support.
  Used by Carmine, Faraday, etc."
  (:require [taoensso.nippy :as nippy]))

(def ^:dynamic *freeze-opts* nil)
(def ^:dynamic *thaw-opts*   nil)

(defmacro with-freeze-opts [opts & body] `(binding [*freeze-opts* ~opts] ~@body))
(defmacro with-thaw-opts   [opts & body] `(binding [*thaw-opts*   ~opts] ~@body))

(deftype WrappedForFreezing [val opts])
(defn wrapped-for-freezing? [x] (instance? WrappedForFreezing x))
(defn    wrap-for-freezing
  "Ensures that given arg (any freezable data type) is wrapped so that
  (tools/freeze <wrapped-arg>) will serialize as
  (nippy/freeze <unwrapped-arg> <opts>)."
  ([x     ] (wrap-for-freezing x nil))
  ([x opts]
   (if (instance? WrappedForFreezing x)
     (let [^WrappedForFreezing x x]
       (if (= (.-opts x) opts)
         x
         (WrappedForFreezing. (.-val x) opts)))
     (WrappedForFreezing.            x  opts))))

(defn freeze
  "Like `nippy/freeze` but merges opts from *freeze-opts*, `wrap-for-freezing`."
  ([x             ] (freeze x nil))
  ([x default-opts]
   (let [default-opts (get default-opts :default-opts default-opts) ; For back compatibility
         merged-opts  (conj (or default-opts {}) *freeze-opts*)]

     (if (instance? WrappedForFreezing x)
       (let [^WrappedForFreezing x x]
         (nippy/freeze (.-val x) (conj merged-opts (.-opts x))))
       (nippy/freeze          x        merged-opts)))))

(defn thaw
  "Like `nippy/thaw` but merges opts  from `*thaw-opts*`."
  ([ba             ] (thaw ba nil))
  ([ba default-opts]
   (let [default-opts (get default-opts :default-opts default-opts) ; For back compatibility
         merged-opts  (conj (or default-opts {}) *thaw-opts*)]
     (nippy/thaw ba merged-opts))))

(comment (thaw (freeze (wrap-for-freezing "wrapped"))))
