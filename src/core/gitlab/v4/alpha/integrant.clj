(ns gitlab.v4.alpha.integrant
  (:require
   [gitlab.v4.alpha :as gitlab]
   [integrant.core :as ig]
   ))


(comment
  (remove-method ig/prep-key ::repository-file)
  )


(defmethod ig/init-key ::repository-file
  [_ opts]
  (gitlab/map->RepositoryFile opts))


(defmethod ig/halt-key! ::repository-file [_ _])
