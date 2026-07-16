import os
from mobile.storage.json_storage import load_tags, load_workflows
from mobile.registries.tag_registry import TagRegistry
from mobile.registries.workflow_registry import WorkFlowRegistry
from mobile.registries.action_registry import ActionRegistry
from mobile.dispatcher import Dispatcher
from mobile.exceptions import UnknownTagError, WorkflowNotFoundError

def execute(tag_id: str):
    try:
        # Resolve paths for config files relative to this bridge script
        base_dir = os.path.dirname(__file__)
        tags_path = os.path.join(base_dir, "mobile", "config", "tags.json")
        workflows_path = os.path.join(base_dir, "mobile", "config", "workflows.json")

        # Load configurations
        tags_dict = load_tags(tags_path)
        workflows_dict = load_workflows(workflows_path)

        # Initialize registries
        tag_registry = TagRegistry(tags_dict)
        workflow_registry = WorkFlowRegistry(workflows_dict)
        action_registry = ActionRegistry()

        # Initialize Dispatcher
        dispatcher = Dispatcher(
            tag_registry,
            workflow_registry,
            action_registry
        )

        # Execute
        errors = dispatcher.execute_uid(tag_id)

        if not errors:
            return f"Success: Tag {tag_id} processed."
        else:
            return f"Warning: Tag {tag_id} processed with errors: {', '.join(errors)}"

    except UnknownTagError as e:
        return f"Error: Tag ID {tag_id} is not registered."
    except WorkflowNotFoundError as e:
        return f"Error: Workflow for Tag ID {tag_id} was not found."
    except Exception as e:
        return f"Unexpected error: {str(e)}"
