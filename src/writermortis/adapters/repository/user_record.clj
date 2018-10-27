(ns writermortis.adapters.repository.user-record)

(def users (atom
            {123 {:id 123 :password "testpass" :username "gob"}
             456 {:id 456 :password "testpass" :username "lucille"}}))

(defn find-by-username
  [username]
  (second (first
           (filter (fn [[id user]] (= username (:username user))) @users))))