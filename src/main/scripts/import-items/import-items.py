#!/usr/bin/env python

import click
from csv import DictReader
from enum import Enum
from io import TextIOBase
from json import dumps
from os.path import basename
from requests import post
from typing import List, Dict

class AccessMode(Enum):

    """The access mode choices."""
    OPEN = 0
    TIERED = 1
    ALL_OR_NOTHING = 2

@click.command()
@click.option('--access-mode', '-m', help='The access mode to use for all items in each INPUT_CSV.', required=False,
              type=click.Choice([name for name, member in AccessMode.__members__.items()], case_sensitive=False))
@click.option('--api-key', '-k', help='The API key for accessing the admin API.', required=True)
@click.argument('hauth-base-url', nargs=1)
@click.argument('input-csv', required=True, nargs=-1, type=click.File('r'))
def import_items(access_mode, api_key, hauth_base_url, input_csv):
    """Import items into Hauth.

    Instructs the Hauth instance at HAUTH_BASE_URL to use the specified access mode for all items in each INPUT_CSV.
    Exits with zero status only if all items are imported successfully.
    """
    request_url = hauth_base_url + '/items'
    request_headers = {'Content-Type': 'application/json', 'X-API-KEY': api_key}
    files = input_csv
    exit_code = 0
    total_item_count = 0

    for file in files:
        base_name = basename(file.name)

        try:
            request_data = request_payload(file, AccessMode[access_mode] if access_mode else None)
            item_count = len(request_data)

            post(request_url, data=dumps(request_data), headers=request_headers).raise_for_status()
            click.secho('File {} imported successfully ({} items)'.format(boldface(base_name), item_count), fg='green')
            total_item_count += item_count
        except Exception as e:
            click.secho('Error while importing file {}: {}'.format(boldface(base_name), repr(e)), fg='red')
            exit_code = 1

    if len(files) > 1:
        click.echo('Total items: {}'.format(total_item_count))

    exit(exit_code)

def request_payload(file: TextIOBase, access_mode_override: AccessMode) -> List[Dict]:
    """Constructs a JSON array to send to the Hauth API endpoint for importing items."""
    return [
        {
            'uid': row['Item ARK'],
            'accessMode': access_mode_override.value if access_mode_override else get_access_mode(row).value
        }
        for row in DictReader(file)
    ]

def get_access_mode(row: Dict) -> AccessMode:
    """Determines the access mode of an item."""
    field_name = 'Visibility'
    field_value = row[field_name]

    if field_value == 'open':
        return AccessMode.OPEN
    elif field_value == 'ucla':
        return AccessMode.TIERED
    elif field_value == 'private':
        # UCLA all-or-nothing access, which is not yet implemented; treat as UCLA tiered access for now
        return AccessMode.TIERED
    elif field_value == 'sinai':
        return AccessMode.ALL_OR_NOTHING
    else:
        raise ValueError('Unknown {} "{}" for row "{}"'.format(field_name, field_value, row))

def boldface(text: str) -> str:
    """Uses ANSI escape codes to enable printing the given text in boldface."""
    return '\033[1m' + text + '\033[22m'

if __name__ == '__main__':
    import_items() # pylint: disable=no-value-for-parameter
