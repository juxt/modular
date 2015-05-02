(require
 '[cylon.user :refer (create-user! hash-password)])

(defn add-user []
  (create-user!
   (-> system :cylon-user-store)
   {:email "malcolm@congreve.com"
    :password (hash-password (-> system :cylon-buddy-user-authenticator) "foobar")}))
