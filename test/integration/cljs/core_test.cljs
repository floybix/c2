(ns c2.core-test
  (:use-macros [c2.util :only [p pp profile bind!]])
  (:require [c2.svg :as svg])
  (:use [c2.core :only [unify]]
        [c2.dom :only [attr children]]))

(set! *print-fn* #(.log js/console %))

(def xhtml "http://www.w3.org/1999/xhtml")

(def container (.createElementNS js/document xhtml "div"))
(def svg-container (.createElementNS js/document "http://www.w3.org/2000/svg" "svg"))
;;Appending to html instead of body here because of PhantomJS page.injectJs() wonky behavior
(.appendChild (.querySelector js/document "html") container)
(.appendChild (.querySelector js/document "html") svg-container)

(defn clear! []
  (set! (.-innerHTML container) "")
  (set! (.-innerHTML svg-container) ""))


(print "\n\nSingle node enter/update/exit\n=============================")
(let [n 100
      mapping (fn [d] [:span {:x d} (str d)])]

  (profile (str "ENTER single tag with " n " data")
           (bind! container (unify (range n) mapping)))
  (let [children (children container)
        fel      (first children)]
    (assert (= n (count children)))
    (assert (= "span" (.toLowerCase (.-nodeName fel))))
    (assert (= "0" (:x (attr fel)))))

  (profile (str "UPDATE single tag, reversing order")
           (bind! container (unify (reverse (range n)) mapping)))
  (let [children (children container)
        fel       (first children)]
    (assert (= n (count children)))
    (assert (= "span" (.toLowerCase (.-nodeName fel))))
    (assert (= (str (dec n)) (:x (attr fel)))))


  (profile (str "UPDATE single tag with new datum")
           (bind! container (unify (range (inc n)) mapping)))
  (assert (= (inc n) (count (children container))))


  (profile (str "REMOVE " (/ n 2) " single tags")
           (bind! container (unify (range (/ n 2)) mapping)))
  (assert (= (/ n 2)  (count (children container)))))



(clear!)


(print "\n\nAtom-updates on single node enter/update/exit\n=============================")
(let [n 100
      mapping (fn [d] [:span {:x d} (str d)])
      !ds (atom (range n))]

  (profile (str "ENTER single tag with " n " data")
           (bind! container (unify @!ds mapping)))
  (let [children (children container)
        fel      (first children)]
    (assert (= n (count children)))
    (assert (= "span" (.toLowerCase (.-nodeName fel))))
    (assert (= "0" (:x (attr fel)))))

  (profile (str "UPDATE single tag, reversing order")
           (swap! !ds reverse))
  (let [children (children container)
        fel       (first children)]
    (assert (= n (count children)))
    (assert (= "span" (.toLowerCase (.-nodeName fel))))
    (assert (= (str (dec n)) (:x (attr fel))))))

(clear!)


(print "\n\nMore complex dataset\n====================")
(let [n 100
      data (map #(hash-map :id % :val (str (rand)))
                (range n))
      new-data (map #(assoc % :val (str (rand))) data)
      mapping (fn [d idx] [:div {:val (:val d)}
                          [:span (str (:id d))]])]
  (profile "ENTER node hiearchy"
           (bind! container (unify data mapping
                                   :key-fn #(:id %))))
  (assert (= 100 (count (children container))))

  (profile "UPDATE/EXIT node hiearchy"
           (bind! container (unify (take 10 new-data) mapping
                                   :key-fn #(:id %))))

  (assert (= 10 (count (children container))))
  (assert (= (:val (first new-data))
             (:val (attr (first (children container)))))))

(clear!)

(print "\n\nHurray, no errors!")
