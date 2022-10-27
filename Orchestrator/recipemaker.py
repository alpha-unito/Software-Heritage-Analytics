import argparse
import numpy as np
import random
import json
import os

def readFile(filename):
    file1 = open(filename,'r')
    Lines = file1.readlines()
    return Lines

def extractKeys(entries, keys_pos):        
    keys = []
    for pos in keys_pos:
        keys.append(entries[pos].split(",")[0][2:])
    return keys

def overlapKeys(entries, recipes, overlap_type, overlap):
    keys = np.array_split(entries, recipes)
    if(overlap_type == 0):
        #copy front
        for recipe in range(1,recipes):
            keys[recipe][:overlap] = keys[recipe-1][:overlap] 
    elif(overlap_type == 1):
        #copy back
        for recipe in range(1,recipes):
            keys[recipe][-overlap:] = keys[recipe-1][-overlap:] 
    else:
        #copy random
        keys_num = int(len(entries)/recipes)
        for recipe in range(1,recipes):
            for pos in random.sample(range(0, keys_num), overlap):
                keys[recipe][pos] = keys[recipe-1][pos] 
    return np.hstack(keys)

def makeRecipe(keys):
    template = {"app" :"simple-project_2.12-1.0.jar",
                "rules" : {"num_slave":2 , "dstream_time": 1000},
                "projects":{
                            }
                }
    for key in keys:
        template["projects"][key]= {'language_type':''}
    return template;
    


if __name__=="__main__":

    parser = argparse.ArgumentParser()
    parser.add_argument("--source", help="Source file contenente le chiavi", default="keylist.csv")
    parser.add_argument("--output", help="Directory dell'output", default="ricette")    
    parser.add_argument("--recipes", help="Numero di ricette da creare", type=int, default=1)
    parser.add_argument("--keys", help="Numero di chiavi per ricetta", type=int, default=10)
    parser.add_argument("--overlap", help="Percentuale di sovrapposizione in caso di creazione di piu' ricette", type=float, default=0)
    parser.add_argument("--overlap_type", help="Tipo di sovrapposizione 0 = testa, 1 = coda, 2 = random", type=int, default=0)
    parser.add_argument("--random", help="Estrarre le chiavi randomicamente (true) altrimenti le chiavi saranno prese in maniera sequenziale", type=bool, default=False)
    args = parser.parse_args() 
    recipes_number = args.recipes
    keys_recipe = args.keys
    keys_overlap = round((keys_recipe*args.overlap)/100)
    overlap_type = args.overlap_type
    dir = './' + args.output

    exist = os.path.exists(dir)
    if not exist:
        os.mkdir(dir)

    with open(args.source) as f:
        content = f.readlines()[1:]

    tot_keys = keys_recipe * recipes_number
    if(args.random):
        keys = extractKeys(content, random.sample(range(0, len(content)-1), tot_keys))
    else:
        keys = extractKeys(content, range(tot_keys))

    if(keys_overlap):
        keys = overlapKeys(keys, recipes_number, overlap_type, keys_overlap)

    for number in range(recipes_number):
        recipe = json.dumps(makeRecipe(keys[number*keys_recipe:(number+1)*keys_recipe]), indent = 4)       
        f = open(f'{dir}/ricetta{number}.json', "w")
        f.write(recipe)
        f.close()
