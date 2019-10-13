(ns objmodel-clj.core-test
  (:require [clojure.test :refer :all]
            [objmodel-clj.core :refer :all])
  (:import [clojure.lang ArityException]
           [java.io StringReader]))

(deftest read-model-arity
  (is (thrown? ArityException (read-model)))
  (is (thrown? ArityException (read-model 1 2))))

(deftest empty-model
  (is (= (read-model (StringReader.
                      ""))
         {:verts '()
          :tex-coords '()
          :faces '()
          :groups {"default" {:vert-indices #{}
                              :tex-coord-indices #{}
                              :face-indices #{}}}})))

(deftest vertex
  (is (= (read-model (StringReader.
                      "v 2 3 4"))
         {:verts '({:x 2.0 :y 3.0 :z 4.0 :w 1.0})
          :tex-coords '()
          :faces '()
          :groups {"default" {:vert-indices #{1}
                              :tex-coord-indices #{}
                              :face-indices #{}}}}))
  (is (= (read-model (StringReader.
                      "v 1 2 3 4"))
         {:verts '({:x 1.0 :y 2.0 :z 3.0 :w 4.0})
          :tex-coords '()
          :faces '()
          :groups {"default" {:vert-indices #{1}
                              :tex-coord-indices #{}
                              :face-indices #{}}}})))

(deftest tex-coord
  (is (= (read-model (StringReader.
                      "vt 3 4"))
       {:verts '()
        :tex-coords '({:u 3.0 :v 4.0 :w 0.0})
        :faces '()
        :groups {"default" {:vert-indices #{}
                            :tex-coord-indices #{1}
                            :face-indices #{}}}}))
  (is (= (read-model (StringReader.
                      "vt 9 5 3"))
       {:verts '()
        :tex-coords '({:u 9.0 :v 5.0 :w 3.0})
        :faces '()
        :groups {"default" {:vert-indices #{}
                            :tex-coord-indices #{1}
                            :face-indices #{}}}})))
