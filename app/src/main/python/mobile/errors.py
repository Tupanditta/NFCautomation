#Este módulo contiene la función encargada de analizar la lista de errores y mostrar un resumen visual

def analize_errors(errors_list):
  if errors_list == "UID_NOT_FOUND":
    print("ERROR: uid not found")
  elif errors_list == "ACTIONS_NOT_FOUND":
    print("ERROR: actions not found")
  elif len(errors_list) == 0:
    print("ERROR NOT FOUND")
  else:
    print(errors_list)