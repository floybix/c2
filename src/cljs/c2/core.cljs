(ns c2.core
  (:use-macros [c2.util :only [p timeout]]
               [clojure.core.match.js :only [match]]
               [iterate :only [iter]])
  (:use [cljs.reader :only [read-string]])
  (:require [pinot.html :as html]
            [pinot.dom :as dom]
            [goog.dom :as gdom]
            [clojure.set :as set]
            [clojure.string :as string]))

;;Lil' helpers
(defn translate [x y]
  {:transform (str "translate(" x "," y ")")})



;;This should be replaced by a macro in c2.util once I figure out how to get macros to generate separate code for CLJ vs. CLJS.
(defn dont-carity [f & args]
  (apply f args))


;;DOM-ish methods
(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))
(extend-type js/HTMLCollection
  ISeqable
  (-seq [array] (array-seq array 0)))

;;This is required so that DOM nodes can be used in sets
(extend-type js/Node
  IHash
  (-hash [x]
    ;;(.-outerHTML x)
    x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;DOM manipulation & hiccup-like things
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn children [node]
  (filter #(= 1 (.-nodeType %))
          (.-childNodes node)))

;; From Weavejester's Hiccup.
(def ^{:doc "Regular expression that parses a CSS-style id and class from a tag name."}
  re-tag #"([^\s\.#]+)(?:#([^\s\.#]+))?(?:\.([^\s#]+))?")

(defn merge-dom [dom-node el]
  (when (not= (.toLowerCase (.-nodeName dom-node))
              (.toLowerCase (name (:tag el))))
    (throw "Cannot merge el into node of a different type"))

  (dom/attr dom-node (:attr el))
  (when-let [txt (first (filter string? (:children el)))]
    (dom/text dom-node txt))
  (iter {for [dom-child el-child] in (map vector (children dom-node)
                                          (remove string? (:children el)))}
        (merge-dom dom-child el-child)))


(def xmlns {:xhtml "http://www.w3.org/1999/xhtml"
            :svg "http://www.w3.org/2000/svg"})

(defn cannonicalize
  "Parse hiccup-like vec into map of {:tag :attr :children}, or return string as itself.
   Based on Pinot's html/normalize-element."
  [x]
  (match [x]
         [(str :when string?)] str
         ;;todo, make explicit match here for attr map and clean up crazy Pinot logic below
         [[tag & content]]   (let [[_ tag id class] (re-matches re-tag (name tag))
                                   [nsp tag]     (let [[nsp t] (string/split tag #":")
                                                       ns-xmlns (xmlns (keyword nsp))]
                                                   (if t
                                                     [(or ns-xmlns nsp) (keyword t)]
                                                     [(:xhtml xmlns) (keyword nsp)]))
                                   tag-attrs        (into {}
                                                          (filter #(not (nil? (second %)))
                                                                  {:id (or id nil)
                                                                   :class (if class (string/replace class #"\." " "))}))
                                   map-attrs        (first content)]

                               (if (map? map-attrs)
                                 {:tag  tag :attr (merge tag-attrs map-attrs) :children  (map cannonicalize (next content))}
                                 {:tag tag :attr tag-attrs :children  (map cannonicalize content)}))))










;;Attach data in Clojure reader readable format to the DOM.
;;Use data-* attr rather than trying to set as a property.
;;See http://dev.w3.org/html5/spec/Overview.html#attr-data
(def node-data-key "c2")

(defn node-type [node]
  (cond
   (vector? node) :hiccup   ;;Hiccup vector
   (string? node) :selector ;;CSS selector string
   :else          :dom      ;;It's an actual DOM node
   ))
(defmulti attach-data (fn [node d] (node-type node)))
(defmethod attach-data :hiccup [node d]
  (assoc-in node [1 (str "data-" node-data-key)]
            (binding [*print-dup* true] (pr-str d))))

(defmethod attach-data :dom [node d]
  (dom/attr node (str "data-" node-data-key)
            (binding [*print-dup* true] (pr-str d))))

(defmulti read-data (fn [node] (node-type node)))
(defmethod read-data :hiccup [node] (get-in node [1 (str "data-" node-data-key)]))
(defmethod read-data :dom [node]
  (if-let [data-str (aget (.-dataset node)
                          node-data-key)]
    (if (not= "" data-str)
      (read-string data-str))))


;; (defmulti unify!
;;   (fn [_ data & args]
;;     (cond (instance? cljs.core.Atom data) :atom
;;           (seq? data)                        :seq
;;           :else                              (throw (js/Error. "Unify! requires data to be seqable or an atom containing seqable")))))
;; (defmethod unify! :atom
;;   [_ atom-data & args]
;;   (apply unify! _ @atom-data args))

;;(defmethod unify! :seq)



(defmulti select node-type)
(defmethod select :selector [selector] (first (dom/query selector)))
(defmethod select :dom [node] node)

(defmulti select-all node-type)
(defmethod select-all :selector [selector] (dom/query selector))
(defmethod select-all :dom [nodes] nodes)

(defn unify!
  "Calls (mapping datum idx) for each datum and appends resulting elements to container.
Automatically updates elements mapped to data according to key-fn (defaults to index) and removes elements that don't match.
Scoped to selector, if given, otherwise applies to all container's children.
Optional enter, update, and exit functions called before DOM is changed; return false to prevent default behavior."
  [container data mapping & {:keys [selector key-fn post-fn update exit enter]
                             :or {key-fn (fn [d idx] idx)
                                  enter  (fn [d idx new-node]
                                           (p "no-op enter called")
                                           new-node)
                                  update (fn [d idx old-node new-node]
                                           (merge-dom old-node (cannonicalize new-node))
                                           (p "no-op update called")
                                           nil)
                                  exit   (fn [d idx old-node]
                                           (p "default remove called"))}}]

  (let [container (select container)
        ;;This logic should be abstracted out via a (unify!) multimethod, once (apply multimethod) is fixed in ClojureScript
        data (if (instance? cljs.core.Atom data)
               ;;Then add a watcher to auto-unify when the atom changes, and deference data for this run
               (do (add-watch data :auto-unify
                              (fn [key data-atom old new]
                                (p "atom updated; automatically calling unify!")
                                (unify! container data mapping
                                        :selector selector
                                        :key-fn key-fn
                                        :enter  enter
                                        :update update
                                        :exit exit
                                        :post-fn post-fn)))
                   @data)
               data)


        existing-nodes-by-key (into {} (map-indexed (fn [i node]
                                                      (let [datum (read-data node)]
                                                        [(key-fn datum i)  {:node node
                                                                            :idx i
                                                                            :datum datum}]))
                                                    (if selector
                                                      (dom/query selector container)
                                                      (children container))))]

    ;;Remove any stale nodes
    (iter {for k in (set/difference (set (keys existing-nodes-by-key))
                                    (set (map key-fn data (range))))}
          (let [{:keys [node idx datum]} (existing-nodes-by-key k)]
            (if (exit datum idx node)
              (dom/unappend node))))


    ;;For each datum, update existing nodes and add new ones
    (iter {for [idx d] in (map-indexed vector data)}
          (let [new-node (attach-data (mapping d idx) d)]
            ;;If there's an existing node
            (if-let [old (existing-nodes-by-key (key-fn d idx))]
              (do
                ;;append it (effectively moving it to the correct index in the container)
                (gdom/appendChild container (:node old))

                ;;If its data is not equal to the new data, replace it
                (if (not= d (:datum old))
                  (when-let [updated-node (update d idx (:node old) new-node)]
                    (dom/replace (:node old) (html/html updated-node)))))

              (let [new-dom-node (html/html new-node)]
                (dom/append container new-dom-node)
                (enter d idx new-dom-node)))))

    ;;Run post-fn, if it was given
    (if post-fn
      ;;Give the browser 10 ms to get its shit together, if the post-fn involves advanced layout.
      ;;Without this delay, CSS3 animations sometimes don't happen.
      (timeout 10 #(post-fn data)))))


(defn instantiate-attrs
  "Evaluates values in an attribute map by passing them the provided datum and index."
  [attrs d idx]
  (into {} (for [[k v] attrs]
             (let [v (cond
                      (keyword? v) (v d)
                      (fn? v) (dont-carity v d idx)
                      :else v)]
               [k v]))))

(defn apply-attrs! [elem attrs d idx]
  (iter {for [k v] in (instantiate-attrs attrs d idx)}
        (dom/attr elem (name k) v)))



(defn snode
  "Returns a function of [datum, index] that builds `tag` nodes with the specified attributes
The attribute values themselves can be functions of [datum, index]."
  [tag attrs]
  (fn [datum idx]
    [tag (instantiate-attrs attrs datum idx)]))

