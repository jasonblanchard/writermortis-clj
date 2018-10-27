(ns writermortis.adapters.mappers.user-mapper)

(defn map [user-record] (select-keys user-record [:id :username]))