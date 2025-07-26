
(ns envman.dotenv
  (:refer-clojure :exclude [load])
  (:require [clojure.string :as str]))

(defprotocol IRenderable
 (-render [this ctx]))

(extend-protocol IRenderable
  String
  (-render [this _]
    this))

(defrecord Var [name]
  IRenderable
  (-render [_ ctx]
    (get ctx name "")))

(defn- parse-single-quoted [lines]
  (loop [lines lines ret []]
    (if (seq lines)
      (let [line (first lines)]
        (if-let [[_ pre post] (re-matches #"([^']*)'(.*)" line)]
          (if (re-matches #"\s*(?:#.*)?" post)
            [[(str/join \newline (conj ret pre))] (rest lines)]
            (throw (ex-info "unexpected characters found after quote" {})))
          (recur (rest lines) (conj ret line))))
      (throw (ex-info "unexpected end of string" {})))))

(defn- parse-fragment [line]
  (loop [s line, ret []]
    (if-let [[_ pre name post] (re-matches #"([^$]*)\$\{([a-zA-Z_][a-zA-Z0-9_]*)\}(.*)" s)]
      (recur post (conj ret pre (->Var name)))
      (conj ret s))))

(defn- parse-double-quoted [lines]
  (loop [lines lines, ret []]
    (if (seq lines)
      (let [line (first lines)]
        (if-let [[_ pre c post] (re-matches #"([^\"\\]*)([\"\\])(.*)" line)]
          (let [pre' (parse-fragment pre)]
            (if (= c "\"")
              (if (re-matches #"\s*(?:#.*)?" post)
                [(into ret pre') (rest lines)]
                (throw (ex-info "unexpected characters found after quote" {})))
              (if-let [[_ escaped code more] (re-matches #"(?:([nt'\"\\])|u([0-9a-f]{4,}))(.*)" post)]
                (recur (cons more (rest lines))
                       (-> ret
                           (into pre')
                           (conj (if escaped
                                   (case escaped "n" \newline "t" \tab escaped)
                                   (char (Long/parseLong code 16))))))
                (throw (ex-info "unexpected escaped sequence found" {})))))
          (recur (rest lines)
                 (-> ret
                     (into (parse-fragment line))
                     (conj \newline)))))
      (throw (ex-info "unexpected end of string" {})))))

(defn- parse-lines [lines {:keys [allow-export?] :or {allow-export? true}}]
  (let [[line & lines] lines
        [_ var val] (re-matches (if allow-export?
                                  #"(?:\s*export\s)?\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*(.*)"
                                  #"\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*(.*)")
                                line)]
    (if (or (nil? var) (nil? val))
      (throw (ex-info "malformed line" {}))
      (let [[parsed-val lines'] (case (get val 0)
                                  \' (parse-single-quoted (cons (subs val 1) lines))
                                  \" (parse-double-quoted (cons (subs val 1) lines))
                                  [(parse-fragment (str/replace val #"\s*(?:#.*)?$" ""))
                                   lines])]
        [var parsed-val lines']))))

(defn parse1 [s opts]
  (let [lines (str/split-lines s)
        [var parsed-val _] (parse-lines lines opts)]
    [var parsed-val]))

(defn parse
  ([s] (parse s {}))
  ([s opts]
   (let [lines (str/split-lines s)]
     (loop [lines lines, ret []]
       (if (seq lines)
         (if (re-matches #"\s*(?:#.*)?" (first lines))
           (recur (rest lines) ret)
           (let [[var parsed-val lines] (parse-lines lines opts)]
             (recur lines (conj ret [var parsed-val]))))
         ret)))))

(defn render-val [ctx parsed-val]
  (str/join (map #(-render % ctx) parsed-val)))

(defn expand-vars [ctx parsed]
  (loop [[[var parsed-val] & more] parsed, ctx ctx, ret []]
    (if var
      (let [val (render-val ctx parsed-val)]
        (recur more (assoc ctx var val) (conj ret [var val])))
      ret)))

(defn load
  ([s] (load s {}))
  ([s {:keys [vars] :as opts}]
   (expand-vars vars (parse s opts))))
