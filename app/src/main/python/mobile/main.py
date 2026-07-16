from mobile.storage.json_storage import load_tags, load_workflows

from mobile.nfc.simulator import NFCSimulator

from mobile.registries.tag_registry import TagRegistry
from mobile.registries.workflow_registry import WorkFlowRegistry
from mobile.registries.action_registry import ActionRegistry

from mobile.dispatcher import Dispatcher

from mobile.errors import analize_errors

from mobile.validators import tag_validator, workflow_validator

from mobile.exceptions import UnknownTagError, WorkflowNotFoundError

import logging

#Leer el NFC
reader = NFCSimulator() #creo la clase reader
uid = reader.read_uid() #llamo a la función de leer uid y la guardo en su variable

#Logging
logging.basicConfig(
  level=logging.INFO,
  format=(
    "%(asctime)s "
    "%(levelname)s "
    "%(name)s "
    "%(message)s"
  )
)

# Configuración (descargar jsons y convertirlos en diccionarios)
tags_dict = load_tags("code/mobile/config/tags.json")
workflows_dict = load_workflows("code/mobile/config/workflows.json")

#Validaciones de los diccionarios descargados de los json
tag_validator.validate_tags_dict(tags_dict)
workflow_validator.validate_workflows_dict(workflows_dict)

# Registries (creo las clases)
tag_registry = TagRegistry(tags_dict)
workflow_registry = WorkFlowRegistry(workflows_dict)
action_registry = ActionRegistry()

# Dispatcher (crear la clase)
dispatcher = Dispatcher(
  tag_registry,
  workflow_registry,
  action_registry
)

# Ejecución (con sus respectivas excepciones)
try: 
  errors_list = dispatcher.execute_uid(uid)
  analize_errors(errors_list)
except UnknownTagError as e:
  print(e)
except WorkflowNotFoundError as e:
  print(e)

