# import-items

A script for importing items into Hauth.

## Recommended setup

Tested with Python 3.6.9.

```bash
#!/bin/bash

# Create a virtual environment to isolate the system Python installation from the script dependencies that we'll download
python3 -m venv venv_import_items

# Activate the virtual environment; deactivate it later by running `deactivate`
. venv_import_items/bin/activate

# Install the script dependencies to the virtual environment
pip install -r requirements.txt
```

## Example usage

Assuming that all CSV files in the current working directory contain open access items, and a Hauth instance is running at http://example.com:

```bash
./import-items.py --api-key=0123456789ABCDEF --access-mode=OPEN http://example.com *.csv
```

For complete usage info:

```bash
./import-items.py --help
```
