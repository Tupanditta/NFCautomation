#Este módulo es quien lee los archivos json y los convierte a diccionarios de python

import json

def load_tags(config_path):
  with open(config_path, "r", encoding="utf-8") as f: #abro el archivo json
    tags = json.load(f) #convertir el archivo json en un diccionario de Python

  return tags #devuelvo el diccionario de tags

def load_workflows(config_path):
  with open(config_path, "r", encoding="utf-8") as f: #abro el archivo json
    workflows = json.load(f) #convertir el archivo json en un diccionario de Python

  return workflows #devuelvo el diccionario de tags