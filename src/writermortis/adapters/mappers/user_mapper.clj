(ns writermortis.adapters.mappers.user-mapper)

(defn map-record [user-record] (select-keys user-record [:id :username]))