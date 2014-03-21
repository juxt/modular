;; Copyright © 2014, JUXT LTD. All Rights Reserved.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(defproject juxt.modular/datomic "0.1.0"
  :description "A modular extension that provides support for Datomic"
  :url "https://github.com/juxt/modular/tree/master/modules/bidi"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[juxt/modular "0.2.0"]
                 [prismatic/schema "0.2.1"]
                 [juxt/datomic-extras "1.0.3"
                  :exclusions [org.slf4j/slf4j-nop
                               org.slf4j/jul-to-slf4j
                               org.slf4j/jcl-over-slf4j
                               org.slf4j/log4j-over-slf4j]]
                 ])
