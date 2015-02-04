;; Copyright © 2014 JUXT LTD.

(ns modular.maker-tests
  (:use clojure.test modular.maker))

(deftest make-args-test
  (testing "Default if no override"
    (is (= '(:a 1) (make-args {} :a 1))))
  (testing "No specified value, no arg"
    (is (= '() (make-args {:a 2}))))
  (testing "Config overrides specified value"
    (is (= '(:a 2) (make-args {:a 2} :a 1))))
  (testing "Nil selects config"
    (is (= '(:a 2) (make-args {:a 2} :a nil))))
  (testing "Nil selects config"
    (is (= '(:a 2) (make-args {:a 2} :a :required))))
  (testing "Preservation of Boolean values"
    (is (= '(:a false :b true) (make-args {:a false :b true} :a nil :b nil))))
  (testing "Exception thrown on required"
    (is (thrown? clojure.lang.ExceptionInfo
                 (make-args {} :a :modular.maker/required))))
  (testing "Exception thrown on odd number of args"
    (is (thrown? java.lang.AssertionError
                 (make-args {} :a))))
  (testing "Mapping with a keyword"
    (is (= '(:a 2) (make-args {:b 2} {:a :b} 1))))
  (testing "Mapping with a vector path"
    (is (= '(:a 2) (make-args {:b {:c 2}} {:a [:b :c]} 1)))))
