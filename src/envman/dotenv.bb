
(ns envman.dotenv
  (:require [clojure.string :as str]))

(defn- parse-single-quoted [lines]
  (loop [lines lines ret []]
    (if (seq lines)
      (let [line (first lines)]
        (if-let [[_ pre post] (re-matches #"([^']*)'(.*)" line)]
          (if (re-matches #"\s*(?:#.*)?" post)
            [(str/join \newline (conj ret pre)) (rest lines)]
            (throw (ex-info "unexpected characters found after quote" {})))
          (recur (rest lines) (conj ret line))))
      (throw (ex-info "unexpected end of string" {})))))

(defn expand-var [vars s]
  (str/replace s #"\$\{([a-zA-Z_][a-zA-Z0-9_]*)\}" (fn [[_ var]] (get vars var ""))))

(defn- expand-var-if-necessary [s {:keys [vars expand?] :or {expand? true}}]
  (cond->> s expand? (expand-var vars)))

(defn- parse-double-quoted [lines opts]
  (loop [lines lines, ret []]
    (if (seq lines)
      (let [line (first lines)]
        (if-let [[_ pre c post] (re-matches #"([^\"\\]*)([\"\\])(.*)" line)]
          (let [pre' (expand-var-if-necessary pre opts)]
            (if (= c "\"")
              (if (re-matches #"\s*(?:#.*)?" post)
                [(str/join (conj ret pre'))
                 (rest lines)]
                (throw (ex-info "unexpected characters found after quote" {})))
              (if-let [[_ escaped code more] (re-matches #"(?:([nt'\"\\])|u([0-9a-f]{4,}))(.*)" post)]
                (recur (cons more (rest lines))
                       (conj ret pre'
                             (if escaped
                               (case escaped "n" \newline "t" \tab escaped)
                               (char (Long/parseLong code 16)))))
                (throw (ex-info "unexpected escaped sequence found" {})))))
          (recur (rest lines)
                 (conj ret (expand-var-if-necessary line opts) \newline))))
      (throw (ex-info "unexpected end of string" {})))))

(defn- parse-lines [lines {:keys [allow-export?] :as opts}]
  (let [[line & lines] lines
        [_ var val] (re-matches (if allow-export?
                                  #"(?:\s*export\s)?\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*(.*)"
                                  #"\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*(.*)")
                                line)]
    (if (or (nil? var) (nil? val))
      (throw (ex-info "malformed line" {}))
      (let [[val' lines'] (case (get val 0)
                            \' (parse-single-quoted (cons (subs val 1) lines))
                            \" (parse-double-quoted (cons (subs val 1) lines) opts)
                            [(-> (str/replace val #"\s*(?:#.*)?$" "")
                                 (expand-var-if-necessary opts))
                             lines])]
        [var val' lines']))))

(defn parse1
  ([s] (parse1 s {}))
  ([s {:keys [vars] :as opts :or {vars (into {} (System/getenv))}}]
   (let [lines (str/split-lines s)
         [var val _] (parse-lines lines (assoc opts :vars vars))]
     [var val])))

(defn parse
  ([s] (parse s {}))
  ([s {:keys [vars] :as opts :or {vars (into {} (System/getenv))}}]
   (let [lines (str/split-lines s)]
     (loop [lines lines, ret [], vars vars]
       (if (seq lines)
         (if (re-matches #"\s*(?:#.*)?" (first lines))
           (recur (rest lines) ret vars)
           (let [[var val lines] (parse-lines lines (assoc opts :vars vars))]
             (recur lines (conj ret [var val]) (assoc vars var val))))
         ret)))))
