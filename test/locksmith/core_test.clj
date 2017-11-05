(ns locksmith.core-test
  (:require [clojure.test :refer :all]
            [locksmith.core :refer :all]))

(def lacinia-schema
  '{:enums {:team_name {:description "Some enumerated teams"
                        :values [:ferrari_scuderia :red_bull :mercedes_gp]}}

    :objects {:car_race {:fields {:winning_driver {:type :car_driver}
                                  :competing_drivers {:type (list (non-null :car_driver))}
                                  :country_name {:type (non-null String)}}}
              :car_driver {:fields {:first_name {:type String}
                                    :team_name {:type :team_name}
                                    :champion {:type Boolean}}}}

    :queries {:car_races {:type (list :car_race)}}})

(deftest clj->gql-test

  (testing "does nothing to data already in the right format"
    (is (= {:first_name "Lewis"}
           ((clj->gql lacinia-schema :car_driver)
            {:first_name "Lewis"}))))

  (testing "converts simple schemas"
    (is (= {:first_name "Lewis"}
           ((clj->gql lacinia-schema :car_driver)
            {:first-name "Lewis"}))))

  (testing "converts complex schemas"
    (is (= {:country_name "GB"
            :winning_driver {:first_name "Lewis"
                             :team_name "mercedes_gp"
                             :champion true}
            :competing_drivers [{:first_name "Lewis"
                                 :team_name "mercedes_gp"
                                 :champion true}
                                {:first_name "Sebastian"
                                 :team_name "ferrari_scuderia"
                                 :champion true}
                                {:first_name "Max"
                                 :team_name "red_bull"
                                 :champion false}]}

           ((clj->gql lacinia-schema :car_race)
            {:country-name "GB"
             :winning-driver {:first-name "Lewis"
                              :team-name :mercedes-gp
                              :champion? true}
             :competing-drivers [{:first-name "Lewis"
                                  :team-name :mercedes-gp
                                  :champion? true}
                                 {:first-name "Sebastian"
                                  :team-name :ferrari-scuderia
                                  :champion? true}
                                 {:first-name "Max"
                                  :team-name :red-bull
                                  :champion? false}]}))))

  (testing "supports aliases"
    (is (= {:country_name "GB"
            :top_driver {:first_name "Lewis"
                         :team_name "mercedes_gp"}}

           ((clj->gql lacinia-schema :car_race {:aliases {:top_driver :winning_driver}})
            {:country-name "GB"
             :top-driver {:first-name "Lewis"
                          :team-name :mercedes-gp}}))))

  (testing "supports top level queries"
    (is (= [{:country_name "GB"
             :winning_driver {:first_name "Lewis"
                              :team_name "mercedes_gp"}}]

           ((clj->gql lacinia-schema :car_races)
            [{:country-name "GB"
              :winning-driver {:first-name "Lewis"
                               :team-name :mercedes-gp}}])))))

(deftest gql->clj-test

  (testing "does nothing to data already in the right format"
    (is (= {:first-name "Lewis"}
           ((gql->clj lacinia-schema :car_driver)
            {:first-name "Lewis"}))))

  (testing "supports simple schemas"
    (is (= {:first-name "Lewis"}
           ((gql->clj lacinia-schema :car_driver)
            {:first_name "Lewis"}))))

  (testing "supports complex schemas"
    (is (= {:country-name "GB"
            :winning-driver {:first-name "Lewis"
                             :team-name :mercedes-gp
                             :champion? true}
            :competing-drivers [{:first-name "Lewis"
                                 :team-name :mercedes-gp
                                 :champion? true}
                                {:first-name "Sebastian"
                                 :team-name :ferrari-scuderia
                                 :champion? true}
                                {:first-name "Max"
                                 :team-name :red-bull
                                 :champion? false}]}

           ((gql->clj lacinia-schema :car_race)
            {:country_name "GB"
             :winning_driver {:first_name "Lewis"
                              :team_name "mercedes_gp"
                              :champion true}
             :competing_drivers [{:first_name "Lewis"
                                  :team_name "mercedes_gp"
                                  :champion true}
                                 {:first_name "Sebastian"
                                  :team_name "ferrari_scuderia"
                                  :champion true}
                                 {:first_name "Max"
                                  :team_name "red_bull"
                                  :champion false}]}))))

  (testing "supports aliases"
    (is (= {:country-name "GB"
            :top-driver {:first-name "Lewis"
                         :team-name :mercedes-gp}}

           ((gql->clj lacinia-schema :car_race {:aliases {:top_driver :winning_driver}})
            {:country_name "GB"
             :top_driver {:first_name "Lewis"
                          :team_name "mercedes_gp"}}))))

  (testing "supports top level queries"
    (is (= [{:country-name "GB"
             :winning-driver {:first-name "Lewis"
                              :team-name :mercedes-gp}}]

           ((gql->clj lacinia-schema :car_races)
            [{:country_name "GB"
              :winning_driver {:first_name "Lewis"
                               :team_name "mercedes_gp"}}])))))

(deftest symmetry-test
  (let [races [{:country-name "GB"
                :winning-driver {:first-name "Lewis"
                                 :team-name :mercedes-gp
                                 :champion? true}
                :competing-drivers [{:first-name "Lewis"
                                     :team-name :mercedes-gp
                                     :champion? true}
                                    {:first-name "Sebastian"
                                     :team-name :ferrari-scuderia
                                     :champion? true}
                                    {:first-name "Max"
                                     :team-name :red-bull
                                     :champion? false}]}]]

    (is (= races
           (-> races
               ((clj->gql lacinia-schema :car_races))
               ((gql->clj lacinia-schema :car_races)))))))

;; todo
;; 1. Circular graphs? Avoid blowing up
