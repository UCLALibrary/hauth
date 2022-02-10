#!/usr/bin/env python

import click
from csv import DictReader
from enum import Enum
from io import TextIOBase
from json import dumps
from os.path import basename
from requests import post

class AccessMode(Enum):
    """The access mode choices."""
    OPEN = 0
    TIERED = 1
    ALL_OR_NOTHING = 2

@click.command()
@click.option('--access-mode', '-m', required=True,
              type=click.Choice([name for name, member in AccessMode.__members__.items()], case_sensitive=False))
@click.argument('hauth-base-url', nargs=1)
@click.argument('input-csv', nargs=-1, type=click.File('r'))
def import_items(access_mode, hauth_base_url, input_csv):
    """
    Import items into Hauth.

    Instructs the Hauth instance at HAUTH_BASE_URL to use the specified access mode for all items in each INPUT_CSV.
    Exits with zero status only if all items are imported successfully.
    """
    request_url = hauth_base_url + '/items'
    files = input_csv
    exit_code = 0

    for file in files:
        base_name = basename(file.name)

        try:
            post(request_url, data=request_payload(file, AccessMode[access_mode])).raise_for_status()
            click.secho('File {} imported successfully'.format(boldface(base_name)), fg='green')
        except Exception as e:
            click.secho('Error while importing file {}: {}'.format(boldface(base_name), repr(e)), fg='red')
            exit_code = 1

    exit(exit_code)

def request_payload(file: TextIOBase, access_mode: AccessMode) -> str:
    """Constructs a JSON array to send to the Hauth API endpoint for importing items."""
    return dumps(
        [
            {
                'uid': uid,
                'accessMode': access_mode.value
            }
            for uid in [row['Item ARK'] for row in DictReader(file)]
        ]
    )

def boldface(text: str) -> str:
    """Uses ANSI escape codes to enable printing the given text in boldface."""
    return '\033[1m' + text + '\033[22m'

if __name__ == '__main__':
    import_items()
