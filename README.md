# locksmith

Want to use GraphQL with Clojure/script but don't want snake_keys everywhere? Use locksmith to change all the keys!

locksmith creates efficient functions to transform GraphQL keys (e.g. `snake_key`, `boolean_key`) into Clojure keys (`snake-key`, `boolean-key?`) and vice versa.
It does this by inspecting your [lacinia](https://github.com/walmartlabs/lacinia) GraphQL schema, deciding which keys need renaming and composing functions to do the renames as fast as possible.

This helps you satisfy GraphQL queries on your server and work with idiomatic Clojure data on your client.

[![Clojars Project](https://img.shields.io/clojars/v/locksmith.svg)](https://clojars.org/locksmith)

## Usage

Imagine you have a schema for a car race, as follows:

```clj
(def lacinia-schema
  '{:enums {:team_name {:description "Some enumerated teams"
                        :values [:ferrari_scuderia :red_bull :mercedes_gp]}}

    :objects {:car_race {:fields {:winning_driver {:type :car_driver}
                                  :competing_drivers {:type (list :car_driver)}
                                  :country_name {:type String}}}
              :car_driver {:fields {:first_name {:type String}
                                    :team_name {:type :team_name}
                                    :champion {:type Boolean}}}}})
```

On your server you have lots of lovely Clojure data, but you have to put loads of underscores in it to satisfy any GraphQL queries that come in.
locksmith's `clj->gql` does this for you!

```clj
(let [car-race-converter (clj->gql lacinia-schema :car_race)]
  (car-race-converter {:country-name "GB"
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

;; {:country_name "GB",
;;  :competing_drivers
;;  ({:champion true, :team_name "mercedes_gp", :first_name "Lewis"}
;;   {:champion true,
;;    :team_name "ferrari_scuderia",
;;    :first_name "Sebastian"}
;;   {:champion false, :team_name "red_bull", :first_name "Max"}),
;;  :winning_driver
;;  {:champion true, :team_name "mercedes_gp", :first_name "Lewis"}}
```

On your client you crave Clojure data, but the GraphQL server is trying to force feed you underscores.
locksmith's `gql->clj` sorts it out!

```clj
(let [car-race-converter (gql->clj lacinia-schema :car_race)]
  (car-race-converter {:country_name "GB"
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
                                            :champion false}]}))

;; {:country-name "GB",
;;  :competing-drivers
;;  ({:champion? true, :team-name :mercedes-gp, :first-name "Lewis"}
;;   {:champion? true,
;;    :team-name :ferrari-scuderia,
;;    :first-name "Sebastian"}
;;   {:champion? false, :team-name :red-bull, :first-name "Max"}),
;;  :winning-driver
;;  {:champion? true, :team-name :mercedes-gp, :first-name "Lewis"}}

```

## Development

[![CircleCI](https://circleci.com/gh/oliyh/locksmith.svg?style=svg)](https://circleci.com/gh/oliyh/locksmith)

## License

Copyright © 2017 oliyh

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
