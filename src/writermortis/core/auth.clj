(ns writermortis.core.auth)

(defn verify-password [user-record, password]
  (= password (:password user-record)))