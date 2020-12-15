(ns cpe.util.excel
	(:require [ht.util.interop :as i]))

(defn export-excel [calculated raw plant-name ]
	(let [sheet (js/ExcelPlus.)
				calculated-count (count calculated)
				raw-count (count raw)]
		(i/ocall sheet :createFile "Raw Data")
		(loop [x 0]
			(when (< x raw-count)
				(i/ocall sheet :writeRow (+ x 1) (clj->js (into
																										(nth raw x))))
				(recur (+ x 1))))
		(i/ocall sheet :createSheet "Calculated Data")
		(loop [x 0]
			(when (< x calculated-count)
				(i/ocall sheet :writeRow (+ x 1) (clj->js (into
																										(nth calculated x))))
				(recur (+ x 1))))
		(i/ocall sheet :saveAs (str plant-name ".xlsx"))))









