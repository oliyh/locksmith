(ns locksmith.core-test
  (:require [clojure.test :refer :all]
            [locksmith.core :refer :all]))

(def snake-schema
  '{:enums {:team_name {:description "Some enumerated teams"
                        :values [:ferrari_scuderia :red_bull :mercedes_gp]}}

    :objects {:car_race {:fields {:winning_driver {:type :car_driver}
                                  :competing_drivers {:type (list (non-null :car_driver))}
                                  :country_name {:type (non-null String)}}}
              :car_driver {:fields {:first_name {:type String}
                                    :team_name {:type :team_name}
                                    :champion {:type Boolean}}}}

    :queries {:car_races {:type (list :car_race)}}})

(def camel-schema
  '{:enums {:teamName {:description "Some enumerated teams"
                        :values [:ferrariScuderia :redBull :mercedesGP]}}

    :objects {:carRace {:fields {:winningDriver {:type :carDriver}
                                  :competingDrivers {:type (list (non-null :carDriver))}
                                  :countryName {:type (non-null String)}}}
              :carDriver {:fields {:firstName {:type String}
                                    :teamName {:type :teamName}
                                    :champion {:type Boolean}}}}

    :queries {:carRaces {:type (list :carRace)}}})

(deftest clj->gql-test

  (testing "does nothing to data already in the right format"
    (testing "snakes"
      (is (= {:first_name "Lewis"}
             ((clj->gql snake-schema :car_driver)
              {:first_name "Lewis"}))))
    (testing "camels"
      (is (= {:firstName "Lewis"}
             ((clj->gql camel-schema :carDriver)
              {:firstName "Lewis"})))))

  (testing "converts simple schemas"
    (are [schema root expected] (= expected
                                   ((clj->gql schema root)
                                    {:first-name "Lewis"}))

      snake-schema :car_driver {:first_name "Lewis"}
      camel-schema :carDriver {:firstName "Lewis"}))

  (testing "converts complex schemas"
    (are [schema root expected] (= expected
                                   ((clj->gql schema root)
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
                                                          :champion? false}]}))

      snake-schema :car_race {:country_name "GB"
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

      camel-schema :carRace {:countryName "GB"
                             :winningDriver {:firstName "Lewis"
                                             :teamName "mercedesGP"
                                             :champion true}
                             :competingDrivers [{:firstName "Lewis"
                                                 :teamName "mercedesGP"
                                                 :champion true}
                                                {:firstName "Sebastian"
                                                 :teamName "ferrariScuderia"
                                                 :champion true}
                                                {:firstName "Max"
                                                 :teamName "redBull"
                                                 :champion false}]}))

  (testing "supports aliases"
    (are [schema root aliases expected] (= expected
                                           ((clj->gql schema root {:aliases aliases})
                                            {:country-name "GB"
                                             :top-driver {:first-name "Lewis"
                                                          :team-name :mercedes-gp}}))

      snake-schema :car_race {:top_driver :winning_driver} {:country_name "GB"
                                                            :top_driver {:first_name "Lewis"
                                                                         :team_name "mercedes_gp"}}

      camel-schema :carRace {:topDriver :winningDriver} {:countryName "GB"
                                                         :topDriver {:firstName "Lewis"
                                                                     :teamName "mercedesGP"}}))

  (testing "supports top level queries"
    (are [schema root expected] (= expected
                                   ((clj->gql schema root)
                                    [{:country-name "GB"
                                      :winning-driver {:first-name "Lewis"
                                                       :team-name :mercedes-gp}}]))

      snake-schema :car_races [{:country_name "GB"
                                :winning_driver {:first_name "Lewis"
                                                 :team_name "mercedes_gp"}}]

      camel-schema :carRaces [{:countryName "GB"
                               :winningDriver {:firstName "Lewis"
                                               :teamName "mercedesGP"}}])))

(deftest gql->clj-test

  (testing "does nothing to data already in the right format"
    (testing "snakes"
      (is (= {:first-name "Lewis"}
             ((gql->clj snake-schema :car_driver)
              {:first-name "Lewis"}))))
    (testing "camels"
      (is (= {:first-name "Lewis"}
             ((gql->clj camel-schema :carDriver)
              {:first-name "Lewis"})))))

  (testing "supports simple schemas"
    (are [schema root to-convert] (= {:first-name "Lewis"}
                                     ((gql->clj schema root)
                                      to-convert))

      snake-schema :car_driver {:first_name "Lewis"}
      camel-schema :carDriver {:firstName "Lewis"}))

  (testing "supports complex schemas"
    (are [schema root to-convert] (= {:country-name "GB"
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
                                     ((gql->clj schema root)
                                      to-convert))

      snake-schema :car_race {:country_name "GB"
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

      camel-schema :carRace {:countryName "GB"
                             :winningDriver {:firstName "Lewis"
                                             :teamName "mercedesGP"
                                             :champion true}
                             :competingDrivers [{:firstName "Lewis"
                                                 :teamName "mercedesGP"
                                                 :champion true}
                                                {:firstName "Sebastian"
                                                 :teamName "ferrariScuderia"
                                                 :champion true}
                                                {:firstName "Max"
                                                 :teamName "redBull"
                                                 :champion false}]}))

  (testing "supports aliases"
    (are [schema root aliases to-convert] (= {:country-name "GB"
                                              :top-driver {:first-name "Lewis"
                                                           :team-name :mercedes-gp}}
                                             ((gql->clj schema root {:aliases aliases})
                                              to-convert))

      snake-schema :car_race {:top_driver :winning_driver} {:country_name "GB"
                                                            :top_driver {:first_name "Lewis"
                                                                         :team_name "mercedes_gp"}}

      camel-schema :carRace {:topDriver :winningDriver} {:countryName "GB"
                                                         :topDriver {:firstName "Lewis"
                                                                     :teamName "mercedesGP"}}))

  (testing "supports top level queries"
    (are [schema root to-convert] (= [{:country-name "GB"
                                       :winning-driver {:first-name "Lewis"
                                                        :team-name :mercedes-gp}}]
                                             ((gql->clj schema root)
                                              to-convert))

      snake-schema :car_races [{:country_name "GB"
                                :winning_driver {:first_name "Lewis"
                                                 :team_name "mercedes_gp"}}]

      camel-schema :carRaces [{:countryName "GB"
                               :winningDriver {:firstName "Lewis"
                                               :teamName "mercedesGP"}}])))

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

    (are [schema root] (is (= races
                              (-> races
                                  ((clj->gql schema root))
                                  ((gql->clj schema root)))))
      snake-schema :car_races
      camel-schema :carRaces)))

;; todo
;; 1. Circular graphs? Avoid blowing up
