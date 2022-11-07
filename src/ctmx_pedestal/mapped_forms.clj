(ns ctmx-pedestal.mapped-forms
  (:require
   [ctmx.rt :as rt]
   [ctmx.form :as form]
   [ctmx.core :as ctmx :refer [defcomponent]]))

(def people (atom {"a" {:person/person-id "a" :person/name "Alice" :person/email "alice@example.com"}
                   "b" {:person/person-id "b" :person/name "Bob" :person/email "bob@example.com"}
                   "d" {:person/person-id "d" :person/name "Daphne" :person/email "daph@example.com"}
                   "e" {:person/person-id "e" :person/name "Emily" :person/email "emily@example.com"}}))

(defn person-by-id [person-id]
  (get @people person-id))
(defn update-person! [person-id name email]
  (swap! people #(update-in % [person-id] (fn [person]
                                            (-> person
                                                (assoc :person/name name)
                                                (assoc :person/email email)))))

  (person-by-id person-id))

(defcomponent ^:endpoint a-form [req idx person-id]
  (ctmx/with-req req
    (let [params    (-> req :form-params form/json-params-pruned)
          idx       (or idx (:idx params))
          person-id (or person-id (:person-id params))
          person    (cond post? (update-person! (:person-id params) (:name params) (:email params))
                          :else (person-by-id person-id))]
      ;; (tap> {:idx idx :person-id person-id :person person :this id :params params :raw-params (:params req)})
      [:li {:id id :style (str (when (= 0 (mod idx 2)) "background-color: #ccc;") " list-style:none;")}
       [:form {:hx-post "a-form"}
        [:input {:type "hidden" :name (path "idx") :value idx}]
        [:input {:type "hidden" :name (path "person-id") :value person-id}]
        [:input {:type "text" :name (path "name") :value (:person/name person)}]
        [:input {:type "email" :name (path "email") :value (:person/email person)}]
        [:button {:type :submit :hx-target (hash ".")} "Save"]]])))

(defcomponent ^:endpoint mapped-forms [req]
  [:div
   [:h2 "People"]
   [:ul
    (rt/map-indexed a-form req (map :person/person-id (vals @people)))]])
