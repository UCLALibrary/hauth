#!/bin/bash

shopt -s globstar

import-items.py --api-key "${1}" "${2}" "${3}"/**/*.csv
