(ns c2.dom-test
  (:use-macros [c2.util :only [p pp profile]])
  (:use [c2.dom :only [attr ->dom append! text]]))

(set! *print-fn* #(.log js/console %))

(def xhtml "http://www.w3.org/1999/xhtml")

(def container (.createElementNS js/document xhtml "div"))
;;Appending to html instead of body here because of PhantomJS page.injectJs() wonky behavior
(.appendChild (.querySelector js/document "html") container)

(defn clear! [] (set! (.-innerHTML container) ""))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;Attribute reading & writing
(assert (= nil (attr container :x)))
(attr container :x 1)
(assert (= "1" (attr container :x)))
(attr container {:y 2 :z 3})
(assert (= {:x "1" :y "2" :z "3"} (attr container)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;DOM Element creation from vectors
(assert (= "<p><span>hello</span><i>0</i><i>1</i><i>2</i></p>"
           (.-outerHTML (->dom [:p [:span "hello"]
                                (map #(vector :i %) (range 3))])))
        "Literal and seq children.")

(assert (= "<span class=\"a b\"></span>"
           (.-outerHTML (->dom [:span.a {:class "b"}])))
        "Class literal and in attr map")



(clear!)
