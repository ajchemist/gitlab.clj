(ns gitlab.v4.alpha-test
  (:require
   [clojure.test :as test :refer [deftest is are testing]]
   [gitlab.v4.alpha :as gitlab]
   ))


(deftest main
  (is
    (try
      (gitlab/request--repository-files-get-raw
        {:gitlab/private-token ""
         :gitlab/project-id    "19806720"
         :gitlab/file-path     "config.edn"})
      (catch Throwable e
        (== (:status (ex-data e)) 404))))


  (is
    (map?
      (read-string
        (slurp
          (gitlab/map->RepositoryFile
            {:private-token "mqBoAzEpP6NxWcSzzKYR"
             :project-id    "19806720"
             :file-path     "config.edn"})))))


  (prn
    (read-string
      (slurp
        (gitlab/map->RepositoryFile
          {:private-token "mqBoAzEpP6NxWcSzzKYR"
           :project-id    "19806720"
           :file-path     "config.edn"
           :ref           "03cb27c780f43be7f1dac5774fea882183bd4275"}))))
  )
