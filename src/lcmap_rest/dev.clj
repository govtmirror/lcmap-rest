;;;; LCMAP REST service development namespace
;;;;
;;;; This namespace is particularly useful when doing active development on the
;;;; lcmap-rest system, as it allows you to easily:
;;;;  * start and stop all the system components
;;;;  * make filesystem changes
;;;;  * make code or configuration changes
;;;; and then reload with all the latest changes -- without having to restart
;;;; the JVM. This namespace can be leveraged to significantly improve
;;;; development time, especially during debugging or progotyping stages.
(ns lcmap-rest.dev
  (:require [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :as repl]
            [com.stuartsierra.component :as component]
            [lcmap-rest.app :as app]
            [lcmap-rest.components :as components]
            [lcmap-rest.util :as util]))

(def state :stopped)
(def system nil)

(defn init []
  (if (util/in? [:initialized :started :running] state)
    (log/error "System has aready been initialized.")
    (do
      (alter-var-root #'system
        (constantly (components/init #'app/app)))
      (alter-var-root #'state (fn [_] :initialized))))
  state)

(defn deinit []
  (if (util/in? [:started :running] state)
    (log/error "System is not stopped; please stop before deinitializing.")
    (do
      (alter-var-root #'system (fn [_] nil))
      (alter-var-root #'state (fn [_] :uninitialized))))
  state)

(defn start []
  (if (= system nil)
    (init))
  (if (util/in? [:started :running] state)
    (log/error "System has already been started.")
    (do
      (alter-var-root #'system component/start)
      (alter-var-root #'state (fn [_] :started))))
  state)

(defn stop []
  (if (= state :stopped)
    (log/error "System already stopped.")
    (do
      (alter-var-root #'system
        (fn [s] (when s (component/stop s))))
      (alter-var-root #'state (fn [_] :stopped))))
  state)

(defn run []
  (if (= state :running)
    (log/error "System is already running.")
    (do
      (if (not (util/in? [:initialized :started :running] state))
        (init))
      (if (not (= state :started))
        (start))
      (alter-var-root #'state (fn [_] :running))))
  state)

(defn -refresh
  ([]
    (repl/refresh))
  ([& args]
    (apply #'repl/refresh args)))

(defn refresh [& args]
  (if (util/in? [:started :running] state)
    (stop))
  (apply -refresh args))

(defn reset []
  (stop)
  (deinit)
  (util/get-config :force-reload)
  (refresh :after 'lcmap-rest.dev/run))