; postgres
;{:dev {:env     {:visualreview-port            "8080"
;                 :visualreview-db-uri          "//localhost/visualreview"
;                 :visualreview-db-user         "Kmandr"
;                 :visualreview-db-password     "vrdev"
;                 :visualreview-screenshots-dir "screenshots"}
;       :plugins [[lein-environ "1.0.0"]]}}

; h2
{:dev {:env     {:visualreview-port "7000"  
                 :visualreview-db-uri "file:./.visualreview/visualreview.db"  
                 :visualreview-db-user ""  
                 :visualreview-db-password ""  
                 :visualreview-screenshots-dir ".visualreview/screenshots"}  
       :plugins [[lein-environ "1.0.0"]]}}