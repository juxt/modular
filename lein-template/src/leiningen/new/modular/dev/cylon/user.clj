(require
 '[cylon.user :refer (create-user!)]
 '[cylon.password :refer (make-password-hash)])

(defn add-user []
  (create-user!
   (-> system :cylon-user-store)
   "malcolm"
   (make-password-hash (-> system :cylon-password-verifier) "foobar")
   "malcolm@congreve.com"
   {}))
