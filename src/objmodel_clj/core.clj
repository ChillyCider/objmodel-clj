(ns objmodel-clj.core
  (:require [clojure.java.io :as io]
            [clojure.string :as s]))

(def ^:private empty-group
  {:vert-indices #{}
   :face-indices #{}
   :tex-coord-indices #{}})

(def ^:private initial-loader-state
  {:verts '()
   :faces '()
   :tex-coords '()
   :mtl-current nil
   :groups {"default" empty-group}
   :groups-current ["default"]})

(defn- add-feature-to-model
  "Update the loader state with a new model feature. This does not add
   features to groups, however."
  [state feat-key feat]
  (update state
          feat-key
          #(conj % feat)))
          
(defn- add-feature-to-groups
  "Update the loader state by adding a feature to a group."
  [state feat-key group-refs-key]
  (reduce
   (fn [state grp]
     (update-in state
                [:groups grp group-refs-key]
                #(conj % (count (feat-key state)))))
   state
   (:groups-current state)))

(defn- handle-v
  "Process a vertex directive and update the loader state"
  ([state x y z]
   (handle-v state x y z "1.0"))
  ([state x y z w]
   (-> state
       (add-feature-to-model :verts
                             {:x (Float/parseFloat x)
                              :y (Float/parseFloat y)
                              :z (Float/parseFloat z)
                              :w (Float/parseFloat w)})
       (add-feature-to-groups :verts
                              :vert-indices))))

(defn- handle-vt
  "Process a texture coordinate directive and update the loader state"
  ([state u v]
   (handle-vt state u v "0"))
  ([state u v w]
   (-> state
       (add-feature-to-model :tex-coords
                             {:u (Float/parseFloat u)
                              :v (Float/parseFloat v)
                              :w (Float/parseFloat w)})
       (add-feature-to-groups :tex-coords
                              :tex-coord-indices))))

(defn- new-face
  "Create a face using a material and reference strings of the form \"v/vt/vn\""
  [mtl ref-groups]
  {:mtl mtl
   :refs (into []
               (map
                (fn [ref-group]
                  (zipmap
                   [:v :vt :vn]
                   (map
                    #(if (empty? %)
                       nil
                       (Integer/parseInt %))
                    (s/split ref-group #"/"))))
                ref-groups))})

(defn- handle-f
  "Process a face directive and update the loader state"
  [state & ref-groups]
  (-> state
      (add-feature-to-model :faces
                            (new-face
                             (:mtl-current state)
                             ref-groups))
      (add-feature-to-groups :faces
                             :face-indices)))

(defn- handle-g
  "Process a group directive and update the loader state"
  [loader-state & group-names]
  (-> loader-state
      (assoc-in [:groups-current] group-names)
      ((fn [loader-state]
         (reduce
          (fn [loader-state group-name]
            (assoc-in loader-state [:groups group-name] empty-group))
          loader-state
          group-names)))))

(defn- handle-usemtl
  "Process a usemtl directive and update the loader state"
  [loader-state mtl]
  (assoc-in loader-state [:mtl-current] mtl))

(def ^:private directive-handlers
  {:v handle-v
   :vt handle-vt
   :f handle-f
   :g handle-g
   :usemtl handle-usemtl})

(defn- strip-comment
  "Strip an OBJ comment from a string if one is present"
  [string]
  (s/replace string #"#.*" ""))

(defn- consume-line
  "Consume a directive line and process it, updating the loader state"
  ([loader-state] loader-state)
  ([loader-state [directive & args]]
   (let [handler (directive directive-handlers)]
     (apply
      handler
      (conj args loader-state)))))

(defn- split-command
  "Split a directive string into parts using whitespace as a delimiter (the head of the result is always a keyword)"
  [string]
  (let [parts (s/split string #"\s+")]
    (assoc parts 0 (keyword (first parts)))))

(defn- valid-directive?
  "Determine whether the loader supports a given directive"
  [[directive & args]]
  (contains? directive-handlers directive))

(defn read-model
  "Read an OBJ model from a file"
  [file]
  (let [state (with-open [r (io/reader file)]
                (transduce (comp
                            (map strip-comment)
                            (map s/trim)
                            (remove empty?)
                            (map split-command)
                            (filter valid-directive?))
                           consume-line
                           initial-loader-state
                           (line-seq r)))]
    (-> state
        (dissoc :mtl-current)
        (dissoc :groups-current))))
