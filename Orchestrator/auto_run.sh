#!/bin/bash

function make_recipe() {
    local app="licensectrl.jar"
    local num_slave=4
    local dstream_time=60
    local array=("$@")

    echo "{"
    echo '    "app": "'"$app"'",'
    echo '    "rules": {'
    echo '        "num_slave": '"$num_slave"','
    echo '        "dstream_time": '"$dstream_time"''
    echo '    },'
    echo '    "projects": {'

    last="${array[${#array[@]}-1]}" 
    for elemento in "${array[@]}"; do
        echo '        "'"$elemento"'": {'
        echo '            "language_type": ""'
        if [ "$elemento" == "$last" ]; then
            echo '        }'
        else 
            echo '        },'
        fi
    done
    echo '    }'
    echo '}'
}

CONT=0
PROJECT_IDS=()
SKIPPED_LINES=${2:-0}
file_csv="/home/ubuntu/Software-Heritage-Analytics/Orchestrator/script/top100recipes/Github-Ranking/csv_urls/$1" 
while IFS=',' read -r col1 col2 ID col4 col5 rest; do
    # Controllo se ha un ID di SWH associato
    if [ -n "$ID" ]; then     
        if [ "$CONT" -lt "$SKIPPED_LINES" ]; then
            CONT=$(($CONT + 1))
            continue
        fi
        PROJECT_IDS+=("$ID")
        CONT=$(($CONT + 1))
        #Controllo se modulo 10
        if [ $((CONT % 10)) -eq 0 ]; then
            make_recipe "${PROJECT_IDS[@]}" > tmp_recipe.json
             python3 -u dashboardclient.py -a "172.20.85.122" -p 4320 -r "tmp_recipe.json" -m node-1 -n licensectrl -D
            unset PROJECT_IDS
            PROJECT_IDS=()
        fi
    fi
done < "$file_csv"

# Creo ricetta con rimantenti 
n_elements="${#PROJECT_IDS[@]}"

if [ "$n_elements" -gt 0 ]; then
    make_recipe "${PROJECT_IDS[@]}" > tmp_recipe.json
    python3 -u dashboardclient.py -a "172.20.85.122" -p 4320 -r "tmp_recipe.json" -m node-1 -n licensectrl -D
fi

echo "SCRIPT TERMINATO"
